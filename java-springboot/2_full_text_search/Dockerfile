# syntax=docker/dockerfile:1.4
# Multi-stage Dockerfile for the 2_full_text_search workshop

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
COPY java-springboot/2_full_text_search/settings.gradle.kts .

# Copy buildSrc for convention plugins
COPY java-springboot/buildSrc buildSrc

# Copy only the build.gradle.kts files for dependency resolution
COPY java-springboot/2_full_text_search/build.gradle.kts 2_full_text_search/
COPY java-springboot/workshop-infrastructure/build.gradle.kts workshop-infrastructure/

# Download Gradle dependencies (this layer will be cached)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :2_full_text_search:dependencies --no-daemon || true

# Copy shared frontend package (required dependency)
COPY workshop-frontend-shared /workshop-frontend-shared
RUN ln -s /workshop-frontend-shared /workspace/workshop-frontend-shared

# Copy frontend package files for npm dependency caching
COPY java-springboot/2_full_text_search/frontend/package*.json 2_full_text_search/frontend/
COPY java-springboot/2_full_text_search/frontend/babel.config.js 2_full_text_search/frontend/

# Download npm dependencies (this layer will be cached)
RUN --mount=type=cache,target=/root/.npm \
    if [ "$SKIP_FRONTEND_BUILD" != "true" ]; then \
        cd 2_full_text_search/frontend && npm install; \
    fi

# Copy the source code
COPY java-springboot/2_full_text_search/src 2_full_text_search/src
COPY java-springboot/2_full_text_search/frontend/src 2_full_text_search/frontend/src
COPY java-springboot/2_full_text_search/frontend/public 2_full_text_search/frontend/public
COPY java-springboot/2_full_text_search/frontend/vue.config.js 2_full_text_search/frontend/
COPY java-springboot/workshop-infrastructure/src workshop-infrastructure/src

# If skipping, ensure prebuilt assets are present in the repo.
RUN if [ "$SKIP_FRONTEND_BUILD" = "true" ]; then \
        test -f 2_full_text_search/src/main/resources/static/index.html; \
    fi

# Build Vue frontend with configurable base path
ARG VUE_APP_BASE_PATH=/
ENV VUE_APP_BASE_PATH=${VUE_APP_BASE_PATH}
RUN --mount=type=cache,target=/root/.npm \
    if [ "$SKIP_FRONTEND_BUILD" != "true" ]; then \
        cd 2_full_text_search/frontend && npm run build; \
    fi

# Build only the 2_full_text_search module jar (skip tests for speed)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :2_full_text_search:bootJar -x test --no-daemon

# 2) Runtime stage: thin image just to run the built jar
FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the Spring Boot fat jar from the build stage
COPY --from=builder /workspace/java-springboot/2_full_text_search/build/libs/2_full_text_search-0.0.1-SNAPSHOT.jar app.jar

# Copy editable source files for in-browser editing (used in containerized mode)
COPY --from=builder /workspace/java-springboot/2_full_text_search/build.gradle.kts /workshop-sources/build.gradle.kts
COPY --from=builder /workspace/java-springboot/2_full_text_search/src/main/resources/application.properties /workshop-sources/src/main/resources/application.properties
COPY --from=builder /workspace/java-springboot/2_full_text_search/src/main/java/com/redis/workshop/search/domain/Movie.java /workshop-sources/src/main/java/com/redis/workshop/search/domain/Movie.java

# Set environment variable to point to the editable source files
ENV WORKSHOP_BASE_PATH=/workshop-sources

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
