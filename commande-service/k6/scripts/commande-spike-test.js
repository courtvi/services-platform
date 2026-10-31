// Test de pic commande-service : même parcours que commande-load-test.js
// (création → consultation → détail → modification → annulation), avec une
// série de pics brutaux de plus en plus hauts, chacun suivi d'un retour à la
// charge de base. Objectif : voir comment le service encaisse chaque pic et
// s'il revient à la normale ensuite (phases "recuperation_<VUs>").
//
// Usage :
//   k6 run -e KC_USERNAME=alice -e KC_PASSWORD=secret k6/scripts/commande-spike-test.js
//   k6 run -e KC_USERS=alice:pwd1,bob:pwd2 -e SPIKE_VUS=500 k6/scripts/commande-spike-test.js
//
// Cible et authentification : mêmes variables que commande-load-test.js
// (BASE_URL, KEYCLOAK_URL, KC_REALM, KC_CLIENT_ID, KC_USERS ou KC_USERNAME/KC_PASSWORD).
//
// Profil de charge (durées en secondes) :
//   BASE_VUS        VUs avant, entre et après les pics         (défaut 5)
//   SPIKE_VUS       VUs au sommet de chaque pic, séparés par des virgules (défaut 1000,2000)
//   BASELINE_S      durée de la charge de base avant le 1er pic (défaut 60)
//   SPIKE_RAMP_S    durée de la montée et de la descente       (défaut 10)
//   SPIKE_HOLD_S    durée du sommet de chaque pic              (défaut 60)
//   RECOVERY_S      durée de la charge de base après chaque pic (défaut 120)
//
// Seuils :
//   SPIKE_P95_MS         p95 toléré pendant un pic              (défaut 8000)
//   SPIKE_MAX_ERROR_RATE taux d'erreur toléré pendant un pic    (défaut 0.10)
//   P95_MS               p95 exigé en récupération             (défaut 2000)
//   MAX_ERROR_RATE       taux d'erreur exigé en récupération   (défaut 0.01)

import exec from 'k6/execution';
import parcours from './commande-load-test.js';

// Vérification du service et des comptes Keycloak : identique au test de charge
export { setup } from './commande-load-test.js';

const BASE_VUS = parseInt(__ENV.BASE_VUS || '5', 10);
const SPIKES = (__ENV.SPIKE_VUS || '1000,2000').split(',').map((v) => parseInt(v.trim(), 10));
const BASELINE_S = parseInt(__ENV.BASELINE_S || '60', 10);
const SPIKE_RAMP_S = parseInt(__ENV.SPIKE_RAMP_S || '10', 10);
const SPIKE_HOLD_S = parseInt(__ENV.SPIKE_HOLD_S || '60', 10);
const RECOVERY_S = parseInt(__ENV.RECOVERY_S || '120', 10);

const SPIKE_P95_MS = __ENV.SPIKE_P95_MS || '8000';
const SPIKE_MAX_ERROR_RATE = __ENV.SPIKE_MAX_ERROR_RATE || '0.10';
const P95_MS = __ENV.P95_MS || '2000';
const MAX_ERROR_RATE = __ENV.MAX_ERROR_RATE || '0.01';

// Construit les stages et la chronologie des phases (fin de phase en secondes
// depuis le début du scénario) : base, puis pour chaque pic pic_<VUs> et recuperation_<VUs>.
const stages = [{ duration: `${BASELINE_S}s`, target: BASE_VUS }];
const phases = [{ name: 'base', endS: BASELINE_S }];
let t = BASELINE_S;
SPIKES.forEach((vus) => {
  stages.push(
    { duration: `${SPIKE_RAMP_S}s`, target: vus },
    { duration: `${SPIKE_HOLD_S}s`, target: vus },
    { duration: `${SPIKE_RAMP_S}s`, target: BASE_VUS },
    { duration: `${RECOVERY_S}s`, target: BASE_VUS },
  );
  t += SPIKE_RAMP_S + SPIKE_HOLD_S + SPIKE_RAMP_S;
  phases.push({ name: `pic_${vus}`, endS: t });
  t += RECOVERY_S;
  phases.push({ name: `recuperation_${vus}`, endS: t });
});

const thresholds = {
  // Référence avant les pics, pour comparer avec les récupérations
  'http_req_duration{api:commande,phase:base}': [`p(95)<${P95_MS}`],
  // Arrêt si le service s'effondre complètement
  'http_req_failed{api:commande}': [{ threshold: 'rate<0.5', abortOnFail: true, delayAbortEval: '30s' }],
};
SPIKES.forEach((vus) => {
  // Pendant le pic : dégradation tolérée, mais le service doit continuer à répondre
  thresholds[`http_req_duration{api:commande,phase:pic_${vus}}`] = [`p(95)<${SPIKE_P95_MS}`];
  thresholds[`http_req_failed{api:commande,phase:pic_${vus}}`] = [`rate<${SPIKE_MAX_ERROR_RATE}`];
  // Après le pic : le service doit revenir aux performances nominales
  thresholds[`http_req_duration{api:commande,phase:recuperation_${vus}}`] = [`p(95)<${P95_MS}`];
  thresholds[`http_req_failed{api:commande,phase:recuperation_${vus}}`] = [`rate<${MAX_ERROR_RATE}`];
  thresholds[`checks{phase:recuperation_${vus}}`] = ['rate>0.99'];
});

export const options = {
  scenarios: {
    pic_commande: {
      executor: 'ramping-vus',
      startVUs: BASE_VUS,
      stages,
      gracefulRampDown: '30s',
    },
  },
  thresholds,
};

function phase() {
  const elapsedS = (Date.now() - exec.scenario.startTime) / 1000;
  const current = phases.find((p) => elapsedS < p.endS);
  return current ? current.name : phases[phases.length - 1].name;
}

export default function (data) {
  // Tag appliqué à toutes les métriques de l'itération (requêtes, checks, métriques custom).
  // Une itération est rattachée à la phase où elle a commencé.
  exec.vu.metrics.tags.phase = phase();
  parcours(data);
}
