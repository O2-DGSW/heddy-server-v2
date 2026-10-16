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

FROM eclipse-temurin:21-jre AS runtime

WORKDIR /app

RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && groupadd --system spring \
    && useradd --system --gid spring spring

RUN mkdir -p /opt/heddy/photo-models \
    && curl --fail --location --retry 3 --max-time 180 \
       'https://huggingface.co/senty-au/selfie_multiclass_256x256-ONNX/resolve/6db8421a7150ac20558f2c24675078eb3a1a04d0/onnx/model.onnx' \
       -o /opt/heddy/photo-models/selfie.onnx \
    && echo '35ec1ecd9ee7f85073c99c00020b7f6751b69506eeacf683bc8665f6117f85b0  /opt/heddy/photo-models/selfie.onnx' | sha256sum -c -
COPY docs/photo-length-model-LICENSE.txt /opt/heddy/photo-models/LICENSE.txt
COPY docs/photo-length-model-NOTICE.txt /opt/heddy/photo-models/NOTICE.txt
COPY --from=build /app/build/libs/*.jar app.jar

USER spring
EXPOSE 8080

ENTRYPOINT ["java", "-Djava.awt.headless=true", "-jar", "/app/app.jar"]
