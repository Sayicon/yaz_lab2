/**
 * FAZ 6 - A: Smoke Test (TDD)
 *
 * Amaç:
 *   - 5 VU, 10 saniye boyunca sistemi test eder
 *   - Hata oranı %0 olmalı
 *   - p95 yanıt süresi < 500ms olmalı
 *
 * Çalıştırma:
 *   k6 run k6/smoke-test.js
 */

import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 5,
  duration: '10s',
  thresholds: {
    http_req_failed:   ['rate<0.01'],    // hata oranı %1'den az (esasen %0)
    http_req_duration: ['p(95)<500'],    // p95 < 500ms
  },
};

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export function setup() {
  // Test kullanıcısı oluştur
  http.post(`${BASE_URL}/auth/register`,
    JSON.stringify({ username: 'smokeuser', password: 'smoke123', email: 'smoke@test.com' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  // Login yap, token al
  const loginRes = http.post(`${BASE_URL}/auth/login`,
    JSON.stringify({ username: 'smokeuser', password: 'smoke123' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  return { token: loginRes.json('token') };
}

export default function (data) {
  const params = {
    headers: {
      'Authorization': `Bearer ${data.token}`,
      'Content-Type': 'application/json',
    },
  };

  // Health check
  const healthRes = http.get(`${BASE_URL}/actuator/health`);
  check(healthRes, { 'health is UP': (r) => r.status === 200 });

  // Kullanıcı oluştur
  const createRes = http.post(`${BASE_URL}/users`,
    JSON.stringify({ username: `user_${__VU}_${__ITER}`, email: `u${__VU}${__ITER}@test.com`, fullName: 'Test User' }),
    params
  );
  check(createRes, { 'create user 201': (r) => r.status === 201 });

  // Oluşturulan kullanıcıyı oku
  if (createRes.status === 201) {
    const userId = createRes.json('id');
    const getRes = http.get(`${BASE_URL}/users/${userId}`, params);
    check(getRes, { 'get user 200': (r) => r.status === 200 });
  }

  sleep(0.5);
}
