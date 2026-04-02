/**
 * FAZ 6 - B: Yük Testi
 *
 * Dört senaryo sırayla çalışır: 50 / 100 / 200 / 500 eş zamanlı kullanıcı
 * Her senaryo: 15s ramp-up → 30s yük → 10s ramp-down
 *
 * Çalıştırma:
 *   k6 run k6/load-test.js --out json=k6/results/load-test.json
 *
 * Ölçülen metrikler:
 *   - Ortalama yanıt süresi (http_req_duration avg)
 *   - p95, p99 yanıt süresi
 *   - Hata oranı (http_req_failed rate)
 *   - RPS (http_reqs rate)
 */

import http from 'k6/http';
import { check, sleep } from 'k6';
import { Trend, Rate } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// Özel metrikler
const createUserDuration = new Trend('create_user_duration');
const getUserDuration    = new Trend('get_user_duration');
const errorRate          = new Rate('errors');

export const options = {
  scenarios: {
    load_50: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 50  },
        { duration: '30s', target: 50  },
        { duration: '10s', target: 0   },
      ],
      startTime: '0s',
      gracefulRampDown: '5s',
      tags: { scenario: 'load_50' },
    },
    load_100: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 100 },
        { duration: '30s', target: 100 },
        { duration: '10s', target: 0   },
      ],
      startTime: '65s',
      gracefulRampDown: '5s',
      tags: { scenario: 'load_100' },
    },
    load_200: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 200 },
        { duration: '30s', target: 200 },
        { duration: '10s', target: 0   },
      ],
      startTime: '130s',
      gracefulRampDown: '5s',
      tags: { scenario: 'load_200' },
    },
    load_500: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '15s', target: 500 },
        { duration: '30s', target: 500 },
        { duration: '10s', target: 0   },
      ],
      startTime: '195s',
      gracefulRampDown: '5s',
      tags: { scenario: 'load_500' },
    },
  },
  thresholds: {
    http_req_failed:   ['rate<0.05'],    // hata oranı < %5
    http_req_duration: ['p(95)<1000'],   // p95 < 1s (yük altında)
    errors:            ['rate<0.05'],
  },
};

export function setup() {
  // Test kullanıcısı oluştur ve token al
  http.post(`${BASE_URL}/auth/register`,
    JSON.stringify({ username: 'loadtestuser', password: 'load123', email: 'load@test.com' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const loginRes = http.post(`${BASE_URL}/auth/login`,
    JSON.stringify({ username: 'loadtestuser', password: 'load123' }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  const token = loginRes.json('token');
  if (!token) {
    console.error('Login başarısız, token alınamadı');
  }
  return { token };
}

export default function (data) {
  const params = {
    headers: {
      'Authorization': `Bearer ${data.token}`,
      'Content-Type': 'application/json',
    },
  };

  // 1. Kullanıcı oluştur
  const createStart = Date.now();
  const createRes = http.post(`${BASE_URL}/users`,
    JSON.stringify({
      username: `vu${__VU}_iter${__ITER}_${Date.now()}`,
      email: `vu${__VU}i${__ITER}${Date.now()}@test.com`,
      fullName: 'Load Test User',
    }),
    params
  );
  createUserDuration.add(Date.now() - createStart);

  const createOk = check(createRes, { 'create user 201': (r) => r.status === 201 });
  errorRate.add(!createOk);

  // 2. Oluşturulan kullanıcıyı oku
  if (createRes.status === 201) {
    const userId = createRes.json('id');
    const getStart = Date.now();
    const getRes = http.get(`${BASE_URL}/users/${userId}`, params);
    getUserDuration.add(Date.now() - getStart);

    const getOk = check(getRes, { 'get user 200': (r) => r.status === 200 });
    errorRate.add(!getOk);
  }

  // 3. Ürün oluştur
  const productRes = http.post(`${BASE_URL}/products`,
    JSON.stringify({
      name: `Product_${__VU}_${__ITER}`,
      description: 'Load test product',
      price: 99.99,
      stock: 100,
    }),
    params
  );
  const productOk = check(productRes, { 'create product 201': (r) => r.status === 201 });
  errorRate.add(!productOk);

  // 4. Ürünü oku
  if (productRes.status === 201) {
    const productId = productRes.json('id');
    const getProductRes = http.get(`${BASE_URL}/products/${productId}`, params);
    check(getProductRes, { 'get product 200': (r) => r.status === 200 });
  }

  sleep(0.5);
}
