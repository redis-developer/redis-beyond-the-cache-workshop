# syntax=docker/dockerfile:1.4
# =============================================================================
# Workshop Hub with Docker-in-Docker (DinD)
# This image contains the Hub app, Docker daemon, and all workshop source files
# =============================================================================

# Stage 1: Build the Workshop Hub application
FROM eclipse-temurin:21-jdk AS builder

# Install Node.js for frontend build
RUN apt-get update \
    && apt-get install -y nodejs npm \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace/java-springboot

# Copy Gradle wrapper and build files first (for caching)
COPY java-springboot/gradlew .
COPY java-springboot/gradle gradle
COPY java-springboot/settings.docker.gradle.kts settings.gradle.kts
COPY java-springboot/build.gradle.kts .
COPY java-springboot/buildSrc buildSrc

# Copy module build files
COPY java-springboot/workshop-hub/build.gradle.kts workshop-hub/
COPY java-springboot/workshop-infrastructure/build.gradle.kts workshop-infrastructure/

# Download Gradle dependencies (cached layer)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :workshop-hub:dependencies --no-daemon || true

# Copy shared frontend package for module frontends
COPY workshop-frontend-shared /workshop-frontend-shared
RUN ln -s /workshop-frontend-shared /workspace/workshop-frontend-shared

# Copy frontend package.json and install dependencies
COPY frontend/package*.json /workspace/frontend/
RUN --mount=type=cache,target=/root/.npm \
    cd /workspace/frontend && npm install

# Copy workshop module frontend package files and install dependencies
COPY java-springboot/1_session_management_frontend/frontend/package*.json 1_session_management_frontend/frontend/
COPY java-springboot/1_session_management_frontend/frontend/babel.config.js 1_session_management_frontend/frontend/
COPY java-springboot/2_full_text_search_frontend/frontend/package*.json 2_full_text_search_frontend/frontend/
COPY java-springboot/2_full_text_search_frontend/frontend/babel.config.js 2_full_text_search_frontend/frontend/
COPY java-springboot/3_distributed_locks_frontend/frontend/package*.json 3_distributed_locks_frontend/frontend/
COPY java-springboot/3_distributed_locks_frontend/frontend/babel.config.js 3_distributed_locks_frontend/frontend/
COPY java-springboot/4_agent_memory_frontend/frontend/package*.json 4_agent_memory_frontend/frontend/
COPY java-springboot/4_agent_memory_frontend/frontend/babel.config.js 4_agent_memory_frontend/frontend/
RUN --mount=type=cache,target=/root/.npm \
    cd 1_session_management_frontend/frontend && npm install
RUN --mount=type=cache,target=/root/.npm \
    cd 2_full_text_search_frontend/frontend && npm install
RUN --mount=type=cache,target=/root/.npm \
    cd 3_distributed_locks_frontend/frontend && npm install
RUN --mount=type=cache,target=/root/.npm \
    cd 4_agent_memory_frontend/frontend && npm install

# Copy frontend source and build
COPY frontend/src /workspace/frontend/src
COPY frontend/public /workspace/frontend/public
COPY frontend/vue.config.js /workspace/frontend/

# Build frontend
RUN --mount=type=cache,target=/root/.npm \
    mkdir -p /workspace/java-springboot/workshop-hub/src/main/resources/static/vue \
    && cd /workspace/frontend && npm run build

# Copy frontend build to Spring Boot static resources
RUN mkdir -p workshop-hub/src/main/resources/static/vue \
    && cp -r /workspace/frontend/dist/* workshop-hub/src/main/resources/static/vue/

# Copy workshop module frontend sources and build assets
COPY java-springboot/1_session_management_frontend/src 1_session_management_frontend/src
COPY java-springboot/1_session_management_frontend/frontend/src 1_session_management_frontend/frontend/src
COPY java-springboot/1_session_management_frontend/frontend/public 1_session_management_frontend/frontend/public
COPY java-springboot/1_session_management_frontend/frontend/vue.config.js 1_session_management_frontend/frontend/
COPY java-springboot/2_full_text_search_frontend/src 2_full_text_search_frontend/src
COPY java-springboot/2_full_text_search_frontend/frontend/src 2_full_text_search_frontend/frontend/src
COPY java-springboot/2_full_text_search_frontend/frontend/public 2_full_text_search_frontend/frontend/public
COPY java-springboot/2_full_text_search_frontend/frontend/vue.config.js 2_full_text_search_frontend/frontend/
COPY java-springboot/3_distributed_locks_frontend/src 3_distributed_locks_frontend/src
COPY java-springboot/3_distributed_locks_frontend/frontend/src 3_distributed_locks_frontend/frontend/src
COPY java-springboot/3_distributed_locks_frontend/frontend/public 3_distributed_locks_frontend/frontend/public
COPY java-springboot/3_distributed_locks_frontend/frontend/vue.config.js 3_distributed_locks_frontend/frontend/
COPY java-springboot/4_agent_memory_frontend/src 4_agent_memory_frontend/src
COPY java-springboot/4_agent_memory_frontend/frontend/src 4_agent_memory_frontend/frontend/src
COPY java-springboot/4_agent_memory_frontend/frontend/public 4_agent_memory_frontend/frontend/public
COPY java-springboot/4_agent_memory_frontend/frontend/vue.config.js 4_agent_memory_frontend/frontend/
ENV VUE_APP_BASE_PATH=/
RUN --mount=type=cache,target=/root/.npm \
    cd 1_session_management_frontend/frontend && npm run build
RUN --mount=type=cache,target=/root/.npm \
    cd 2_full_text_search_frontend/frontend && npm run build
RUN --mount=type=cache,target=/root/.npm \
    cd 3_distributed_locks_frontend/frontend && npm run build
RUN --mount=type=cache,target=/root/.npm \
    cd 4_agent_memory_frontend/frontend && npm run build

# Copy source code
COPY java-springboot/workshop-hub/src workshop-hub/src
COPY java-springboot/workshop-infrastructure/src workshop-infrastructure/src

# Build the application
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :workshop-hub:bootJar -x test --no-daemon

# =============================================================================
# Stage 2: Runtime image with full Docker daemon (DinD)
# =============================================================================
FROM docker:24-dind

# Install JDK and docker-compose
RUN apk add --no-cache openjdk21-jre bash curl docker-cli-compose

WORKDIR /app

# Copy the built JAR
COPY --from=builder /workspace/java-springboot/workshop-hub/build/libs/workshop-hub-1.0.0.jar app.jar

# Copy the docker-compose file for DinD mode
COPY java-springboot/workshop-hub/docker-compose.internal.yml /app/docker-compose.internal.yml

# Copy all workshop source files (for building workshops inside DinD)
COPY . /workshops

# Ensure prebuilt workshop frontends are present in the build context
COPY --from=builder /workspace/java-springboot/1_session_management_frontend/src/main/resources/static /workshops/java-springboot/1_session_management_frontend/src/main/resources/static
COPY --from=builder /workspace/java-springboot/2_full_text_search_frontend/src/main/resources/static /workshops/java-springboot/2_full_text_search_frontend/src/main/resources/static
COPY --from=builder /workspace/java-springboot/3_distributed_locks_frontend/src/main/resources/static /workshops/java-springboot/3_distributed_locks_frontend/src/main/resources/static
COPY --from=builder /workspace/java-springboot/4_agent_memory_frontend/src/main/resources/static /workshops/java-springboot/4_agent_memory_frontend/src/main/resources/static

# Copy the startup script
COPY java-springboot/workshop-hub/scripts/entrypoint.sh /entrypoint.sh
RUN chmod +x /entrypoint.sh

# Environment variables for container (DinD) mode
ENV WORKSHOP_ROOT_PATH=/workshops
ENV WORKSHOP_LOCAL_MODE=false
ENV WORKSHOP_COMPOSE_DIR=/app
ENV DOCKER_HOST=unix:///var/run/docker.sock

# Expose ports: 9000 for Hub
EXPOSE 9000

# Use our custom entrypoint that starts Docker daemon and then the app
ENTRYPOINT ["/entrypoint.sh"]
