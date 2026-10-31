// Smoke test commande-service : vérifie que le service est vivant et que le
// parcours principal d'une commande fonctionne (créer → lire → modifier → annuler).
//
// Usage :
//   k6 run -e KC_USERNAME=alice -e KC_PASSWORD=secret k6/scripts/commande-smoke-test.js
//
// Variables d'environnement :
//   BASE_URL      URL de commande-service              (défaut http://localhost:8082)
//   KEYCLOAK_URL  URL Keycloak (doit correspondre à l'issuer attendu par le service)
//                                                      (défaut http://localhost:30090)
//   KC_REALM      realm Keycloak                        (défaut lorrconnect)
//   KC_CLIENT_ID  client public avec direct access grant (défaut haller)
//   KC_USERNAME / KC_PASSWORD  utilisateur ayant le rôle CLIENT (obligatoires)
//   ACCESS_TOKEN  JWT déjà obtenu : court-circuite l'authentification Keycloak

import http from 'k6/http';
import { check, group, fail, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8082';
const KEYCLOAK_URL = __ENV.KEYCLOAK_URL || 'http://localhost:30090';
const KC_REALM = __ENV.KC_REALM || 'lorrconnect';
const KC_CLIENT_ID = __ENV.KC_CLIENT_ID || 'haller';

export const options = {
  scenarios: {
    smoke: {
      executor: 'constant-vus',
      vus: 1,
      duration: '30s',
    },
  },
  thresholds: {
    http_req_failed: ['rate==0'],
    http_req_duration: ['p(95)<500'],
    checks: ['rate==1'],
  },
};

export function setup() {
  const health = http.get(`${BASE_URL}/manage/health`);
  console.log(health.status, health.body);
  check(health, {
    'health: status 200': (r) => r.status === 200,
    'health: UP': (r) => r.json('status') === 'UP',
  }) || fail(`commande-service indisponible sur ${BASE_URL}`);

  if (__ENV.ACCESS_TOKEN) {
    return { token: __ENV.ACCESS_TOKEN };
  }
  if (!__ENV.KC_USERNAME || !__ENV.KC_PASSWORD) {
    fail('Définir KC_USERNAME/KC_PASSWORD ou ACCESS_TOKEN');
  }

  const res = http.post(
    `${KEYCLOAK_URL}/realms/${KC_REALM}/protocol/openid-connect/token`,
    {
      grant_type: 'password',
      client_id: KC_CLIENT_ID,
      username: __ENV.KC_USERNAME,
      password: __ENV.KC_PASSWORD,
    },
  );
  console.log(res.status, res.body);
  check(res, { 'keycloak: token obtenu': (r) => r.status === 200 && !!r.json('access_token') })
    || fail(`Authentification Keycloak échouée (${res.status})`);

  return { token: res.json('access_token') };
}

// Format attendu par @JsonFormat : yyyy-MM-dd'T'HH:mm:ss (sans millisecondes ni fuseau)
function dateLivraison() {
  return new Date(Date.now() + 24 * 3600 * 1000).toISOString().slice(0, 19);
}

export default function (data) {
  const params = {
    headers: {
      Authorization: `Bearer ${data.token}`,
      'Content-Type': 'application/json',
    },
  };
  const url = `${BASE_URL}/api/commandes`;
  const reference = `K6-SMOKE-${__VU}-${__ITER}-${Date.now()}`;
  let id;

  group('POST /api/commandes', () => {
    const res = http.post(url, JSON.stringify({
      reference,
      dateLivraison: dateLivraison(),
      lignes: [
        { article: 'Baguette', quantite: 2, prixUnitaire: 1.1, total: 2.2 },
        { article: 'Croissant', quantite: 1, prixUnitaire: 1.5, total: 1.5 },
      ],
    }), params);
    console.log(res.status, res.body);
    check(res, {
      'create: status 201': (r) => r.status === 201,
      'create: id présent': (r) => !!r.json('id'),
      'create: statut CREEE': (r) => r.json('statut') === 'CREEE',
    });
    id = res.status === 201 ? res.json('id') : undefined;
  });

  if (!id) {
    return;
  }

  group('GET /api/commandes', () => {
    const res = http.get(url, params);
    console.log(res.status, res.body);
    check(res, {
      'list: status 200': (r) => r.status === 200,
      'list: contient la commande créée': (r) => r.json().some((c) => c.id === id),
    });
  });

  group('GET /api/commandes/{id}', () => {
    const res = http.get(`${url}/${id}`, params);
    console.log(res.status, res.body);
    check(res, {
      'get: status 200': (r) => r.status === 200,
      'get: bonne référence': (r) => r.json('reference') === reference,
    });
  });

  group('GET /api/commandes/{id}/detail', () => {
    const res = http.get(`${url}/${id}/detail`, params);
    console.log(res.status, res.body);
    check(res, {
      'detail: status 200': (r) => r.status === 200,
      'detail: 2 lignes': (r) => r.json('lignes.#') === 2,
    });
  });

  group('PUT /api/commandes/{id}', () => {
    const res = http.put(`${url}/${id}`, JSON.stringify({
      reference: `${reference}-MAJ`,
      dateLivraison: dateLivraison(),
    }), params);
    console.log(res.status, res.body);
    check(res, {
      'update: status 200': (r) => r.status === 200,
      'update: référence modifiée': (r) => r.json('reference') === `${reference}-MAJ`,
    });
  });

  // Annulation : sert aussi de nettoyage des données créées par le test
  group('DELETE /api/commandes/{id}', () => {
    const res = http.del(`${url}/${id}`, null, params);
    console.log(res.status, res.body);
    check(res, { 'annuler: status 204': (r) => r.status === 204 });

    const after = http.get(`${url}/${id}`, params);
    console.log(after.status, after.body);
    check(after, { 'annuler: statut ANNULEE': (r) => r.json('statut') === 'ANNULEE' });
  });

  sleep(1);
}
