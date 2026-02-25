# syntax=docker/dockerfile:1.4
# Multi-stage Dockerfile for the 3_distributed_locks workshop

FROM eclipse-temurin:21-jdk AS builder

ARG SKIP_FRONTEND_BUILD=false

# Install Node.js and npm for building the Vue frontend
RUN if [ "$SKIP_FRONTEND_BUILD" != "true" ]; then \
        apt-get update \
        && apt-get install -y nodejs npm \
        && rm -rf /var/lib/apt/lists/*; \
    fi

WORKDIR /workspace/java-springboot

# Copy Gradle wrapper and configuration files first (for caching)
COPY java-springboot/gradlew .
COPY java-springboot/gradle gradle
COPY java-springboot/build.gradle.kts .
COPY java-springboot/3_distributed_locks/settings.gradle.kts .

# Copy buildSrc for convention plugins
COPY java-springboot/buildSrc buildSrc

# Copy only the build.gradle.kts files for dependency resolution
COPY java-springboot/3_distributed_locks/build.gradle.kts 3_distributed_locks/
COPY java-springboot/workshop-infrastructure/build.gradle.kts workshop-infrastructure/

# Download Gradle dependencies (this layer will be cached)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :3_distributed_locks:dependencies --no-daemon || true

# Copy shared frontend package (required dependency)
COPY workshop-frontend-shared /workshop-frontend-shared
RUN ln -s /workshop-frontend-shared /workspace/workshop-frontend-shared

# Copy frontend package files for npm dependency caching
COPY java-springboot/3_distributed_locks/frontend/package*.json 3_distributed_locks/frontend/
COPY java-springboot/3_distributed_locks/frontend/babel.config.js 3_distributed_locks/frontend/

# Download npm dependencies (this layer will be cached)
RUN --mount=type=cache,target=/root/.npm \
    if [ "$SKIP_FRONTEND_BUILD" != "true" ]; then \
        cd 3_distributed_locks/frontend && npm install; \
    fi

# Copy the source code
COPY java-springboot/3_distributed_locks/src 3_distributed_locks/src
COPY java-springboot/3_distributed_locks/frontend/src 3_distributed_locks/frontend/src
COPY java-springboot/3_distributed_locks/frontend/public 3_distributed_locks/frontend/public
COPY java-springboot/3_distributed_locks/frontend/vue.config.js 3_distributed_locks/frontend/
COPY java-springboot/workshop-infrastructure/src workshop-infrastructure/src

# If skipping, ensure prebuilt assets are present in the repo.
RUN if [ "$SKIP_FRONTEND_BUILD" = "true" ]; then \
        test -f 3_distributed_locks/src/main/resources/static/index.html; \
    fi

# Build Vue frontend with configurable base path
ARG VUE_APP_BASE_PATH=/
ENV VUE_APP_BASE_PATH=${VUE_APP_BASE_PATH}
RUN --mount=type=cache,target=/root/.npm \
    if [ "$SKIP_FRONTEND_BUILD" != "true" ]; then \
        cd 3_distributed_locks/frontend && npm run build; \
    fi

# Build only the 3_distributed_locks module jar (skip tests for speed)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :3_distributed_locks:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the Spring Boot fat jar from the build stage
COPY --from=builder /workspace/java-springboot/3_distributed_locks/build/libs/3_distributed_locks-0.0.1-SNAPSHOT.jar app.jar

# Copy editable source files for in-browser editing (used in containerized mode)
COPY --from=builder /workspace/java-springboot/3_distributed_locks/build.gradle.kts /workshop-sources/build.gradle.kts
COPY --from=builder /workspace/java-springboot/3_distributed_locks/src/main/resources/application.properties /workshop-sources/src/main/resources/application.properties
COPY --from=builder /workspace/java-springboot/3_distributed_locks/src/main/java/com/redis/workshop/locks/service/LockManager.java /workshop-sources/src/main/java/com/redis/workshop/locks/service/LockManager.java
COPY --from=builder /workspace/java-springboot/3_distributed_locks/src/main/java/com/redis/workshop/locks/service/PurchaseService.java /workshop-sources/src/main/java/com/redis/workshop/locks/service/PurchaseService.java

# Set environment variable to point to the editable source files
ENV WORKSHOP_BASE_PATH=/workshop-sources

EXPOSE 8082

ENTRYPOINT ["java", "-jar", "app.jar"]
