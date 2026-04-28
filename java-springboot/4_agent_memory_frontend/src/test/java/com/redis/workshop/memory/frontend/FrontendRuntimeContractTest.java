package com.redis.workshop.memory.frontend;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrontendRuntimeContractTest {

    @Test
    void runtimeConfigurationUsesSessionEnvironmentVariables() throws IOException {
        Path moduleDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();

        String properties = Files.readString(moduleDir.resolve("src/main/resources/application.properties"), StandardCharsets.UTF_8);
        String vueConfig = Files.readString(moduleDir.resolve("frontend/vue.config.js"), StandardCharsets.UTF_8);

        assertTrue(properties.contains("WORKSHOP_SESSION_BACKEND_URL"));
        assertTrue(properties.contains("WORKSHOP_SESSION_WORKSPACE_PATH"));
        assertFalse(properties.contains("127.0.0.1:18083"));

        assertTrue(vueConfig.contains("WORKSHOP_SESSION_BACKEND_URL"));
        assertFalse(vueConfig.contains("http://localhost:18083"));
    }
}
