# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# 1) Build stage — Maven ile fat-jar üretir.
#    pom.xml önce tek başına kopyalanıp bağımlılıklar indirilir; src/ değiştiğinde
#    bu katman cache'ten gelir, yalnızca kod derlemesi tekrar çalışır.
# ---------------------------------------------------------------------------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /build

# Optional build secret for local HTTPS-inspection roots. It never becomes an image layer.
RUN --mount=type=secret,id=trusted_ca,required=false \
    if [ -s /run/secrets/trusted_ca ]; then \
      keytool -importcert -noprompt -trustcacerts -alias eventhub-build-ca \
        -file /run/secrets/trusted_ca -cacerts -storepass changeit; \
    fi

COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline

COPY src src
RUN mvn -B -ntp clean package -DskipTests \
    && cp target/event-hub-*.jar app.jar

# ---------------------------------------------------------------------------
# 2) Runtime stage — yalnızca JRE + üretilen jar.
#    NOT: javacv/ffmpeg-platform native kütüphaneleri glibc gerektirir, bu yüzden
#    Alpine (musl) yerine Debian/Ubuntu tabanlı bir JRE imajı kullanılıyor.
# ---------------------------------------------------------------------------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

RUN groupadd --system eventhub && useradd --system --gid eventhub --no-create-home eventhub \
    && mkdir -p /app-logs && chown -R eventhub:eventhub /app-logs /app

COPY --from=build --chown=eventhub:eventhub /build/app.jar app.jar

# docker-stack/docker-compose.yml içindeki servis adları/portlarıyla eşleşen
# varsayılanlar — application.yaml bu değerleri ${VAR:default} olarak okuyor.
ENV LOG_PATH=/app-logs \
    DB_HOST=postgres \
    DB_PORT=5432 \
    REDIS_HOST=redis \
    REDIS_PORT=6379 \
    JAVA_OPTS=""

EXPOSE 8080

USER eventhub

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=5 \
    CMD ["bash", "-c", "exec 3<>/dev/tcp/127.0.0.1/8080 || exit 1"]

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
