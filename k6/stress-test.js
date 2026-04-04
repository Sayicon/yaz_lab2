/**
 * Stress Test — Sistemin kırılma noktasını bul
 * 500 → 1000 → 2000 VU kademeli artış
 *
 * Çalıştırma:
 *   k6 run k6/stress-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const errorRate = new Rate('errors');

export const options = {
  scenarios: {
    stress_500: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 500 },
        { duration: '20s', target: 500 },
        { duration: '5s',  target: 0   },
      ],
      startTime: '0s',
      gracefulRampDown: '5s',
    },
    stress_1000: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 1000 },
        { duration: '20s', target: 1000 },
        { duration: '5s',  target: 0    },
      ],
      startTime: '45s',
      gracefulRampDown: '5s',
    },
    stress_2000: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 2000 },
        { duration: '20s', target: 2000 },
        { duration: '5s',  target: 0    },
      ],
      startTime: '90s',
      gracefulRampDown: '5s',
    },
  },
  thresholds: {
    http_req_failed:   ['rate<0.20'],   // %20'ye kadar tolere et
    http_req_duration: ['p(95)<3000'],  // p95 < 3s
    errors:            ['rate<0.20'],
  },
};

export function setup() {
  http.post(`${BASE_URL}/auth/register`,
    JSON.stringify({ username: 'stressuser', password: 'stress123', email: 'stress@test.com' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  const res = http.post(`${BASE_URL}/auth/login`,
    JSON.stringify({ username: 'stressuser', password: 'stress123' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  return { token: res.json('token') };
}

export default function (data) {
  const params = {
    headers: {
      'Authorization': `Bearer ${data.token}`,
      'Content-Type': 'application/json',
    },
  };

  const createRes = http.post(`${BASE_URL}/users`,
    JSON.stringify({
      username: `vu${__VU}_${__ITER}_${Date.now()}`,
      email: `vu${__VU}${__ITER}${Date.now()}@test.com`,
      fullName: 'Stress Test User',
    }),
    params
  );
  errorRate.add(!check(createRes, { 'create 201': r => r.status === 201 }));

  if (createRes.status === 201) {
    const getRes = http.get(`${BASE_URL}/users/${createRes.json('id')}`, params);
    errorRate.add(!check(getRes, { 'get 200': r => r.status === 200 }));
  }

  sleep(0.3);
}
