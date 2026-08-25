# 멀티스테이지 빌드 — 1단계에서 bootJar 를 만들고 2단계 JRE 에서만 실행한다.
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# 의존성 레이어 캐싱을 위해 빌드 스크립트를 먼저 복사한다.
COPY gradlew ./
COPY gradle ./gradle
COPY settings.gradle build.gradle gradle.properties ./
RUN chmod +x ./gradlew && ./gradlew --no-daemon dependencies > /dev/null 2>&1 || true

COPY src ./src
RUN ./gradlew --no-daemon bootJar

FROM eclipse-temurin:21-jre-alpine AS runtime

WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring

COPY --from=build /app/build/libs/*.jar app.jar

USER spring
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
