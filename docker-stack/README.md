# EventHub - Postgres + Elasticsearch + Grafana Stack

## Çalıştırma

```bash
cd docker-stack
docker compose up -d
```

Servisler ayağa kalktıktan sonra (Elasticsearch ilk açılışta ~30-60 sn sürebilir):

| Servis        | URL                       | Kullanıcı/Şifre        |
|---------------|---------------------------|-------------------------|
| Frontend      | http://localhost:8081     | (uygulama içi giriş)     |
| Backend API   | http://localhost:8080     | (uygulama içi giriş)     |
| Postgres      | localhost:5432            | postgres / (.env DB_PASSWORD) |
| Elasticsearch | http://localhost:9200     | (auth kapalı, dev amaçlı) |
| Kibana        | http://localhost:5601     | -                        |
| Grafana       | http://localhost:3000     | admin / admin            |

`eventhub-frontend` servisi, `../../eventhub-frontend` klasöründeki React
uygulamasını Vite ile derleyip nginx ile sunar; nginx `/api/*` isteklerini aynı origin
üzerinden `eventhub-backend`'e proxy'ler (bkz. `eventhub-frontend/nginx.conf`) —
böylece HttpOnly auth cookie'leri cross-origin sorunu yaşamadan çalışır.

Grafana'ya girdiğinde **Elasticsearch-Logs** ve **Postgres-EventHub** datasource'ları
otomatik olarak tanımlı gelir (provisioning ile), elle datasource eklemene gerek yok.

## Spring Boot'tan Elasticsearch'e log gönderme (Filebeat ile)

Akış: **Spring Boot (Logback → JSON dosya) → Filebeat (dosyayı izler) → Elasticsearch
(`eventhub-logs-*` index) → Grafana**.

### 1. `pom.xml`'e bağımlılık ekle

```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### 2. `logback-spring.xml`'i projene kopyala

Bu repo'daki `spring-boot-config/logback-spring.xml` dosyasını Spring Boot projenin
`src/main/resources/` klasörüne kopyala. (Zaten bir `logback-spring.xml`'in varsa
appender kısımlarını mevcut dosyana entegre et.)

Bu dosya JSON formatında logları `${LOG_PATH:-app-logs}/app.json` altına yazar
(rolling policy ile günlük döner, 7 gün saklar).

### 3. Log dosya yolunun docker-compose ile eşleşmesi

Filebeat container'ı `./app-logs` klasörünü (docker-compose.yml'in bulunduğu yere göre)
okuyor. İki seçenek var:

- **Kolay yol:** Spring Boot uygulamanı `docker-stack/` klasörünün içinden çalıştır
  (çalışma dizini `docker-stack` olursa `app-logs` klasörü otomatik oraya düşer).
- **Esnek yol:** Uygulamayı nereden çalıştırırsan çalıştır, JVM argümanı ile hedef
  klasörü doğrudan `docker-stack/app-logs`'a yönlendir:
  ```bash
  java -DLOG_PATH=/mutlak/yol/docker-stack/app-logs -jar eventhub.jar
  ```
  IDE'den çalıştırıyorsan (IntelliJ) VM options'a `-DLOG_PATH=...` ekle.

`docker-stack/app-logs` klasörü ilk çalıştırmada otomatik oluşur, elle oluşturmana
gerek yok.

### 4. Ayağa kaldır

```bash
cd docker-stack
docker compose up -d
```

Sonra Spring Boot uygulamanı çalıştır. `app-logs/app.json` dosyası oluşur oluşmaz
Filebeat onu okuyup Elasticsearch'e basmaya başlar. Kontrol etmek için:

```bash
curl http://localhost:9200/eventhub-logs-*/_search?pretty
```

Kayıt görüyorsan Grafana → **Elasticsearch-Logs** datasource → Explore ekranından
logları sorgulayabilirsin.

### MDC ile correlation ID (opsiyonel ama önerilir)

`GlobalExceptionHandler`'da eklediğimiz `MDC.put("requestUri", ...)` gibi alanlar
`includeMdc: true` sayesinde otomatik olarak JSON log satırına dahil olur ve
Elasticsearch'te aranabilir/filtrelenebilir hale gelir. İstersen her istek için bir
`traceId` üreten bir `Filter`/`Interceptor` da ekleyebiliriz — isteğe bağlı bir sonraki
adım olarak söyleyebilirsin.

## Kapatma / temizleme

```bash
docker compose down          # container'ları durdurur, veriler kalır
docker compose down -v       # container + volume'ları siler (veriler kaybolur)
```
