# AGENTS.md — YazLab II · Proje 1

> **⚠️ AGENT KURALI:** Herhangi bir adımı tamamladığında, bir karar verdiğinde veya dosyada değişiklik yaptığında **bu AGENTS.md dosyasını güncelle**. İlgili checkbox'ı işaretle (`- [x]`), gerekirse not düş. Bu dosya projenin tek source of truth'udur.

---

## Proje Özeti

Kocaeli Üniversitesi BSM YazLab II kapsamında geliştirilen bu proje, **Mikroservis Mimarisi** üzerine kurulu, merkezi bir **Dispatcher (API Gateway)** üzerinden yönetilen, güvenli ve ölçeklenebilir bir dağıtık sistem uygulamasıdır.

Sistem en az 4 bağımsız servisten oluşur:
- **Dispatcher** — Tek giriş noktası. Tüm yönlendirme, yetkilendirme doğrulama ve loglama burada yapılır.
- **Auth/Login Service** — JWT tabanlı kimlik doğrulama.
- **User Service** — Kullanıcı yönetimi (Spring Boot).
- **Product/Order Service** — İş mantığı servisi (Spring Boot).

Mikroservisler yalnızca iç Docker ağında erişilebilir; dış dünyaya sadece Dispatcher açıktır (Network Isolation).

---

## Teknoloji Yığını

| Bileşen | Teknoloji |
|---|---|
| Dispatcher | Java · Spring Boot · Spring Cloud Gateway |
| Auth/Login Service | Java · Spring Boot · Spring Security · JJWT |
| User / Product / Order Service | Java · Spring Boot · Maven |
| Dispatcher DB | Redis |
| Mikroservis DB'leri | MongoDB (her servis izole, ayrı DB) |
| UI / Dashboard | HTML + Tailwind CSS + Grafana embed (`ui/index.html`) |
| Container | Docker + docker-compose |
| Monitoring | Prometheus + Grafana (Micrometer) |
| Yük Testi | k6 |
| Test Framework | JUnit 5 · Mockito · Spring Boot Test (tüm servisler) |

---

## UI / Dashboard Tasarımı

> **Kaynak dosyalar:** `index.html` (Google Stitch export) · `DESIGN.md` (tasarım sistemi)

### Tema: "The Kinetic Console" — Utilitarian Brutalism
- **Renk paleti:** `background` #0d1117 → `surface` #161b22 → `surface-container` #1c2026 (tonal katmanlama, gölge yok)
- **Tipografi:** JetBrains Mono — exclusive. Gradyan yok, köşe yarıçapı max 6px.
- **Durum renkleri:** `tertiary` #3fb950 (UP/OK) · `warning` #d29922 · `error` #f85149 (CRITICAL)
- **Zebra tablo:** satır arası `surface` / `surface-container-low` dönüşümü
- **Ghost border:** `outline-variant` %40 opaklık ile dış çerçeve

### Sayfalar (sidebar navigasyonu)
1. **Overview** — Summary bar (4 KPI kart) + Service Grid (6 kart: Dispatcher, Auth, User, Redis, MongoDB, Product)
2. **Live Metrics** — Grafana iframe placeholder + Redis log tablosu (timestamp · method · path · status · latency · service)
3. **API Explorer** — Endpoint sidebar + Request/Response builder
4. **Load Test Results** — k6 sonuçları tablosu
5. **System Info** — Sistem bilgileri

### Uygulama Kuralları (Faz 5'te)
- `index.html` `ui/` klasörüne taşı; Thymeleaf/static resource olarak sun veya ayrı Nginx container'da çalıştır.
- Grafana iframe: `http://localhost:3000/d/dashboard` → `docker-compose` ayağa kalkınca otomatik bağlanır.
- Log tablosu: Dispatcher'dan `GET /api/logs?limit=50` endpoint'i çeker (Redis'ten son 50 log).
- Statik sayı/mock data yerine gerçek API çağrıları (fetch/XHR) ile değiştirilecek alanlar: Total Requests, Error Rate, Avg Latency, servis ping değerleri.

---

## Mimari Notlar

- **RMM Seviye 2** zorunlu: kaynak bazlı URI'ler (`/users/{id}`), doğru HTTP metodları (GET/POST/PUT/DELETE), doğru HTTP status kodları.
- **TDD zorunlu** (Dispatcher için): test dosyalarının commit zaman damgası fonksiyonel koddan önce olmalı.
- **OOP + SOLID** tüm servislerde geçerli (Interface, Encapsulation, Abstraction, Inheritance ve Polymorphism).
- **docker-compose up** ile tüm sistem tek seferde ayağa kalkmalı.
- Servisler arası veri transferi JSON.
- Her fazda önce testler yazılır ve commit'lenir (kısım A), sonra uygulama geliştirilir (kısım B).

---

## Geliştirme Kuralları

- Her commit mantıklı ve açıklayıcı olmalı. Her iki üye eşit commit yükü taşımalı.
- Branch stratejisi: Branch yönetimi Biz yani kullanıcılar tarafından elle yapılır faz sonunda gerekli push işlemini gerçekleriz.
- Rapor `README.md` olarak GitHub'da Markdown + Mermaid ile yazılır.
- Yük testi sonuçları (50 / 100 / 200 / 500 eş zamanlı istek) rapora ve UI'a yansıtılır.
- **Test logları:** Her fazın B kısmı tamamlandığında `mvn test` çıktısı `test-logs/faz-N.txt` dosyasına kaydedilip commit'lenir. Bu çıktılar README'deki "Test Senaryoları ve Sonuçları" bölümüne özetlenerek eklenir (proje raporu gereği).

---

## Fazlar

---

### FAZ 1 — Proje İskeleti & Docker Altyapısı
**Sorumlu: Kerem**

#### A — Testler (önce commit'le)
- [x] Dispatcher `/health` endpoint'inin 200 döndürdüğünü doğrulayan JUnit 5 testi yaz (`@SpringBootTest` ile — Docker gerektirmez). → `dispatcher/src/test/java/com/yazlab/dispatcher/HealthEndpointTest.java`
- [x] `docker-compose up` sonrası tüm servislerin ayakta olduğunu kontrol eden smoke test betiği yaz. → `tests/smoke-test.sh` (**Not:** Bu JUnit ile yapılamaz; Docker ağ seviyesinde çalışır. `curl` veya `wget` ile her servisin `/health` endpoint'i kontrol edilir.)
- [x] Redis bağlantısının açık olduğunu test et (ping/pong). → `tests/redis-check.sh` (**Not:** Bu da shell script olmalı; `redis-cli ping` ile kontrol edilir. JUnit testi Docker ayaktayken çalışacağı için bağımsız bir betik daha güvenlidir.)
- [x] **Testleri commit'le** (zaman damgası B'den önce olmalı).

#### B — Uygulama
- [x] Proje klasör yapısını oluştur: `dispatcher/`, `auth-service/`, `user-service/`, `product-service/`, `docker-compose.yml`, `k6/`, `grafana/`, `prometheus/`.
- [x] Her servis için `Dockerfile` yaz (Java: `maven` multi-stage build).
- [x] `docker-compose.yml`: tüm servisleri, Redis'i ve MongoDB'yi tanımla. Mikroservisler `internal` network'te, Dispatcher dışa açık.
- [x] Spring Boot Dispatcher'a `/health` endpoint'i ekle (`/actuator/health` — Spring Boot Actuator ile).
- [x] Testleri çalıştır → çıktıyı `test-logs/faz-1.txt` olarak kaydet ve commit'le. (2/2 passed)
- [x] **AGENTS.md'yi güncelle.**

---

### FAZ 2 — Auth Service & JWT
**Sorumlu: Efe**

#### A — Testler (önce commit'le)
- [x] JUnit 5: `POST /auth/login` geçerli credential → 200 + JWT döner. → `auth-service/.../LoginEndpointTest.java`
- [x] JUnit 5: `POST /auth/login` yanlış credential → 401 döner. → `auth-service/.../LoginEndpointTest.java`
- [x] JWT decode testi: geçerli token → payload doğru; süresi dolmuş → 401. → `auth-service/.../JwtTokenTest.java`
- [x] Dispatcher geçersiz token ile gelen isteği 401 ile reddeder. → `dispatcher/.../JwtAuthFilterTest.java`
- [x] **Testleri commit'le.**

#### B — Uygulama
- [x] Auth Service: Spring Boot + Spring Security + JJWT. `POST /auth/login`, `POST /auth/register`, `POST /auth/validate` endpoint'leri.
- [x] Kullanıcı verisi MongoDB'de saklanır (auth-service'in kendi izole DB'si). → `User` entity, `UserRepository`, `AuthService`
- [x] Dispatcher'a JWT doğrulama filter'ı ekle (`JwtAuthFilter implements GlobalFilter`): her istekte `Authorization: Bearer <token>` kontrol edilir, `/auth/**` hariç.
- [x] Dispatcher JWT'yi local olarak doğrular (JJWT + paylaşılan secret key). Auth Service'e HTTP çağrısı yapılmaz.
- [x] Testleri çalıştır → `test-logs/faz-2.txt` olarak kaydet ve commit'le. (12/12 passed)
- [x] **AGENTS.md'yi güncelle.**

---

### FAZ 3 — Dispatcher Yönlendirme & Loglama
**Sorumlu: Kerem**

#### A — Testler (önce commit'le)
- [x] `GET /users/` isteğinin User Service'e yönlendirildiğini doğrula (mock servis ile JUnit 5 + MockWebServer). → `dispatcher/.../RoutingTest.java`
- [x] `GET /products/` isteğinin Product Service'e yönlendirildiğini doğrula (JUnit 5). → `dispatcher/.../RoutingTest.java`
- [x] Ulaşılamayan servise istek → 502 / 503 döner (JUnit 5). → `dispatcher/.../RoutingTest.java`
- [x] Hatalı URL'ye istek → 404 döner (JUnit 5). → `dispatcher/.../RoutingTest.java`
- [x] Redis'e log kaydının düştüğünü test et (JUnit 5 + Testcontainers Redis). → `dispatcher/.../RedisLoggingTest.java`
- [x] **Testleri commit'le.**

#### B — Uygulama
- [x] Dispatcher'a URL-tabanlı dinamik proxy yönlendirme ekle (`WebClient` async reverse proxy). → Spring Cloud Gateway (WebClient tabanlı) kullanılıyor
- [x] Yönlendirme tablosunu Redis'te tut (servis adı → internal URL). → `RoutingTableInitializer` startup'ta Redis hash'e yazar (`routing-table`)
- [x] Her istek/yanıt için Redis'e log yaz: timestamp, method, path, status, latency. → `RequestLoggingFilter` `request-logs` listesine yazar
- [x] 4xx / 5xx hata kodlarını doğru döndür (asla `200 + {"error": true}` değil). → Spring Cloud Gateway native 4xx/5xx yönetimi
- [x] Testleri çalıştır → `test-logs/faz-3.txt` olarak kaydet ve commit'le. (10/10 passed, 2 skipped — RedisLoggingTest Docker erişimi)
- [x] **AGENTS.md'yi güncelle.**

---

### FAZ 4 — User Service & Product Service (Spring Boot)
**Sorumlu: Efe**

#### A — Testler (önce commit'le)
- [x] JUnit 5: User Service CRUD — `POST /users` → 201, `GET /users/{id}` → 200 veya 404, `PUT /users/{id}` → 200, `DELETE /users/{id}` → 204. → `user-service/.../UserCrudTest.java`
- [x] JUnit 5: Product Service CRUD (aynı pattern). → `product-service/.../ProductCrudTest.java`
- [x] MongoDB bağlantısı entegrasyon testi (flapdoodle embedded MongoDB — her test öncesi DB temizleniyor).
- [ ] **Testleri commit'le.**

#### B — Uygulama
- [ ] User Service: Spring Boot + Spring Data MongoDB. `User` entity, `UserRepository`, `UserService`, `UserController`. RMM Seviye 2 uyumlu endpoint'ler.
- [ ] Product Service: aynı yapı (`Product` entity). Her servisin kendi izole MongoDB DB'si.
- [ ] Her servis yalnızca internal Docker network'te erişilebilir.
- [ ] Network isolation'ı belgele: `docker-compose.yml`'de mikroservislerin `ports` yerine sadece `expose` kullandığını göster ve `docker network inspect` çıktısını ekran görüntüsüyle rapora ekle. (**Not:** Bu JUnit ile test edilemez — network isolation Docker ağ seviyesinde çalışır, servis kodu bunu görmez.)
- [ ] Testleri çalıştır → `mvn test > test-logs/faz-4.txt` olarak kaydet ve commit'le.
- [ ] **AGENTS.md'yi güncelle.**

---

### FAZ 5 — Monitoring & UI
**Sorumlu: Kerem**

#### A — Testler (önce commit'le)
- [ ] Dispatcher `/actuator/prometheus` endpoint'inin Prometheus formatında veri döndürdüğünü test et (JUnit 5 + MockMvc).
- [ ] Grafana container'ının ayakta ve datasource'un bağlı olduğunu smoke test ile doğrula.
- [ ] **Testleri commit'le.**

#### B — Uygulama
- [ ] `micrometer-registry-prometheus` bağımlılığını ekle → Dispatcher `/actuator/prometheus` endpoint'i.
- [ ] `prometheus.yml` scrape konfigürasyonu yaz.
- [ ] Grafana'ya Prometheus datasource ekle; trafik dashboard'u oluştur (RPS, latency, hata oranı, servis başına istek).
- [ ] `index.html`'i `ui/index.html` olarak projeye entegre et (Google Stitch tasarımına sadık kal — bkz. `DESIGN.md`). Grafana iframe ve log tablosunu gerçek API'ye bağla (`GET /api/logs?limit=50`).
- [ ] UI için `ui/` klasörünü Nginx container veya Dispatcher static resource olarak sun.
- [ ] Testleri çalıştır → `test-logs/faz-5.txt` olarak kaydet ve commit'le.
- [ ] **AGENTS.md'yi güncelle.**

---

### FAZ 6 — Yük Testi & Sonuçlar
**Sorumlu: Efe**

#### A — Testler (önce commit'le)
- [ ] k6 script'ini küçük yük (5 VU, 10s) ile çalıştır; hata oranı %0 olduğunu doğrula.
- [ ] p95 < 500ms threshold'unu k6 `thresholds` ile tanımla ve test et.
- [ ] **Testleri commit'le.**

#### B — Uygulama
- [ ] k6 yük testi script'leri yaz: 50 / 100 / 200 / 500 eş zamanlı kullanıcı senaryoları.
- [ ] Her senaryo için ölçüm: ortalama yanıt süresi, p95, p99, hata oranı, RPS.
- [ ] Sonuçları `k6/results/` klasörüne JSON olarak çıkar.
- [ ] Grafana dashboard'una yük testi sonuçlarını ekle ve README'ye tablo olarak yaz.
- [ ] Testleri çalıştır → `test-logs/faz-6-k6.txt` olarak kaydet ve commit'le.
- [ ] **AGENTS.md'yi güncelle.**

---

### FAZ 7 — Rapor & Son Kontroller
**Sorumlu: Kerem + Efe**

- [ ] `README.md` tamamla: proje açıklaması, mimari, Mermaid diyagramları (sequence, class, akış), RMM açıklaması, test senaryoları ve sonuçları (`test-logs/` özetleri dahil), yük testi tablosu, ekran görüntüleri, sınırlılıklar.
- [ ] `docker-compose up` ile sıfırdan tam sistem kurulumu test edilir.
- [ ] TDD commit zaman damgaları kontrol edilir (test commit'leri daima önce).
- [ ] GitHub commit geçmişi: her iki üyenin katkısı dengeli mi kontrol edilir.
- [ ] Tüm `test-logs/faz-N.txt` dosyalarının repoda mevcut olduğunu doğrula.
- [ ] Son teslim: tüm kod + çalışan uygulama + README tek sıkıştırılmış dosya olarak yüklenir.
- [ ] **AGENTS.md'yi güncelle (proje tamamlandı olarak işaretle).**

---

## Teslim Tarihleri

| Olay | Tarih |
|---|---|
| Grup oluşturma son tarihi | 26 Şubat 2026, 17:00 |
| Proje son teslim | 5 Nisan 2026, 23:59 |
| Sunum | 6–17 Nisan 2026 |
