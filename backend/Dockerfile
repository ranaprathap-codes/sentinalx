# Build stage
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle
COPY sentinelx-shared/build.gradle.kts ./sentinelx-shared/
COPY sentinx-auth/build.gradle.kts ./sentinelx-auth/
COPY sentinelx-payments/build.gradle.kts ./sentinelx-payments/
COPY sentinelx-risk/build.gradle.kts ./sentinelx-risk/
COPY sentinelx-threatlab/build.gradle.kts ./sentinelx-threatlab/
COPY sentinelx-community/build.gradle.kts ./sentinelx-community/
COPY sentinelx-dryrun/build.gradle.kts ./sentinelx-dryrun/
COPY sentinelx-ops/build.gradle.kts ./sentinelx-ops/
COPY sentinelx-ai/build.gradle.kts ./sentinelx-ai/
COPY sentinelx-app/build.gradle.kts ./sentinelx-app/

RUN gradle dependencies --no-daemon

COPY . .

RUN gradle :sentinelx-app:bootJar --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -g 1000 -S sentinelx && \
    adduser -u 1000 -S sentinelx -G sentinelx

COPY --from=builder /app/sentinelx-app/build/libs/*.jar app.jar

RUN chown -R sentinelx:sentinelx /app

USER sentinelx

EXPOSE 8080

ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]