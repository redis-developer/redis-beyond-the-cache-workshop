# syntax=docker/dockerfile:1.4

FROM eclipse-temurin:21-jdk AS builder

ARG SOURCE_REVISION=local

WORKDIR /workspace/java-springboot

COPY java-springboot/gradlew .
COPY java-springboot/gradle gradle
COPY java-springboot/build.gradle.kts .
COPY java-springboot/buildSrc buildSrc

RUN chmod +x gradlew \
    && printf '%s\n' \
      'rootProject.name = "redis-platform-execution-plane-image"' \
      'include("platform_contracts")' \
      'include("platform_execution_plane")' \
      > settings.gradle.kts

COPY java-springboot/platform_contracts/build.gradle.kts platform_contracts/
COPY java-springboot/platform_execution_plane/build.gradle.kts platform_execution_plane/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :platform_execution_plane:dependencies --no-daemon || true

COPY java-springboot/platform_contracts/src platform_contracts/src
COPY java-springboot/platform_execution_plane/src platform_execution_plane/src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :platform_execution_plane:bootJar -x test --no-daemon \
    && find platform_execution_plane/build/libs -name '*.jar' ! -name '*-plain.jar' -exec cp {} /tmp/app.jar \;

FROM eclipse-temurin:21-jre

ARG SOURCE_REVISION=local

WORKDIR /app

COPY --from=builder /tmp/app.jar /app/app.jar

LABEL org.opencontainers.image.title="Redis Workshop Platform Execution Plane"
LABEL org.opencontainers.image.description="Cloud Run execution plane for Redis workshop session services"
LABEL org.opencontainers.image.source-revision="${SOURCE_REVISION}"

ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
