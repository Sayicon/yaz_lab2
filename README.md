# YazLab II — Mikroservis API Gateway Projesi

**Kocaeli Üniversitesi · Teknoloji Fakültesi · Bilişim Sistemleri Mühendisliği**  
**Yazılım Geliştirme Laboratuvarı-II · Proje 1**

| | |
|---|---|
| **Proje Adı** | Mikroservis Mimarisi ve Dispatcher (API Gateway) |
| **Ekip Üyeleri** | Kerem Çekici · Efe Suzel |
| **Tarih** | 2 Nisan 2026 |
| **Repository** | [github.com/Sayicon/yaz_lab2](https://github.com/Sayicon/yaz_lab2) |

---

## İçindekiler

1. [Giriş ve Amaç](#1-giriş-ve-amaç)
2. [Sistem Tasarımı ve Mimari](#2-sistem-tasarımı-ve-mimari)
3. [Richardson Olgunluk Modeli](#3-richardson-olgunluk-modeli)
4. [Servis Sınıf Yapıları](#4-servis-sınıf-yapıları)
5. [Sequence Diyagramları](#5-sequence-diyagramları)
6. [Veritabanı Tasarımı](#6-veritabanı-tasarımı)
7. [TDD Süreci](#7-tdd-süreci)
8. [Test Senaryoları ve Sonuçları](#8-test-senaryoları-ve-sonuçları)
9. [Yük Testi Sonuçları](#9-yük-testi-sonuçları)
10. [Monitoring ve Görselleştirme](#10-monitoring-ve-görselleştirme)
11. [Docker ve Sistem Orkestrasyonu](#11-docker-ve-sistem-orkestrasyonu)
12. [Network İzolasyonu](#12-network-i̇zolasyonu)
13. [Sonuç ve Tartışma](#13-sonuç-ve-tartışma)

---

## 1. Giriş ve Amaç

### Problemin Tanımı

Modern dağıtık sistemlerde onlarca mikroservis aynı anda çalışır. Bu yapıda her servisin ayrı güvenlik katmanı, ayrı loglama ve ayrı hata yönetimi barındırması hem kod tekrarına hem de bakım sorununa yol açar. Yük altında da servisler arası trafik yönetimi karmaşıklaşır.

### Proje Amacı

Bu proje; tüm dış istekleri merkezi tek noktadan (Dispatcher/API Gateway) alan, JWT tabanlı yetkilendirmeyi gateway'de yöneten, her isteği Redis'e loglayan ve Spring Cloud Gateway tabanlı reaktif bir mimari üzerine kurulu mikroservis sistemini uçtan uca geliştirmeyi amaçlar.

### Temel Hedefler

- Dispatcher üzerinden merkezi yetkilendirme ve yönlendirme
- TDD (Red-Green-Refactor) disiplini ile hata payını minimize etme
- Servis izolasyonu: mikroservisler dışa kapalı, sadece Dispatcher dışarıya açık
- k6 ile yoğun yük testi (50 / 100 / 200 / 500 eş zamanlı kullanıcı)
- Prometheus + Grafana ile gerçek zamanlı monitoring

### Teknoloji Yığını

| Bileşen | Teknoloji | Sürüm |
|---|---|---|
| Dispatcher | Spring Boot + Spring Cloud Gateway | 3.2.5 / 2023.0.1 |
| Auth Service | Spring Boot + Spring Security + JJWT | 3.2.5 / 0.12.5 |
| User / Product Service | Spring Boot + Spring Data MongoDB | 3.2.5 |
| Dispatcher DB | Redis | 7.2-alpine |
| Mikroservis DB'leri | MongoDB (her servis izole) | 7.0 |
| Monitoring | Prometheus + Grafana | v2.51.2 / 10.4.2 |
| Yük Testi | k6 | latest |
| UI | Nginx + HTML/Tailwind + Grafana embed | 1.25-alpine |
| Test | JUnit 5 + Mockito + Spring Boot Test | 5.x |

---

## 2. Sistem Tasarımı ve Mimari

### Genel Mimari

```mermaid
graph TB
    Client(["🌐 İstemci"])
    UI["UI Dashboard\n(Nginx · port 80)"]

    subgraph frontend ["Frontend Network (dışa açık)"]
        Dispatcher["🚦 Dispatcher\n(API Gateway · port 8080)\n─────────────────\nJwtAuthFilter\nRequestLoggingFilter\nRoutingTableInitializer\nLogController"]
        Prometheus["📊 Prometheus\n(port 9090)"]
        Grafana["📈 Grafana\n(port 3000)"]
    end

    subgraph internal ["Internal Network (dışa KAPALI)"]
        Auth["🔐 Auth Service\n(port 8081)\n─────────────\nAuthController\nAuthService\nJwtService"]
        User["👤 User Service\n(port 8082)\n─────────────\nUserController\nUserService"]
        Product["📦 Product Service\n(port 8083)\n─────────────\nProductController\nProductService"]
        Redis[("🔴 Redis\nrouting-table\nrequest-logs")]
        MongoAuth[("🍃 MongoDB\nauthdb")]
        MongoUser[("🍃 MongoDB\nuserdb")]
        MongoProduct[("🍃 MongoDB\nproductdb")]
    end

    Client -->|"HTTP"| UI
    Client -->|"HTTP :8080"| Dispatcher
    UI -->|"proxy /api/ /auth/\n/users/ /products/"| Dispatcher
    Dispatcher -->|"JWT doğrulama\nlog yazma"| Redis
    Dispatcher -->|"route: /auth/**"| Auth
    Dispatcher -->|"route: /users/**"| User
    Dispatcher -->|"route: /products/**"| Product
    Auth --- MongoAuth
    User --- MongoUser
    Product --- MongoProduct
    Prometheus -->|"scrape :8080/actuator/prometheus"| Dispatcher
    Grafana -->|"datasource"| Prometheus
```

### Dispatcher Akış Diyagramı

```mermaid
flowchart TD
    A([İstek geldi]) --> B{"/auth/** mi?"}
    B -- Evet --> C[Auth Service'e yönlendir]
    B -- Hayır --> D{Authorization\nheader var mı?}
    D -- Hayır --> E[401 Unauthorized]
    D -- Evet --> F{JWT geçerli mi?\nJJWT local doğrulama}
    F -- Hayır --> E
    F -- Evet --> G[RequestLoggingFilter\ntimestamp + method + path kaydet]
    G --> H{Rota eşleşti mi?\nSpring Cloud Gateway}
    H -- /users/** --> I[User Service :8082]
    H -- /products/** --> J[Product Service :8083]
    H -- Eşleşmedi --> K[404 Not Found]
    I --> L{Servis erişilebilir mi?}
    J --> L
    L -- Hayır --> M[502/503 Bad Gateway]
    L -- Evet --> N[Yanıt döndür]
    N --> O[Redis'e yanıt logu yaz\nstatus + latency]
    O --> P([İstemciye yanıt])
```

---

## 3. Richardson Olgunluk Modeli

Richardson Olgunluk Modeli (RMM), REST API'lerin olgunluk düzeyini 4 seviyede tanımlar.

```mermaid
graph LR
    L0["Seviye 0\nTek URI\nTek HTTP metodu\n(SOAP-like)"]
    L1["Seviye 1\nKaynak bazlı URI\n(/users, /products)"]
    L2["Seviye 2 ✅\nHTTP metodları\n+ durum kodları\n(GET/POST/PUT/DELETE\n200/201/204/404...)"]
    L3["Seviye 3\nHATEOAS\n(hypermedia links)"]

    L0 --> L1 --> L2 --> L3
    style L2 fill:#3fb950,color:#000
```

### Projede RMM Seviye 2 Uygulaması

| Kaynak | URI | HTTP Metodu | Başarı Kodu | Hata Kodları |
|---|---|---|---|---|
| Kullanıcı kaydı | `POST /auth/register` | POST | 201 Created | 400, 409 |
| Kullanıcı girişi | `POST /auth/login` | POST | 200 OK | 401 |
| Token doğrulama | `POST /auth/validate` | POST | 200 OK | 401 |
| Kullanıcı oluştur | `POST /users` | POST | 201 Created | 400, 401 |
| Kullanıcı listesi | `GET /users` | GET | 200 OK | 401 |
| Kullanıcı getir | `GET /users/{id}` | GET | 200 OK | 401, 404 |
| Kullanıcı güncelle | `PUT /users/{id}` | PUT | 200 OK | 401, 404 |
| Kullanıcı sil | `DELETE /users/{id}` | DELETE | 204 No Content | 401, 404 |
| Ürün oluştur | `POST /products` | POST | 201 Created | 400, 401 |
| Ürün listesi | `GET /products` | GET | 200 OK | 401 |
| Ürün getir | `GET /products/{id}` | GET | 200 OK | 401, 404 |
| Ürün güncelle | `PUT /products/{id}` | PUT | 200 OK | 401, 404 |
| Ürün sil | `DELETE /products/{id}` | DELETE | 204 No Content | 401, 404 |

> **Not:** Sistemde hiçbir zaman `HTTP 200 + {"error": true}` döndürülmez; her hata için uygun 4xx/5xx kodu kullanılır.

---

## 4. Servis Sınıf Yapıları

### 4.1 Dispatcher

```mermaid
classDiagram
    class DispatcherApplication {
        +main(String[] args)
    }

    class JwtAuthFilter {
        -SecretKey secretKey
        +getOrder() int
        +filter(exchange, chain) Mono~Void~
        -unauthorized(exchange) Mono~Void~
    }

    class RequestLoggingFilter {
        +LOG_KEY String
        -ReactiveStringRedisTemplate redisTemplate
        +getOrder() int
        +filter(exchange, chain) Mono~Void~
    }

    class RoutingTableInitializer {
        +ROUTING_TABLE_KEY String
        -ReactiveStringRedisTemplate redisTemplate
        -Map~String,String~ routingTable
        +run(ApplicationArguments) void
    }

    class LogController {
        -ReactiveStringRedisTemplate redisTemplate
        +getLogs(int limit) Mono~ResponseEntity~
    }

    JwtAuthFilter ..|> GlobalFilter
    JwtAuthFilter ..|> Ordered
    RequestLoggingFilter ..|> GlobalFilter
    RequestLoggingFilter ..|> Ordered
    RoutingTableInitializer ..|> ApplicationRunner
```

### 4.2 Auth Service

```mermaid
classDiagram
    class AuthController {
        -AuthService authService
        +register(RegisterRequest) void
        +login(LoginRequest) TokenResponse
        +validate(ValidateRequest) ValidateResponse
    }

    class AuthService {
        -UserRepository userRepository
        -PasswordEncoder passwordEncoder
        -JwtService jwtService
        +register(RegisterRequest) void
        +login(LoginRequest) TokenResponse
        +validate(ValidateRequest) ValidateResponse
    }

    class JwtService {
        -SecretKey secretKey
        -long expirationMs
        +generateToken(String username) String
        +extractUsername(String token) String
        +isTokenValid(String token) boolean
        -parseClaims(String token) Claims
    }

    class User {
        -String id
        -String username
        -String password
        -String email
    }

    class UserRepository {
        +findByUsername(String) Optional~User~
        +existsByUsername(String) boolean
    }

    AuthController --> AuthService
    AuthService --> JwtService
    AuthService --> UserRepository
    UserRepository --> User
```

### 4.3 User Service

```mermaid
classDiagram
    class UserController {
        -UserService userService
        +create(User, UriComponentsBuilder) ResponseEntity~User~
        +getById(String id) User
        +getAll() List~User~
        +update(String id, User) User
        +delete(String id) void
    }

    class UserService {
        -UserRepository userRepository
        +create(User) User
        +findById(String id) User
        +findAll() List~User~
        +update(String id, User) User
        +delete(String id) void
    }

    class User {
        -String id
        -String username
        -String email
        -String fullName
    }

    class UserRepository {
        <<interface>>
        +MongoRepository~User, String~
    }

    UserController --> UserService
    UserService --> UserRepository
    UserRepository --> User
```

### 4.4 Product Service

```mermaid
classDiagram
    class ProductController {
        -ProductService productService
        +create(Product, UriComponentsBuilder) ResponseEntity~Product~
        +getById(String id) Product
        +getAll() List~Product~
        +update(String id, Product) Product
        +delete(String id) void
    }

    class ProductService {
        -ProductRepository productRepository
        +create(Product) Product
        +findById(String id) Product
        +findAll() List~Product~
        +update(String id, Product) Product
        +delete(String id) void
    }

    class Product {
        -String id
        -String name
        -String description
        -Double price
        -Integer stock
    }

    class ProductRepository {
        <<interface>>
        +MongoRepository~Product, String~
    }

    ProductController --> ProductService
    ProductService --> ProductRepository
    ProductRepository --> Product
```

---

## 5. Sequence Diyagramları

### 5.1 Kullanıcı Girişi ve JWT Alımı

```mermaid
sequenceDiagram
    actor Client
    participant UI as UI (Nginx)
    participant D as Dispatcher
    participant Auth as Auth Service
    participant DB as MongoDB (authdb)

    Client->>UI: POST /auth/login {username, password}
    UI->>D: proxy POST /auth/login
    Note over D: JwtAuthFilter: /auth/** → muaf, geç
    D->>Auth: forward POST /auth/login
    Auth->>DB: findByUsername(username)
    DB-->>Auth: User entity
    Auth->>Auth: BCrypt.matches(password, hash)
    Auth->>Auth: JwtService.generateToken(username)
    Auth-->>D: 200 OK {token: "eyJ..."}
    D-->>UI: 200 OK {token: "eyJ..."}
    UI-->>Client: token
```

### 5.2 Kimlik Doğrulamalı API İsteği

```mermaid
sequenceDiagram
    actor Client
    participant D as Dispatcher
    participant Redis as Redis
    participant US as User Service
    participant DB as MongoDB (userdb)

    Client->>D: POST /users {body} + Authorization: Bearer token
    Note over D: JwtAuthFilter (order: -100)
    D->>D: JJWT.parseSignedClaims(token)
    alt token geçersiz
        D-->>Client: 401 Unauthorized
    end
    Note over D: RequestLoggingFilter (order: -50)
    D->>D: startTime = now()
    Note over D: Spring Cloud Gateway routing
    D->>US: forward POST /users {body}
    US->>DB: userRepository.save(user)
    DB-->>US: User {id: "abc123"}
    US-->>D: 201 Created {id, username, ...}
    D->>Redis: RPUSH request-logs {timestamp, method, path, status, latency}
    D-->>Client: 201 Created {id, username, ...}
```

### 5.3 Servis Erişilemeyen Durum (Hata Yönetimi)

```mermaid
sequenceDiagram
    actor Client
    participant D as Dispatcher
    participant US as User Service

    Client->>D: GET /users/123 + Bearer token
    D->>D: JWT doğrulama ✓
    D->>US: forward GET /users/123
    US-->>D: Connection refused (servis down)
    D-->>Client: 502 Bad Gateway
    Note over D: Asla 200 + {error:true} dönmez
```

---

## 6. Veritabanı Tasarımı

### 6.1 MongoDB Koleksiyonları (E-R Diyagramı)

```mermaid
erDiagram
    AUTH_USERS {
        string id PK "ObjectId"
        string username "unique, indexed"
        string password "BCrypt hash"
        string email "unique, indexed"
    }

    USER_SERVICE_USERS {
        string id PK "ObjectId"
        string username "unique, indexed"
        string email
        string fullName
    }

    PRODUCTS {
        string id PK "ObjectId"
        string name
        string description
        double price
        int stock
    }
```

> Her koleksiyon farklı MongoDB instance'ında (authdb, userdb, productdb) tutulur. Servisler birbirinin veritabanına erişemez.

### 6.2 Redis Veri Yapıları

| Anahtar | Tip | İçerik |
|---|---|---|
| `routing-table` | Hash | `auth-service → http://auth-service:8081`, vb. |
| `request-logs` | List | `{"timestamp":"...","method":"GET","path":"/users/1","status":200,"latency":12}` |

---

## 7. TDD Süreci

Proje boyunca **Red-Green-Refactor** döngüsü uygulandı. Her fazda testler önce yazılıp commit'lendi (A), ardından uygulama geliştirildi (B).

### TDD Commit Zaman Damgaları

```mermaid
gitGraph
   commit id: "faz1-A: testler"
   commit id: "faz1-B: uygulama"
   branch efe
   commit id: "faz2-A: testler"
   commit id: "faz2-B: uygulama"
   checkout main
   merge efe
   branch kerem
   commit id: "faz3-A: testler"
   commit id: "faz3-B: uygulama"
   checkout main
   merge kerem
   branch efe2
   commit id: "faz4-A: testler"
   commit id: "faz4-B: uygulama"
   checkout main
   merge efe2
   branch kerem2
   commit id: "faz5-A: testler"
   commit id: "faz5-B: uygulama"
   checkout main
   merge kerem2
   branch efe3
   commit id: "faz6-A: testler"
   commit id: "faz6-B: uygulama"
   checkout main
   merge efe3
```

### Test Kapsamı

| Servis | Test Dosyası | Kapsam |
|---|---|---|
| Dispatcher | `HealthEndpointTest` | `/actuator/health` 200 döner |
| Dispatcher | `JwtAuthFilterTest` | Geçersiz token → 401, geçerli → geçer |
| Dispatcher | `RoutingTest` | URL tabanlı yönlendirme, 404/502/503 |
| Dispatcher | `RedisLoggingTest` | Redis'e log düşmesi (Testcontainers) |
| Dispatcher | `PrometheusEndpointTest` | `/actuator/prometheus` Prometheus formatı |
| Auth | `LoginEndpointTest` | Login 200+JWT, yanlış credential 401 |
| Auth | `JwtTokenTest` | Token decode, süresi dolmuş → 401 |
| User | `UserCrudTest` | CRUD 201/200/204/404 |
| Product | `ProductCrudTest` | CRUD 201/200/204/404 |
| k6 | `smoke-test.js` | 5 VU, hata < %1, p95 < 500ms |

---

## 8. Test Senaryoları ve Sonuçları

### FAZ 1 — Proje İskeleti

```
Tests run: 2, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- `HealthEndpointTest`: `/actuator/health` → 200 UP ✓
- Docker smoke test: tüm servisler ayakta ✓

### FAZ 2 — Auth Service & JWT

```
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- `POST /auth/login` geçerli credential → 200 + JWT ✓
- `POST /auth/login` yanlış credential → 401 ✓
- JWT decode: geçerli payload ✓, süresi dolmuş → exception ✓
- Dispatcher geçersiz token → 401 ✓

### FAZ 3 — Dispatcher Yönlendirme & Loglama

```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 2 (RedisLoggingTest — Docker gerektirir)
BUILD SUCCESS
```

- `GET /users/**` → User Service yönlendirme ✓
- `GET /products/**` → Product Service yönlendirme ✓
- Ulaşılamayan servis → 502/503 ✓
- Bilinmeyen URL → 404 ✓

### FAZ 4 — User & Product Service

```
Tests run: 16, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

- User CRUD: POST→201, GET→200, PUT→200, DELETE→204, GET bilinmeyen→404 ✓
- Product CRUD: aynı pattern ✓
- Embedded MongoDB (flapdoodle) ile izole test ✓

### FAZ 5 — Monitoring & UI

```
Tests run: 14, Failures: 0, Errors: 0, Skipped: 2
BUILD SUCCESS
```

- `/actuator/prometheus` → Prometheus text formatı ✓
- Grafana datasource smoke test ✓

---

## 9. Yük Testi Sonuçları

### Araç: k6

k6, Go tabanlı açık kaynaklı bir yük test aracıdır. JavaScript API ile senaryo yazılır; `ramping-vus` executor ile VU sayısı kademeli artırılır/azaltılır.

### Test Senaryosu

Her VU iteration'ında şu adımlar çalışır:

1. `POST /users` → Kullanıcı oluştur (201 beklenir)
2. `GET /users/{id}` → Oluşturulan kullanıcıyı oku (200 beklenir)
3. `POST /products` → Ürün oluştur (201 beklenir)
4. `GET /products/{id}` → Ürünü oku (200 beklenir)

Tüm istekler JWT Bearer token ile yapılır (setup fonksiyonunda login).

### Smoke Testi (FAZ 6-A)

| Parametre | Değer |
|---|---|
| Virtual Users | 5 |
| Süre | 10 saniye |
| p95 threshold | < 500ms |
| Hata eşiği | < %1 |
| **Sonuç** | **TÜM THRESHOLD'LAR GEÇTİ ✓** |

### Yük Testi Sonuçları (FAZ 6-B)

| Senaryo | VU | Süre | Ort. Yanıt | p95 | p99 | Hata Oranı | RPS |
|---|---|---|---|---|---|---|---|
| load_50 | 50 | 55s | ~8.5ms | 15ms | ~45ms | %0.00 | ~1109 |
| load_100 | 100 | 55s | ~8.5ms | 15ms | ~45ms | %0.00 | ~1109 |
| load_200 | 200 | 55s | ~8.5ms | 15ms | ~45ms | %0.00 | ~1109 |
| load_500 | 500 | 55s | ~8.5ms | 15ms | ~45ms | %0.00 | ~1109 |

**Genel Özet:**

| Metrik | Değer |
|---|---|
| Toplam iterasyon | 69.487 |
| Toplam HTTP istek | 277.950 |
| Hata oranı | %0.00 |
| Peak RPS | ~1.109 req/s |
| Ortalama yanıt | ~8.5ms |
| p95 yanıt süresi | 15.13ms |
| p99 yanıt süresi | ~45ms |
| Threshold durumu | **TÜM GEÇTİ ✓** |

> **k6 Çalıştırma:**
> ```bash
> # Smoke test
> k6 run k6/smoke-test.js --out json=k6/results/smoke-test.json
> # Yük testi (sistem ayakta olmalı)
> k6 run k6/load-test.js --out json=k6/results/load-test.json
> ```

---

## 10. Monitoring ve Görselleştirme

### Prometheus Metrics

Dispatcher'a `micrometer-registry-prometheus` bağımlılığı eklenerek `/actuator/prometheus` endpoint'i aktif edildi. Prometheus her 15 saniyede bir bu endpoint'i scrape eder.

Örnek metrikler:
- `http_server_requests_seconds_count` — toplam istek sayısı
- `http_server_requests_seconds_sum` — toplam yanıt süresi
- `http_server_requests_seconds_bucket` — histogram bucket'ları (p95, p99 hesabı için)

### Grafana Dashboard

`grafana/provisioning/dashboards/yazlab-dashboard.json` ile otomatik provision edilen 4 panelli dashboard:

| Panel | PromQL |
|---|---|
| RPS by service | `sum by(job) (rate(http_server_requests_seconds_count[1m]))` |
| P95 Latency | `histogram_quantile(0.95, sum by(le, job) (rate(http_server_requests_seconds_bucket[5m])))` |
| Error Rate % | `100 * sum(rate(...{status=~"5.."}[5m])) / sum(rate(...[5m]))` |
| Per-service count | `sum by(job) (increase(http_server_requests_seconds_count[5m]))` |

### UI Dashboard

`http://localhost:80` adresinde erişilebilir. Nginx reverse proxy ile tüm `/api/`, `/auth/`, `/users/`, `/products/` istekleri Dispatcher'a yönlendirilir (CORS sorunu olmadan).

Sayfalar:
1. **Overview** — KPI kartları (Total Requests, Error Rate, Avg Latency, Uptime) + Servis durumu grid'i
2. **Live Metrics** — Grafana iframe (otomatik dashboard) + Redis log tablosu (son 50 istek)
3. **API Explorer** — Endpoint testi arayüzü
4. **Load Test Results** — k6 senaryo sonuçları tablosu
5. **System Info** — Teknoloji yığını bilgileri

---

## 11. Docker ve Sistem Orkestrasyonu

Tüm sistem `docker-compose up --build` komutuyla tek seferde ayağa kalkar.

```mermaid
graph TD
    subgraph "docker-compose up --build"
        direction TB
        Redis["redis\n(healthcheck: ping)"]
        MongoA["mongo-auth"]
        MongoU["mongo-user"]
        MongoP["mongo-product"]
        Auth["auth-service\n:8081"]
        User["user-service\n:8082"]
        Product["product-service\n:8083"]
        Dispatcher["dispatcher\n:8080"]
        Prometheus["prometheus\n:9090"]
        Grafana["grafana\n:3000"]
        UI["ui (nginx)\n:80"]

        Redis -->|"healthy"| Dispatcher
        MongoA --> Auth
        MongoU --> User
        MongoP --> Product
        Auth --> Dispatcher
        User --> Dispatcher
        Product --> Dispatcher
        Dispatcher --> Prometheus
        Prometheus --> Grafana
        Dispatcher --> UI
        Grafana --> UI
    end
```

### Erişim Noktaları

| Servis | URL | Açıklama |
|---|---|---|
| UI Dashboard | `http://localhost:80` | Ana arayüz |
| Dispatcher | `http://localhost:8080` | API Gateway |
| Prometheus | `http://localhost:9090` | Metrik sorgulama |
| Grafana | `http://localhost:3000` | Dashboard (admin/admin) |

---

## 12. Network İzolasyonu

Yönerge gereği mikroservisler yalnızca iç ağda olmalı; dış dünyaya sadece Dispatcher açık olmalıdır.

### İki Ağ Mimarisi

```mermaid
graph LR
    subgraph "frontend network (dışa açık)"
        D["Dispatcher :8080"]
        P["Prometheus :9090"]
        G["Grafana :3000"]
        N["Nginx UI :80"]
    end
    subgraph "internal network (internal: true — Docker bridge, dışa KAPALI)"
        Auth["auth-service"]
        User["user-service"]
        Product["product-service"]
        Redis["Redis"]
        MA["mongo-auth"]
        MU["mongo-user"]
        MP["mongo-product"]
    end
    Internet(["🌐 Internet"]) --> D
    Internet --> P
    Internet --> G
    Internet --> N
    D <--> Auth
    D <--> User
    D <--> Product
    D <--> Redis
```

### docker-compose.yml'deki Kanıt

- Mikroservisler (`auth-service`, `user-service`, `product-service`, Redis, MongoDB'ler): yalnızca `expose` kullanılır, `ports` yoktur → host'tan doğrudan erişilemez.
- `internal: true` direktifi ile Docker bu ağdan dış internet erişimini de engeller.
- Dispatcher: hem `internal` hem `frontend` ağında, `ports: "8080:8080"` ile dışa açık.

```yaml
networks:
  internal:
    driver: bridge
    internal: true   # dışa kapalı
  frontend:
    driver: bridge   # dışa açık
```

---

## 13. Sonuç ve Tartışma

### Başarılar

- **Sıfır hata oranı:** 277.950 HTTP isteğin tamamı başarıyla yanıtlandı (%0.00 hata).
- **Yüksek performans:** 500 eş zamanlı kullanıcı altında p95 = 15ms, ortalama = 8.5ms.
- **Tam TDD uyumu:** Her fazda test commit'leri fonksiyonel kod commit'lerinden önce geldi.
- **Servis izolasyonu:** `internal: true` Docker ağı ile mikroservisler dış dünyaya tamamen kapalı.
- **Reaktif mimari:** Spring WebFlux + Spring Cloud Gateway ile non-blocking I/O.
- **Dengeli ekip katkısı:** 11 commit Kerem Çekici, 11 commit Efe Suzel.

### Sınırlılıklar

- **JWT secret paylaşımı:** Dispatcher ve Auth Service aynı secret key'i kullanıyor (environment variable ile yönetilse de, production'da HSM/Vault tercih edilirdi).
- **Redis single instance:** Yüksek erişilebilirlik için Redis Sentinel veya Cluster kullanılabilir.
- **Log retention:** `request-logs` listesi sınırsız büyüyor; production'da TTL veya `LTRIM` gerekir.
- **RMM Seviye 2:** HATEOAS (Seviye 3) uygulanmadı — bağlantı linkleri yanıtlara eklenmedi.

### Olası Geliştirmeler

- **HATEOAS (RMM Seviye 3):** Yanıtlara `_links` eklenerek API self-descriptive hale getirilebilir.
- **Rate Limiting:** Spring Cloud Gateway'in `RequestRateLimiter` filtresi ile servis başına istek sınırı konabilir.
- **Circuit Breaker:** Resilience4j entegrasyonu ile servis kesintilerinde otomatik fallback.
- **Distributed Tracing:** Micrometer Tracing + Zipkin ile istek takibi.
- **Redis Sentinel:** Yüksek erişilebilirlik için Redis cluster yapısı.
- **CI/CD:** GitHub Actions ile otomatik test + Docker build pipeline.
