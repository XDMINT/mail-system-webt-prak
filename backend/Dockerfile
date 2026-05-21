FROM eclipse-temurin:25-jdk-noble AS builder
LABEL authors="timguenther"

WORKDIR /workspace

COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts ./
COPY gradle/ gradle/
COPY backend/build.gradle.kts backend/build.gradle.kts
COPY frontend/build.gradle.kts frontend/build.gradle.kts
COPY backend/src/ backend/src/

RUN chmod +x gradlew \
    && ./gradlew :backend:bootJar --no-daemon

FROM eclipse-temurin:25-jre-noble

WORKDIR /mail-system

COPY --from=builder /workspace/backend/build/libs/*.jar /mail-system/app.jar

RUN addgroup --system backend \
    && adduser --system --ingroup backend --no-create-home backend \
    && chown -R backend:backend /mail-system

USER backend

EXPOSE 8080

CMD ["java", "-jar", "/mail-system/app.jar"]
