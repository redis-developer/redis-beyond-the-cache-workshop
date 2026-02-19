# Multi-stage Dockerfile for the 1_session_management workshop

# 1) Build stage: build Spring Boot jar (includes Vue frontend)
FROM eclipse-temurin:21-jdk AS builder

# Install Node.js and npm for building the Vue frontend
RUN apt-get update \
    && apt-get install -y nodejs npm \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /workspace/java-springboot

# Copy Gradle wrapper and configuration files first (for caching)
COPY java-springboot/gradlew .
COPY java-springboot/gradle gradle
COPY java-springboot/settings.gradle.kts .
COPY java-springboot/build.gradle.kts .

# Copy buildSrc for convention plugins
COPY java-springboot/buildSrc buildSrc

# Copy only the build.gradle.kts files for dependency resolution
COPY java-springboot/1_session_management/build.gradle.kts 1_session_management/
COPY java-springboot/workshop-infrastructure/build.gradle.kts workshop-infrastructure/
COPY java-springboot/workshop-hub/build.gradle.kts workshop-hub/

# Download Gradle dependencies (this layer will be cached)
RUN ./gradlew :1_session_management:dependencies --no-daemon || true

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
RUN cd 1_session_management/frontend && npm install

# Now copy the rest of the source code
COPY java-springboot/1_session_management/src 1_session_management/src
COPY java-springboot/1_session_management/frontend/src 1_session_management/frontend/src
COPY java-springboot/1_session_management/frontend/public 1_session_management/frontend/public
COPY java-springboot/1_session_management/frontend/vue.config.js 1_session_management/frontend/
COPY java-springboot/workshop-infrastructure/src workshop-infrastructure/src
COPY java-springboot/workshop-hub/src workshop-hub/src

# Build Vue frontend with proxy base path
# This sets the publicPath so assets work via the Hub proxy
ARG VUE_APP_BASE_PATH=/workshop/session-management/
ENV VUE_APP_BASE_PATH=${VUE_APP_BASE_PATH}
RUN cd 1_session_management/frontend && npm run build

# Build only the 1_session_management module jar (skip tests for speed)
RUN ./gradlew :1_session_management:bootJar -x test --no-daemon

# 2) Runtime stage: thin image just to run the built jar
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the Spring Boot fat jar from the build stage
COPY --from=builder /workspace/java-springboot/1_session_management/build/libs/1_session_management-0.0.1-SNAPSHOT.jar app.jar

# Source files for in-browser editing are provided via volume mount in docker-compose.yml
# The WORKSHOP_BASE_PATH environment variable tells the app where to find them

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]

