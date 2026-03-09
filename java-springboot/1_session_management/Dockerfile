# syntax=docker/dockerfile:1.4
# Multi-stage Dockerfile for the 1_session_management workshop

# 1) Build stage: build Spring Boot jar (includes Vue frontend)
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
COPY java-springboot/1_session_management/settings.gradle.kts .

# Copy buildSrc for convention plugins
COPY java-springboot/buildSrc buildSrc

# Copy only the build.gradle.kts files for dependency resolution
COPY java-springboot/1_session_management/build.gradle.kts 1_session_management/
COPY java-springboot/workshop-infrastructure/build.gradle.kts workshop-infrastructure/
COPY java-springboot/workshop-hub/build.gradle.kts workshop-hub/

# Download Gradle dependencies (this layer will be cached)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :1_session_management:dependencies --no-daemon || true

# Copy shared frontend package (required dependency)
# Two different paths are needed:
# 1. npm package.json uses file:../../../../workshop-frontend-shared from /workspace/java-springboot/1_session_management/frontend/
#    -> resolves to /workshop-frontend-shared
# 2. CSS imports use ../../../../workshop-frontend-shared from /workspace/java-springboot/1_session_management/frontend/src/
#    -> resolves to /workspace/workshop-frontend-shared
COPY workshop-frontend-shared /workshop-frontend-shared
RUN ln -s /workshop-frontend-shared /workspace/workshop-frontend-shared

# Copy frontend package files for npm dependency caching
COPY java-springboot/1_session_management/frontend/package*.json 1_session_management/frontend/
COPY java-springboot/1_session_management/frontend/babel.config.js 1_session_management/frontend/

# Download npm dependencies (this layer will be cached)
RUN --mount=type=cache,target=/root/.npm \
    if [ "$SKIP_FRONTEND_BUILD" != "true" ]; then \
        cd 1_session_management/frontend && npm install; \
    fi

# Now copy the rest of the source code
COPY java-springboot/1_session_management/src 1_session_management/src
COPY java-springboot/1_session_management/frontend/src 1_session_management/frontend/src
COPY java-springboot/1_session_management/frontend/public 1_session_management/frontend/public
COPY java-springboot/1_session_management/frontend/vue.config.js 1_session_management/frontend/
COPY java-springboot/workshop-infrastructure/src workshop-infrastructure/src
COPY java-springboot/workshop-hub/src workshop-hub/src

# If skipping, ensure prebuilt assets are present in the repo.
RUN if [ "$SKIP_FRONTEND_BUILD" = "true" ]; then \
        test -f 1_session_management/src/main/resources/static/index.html; \
    fi

# Build Vue frontend with configurable base path
# Default: / (root path) - works both standalone and via Hub proxy (which rewrites paths)
ARG VUE_APP_BASE_PATH=/
ENV VUE_APP_BASE_PATH=${VUE_APP_BASE_PATH}
RUN --mount=type=cache,target=/root/.npm \
    if [ "$SKIP_FRONTEND_BUILD" != "true" ]; then \
        cd 1_session_management/frontend && npm run build; \
    fi

# Build only the 1_session_management module jar (skip tests for speed)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :1_session_management:bootJar -x test --no-daemon

# 2) Runtime stage: thin image just to run the built jar
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the Spring Boot fat jar from the build stage
COPY --from=builder /workspace/java-springboot/1_session_management/build/libs/1_session_management-0.0.1-SNAPSHOT.jar app.jar

# Copy editable source files for in-browser editing (used in containerized mode)
# These files can be modified at runtime via the code editor
COPY --from=builder /workspace/java-springboot/1_session_management/build.gradle.kts /workshop-sources/build.gradle.kts
COPY --from=builder /workspace/java-springboot/1_session_management/src/main/resources/application.properties /workshop-sources/src/main/resources/application.properties
COPY --from=builder /workspace/java-springboot/1_session_management/src/main/java/com/redis/workshop/session/config/SecurityConfig.java /workshop-sources/src/main/java/com/redis/workshop/session/config/SecurityConfig.java

# Set environment variable to point to the editable source files
# This can be overridden at runtime for local development
ENV WORKSHOP_BASE_PATH=/workshop-sources

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
