plugins {
    java
    id("org.springframework.boot")
    id("io.spring.dependency-management")
}

group = "com.redis.workshop"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":workshop-infrastructure"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")

    // TODO: Add Redis OM Spring dependency for full-text search
    // implementation("com.redis.om:redis-om-spring:1.0.4")
    // annotationProcessor("com.redis.om:redis-om-spring:1.0.4")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
