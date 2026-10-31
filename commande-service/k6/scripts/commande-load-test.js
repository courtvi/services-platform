// Test de charge commande-service : parcours complet d'une commande
// (création → consultation → détail → modification → annulation)
// avec une montée progressive du nombre de VUs.
//
// Usage :
//   k6 run -e KC_USERNAME=alice -e KC_PASSWORD=secret k6/scripts/commande-load-test.js
//   k6 run -e KC_USERS=alice:pwd1,bob:pwd2 -e TARGET_VUS=30 -e P95_MS=1500 k6/scripts/commande-load-test.js
//
// Cible et authentification :
//   BASE_URL       URL de commande-service                   (défaut http://localhost:8082)
//   KEYCLOAK_URL   URL Keycloak (issuer attendu par le service) (défaut http://localhost:30090)
//   KC_REALM       realm Keycloak                             (défaut lorrconnect)
//   KC_CLIENT_ID   client public avec direct access grant     (défaut lorrconnectvin)
//   KC_USERS       liste "user:password,user2:password2" (rôle CLIENT) — les VUs se
//                  répartissent les comptes
//   KC_USERNAME / KC_PASSWORD  compte unique si KC_USERS n'est pas défini
//
// Profil de charge :
//   TARGET_VUS     nombre de VUs au palier                    (défaut 50)
//   RAMP_UP        durée de chaque demi-montée (0 → 50 % → 100 %) (défaut 1m)
//   HOLD           durée du palier                            (défaut 5m)
//   RAMP_DOWN      durée de la descente                       (défaut 1m)
//
// Seuils (valeurs par défaut calibrées pour minikube local) :
//   P95_MS         p95 de toutes les requêtes commande-service  (défaut 2000)
//   P99_MS         p99 de toutes les requêtes commande-service  (défaut 4000)
//   CREATE_P95_MS  p95 de la création (inclut l'envoi SMTP)     (défaut 3000)
//   MAX_ERROR_RATE taux d'erreur HTTP maximal                   (défaut 0.01)

import http from 'k6/http';
import { check, group, fail, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:30090';
const KC_REALM = __ENV.KC_REALM || 'lorrconnect';
const KC_CLIENT_ID = __ENV.KC_CLIENT_ID || 'lorrconnectvin';

const TARGET_VUS = parseInt(__ENV.TARGET_VUS || '50', 10);
const P95_MS = __ENV.P95_MS || '2000';
const P99_MS = __ENV.P99_MS || '4000';
const CREATE_P95_MS = __ENV.CREATE_P95_MS || '3000';
const MAX_ERROR_RATE = __ENV.MAX_ERROR_RATE || '0.01';

const API = `${BASE_URL}/api/commandes`;

// Noms fixes pour les URLs avec id : évite une série de métriques par commande
const EP = {
  create: 'POST /api/commandes',
  get: 'GET /api/commandes/{id}',
  detail: 'GET /api/commandes/{id}/detail',
  update: 'PUT /api/commandes/{id}',
  cancel: 'DELETE /api/commandes/{id}',
};

const commandesCreees = new Counter('commandes_creees');
// Itérations abandonnées faute de token (Keycloak saturé ou indisponible)
const authEchecs = new Counter('auth_echecs');
// Durée totale du parcours, temps de réflexion inclus
const parcoursDuration = new Trend('parcours_duration', true);

export const options = {
  scenarios: {
    parcours_commande: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: __ENV.RAMP_UP || '1m', target: Math.max(1, Math.ceil(TARGET_VUS / 2)) },
        { duration: __ENV.RAMP_UP || '1m', target: TARGET_VUS },
        { duration: __ENV.HOLD || '5m', target: TARGET_VUS },
        { duration: __ENV.RAMP_DOWN || '1m', target: 0 },
      ],
      gracefulRampDown: '30s',
    },
  },
  thresholds: {
    // Seules les requêtes vers commande-service comptent (pas les appels Keycloak)
    'http_req_duration{api:commande}': [`p(95)<${P95_MS}`, `p(99)<${P99_MS}`],
    [`http_req_duration{name:${EP.create}}`]: [`p(95)<${CREATE_P95_MS}`],
    [`http_req_duration{name:${EP.get}}`]: [`p(95)<${P95_MS}`],
    [`http_req_duration{name:${EP.detail}}`]: [`p(95)<${P95_MS}`],
    [`http_req_duration{name:${EP.update}}`]: [`p(95)<${P95_MS}`],
    [`http_req_duration{name:${EP.cancel}}`]: [`p(95)<${P95_MS}`],
    'http_req_failed{api:commande}': [
      `rate<${MAX_ERROR_RATE}`,
      // Arrêt anticipé si le service s'effondre (> 10 % d'erreurs)
      { threshold: 'rate<0.1', abortOnFail: true, delayAbortEval: '1m' },
    ],
    checks: ['rate>0.99'],
  },
};

function parseUsers() {
  if (__ENV.KC_USERS) {
    return __ENV.KC_USERS.split(',').map((entry) => {
      const i = entry.indexOf(':');
      return { username: entry.slice(0, i), password: entry.slice(i + 1) };
    });
  }
  if (__ENV.KC_USERNAME && __ENV.KC_PASSWORD) {
    return [{ username: __ENV.KC_USERNAME, password: __ENV.KC_PASSWORD }];
  }
  return [];
}

const USERS = parseUsers();

export function setup() {
  const health = http.get(`${BASE_URL}/manage/health`, { tags: { api: 'manage' } });
  check(health, { 'health: UP': (r) => r.status === 200 && safeJson(r, 'status') === 'UP' })
    || fail(`commande-service indisponible sur ${BASE_URL}`);

  if (USERS.length === 0) {
    fail('Définir KC_USERS ou KC_USERNAME/KC_PASSWORD');
  }
  // Vérifie chaque compte une fois avant de lancer la charge
  USERS.forEach((u) => {
    if (!login(u)) {
      fail(`Authentification Keycloak échouée pour ${u.username}`);
    }
  });
}

// --- Authentification -------------------------------------------------------
// Les access tokens du realm expirent au bout de 5 min (accessTokenLifespan=300),
// soit moins que la durée du test : chaque VU garde son token et le renouvelle
// 30 s avant son expiration.

let session = null;

function login(user) {
  const res = http.post(
    `${KEYCLOAK_URL}/realms/${KC_REALM}/protocol/openid-connect/token`,
    { grant_type: 'password', client_id: KC_CLIENT_ID, username: user.username, password: user.password },
    { tags: { api: 'keycloak', name: 'keycloak token' } },
  );
  if (res.status !== 200) {
    console.error(`Keycloak ${res.status} pour ${user.username}: ${res.body}`);
    return null;
  }
  return {
    token: res.json('access_token'),
    expiresAt: Date.now() + (res.json('expires_in') - 30) * 1000,
  };
}

function ensureSession() {
  if (!session || Date.now() >= session.expiresAt) {
    session = login(USERS[(__VU - 1) % USERS.length]);
  }
  return session !== null;
}

function authParams(name) {
  ensureSession();
  return {
    headers: {
      Authorization: `Bearer ${session ? session.token : ''}`,
      'Content-Type': 'application/json',
    },
    tags: { api: 'commande', name },
  };
}

// --- Helpers ----------------------------------------------------------------

// r.json() lève une exception si le corps n'est pas du JSON (page d'erreur, timeout…).
// Ne pas transmettre un sélecteur undefined : k6 le traite comme un vrai sélecteur
// et renvoie undefined au lieu du corps complet.
function safeJson(res, selector) {
  try {
    return selector === undefined ? res.json() : res.json(selector);
  } catch (e) {
    return undefined;
  }
}

function logFailure(res, step) {
  if (res.status >= 400 || res.status === 0) {
    console.warn(`[${step}] VU ${__VU} → ${res.status} ${res.error || ''} ${String(res.body).slice(0, 200)}`);
  }
}

// Temps de réflexion entre deux actions utilisateur : 0,5 à 1,5 s
function think() {
  sleep(0.5 + Math.random());
}

// Format attendu par @JsonFormat : yyyy-MM-dd'T'HH:mm:ss (sans millisecondes ni fuseau)
function dateLivraison(jours) {
  return new Date(Date.now() + jours * 24 * 3600 * 1000).toISOString().slice(0, 19);
}

const ARTICLES = [
  { article: 'Baguette', prixUnitaire: 1.1 },
  { article: 'Croissant', prixUnitaire: 1.5 },
  { article: 'Brioche', prixUnitaire: 3.5 },
  { article: 'Eclair', prixUnitaire: 2.8 },
  { article: 'Macaron', prixUnitaire: 1.8 },
  { article: 'Pain complet', prixUnitaire: 2.0 },
];

// 1 à 4 lignes aléatoires, pour ne pas envoyer toujours la même commande
function lignesAleatoires() {
  const nb = 1 + Math.floor(Math.random() * 4);
  const shuffled = ARTICLES.slice().sort(() => Math.random() - 0.5);
  return shuffled.slice(0, nb).map((a) => {
    const quantite = 1 + Math.floor(Math.random() * 5);
    return { ...a, quantite, total: Math.round(a.prixUnitaire * quantite * 100) / 100 };
  });
}

// --- Parcours ---------------------------------------------------------------

export default function () {
  // Sans token, les requêtes renverraient 401 et seraient comptées comme des erreurs
  // de commande-service alors que c'est Keycloak qui a échoué : on saute l'itération.
  if (!ensureSession()) {
    authEchecs.add(1);
    think();
    return;
  }

  const start = Date.now();
  const reference = `K6-LOAD-${__VU}-${__ITER}-${Date.now()}`;
  const lignes = lignesAleatoires();
  let id;

  group('creation', () => {
    const res = http.post(API, JSON.stringify({
      reference,
      dateLivraison: dateLivraison(1 + Math.floor(Math.random() * 7)),
      lignes,
    }), authParams(EP.create));
    logFailure(res, 'creation');
    const ok = check(res, {
      'creation: status 201': (r) => r.status === 201,
      'creation: statut CREEE': (r) => safeJson(r, 'statut') === 'CREEE',
    });
    if (ok) {
      id = safeJson(res, 'id');
      commandesCreees.add(1);
    }
  });

  // Sans commande créée, le reste du parcours n'a pas de sens
  if (!id) {
    think();
    return;
  }
  think();

  group('consultation', () => {
    const res = http.get(`${API}/${id}`, authParams(EP.get));
    logFailure(res, 'consultation');
    check(res, {
      'consultation: status 200': (r) => r.status === 200,
      'consultation: bonne référence': (r) => safeJson(r, 'reference') === reference,
    });
  });
  think();

  group('detail', () => {
    const res = http.get(`${API}/${id}/detail`, authParams(EP.detail));
    logFailure(res, 'detail');
    check(res, {
      'detail: status 200': (r) => r.status === 200,
      'detail: nombre de lignes': (r) => safeJson(r, 'lignes.#') === lignes.length,
    });
  });
  think();

  group('modification', () => {
    const res = http.put(`${API}/${id}`, JSON.stringify({
      reference: `${reference}-MAJ`,
      dateLivraison: dateLivraison(3),
    }), authParams(EP.update));
    logFailure(res, 'modification');
    check(res, {
      'modification: status 200': (r) => r.status === 200,
      'modification: référence modifiée': (r) => safeJson(r, 'reference') === `${reference}-MAJ`,
    });
  });
  think();

  // Annulation : termine le parcours et évite de laisser des commandes CREEE en base
  group('annulation', () => {
    const res = http.del(`${API}/${id}`, null, authParams(EP.cancel));
    logFailure(res, 'annulation');
    check(res, { 'annulation: status 204': (r) => r.status === 204 });
  });

  parcoursDuration.add(Date.now() - start);
  think();
}
