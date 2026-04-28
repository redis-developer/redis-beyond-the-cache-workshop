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
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.flywaydb:flyway-core")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    developmentOnly("org.springframework.boot:spring-boot-devtools")

    runtimeOnly("com.h2database:h2")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

val controlPlaneFrontendDir = rootProject.projectDir.parentFile.resolve("frontend")
val controlPlaneFrontendDistDir = controlPlaneFrontendDir.resolve("dist")
val workshopRegistryFile = rootProject.projectDir.parentFile.resolve("workshops.yaml")
val skipFrontendBuild = (rootProject.findProperty("skipFrontendBuild") as String?)?.toBoolean() == true

val buildControlPlaneFrontend = tasks.register<Exec>("buildControlPlaneFrontend") {
    workingDir = controlPlaneFrontendDir
    onlyIf { !skipFrontendBuild }
    commandLine("npm", "run", "build")
    inputs.dir(controlPlaneFrontendDir.resolve("src"))
    inputs.dir(controlPlaneFrontendDir.resolve("public"))
    inputs.files(
        controlPlaneFrontendDir.resolve("package.json"),
        controlPlaneFrontendDir.resolve("package-lock.json"),
        controlPlaneFrontendDir.resolve("vue.config.js")
    )
    outputs.dir(controlPlaneFrontendDistDir)
}

tasks.named<Copy>("processResources") {
    dependsOn(buildControlPlaneFrontend)
    from(workshopRegistryFile)
    from(controlPlaneFrontendDistDir) {
        into("static")
    }
}

tasks.named("bootRun") {
    dependsOn(buildControlPlaneFrontend)
}
