# syntax=docker/dockerfile:1.4

FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace/java-springboot

COPY java-springboot/gradlew .
COPY java-springboot/gradle gradle
COPY java-springboot/build.gradle.kts .
COPY java-springboot/1_session_management/settings.gradle.kts .
COPY java-springboot/buildSrc buildSrc
COPY java-springboot/1_session_management/build.gradle.kts 1_session_management/
COPY java-springboot/workshop-infrastructure/build.gradle.kts workshop-infrastructure/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :1_session_management:dependencies --no-daemon || true

COPY java-springboot/1_session_management/src 1_session_management/src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :1_session_management:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/java-springboot/1_session_management/build/libs/1_session_management-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 18080

ENTRYPOINT ["java", "-jar", "app.jar"]
