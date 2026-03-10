# syntax=docker/dockerfile:1.4

FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace/java-springboot

COPY java-springboot/gradlew .
COPY java-springboot/gradle gradle
COPY java-springboot/build.gradle.kts .
COPY java-springboot/4_agent_memory/settings.gradle.kts .
COPY java-springboot/buildSrc buildSrc
COPY java-springboot/4_agent_memory/build.gradle.kts 4_agent_memory/
COPY java-springboot/workshop-infrastructure/build.gradle.kts workshop-infrastructure/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :4_agent_memory:dependencies --no-daemon || true

COPY java-springboot/4_agent_memory/src 4_agent_memory/src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :4_agent_memory:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/java-springboot/4_agent_memory/build/libs/4_agent_memory-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 18083

ENTRYPOINT ["java", "-jar", "app.jar"]
