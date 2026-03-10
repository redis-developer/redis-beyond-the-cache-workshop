# syntax=docker/dockerfile:1.4

FROM eclipse-temurin:21-jdk AS builder

ARG SKIP_FRONTEND_BUILD=false

RUN if [ "$SKIP_FRONTEND_BUILD" != "true" ]; then \
        apt-get update \
        && apt-get install -y nodejs npm \
        && rm -rf /var/lib/apt/lists/*; \
    fi

WORKDIR /workspace/java-springboot

COPY java-springboot/gradlew .
COPY java-springboot/gradle gradle
COPY java-springboot/build.gradle.kts .
COPY java-springboot/3_distributed_locks_frontend/settings.gradle.kts .
COPY java-springboot/buildSrc buildSrc
COPY java-springboot/3_distributed_locks_frontend/build.gradle.kts 3_distributed_locks_frontend/
COPY java-springboot/workshop-infrastructure/build.gradle.kts workshop-infrastructure/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :3_distributed_locks_frontend:dependencies --no-daemon || true

COPY workshop-frontend-shared /workshop-frontend-shared
RUN ln -s /workshop-frontend-shared /workspace/workshop-frontend-shared

COPY java-springboot/3_distributed_locks_frontend/frontend/package*.json 3_distributed_locks_frontend/frontend/
COPY java-springboot/3_distributed_locks_frontend/frontend/babel.config.js 3_distributed_locks_frontend/frontend/

RUN --mount=type=cache,target=/root/.npm \
    if [ "$SKIP_FRONTEND_BUILD" != "true" ]; then \
        cd 3_distributed_locks_frontend/frontend && npm install; \
    fi

COPY java-springboot/3_distributed_locks_frontend/src 3_distributed_locks_frontend/src
COPY java-springboot/3_distributed_locks_frontend/frontend/src 3_distributed_locks_frontend/frontend/src
COPY java-springboot/3_distributed_locks_frontend/frontend/public 3_distributed_locks_frontend/frontend/public
COPY java-springboot/3_distributed_locks_frontend/frontend/vue.config.js 3_distributed_locks_frontend/frontend/
COPY java-springboot/workshop-infrastructure/src workshop-infrastructure/src

RUN if [ "$SKIP_FRONTEND_BUILD" = "true" ]; then \
        test -f 3_distributed_locks_frontend/src/main/resources/static/index.html; \
    fi

ARG VUE_APP_BASE_PATH=/
ENV VUE_APP_BASE_PATH=${VUE_APP_BASE_PATH}
RUN --mount=type=cache,target=/root/.npm \
    if [ "$SKIP_FRONTEND_BUILD" != "true" ]; then \
        cd 3_distributed_locks_frontend/frontend && npm run build; \
    fi

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :3_distributed_locks_frontend:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/java-springboot/3_distributed_locks_frontend/build/libs/3_distributed_locks_frontend-0.0.1-SNAPSHOT.jar app.jar

ENV WORKSHOP_SOURCE_PATH=/workshop-sources
ENV WORKSHOP_BASE_PATH=/workshop-sources

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
