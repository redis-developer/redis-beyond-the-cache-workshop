# syntax=docker/dockerfile:1.4

FROM eclipse-temurin:21-jdk AS builder

WORKDIR /workspace/java-springboot

COPY java-springboot/gradlew .
COPY java-springboot/gradle gradle
COPY java-springboot/build.gradle.kts .
COPY java-springboot/3_distributed_locks/settings.gradle.kts .
COPY java-springboot/buildSrc buildSrc
COPY java-springboot/3_distributed_locks/build.gradle.kts 3_distributed_locks/
COPY java-springboot/workshop-infrastructure/build.gradle.kts workshop-infrastructure/

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :3_distributed_locks:dependencies --no-daemon || true

COPY java-springboot/3_distributed_locks/src 3_distributed_locks/src

RUN --mount=type=cache,target=/root/.gradle \
    ./gradlew :3_distributed_locks:bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=builder /workspace/java-springboot/3_distributed_locks/build/libs/3_distributed_locks-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 18082

ENTRYPOINT ["java", "-jar", "app.jar"]
