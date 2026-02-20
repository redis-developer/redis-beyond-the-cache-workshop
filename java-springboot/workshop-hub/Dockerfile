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
RUN ./gradlew :workshop-hub:dependencies --no-daemon || true

# Copy shared frontend package
COPY workshop-frontend-shared /workspace/workshop-frontend-shared

# Copy frontend package.json and install dependencies
COPY frontend/package*.json /workspace/frontend/
RUN cd /workspace/frontend && npm install

# Copy frontend source and build
COPY frontend/src /workspace/frontend/src
COPY frontend/public /workspace/frontend/public
COPY frontend/vue.config.js /workspace/frontend/

# Build frontend
RUN mkdir -p /workspace/java-springboot/workshop-hub/src/main/resources/static/vue \
    && cd /workspace/frontend && npm run build

# Copy frontend build to Spring Boot static resources
RUN mkdir -p workshop-hub/src/main/resources/static/vue \
    && cp -r /workspace/frontend/dist/* workshop-hub/src/main/resources/static/vue/

# Copy source code
COPY java-springboot/workshop-hub/src workshop-hub/src
COPY java-springboot/workshop-infrastructure/src workshop-infrastructure/src

# Build the application
RUN ./gradlew :workshop-hub:bootJar -x test --no-daemon

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

