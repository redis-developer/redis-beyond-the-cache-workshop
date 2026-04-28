# syntax=docker/dockerfile:1.4

FROM eclipse-temurin:21-jdk AS builder

ARG SOURCE_REVISION=local
ARG SESSION_MANAGEMENT_RUNNER_IMAGE=
ARG HUB_ACTOR_ID=raphael-gcp-test
ARG HUB_ACTOR_TYPE=LEARNER
ARG HUB_ACTOR_ROLES=platform:learner

RUN apt-get update \
    && apt-get install -y nodejs npm \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace/java-springboot

COPY java-springboot/gradlew .
COPY java-springboot/gradle gradle
COPY java-springboot/build.gradle.kts .
COPY java-springboot/buildSrc buildSrc

RUN chmod +x gradlew \
    && printf '%s\n' \
      'rootProject.name = "redis-platform-control-plane-image"' \
      'include("platform_control_plane")' \
      > settings.gradle.kts

COPY java-springboot/platform_control_plane/build.gradle.kts platform_control_plane/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :platform_control_plane:dependencies --no-daemon || true

COPY workshop-frontend-shared /workspace/workshop-frontend-shared
COPY frontend/package*.json /workspace/frontend/

RUN --mount=type=cache,target=/root/.npm \
    cd /workspace/frontend && npm ci

COPY frontend/src /workspace/frontend/src
COPY frontend/public /workspace/frontend/public
COPY frontend/vue.config.js /workspace/frontend/

RUN --mount=type=cache,target=/root/.npm \
    cd /workspace/frontend \
    && VUE_APP_ACTOR_ID="$HUB_ACTOR_ID" \
      VUE_APP_ACTOR_TYPE="$HUB_ACTOR_TYPE" \
      VUE_APP_ACTOR_ROLES="$HUB_ACTOR_ROLES" \
      npx vue-cli-service build

COPY java-springboot/platform_control_plane/src platform_control_plane/src
COPY workshops.yaml platform_control_plane/src/main/resources/workshops.yaml

RUN mkdir -p platform_control_plane/src/main/resources/static \
    && cp -R /workspace/frontend/dist/. platform_control_plane/src/main/resources/static/

RUN if [ -n "$SESSION_MANAGEMENT_RUNNER_IMAGE" ]; then \
      sed -i "0,/      combined: .*/s|      combined: .*|      combined: ${SESSION_MANAGEMENT_RUNNER_IMAGE}|" \
        platform_control_plane/src/main/resources/workshops.yaml; \
    fi

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :platform_control_plane:bootJar -x test --no-daemon \
    && find platform_control_plane/build/libs -name '*.jar' ! -name '*-plain.jar' -exec cp {} /tmp/app.jar \;

FROM eclipse-temurin:21-jre

ARG SOURCE_REVISION=local

WORKDIR /app

COPY --from=builder /tmp/app.jar /app/app.jar

LABEL org.opencontainers.image.title="Redis Workshop Platform Control Plane"
LABEL org.opencontainers.image.description="Cloud Run control plane for Redis workshop sessions"
LABEL org.opencontainers.image.source-revision="${SOURCE_REVISION}"

ENV SERVER_PORT=8080
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
