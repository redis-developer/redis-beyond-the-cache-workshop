# syntax=docker/dockerfile:1.4
# Multi-stage Dockerfile for the 4_agent_memory workshop

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
COPY java-springboot/4_agent_memory/settings.gradle.kts .

# Copy buildSrc for convention plugins
COPY java-springboot/buildSrc buildSrc

# Copy only the build.gradle.kts files for dependency resolution
COPY java-springboot/4_agent_memory/build.gradle.kts 4_agent_memory/
COPY java-springboot/workshop-infrastructure/build.gradle.kts workshop-infrastructure/

# Download Gradle dependencies (this layer will be cached)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :4_agent_memory:dependencies --no-daemon || true

# Copy shared frontend package (required dependency)
COPY workshop-frontend-shared /workshop-frontend-shared
RUN ln -s /workshop-frontend-shared /workspace/workshop-frontend-shared

# Copy frontend package files for npm dependency caching
COPY java-springboot/4_agent_memory/frontend/package*.json 4_agent_memory/frontend/
COPY java-springboot/4_agent_memory/frontend/babel.config.js 4_agent_memory/frontend/

# Download npm dependencies (this layer will be cached)
RUN --mount=type=cache,target=/root/.npm \
    if [ "$SKIP_FRONTEND_BUILD" != "true" ]; then \
        cd 4_agent_memory/frontend && npm install; \
    fi

# Copy the source code
COPY java-springboot/4_agent_memory/src 4_agent_memory/src
COPY java-springboot/4_agent_memory/frontend/src 4_agent_memory/frontend/src
COPY java-springboot/4_agent_memory/frontend/public 4_agent_memory/frontend/public
COPY java-springboot/4_agent_memory/frontend/vue.config.js 4_agent_memory/frontend/
COPY java-springboot/workshop-infrastructure/src workshop-infrastructure/src

# Build Vue frontend with configurable base path
ARG VUE_APP_BASE_PATH=/
ENV VUE_APP_BASE_PATH=${VUE_APP_BASE_PATH}
RUN --mount=type=cache,target=/root/.npm \
    if [ "$SKIP_FRONTEND_BUILD" != "true" ]; then \
        cd 4_agent_memory/frontend && npm run build; \
    fi

# Build only the 4_agent_memory module jar (skip tests for speed)
RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :4_agent_memory:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

# Copy the Spring Boot fat jar from the build stage
COPY --from=builder /workspace/java-springboot/4_agent_memory/build/libs/4_agent_memory-0.0.1-SNAPSHOT.jar app.jar

# Copy editable source files for in-browser editing (used in containerized mode)
COPY --from=builder /workspace/java-springboot/4_agent_memory/build.gradle.kts /workshop-sources/build.gradle.kts
COPY --from=builder /workspace/java-springboot/4_agent_memory/src/main/resources/application.properties /workshop-sources/src/main/resources/application.properties
COPY --from=builder /workspace/java-springboot/4_agent_memory/src/main/java/com/redis/workshop/memory/service/AgentMemoryService.java /workshop-sources/src/main/java/com/redis/workshop/memory/service/AgentMemoryService.java

# Set environment variable to point to the editable source files
ENV WORKSHOP_BASE_PATH=/workshop-sources

EXPOSE 8083

ENTRYPOINT ["java", "-jar", "app.jar"]

