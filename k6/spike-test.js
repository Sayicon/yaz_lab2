/**
 * Spike Test — Sistemin kırılma noktasını bul
 * Sleep yok, maksimum throughput, 5000 VU'ya kadar
 */

import http from 'k6/http';
import { check } from 'k6';
import { Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const errorRate = new Rate('errors');

export const options = {
  scenarios: {
    spike_3000: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 3000 },
        { duration: '20s', target: 3000 },
        { duration: '5s',  target: 0   },
      ],
      startTime: '0s',
      gracefulRampDown: '5s',
    },
    spike_5000: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 5000 },
        { duration: '20s', target: 5000 },
        { duration: '5s',  target: 0   },
      ],
      startTime: '45s',
      gracefulRampDown: '5s',
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.50'],
    errors:          ['rate<0.50'],
  },
};

export function setup() {
  http.post(`${BASE_URL}/auth/register`,
    JSON.stringify({ username: 'spikeuser', password: 'spike123', email: 'spike@test.com' }),
    { headers: { 'Content-Type': 'application/json' } }
  );
  const res = http.post(`${BASE_URL}/auth/login`,
    JSON.stringify({ username: 'spikeuser', password: 'spike123' }),
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

  const res = http.post(`${BASE_URL}/users`,
    JSON.stringify({
      username: `vu${__VU}_${__ITER}_${Date.now()}`,
      email: `vu${__VU}${__ITER}${Date.now()}@test.com`,
      fullName: 'Spike Test',
    }),
    params
  );
  errorRate.add(!check(res, { '201': r => r.status === 201 }));
}
