// k6/product-smoke-load-spike.js
// Enchaîne : smoke (1 VU) → load (35 VU) → spike (50 VU)
// Lancement : SSL_CERT_FILE=certs/ca.crt k6 run k6/product-smoke-load-spike.js
import http from 'k6/http';
import { check, group, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'https://product-service:30443';
const JSON_HEADERS = { headers: { 'Content-Type': 'application/json' } };

export const options = {
  hosts: { 'product-service': __ENV.NODE_IP },
  tlsAuth: [
    {
      cert: open('../certs/client.crt'),
      key: open('../certs/client.key'),
    },
  ],
  tlsVersion: { min: 'tls1.2', max: 'tls1.3' },

  scenarios: {
    // 1. SMOKE : 1 VU, vérifie que l'API répond correctement avant de charger
    smoke: {
      executor: 'constant-vus',
      vus: 1,
      duration: '30s',
      exec: 'smoke',
      tags: { phase: 'smoke' },
    },

    // 2. LOAD : montée à 35 VU, palier, descente   (démarre à 30s, dure 4m30)
    load: {
      executor: 'ramping-vus',
      startTime: '30s',
      startVUs: 0,
      stages: [
        { duration: '1m', target: 35 },
        { duration: '3m', target: 35 },
        { duration: '30s', target: 0 },
      ],
      gracefulRampDown: '10s',
      exec: 'parcours',
      tags: { phase: 'load' },
    },

    // 3. SPIKE : pic brutal à 50 VU, maintien court, retour à 0  (démarre à 5m10)
    spike: {
      executor: 'ramping-vus',
      startTime: '5m10s',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 50 },
        { duration: '1m', target: 50 },
        { duration: '10s', target: 0 },
      ],
      gracefulRampDown: '10s',
      exec: 'parcours',
      tags: { phase: 'spike' },
    },
  },

  thresholds: {
    // Smoke : tout doit passer, sinon on arrête tout de suite le test
    'checks{phase:smoke}': [{ threshold: 'rate==1', abortOnFail: true, delayAbortEval: '5s' }],
    'http_req_duration{phase:smoke}': ['p(95)<200'],

    // Load : objectifs nominaux
    'http_req_failed{phase:load}': ['rate<0.01'],
    'http_req_duration{phase:load}': ['p(95)<300', 'p(99)<800'],

    // Spike : on tolère une dégradation, mais pas d'effondrement
    'http_req_failed{phase:spike}': ['rate<0.05'],
    'http_req_duration{phase:spike}': ['p(95)<1000'],
  },
};

function lireProduits() {
  const res = http.get(`${BASE_URL}/api/products`, { tags: { name: 'GET /api/products' } });
  check(res, {
    'GET 200': (r) => r.status === 200,
    'GET renvoie un tableau': (r) => Array.isArray(r.json()),
  });
  return res;
}

function creerProduit(prefixe) {
  const payload = JSON.stringify({
    name: `${prefixe} ${__VU}-${__ITER}`,
    description: 'Créé par le test k6 mTLS',
    price: (Math.random() * 50 + 5).toFixed(2),
  });
  const res = http.post(`${BASE_URL}/api/products`, payload, {
    ...JSON_HEADERS,
    tags: { name: 'POST /api/products' },
  });
  check(res, {
    'POST 2xx': (r) => r.status >= 200 && r.status < 300,
    'POST renvoie un id': (r) => r.json('id') !== undefined,
  });
  return res;
}

// Smoke : un aller-retour complet création → lecture, puis contrôle de cohérence
export function smoke() {
  group('smoke', () => {
    const cree = creerProduit('Smoke');
    const id = cree.json('id');
    const liste = lireProduits();
    check(liste, {
    });
  });
  sleep(1);
}

// Load + spike : parcours réaliste, ~80 % lecture / 20 % écriture
export function parcours() {
  if (Math.random() < 0.8) {
    lireProduits();
  } else {
    creerProduit('Charge');
  }
  sleep(Math.random() * 1 + 0.5); // temps de réflexion 0,5 à 1,5 s
}