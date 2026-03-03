package com.redis.workshop.memory.infrastructure;

import com.redis.workshop.infrastructure.WorkshopConfig;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Agent Memory Workshop Configuration.
 * Defines which files can be edited and their original content for the workshop.
 */
@Component
public class AgentMemoryWorkshopConfig implements WorkshopConfig {

    private static final Map<String, String> EDITABLE_FILES = Map.ofEntries(
        Map.entry("build.gradle.kts", "build.gradle.kts"),
        Map.entry("application.properties", "src/main/resources/application.properties"),
        Map.entry("AgentMemoryService.java", "src/main/java/com/redis/workshop/memory/service/AgentMemoryService.java")
    );

    private static final Map<String, String> ORIGINAL_CONTENTS = Map.ofEntries(
        Map.entry("build.gradle.kts", """
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
            }

            dependencies {
                implementation(project(":workshop-infrastructure"))

                implementation("org.springframework.boot:spring-boot-starter-web")

                // TODO: Uncomment the line below to enable the Agent Memory Client
                // implementation("com.github.redis.agent-memory-server:agent-memory-client:0.1.0")

                // HTTP client for REST API calls (used before adding the official client)
                implementation("org.springframework.boot:spring-boot-starter-webflux")

                testImplementation("org.springframework.boot:spring-boot-starter-test")
                testRuntimeOnly("org.junit.platform:junit-platform-launcher")
            }

            tasks.withType<Test> {
                useJUnitPlatform()
            }
            """),
        Map.entry("application.properties", """
            # Application name
            spring.application.name=agent-memory

            # Server configuration
            server.port=8083

            # Agent Memory Server configuration
            agent-memory.server.url=http://localhost:8000
            agent-memory.server.namespace=workshop

            # Disable authentication for development (AMS default)
            # agent-memory.server.auth-token=your-token-here

            # Logging
            logging.level.com.redis.workshop.memory=INFO
            logging.level.org.springframework.web.reactive.function.client=DEBUG
            """)
    );

    @Override
    public Map<String, String> getEditableFiles() {
        return EDITABLE_FILES;
    }

    @Override
    public String getOriginalContent(String fileName) {
        return ORIGINAL_CONTENTS.get(fileName);
    }

    @Override
    public String getModuleName() {
        return "4_agent_memory";
    }

    @Override
    public String getWorkshopTitle() {
        return "Agent Memory Server";
    }

    @Override
    public String getWorkshopDescription() {
        return "Give your AI agents persistent memory with Redis-powered Agent Memory Server.";
    }
}

