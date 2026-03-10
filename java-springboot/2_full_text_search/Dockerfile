# syntax=docker/dockerfile:1.4

FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace/java-springboot

COPY java-springboot/gradlew .
COPY java-springboot/gradle gradle
COPY java-springboot/build.gradle.kts .
COPY java-springboot/2_full_text_search/settings.gradle.kts .
COPY java-springboot/buildSrc buildSrc
COPY java-springboot/2_full_text_search/build.gradle.kts 2_full_text_search/
COPY java-springboot/workshop-infrastructure/build.gradle.kts workshop-infrastructure/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :2_full_text_search:dependencies --no-daemon || true

COPY java-springboot/2_full_text_search/src 2_full_text_search/src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :2_full_text_search:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/java-springboot/2_full_text_search/build/libs/2_full_text_search-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 18081

ENTRYPOINT ["java", "-jar", "app.jar"]
