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
| Dispatcher | Python · FastAPI |
| Auth/Login Service | Python · FastAPI · PyJWT |
| User / Product / Order Service | Java · Spring Boot |
| Dispatcher DB | Redis |
| Mikroservis DB'leri | MongoDB (her servis izole, ayrı DB) |
| UI / Dashboard | HTML + Grafana embed |
| Container | Docker + docker-compose |
| Monitoring | Prometheus + Grafana |
| Yük Testi | k6 |
| Test Framework | pytest (Python) · JUnit 5 (Java) |

---

## Mimari Notlar

- **RMM Seviye 2** zorunlu: kaynak bazlı URI'ler (`/users/{id}`), doğru HTTP metodları (GET/POST/PUT/DELETE), doğru HTTP status kodları.
- **TDD zorunlu** (Dispatcher için): test dosyalarının commit zaman damgası fonksiyonel koddan önce olmalı.
- **OOP + SOLID** tüm servislerde geçerli.
- **docker-compose up** ile tüm sistem tek seferde ayağa kalkmalı.
- Servisler arası veri transferi JSON.
- Her fazda önce testler yazılır ve commit'lenir (kısım A), sonra uygulama geliştirilir (kısım B).

---

## Geliştirme Kuralları

- Her commit mantıklı ve açıklayıcı olmalı. Her iki üye eşit commit yükü taşımalı.
- Branch stratejisi: Branch yönetimi Biz yani kullanıcılar tarafından elle yapılır faz sonunda gerekli push işlemini gerçekleriz.
- Rapor `README.md` olarak GitHub'da Markdown + Mermaid ile yazılır.
- Yük testi sonuçları (50 / 100 / 200 / 500 eş zamanlı istek) rapora ve UI'a yansıtılır.
- **Test logları:** Her fazın B kısmı tamamlandığında `pytest --tb=short -v` / `mvn test` çıktısı `test-logs/faz-N.txt` dosyasına kaydedilip commit'lenir. Bu çıktılar README'deki "Test Senaryoları ve Sonuçları" bölümüne özetlenerek eklenir (proje raporu gereği).

---

## Fazlar

---

### FAZ 1 — Proje İskeleti & Docker Altyapısı
**Sorumlu: Kerem**

#### A — Testler (önce commit'le)
- [x] Dispatcher `/health` endpoint'inin 200 döndürdüğünü doğrulayan pytest testi yaz. → `tests/test_dispatcher_health.py`
- [x] `docker-compose up` sonrası tüm servislerin ayakta olduğunu kontrol eden smoke test betiği yaz. → `tests/test_smoke.py`
- [x] Redis bağlantısının açık olduğunu test et (ping/pong). → `tests/test_redis_connection.py`
- [x] **Testleri commit'le** (zaman damgası B'den önce olmalı).

#### B — Uygulama
- [ ] Proje klasör yapısını oluştur: `dispatcher/`, `auth-service/`, `user-service/`, `product-service/`, `docker-compose.yml`, `k6/`, `grafana/`, `prometheus/`.
- [ ] Her servis için `Dockerfile` yaz (Python: `uvicorn`, Java: `maven` multi-stage build).
- [ ] `docker-compose.yml`: tüm servisleri, Redis'i ve MongoDB'yi tanımla. Mikroservisler `internal` network'te, Dispatcher dışa açık.
- [ ] FastAPI Dispatcher'a `/health` endpoint'i ekle.
- [ ] Testleri çalıştır → çıktıyı `test-logs/faz-1.txt` olarak kaydet ve commit'le.
- [ ] **AGENTS.md'yi güncelle.**

---

### FAZ 2 — Auth Service & JWT
**Sorumlu: Efe**

#### A — Testler (önce commit'le)
- [ ] `POST /auth/login` geçerli credential → 200 + JWT döner (pytest).
- [ ] `POST /auth/login` yanlış credential → 401 döner (pytest).
- [ ] JWT decode testi: geçerli token → payload doğru; süresi dolmuş → hata (pytest).
- [ ] Dispatcher geçersiz token ile gelen isteği 401 ile reddeder (pytest).
- [ ] **Testleri commit'le.**

#### B — Uygulama
- [ ] Auth Service: FastAPI + PyJWT. `POST /auth/login`, `POST /auth/register`, `POST /auth/validate` endpoint'leri.
- [ ] Kullanıcı verisi MongoDB'de saklanır (auth-service'in kendi izole DB'si).
- [ ] Dispatcher'a JWT doğrulama middleware'i ekle: her istekte `Authorization: Bearer <token>` kontrol edilir, `/auth/*` hariç.
- [ ] Dispatcher, token doğrulama için Auth Service'e internal ağdan istek atar.
- [ ] Testleri çalıştır → `test-logs/faz-2.txt` olarak kaydet ve commit'le.
- [ ] **AGENTS.md'yi güncelle.**

---

### FAZ 3 — Dispatcher Yönlendirme & Loglama
**Sorumlu: Kerem**

#### A — Testler (önce commit'le)
- [ ] `GET /users/` isteğinin User Service'e yönlendirildiğini doğrula (mock servis ile pytest).
- [ ] `GET /products/` isteğinin Product Service'e yönlendirildiğini doğrula (pytest).
- [ ] Ulaşılamayan servise istek → 502 / 503 döner (pytest).
- [ ] Hatalı URL'ye istek → 404 döner (pytest).
- [ ] Redis'e log kaydının düştüğünü test et (pytest).
- [ ] **Testleri commit'le.**

#### B — Uygulama
- [ ] Dispatcher'a URL-tabanlı dinamik proxy yönlendirme ekle (`httpx` async reverse proxy).
- [ ] Yönlendirme tablosunu Redis'te tut (servis adı → internal URL).
- [ ] Her istek/yanıt için Redis'e log yaz: timestamp, method, path, status, latency.
- [ ] 4xx / 5xx hata kodlarını doğru döndür (asla `200 + {"error": true}` değil).
- [ ] Testleri çalıştır → `test-logs/faz-3.txt` olarak kaydet ve commit'le.
- [ ] **AGENTS.md'yi güncelle.**

---

### FAZ 4 — User Service & Product Service (Spring Boot)
**Sorumlu: Efe**

#### A — Testler (önce commit'le)
- [ ] JUnit 5: User Service CRUD — `POST /users` → 201, `GET /users/{id}` → 200 veya 404, `PUT /users/{id}` → 200, `DELETE /users/{id}` → 204.
- [ ] JUnit 5: Product Service CRUD (aynı pattern).
- [ ] Dispatcher'ı bypass eden doğrudan isteğin reddedildiğini test et (network isolation).
- [ ] MongoDB bağlantısı entegrasyon testi (Testcontainers kullanılabilir).
- [ ] **Testleri commit'le.**

#### B — Uygulama
- [ ] User Service: Spring Boot + Spring Data MongoDB. `User` entity, `UserRepository`, `UserService`, `UserController`. RMM Seviye 2 uyumlu endpoint'ler.
- [ ] Product Service: aynı yapı (`Product` entity). Her servisin kendi izole MongoDB DB'si.
- [ ] Her servis yalnızca internal Docker network'te erişilebilir.
- [ ] Network isolation'ı ekran görüntüleriyle belgele (rapora eklenecek).
- [ ] Testleri çalıştır → `mvn test > test-logs/faz-4.txt` olarak kaydet ve commit'le.
- [ ] **AGENTS.md'yi güncelle.**

---

### FAZ 5 — Monitoring & UI
**Sorumlu: Kerem**

#### A — Testler (önce commit'le)
- [ ] Dispatcher `/metrics` endpoint'inin Prometheus formatında veri döndürdüğünü test et (pytest).
- [ ] Grafana container'ının ayakta ve datasource'un bağlı olduğunu smoke test ile doğrula.
- [ ] **Testleri commit'le.**

#### B — Uygulama
- [ ] `prometheus-fastapi-instrumentator` ekle → Dispatcher `/metrics` endpoint'i.
- [ ] `prometheus.yml` scrape konfigürasyonu yaz.
- [ ] Grafana'ya Prometheus datasource ekle; trafik dashboard'u oluştur (RPS, latency, hata oranı, servis başına istek).
- [ ] Basit HTML UI: Grafana iframe embed + Redis'ten son N logu çeken log tablosu.
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
