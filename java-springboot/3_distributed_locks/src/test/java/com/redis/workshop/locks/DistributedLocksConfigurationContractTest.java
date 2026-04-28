package com.redis.workshop.locks;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DistributedLocksConfigurationContractTest {

    @Test
    void backendApplicationPropertiesUseEnvDrivenRedisAndPostgresDefaults() throws IOException {
        Properties properties = loadProperties(resolveBackendModulePath().resolve("src/main/resources/application.properties"));

        assertEquals(
            "${SPRING_DATASOURCE_URL:jdbc:postgresql://${POSTGRES_HOST:localhost}:${POSTGRES_PORT:5432}/${POSTGRES_DB:workshop}}",
            properties.getProperty("spring.datasource.url")
        );
        assertEquals(
            "${SPRING_DATASOURCE_USERNAME:${POSTGRES_USER:workshop}}",
            properties.getProperty("spring.datasource.username")
        );
        assertEquals(
            "${SPRING_DATASOURCE_PASSWORD:${POSTGRES_PASSWORD:workshop}}",
            properties.getProperty("spring.datasource.password")
        );
        assertEquals(
            "${SPRING_DATA_REDIS_HOST:${REDIS_HOST:localhost}}",
            properties.getProperty("spring.data.redis.host")
        );
        assertEquals(
            "${SPRING_DATA_REDIS_PORT:${REDIS_PORT:6379}}",
            properties.getProperty("spring.data.redis.port")
        );
        assertEquals(
            "${SPRING_REDIS_HOST:${SPRING_DATA_REDIS_HOST:${REDIS_HOST:localhost}}}",
            properties.getProperty("spring.redis.host")
        );
        assertEquals(
            "${SPRING_REDIS_PORT:${SPRING_DATA_REDIS_PORT:${REDIS_PORT:6379}}}",
            properties.getProperty("spring.redis.port")
        );
    }

    @Test
    void editableResetConfigurationStaysAlignedWithBackendDefaults() throws IOException {
        Properties backendProperties = loadProperties(resolveBackendModulePath().resolve("src/main/resources/application.properties"));
        Properties resetProperties = loadProperties(
            resolveRepoRoot()
                .resolve("java-springboot/3_distributed_locks_frontend/src/main/resources/workshop-files/application.properties")
        );

        assertEquals(backendProperties.getProperty("spring.datasource.url"), resetProperties.getProperty("spring.datasource.url"));
        assertEquals(backendProperties.getProperty("spring.datasource.username"), resetProperties.getProperty("spring.datasource.username"));
        assertEquals(backendProperties.getProperty("spring.datasource.password"), resetProperties.getProperty("spring.datasource.password"));
        assertEquals(backendProperties.getProperty("spring.data.redis.host"), resetProperties.getProperty("spring.data.redis.host"));
        assertEquals(backendProperties.getProperty("spring.data.redis.port"), resetProperties.getProperty("spring.data.redis.port"));
        assertEquals(backendProperties.getProperty("spring.redis.host"), resetProperties.getProperty("spring.redis.host"));
        assertEquals(backendProperties.getProperty("spring.redis.port"), resetProperties.getProperty("spring.redis.port"));
    }

    private static Properties loadProperties(Path path) throws IOException {
        Properties properties = new Properties();
        properties.load(new StringReader(Files.readString(path, StandardCharsets.UTF_8)));
        return properties;
    }

    private static Path resolveBackendModulePath() {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        String directoryName = userDir.getFileName() != null ? userDir.getFileName().toString() : "";

        return switch (directoryName) {
            case "3_distributed_locks" -> userDir;
            case "java-springboot" -> userDir.resolve("3_distributed_locks");
            case "redis-beyond-the-cache-workshop" -> userDir.resolve("java-springboot").resolve("3_distributed_locks");
            default -> userDir.resolve("java-springboot").resolve("3_distributed_locks");
        };
    }

    private static Path resolveRepoRoot() {
        Path userDir = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        String directoryName = userDir.getFileName() != null ? userDir.getFileName().toString() : "";

        return switch (directoryName) {
            case "3_distributed_locks" -> userDir.getParent() != null ? userDir.getParent().getParent() : userDir;
            case "java-springboot" -> userDir.getParent() != null ? userDir.getParent() : userDir;
            case "redis-beyond-the-cache-workshop" -> userDir;
            default -> userDir;
        };
    }
}
