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
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://repo.spring.io/milestone") }
    maven { url = uri("https://repo.spring.io/snapshot") }
}

val springAiVersion = "1.1.2"

dependencies {
    implementation(project(":workshop-infrastructure"))

    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-webflux")

    // Spring AI for OpenAI integration
    implementation(platform("org.springframework.ai:spring-ai-bom:$springAiVersion"))
    implementation("org.springframework.ai:spring-ai-starter-model-openai")

    // Agent Memory Server Java SDK (from Maven Central)
    implementation("com.redis:agent-memory-client-java:0.1.0")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
