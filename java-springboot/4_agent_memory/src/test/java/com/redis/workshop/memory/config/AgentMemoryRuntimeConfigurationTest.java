package com.redis.workshop.memory.config;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AgentMemoryRuntimeConfigurationTest {

    @Test
    void applicationPropertiesUseEnvDrivenAgentMemoryUrl() throws IOException {
        Path moduleDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        String properties = Files.readString(moduleDir.resolve("src/main/resources/application.properties"), StandardCharsets.UTF_8);

        assertTrue(properties.contains("AGENT_MEMORY_SERVER_URL"));
        assertFalse(properties.contains("http://localhost:8000"));
    }
}
