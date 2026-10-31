import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL;   // http://<minikube ip>:31803
const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const options = {
  scenarios: {
    smoke: { executor: 'constant-vus', vus: 1, duration: '30s', exec: 'parcours', tags: { phase: 'smoke' } },
    load: {
      executor: 'ramping-vus', startTime: '30s', startVUs: 0,
      stages: [{ duration: '1m', target: 35 }, { duration: '3m', target: 35 }, { duration: '30s', target: 0 }],
      gracefulRampDown: '10s', exec: 'parcours', tags: { phase: 'load' },
    },
    spike: {
      executor: 'ramping-vus', startTime: '5m10s', startVUs: 0,
      stages: [{ duration: '10s', target: 50 }, { duration: '1m', target: 50 }, { duration: '10s', target: 0 }],
      gracefulRampDown: '10s', exec: 'parcours', tags: { phase: 'spike' },
    },
  },
  thresholds: {
    'checks{phase:smoke}': [{ threshold: 'rate==1', abortOnFail: true, delayAbortEval: '5s' }],
    'http_req_failed{phase:load}': ['rate<0.01'],
    'http_req_duration{phase:load}': ['p(95)<400'],
    'http_req_failed{phase:spike}': ['rate<0.05'],
    'http_req_duration{phase:spike}': ['p(95)<1200'],
  },
};

export function parcours() {
  if (Math.random() < 0.8) {
    const r = http.get(`${BASE_URL}/api/products`, { tags: { name: 'GET /api/products' } });
    check(r, { 'GET 200': (x) => x.status === 200 });
  } else {
    const body = JSON.stringify({
      name: `Gateway ${__VU}-${__ITER}`,
      description: 'k6 via gateway',
      price: (Math.random() * 50 + 5).toFixed(2),
    });
    const r = http.post(`${BASE_URL}/api/products`, body, { headers: JSON_HEADERS, tags: { name: 'POST /api/products' } });
    check(r, { 'POST 2xx': (x) => x.status >= 200 && x.status < 300 });
  }
  sleep(Math.random() + 0.5);
}
