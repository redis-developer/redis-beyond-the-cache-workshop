# syntax=docker/dockerfile:1.4

FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace/java-springboot

COPY java-springboot/gradlew .
COPY java-springboot/gradle gradle
COPY java-springboot/build.gradle.kts .
COPY java-springboot/settings.gradle.kts .
COPY java-springboot/buildSrc buildSrc
COPY java-springboot/workshop-infrastructure/build.gradle.kts workshop-infrastructure/
COPY java-springboot/spring_ai_multi_agent_course/3_multi_agent_patterns/build.gradle.kts spring_ai_multi_agent_course/3_multi_agent_patterns/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :3_multi_agent_patterns:dependencies --no-daemon || true

COPY java-springboot/workshop-infrastructure/src workshop-infrastructure/src
COPY java-springboot/spring_ai_multi_agent_course/3_multi_agent_patterns/src spring_ai_multi_agent_course/3_multi_agent_patterns/src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :3_multi_agent_patterns:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/java-springboot/spring_ai_multi_agent_course/3_multi_agent_patterns/build/libs/3_multi_agent_patterns-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 18086

ENTRYPOINT ["java", "-jar", "app.jar"]
