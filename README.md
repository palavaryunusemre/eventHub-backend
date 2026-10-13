# EventHub — Etkinlik/Seminer Platformu (Backend)

Spring Boot tabanlı backend. Herkesin etkinlik/seminer/söyleşi oluşturabildiği,
katılımcıların QR kod ile kayıt olup check-in yaptığı ve etkinlik sonunda QR
doğrulamalı sertifika aldığı bir platform. Üniversite içi kullanım (kurum
e-postasıyla doğrulanmış kullanıcılar + kuruma özel etkinlikler) ve herkese
açık etkinlikler birlikte desteklenir.

---

## İçindekiler

- [Teknoloji Yığını](#teknoloji-yığını)
- [Çalıştırma](#çalıştırma)
- [Roller](#roller)
- [Yanıt Zarfı (Result / DataResult)](#yanıt-zarfı-result--dataresult)
- [Kimlik Doğrulama](#kimlik-doğrulama)
- [REST API](#rest-api)
- [Etkinlik Akışı](#etkinlik-akışı)

---

## Teknoloji Yığını

| Katman | Teknoloji |
|---|---|
| Framework | Spring Boot `4.1.0` (Java `17`) |
| Veritabanı | PostgreSQL (Spring Data JPA / Hibernate) |
| Kimlik doğrulama | Spring Security + JWT (`jjwt` `0.11.5`), stateless |
| QR üretimi | ZXing (`com.google.zxing:core`/`javase`) |
| Sertifika PDF | Thymeleaf şablonu + OpenHTMLtoPDF |
| E-posta doğrulama | `spring-boot-starter-mail` (bağımsız `EmailVerificationService`) |
| Log altyapısı (docker-stack) | Elasticsearch + Kibana + Filebeat + Grafana (`docker-stack/docker-compose.yml`) |

## Çalıştırma

Destek servisleri (Postgres, Redis, ELK, Grafana) için:
```bash
cd docker-stack && docker compose up -d
```

Backend:
```bash
./mvnw spring-boot:run
```

`src/main/resources/application.yaml` tarafından okunan ortam değişkenleri:

| Ortam Değişkeni | Amaç |
|---|---|
| `DB_PASSWORD` | PostgreSQL şifresi |
| `USER_PASSWORD` | `spring.security.user.password` |
| `JWT_SECRET_KEY` | JWT imzalama anahtarı (Base64, HMAC-SHA256) |

İlk `ADMIN` kullanıcısı otomatik seed edilmez; ilk kaydı doğrudan veritabanına
(ör. `psql` ile) veya rol kısıtı olmayan `POST /api/auth/signIn` çağrısıyla elle
oluşturup rolünü sonradan `ADMIN`'e çekmeniz gerekir.

## Roller

| Rol | Kapsam |
|---|---|
| `ADMIN` | Etkinlik onay/red kuyruğunu yönetir, kurum/kategori CRUD'u yapar, tüm kullanıcıları yönetir |
| `USER` | Etkinlik oluşturabilir (admin onayından sonra yayınlanır), etkinliklere kayıt olur, QR ile check-in yapar, sertifikalarını görür |

## Yanıt Zarfı (Result / DataResult)

Tüm controller'lar `ResponseEntity<Result<T>>` ya da `ResponseEntity<DataResult<T>>`
döner (`utilities.result` paketi):

```jsonc
// Result<T>
{ "success": true, "code": 200, "message": "string", "errors": ["..."] }

// DataResult<T> (Result'ı extend eder)
{ "success": true, "code": 200, "data": { /* T */ }, "pageInfo": { /* sayfalı yanıtlarda */ } }
```

## Kimlik Doğrulama

- JWT — `Authorization: Bearer <token>` header'ı ya da HttpOnly cookie (`JwtAuthenticationFilter`).
- `POST /api/auth/logIn` başarılı girişte `eventhub_jwt`/`eventhub_user`/`eventhub_refresh` cookie'lerini set eder.
- Token claim'leri: `sub`=email, `userID`, `email`, `role` (`ROLE_` önekli), `institutionId`, `firstName`, `lastName`.

## REST API

Base path'ler `config/RestApis.java`'dan gelir.

### Auth — `/api/auth`

| Method | Path | Rol | Açıklama |
|---|---|---|---|
| POST | `/api/auth/logIn` | herkes | `LogInRequest{email, password}` |
| POST | `/api/auth/signIn` | herkes (public) | Yeni kullanıcı kaydı — `SignInRequest{email, firstName, lastName, password, role, institutionId}` |
| PUT | `/api/auth/update` | ADMIN | `UpdateUserRequest` |
| DELETE | `/api/auth/delete?id=` | ADMIN | Soft delete |
| GET | `/api/auth/users` | ADMIN | Kullanıcı listesi |
| PUT | `/api/auth/change-password` | herkes | Kendi parolasını değiştirir |
| POST | `/api/auth/refresh`, `/api/auth/logout` | herkes | Refresh cookie ile token yenileme/çıkış |

### Events — `/api/events`

- `GET /`, `GET /{id}` — herkese açık (public etkinlikler + kullanıcının kurumuna özel etkinlikler).
- `POST /`, `PUT /{id}`, `DELETE /{id}` — kimlik doğrulaması gerektirir; sadece sahip/ADMIN düzenleyebilir.
- `POST /{id}/submit-for-review` — DRAFT → PENDING_REVIEW.

### Admin Event Review — `/api/admin/events` (ADMIN)

- `GET /pending`, `POST /{id}/approve`, `POST /{id}/reject` (`{rejectionReason}`).

### Registrations — `/api/registrations` + `/api/events/{id}/register`

- Kayıt oluşturma, iptal, kendi kayıtlarını listeleme (`qrToken` her kayıtta üretilir).

### Check-in — `/api/checkins`

- `POST /scan` `{qrToken}` — organizer/staff tarafından QR tarandığında çağrılır.

### Certificates — `/api/certificates` + `/api/verify/{code}`

- Kendi sertifikalarını listeleme, PDF indirme.
- `GET /api/verify/{code}` — **public**, sertifikanın geçerliliğini herkese doğrular.

### Institutions / Categories — `/api/institutions`, `/api/categories`

- `GET /` herkese açık, CRUD ADMIN'e özel.

### Audit — `/api/audit` (ADMIN)

`GET /api/audit/logs` — kullanıcı/etkinlik/kurum üzerindeki değişikliklerin denetim kaydı.

## Etkinlik Akışı

1. Kullanıcı etkinlik oluşturur (`DRAFT`) → `submit-for-review` ile `PENDING_REVIEW`'a geçer.
2. ADMIN onaylar (`PUBLISHED`) veya reddeder (`REJECTED`, sebep ile).
3. Katılımcılar `PUBLISHED` etkinliğe kayıt olur, her kayıt için benzersiz bir `qrToken` üretilir.
4. Etkinlik günü, organizer katılımcının QR'ını `POST /api/checkins/scan` ile tarar; kayıt `CHECKED_IN`'e geçer.
5. Etkinlik `COMPLETED` olduğunda ve `certificateEnabled=true` ise, check-in yapmış katılımcılar için sertifika (benzersiz doğrulama kodu + PDF) üretilebilir; herkes `GET /api/verify/{code}` ile doğrulayabilir.
