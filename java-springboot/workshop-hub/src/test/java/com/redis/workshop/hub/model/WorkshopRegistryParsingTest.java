package com.redis.workshop.hub.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkshopRegistryParsingTest {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

    @Test
    void parsesRicherRuntimeSchema() throws Exception {
        String yaml = """
            version: 1
            workshops:
              - id: demo_workshop
                title: Demo Workshop
                description: Demo
                difficulty: Beginner
                estimatedMinutes: 10
                serviceName: demo-legacy
                port: 8080
                url: /workshop/demo/
                dockerfile: java-springboot/demo/Dockerfile
                frontendServiceName: demo-frontend
                frontendPort: 8088
                frontendDockerfile: java-springboot/demo/frontend.Dockerfile
                backendServiceName: demo-backend
                backendPort: 18080
                backendDockerfile: java-springboot/demo/backend.Dockerfile
                infrastructureDependencies: [redis, postgres]
                envOverrides:
                  common:
                    LOG_LEVEL: DEBUG
                  frontend:
                    VUE_APP_DEMO_MODE: enabled
                  backend:
                    SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/workshop
                sidecars:
                  - serviceName: demo-sidecar
                    image: demo:latest
                    port: 9000
                    env:
                      SIDEKICK_MODE: enabled
                    dependsOn: [redis]
                    command: [demo-sidecar, serve]
                    healthcheck:
                      test: [CMD, curl, -f, http://localhost:9000/health]
                      interval: 10s
                      timeout: 5s
                      retries: 3
                      startPeriod: 15s
                redisFlavor: stack
                frontendPrebuild: false
                topics: [One]
            """;

        WorkshopRegistry registry = mapper.readValue(yaml, WorkshopRegistry.class);
        Workshop workshop = registry.getWorkshops().getFirst();

        assertEquals("demo-frontend", workshop.getFrontendServiceName());
        assertEquals(Integer.valueOf(8088), workshop.getFrontendPort());
        assertEquals("java-springboot/demo/frontend.Dockerfile", workshop.getFrontendDockerfile());
        assertEquals("demo-backend", workshop.getBackendServiceName());
        assertEquals(Integer.valueOf(18080), workshop.getBackendPort());
        assertEquals("java-springboot/demo/backend.Dockerfile", workshop.getBackendDockerfile());

        assertEquals("demo-frontend", workshop.getEffectiveFrontendServiceName());
        assertEquals(8088, workshop.getEffectiveFrontendPort());
        assertEquals("java-springboot/demo/frontend.Dockerfile", workshop.getEffectiveFrontendDockerfile());
        assertEquals("demo-backend", workshop.getEffectiveBackendServiceName());
        assertEquals(18080, workshop.getEffectiveBackendPort());
        assertEquals("java-springboot/demo/backend.Dockerfile", workshop.getEffectiveBackendDockerfile());

        assertEquals(java.util.List.of("redis", "postgres"), workshop.getInfrastructureDependencies());
        assertEquals(java.util.Map.of("LOG_LEVEL", "DEBUG"), workshop.getCommonEnvOverrides());
        assertEquals(java.util.Map.of("VUE_APP_DEMO_MODE", "enabled"), workshop.getFrontendEnvOverrides());
        assertEquals(
            java.util.Map.of("SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/workshop"),
            workshop.getBackendEnvOverrides()
        );
        assertEquals("stack", workshop.getRedisFlavor());
        assertTrue(workshop.usesRedisStack());
        assertEquals(Boolean.FALSE, workshop.getFrontendPrebuild());
        assertTrue(!workshop.isFrontendPrebuildEnabled());

        WorkshopSidecar sidecar = workshop.getSidecars().getFirst();
        assertEquals("demo-sidecar", sidecar.getServiceName());
        assertEquals("demo:latest", sidecar.getImage());
        assertEquals(Integer.valueOf(9000), sidecar.getPort());
        assertEquals(java.util.Map.of("SIDEKICK_MODE", "enabled"), sidecar.getEnv());
        assertEquals(java.util.List.of("redis"), sidecar.getDependsOn());
        assertEquals(java.util.List.of("demo-sidecar", "serve"), sidecar.getCommand());
        assertNotNull(sidecar.getHealthcheck());
        assertEquals(java.util.List.of("CMD", "curl", "-f", "http://localhost:9000/health"), sidecar.getHealthcheck().getTest());
        assertEquals("10s", sidecar.getHealthcheck().getInterval());
        assertEquals("5s", sidecar.getHealthcheck().getTimeout());
        assertEquals(Integer.valueOf(3), sidecar.getHealthcheck().getRetries());
        assertEquals("15s", sidecar.getHealthcheck().getStartPeriod());
    }

    @Test
    void fallsBackToLegacyDefaultsWhenRicherFieldsAreMissing() throws Exception {
        String yaml = """
            version: 1
            workshops:
              - id: legacy_workshop
                title: Legacy Workshop
                description: Legacy
                difficulty: Beginner
                estimatedMinutes: 20
                serviceName: legacy-service
                port: 8090
                url: /workshop/legacy-service/
                dockerfile: java-springboot/legacy/Dockerfile
                topics: [Legacy]
            """;

        WorkshopRegistry registry = mapper.readValue(yaml, WorkshopRegistry.class);
        Workshop workshop = registry.getWorkshops().getFirst();

        assertEquals("legacy-service", workshop.getEffectiveFrontendServiceName());
        assertEquals(8090, workshop.getEffectiveFrontendPort());
        assertEquals("java-springboot/legacy/Dockerfile", workshop.getEffectiveFrontendDockerfile());
        assertEquals("legacy-service", workshop.getEffectiveBackendServiceName());
        assertEquals(8090, workshop.getEffectiveBackendPort());
        assertEquals("java-springboot/legacy/Dockerfile", workshop.getEffectiveBackendDockerfile());
        assertEquals(java.util.List.of("redis"), workshop.getInfrastructureDependencies());
        assertTrue(workshop.getCommonEnvOverrides().isEmpty());
        assertTrue(workshop.getFrontendEnvOverrides().isEmpty());
        assertTrue(workshop.getBackendEnvOverrides().isEmpty());
        assertTrue(workshop.getSidecars().isEmpty());
        assertEquals("standard", workshop.getRedisFlavor());
        assertEquals(Boolean.TRUE, workshop.getFrontendPrebuild());
        assertTrue(workshop.isFrontendPrebuildEnabled());

        // Canonical workshop URL remains the frontend URL field.
        assertEquals("/workshop/legacy-service/", workshop.getUrl());
    }

    @Test
    void currentRegistryDescribesNamedWorkshopsAndTheirRuntimeMetadata() throws Exception {
        WorkshopRegistry registry = mapper.readValue(Files.newInputStream(findRegistryPath()), WorkshopRegistry.class);

        Workshop sessions = findWorkshop(registry, "1_session_management");
        assertRuntimeMetadata(
            sessions,
            "session-management",
            8080,
            "java-springboot/1_session_management_frontend/Dockerfile",
            "session-management-api",
            18080,
            "java-springboot/1_session_management/Dockerfile"
        );
        assertEquals(java.util.List.of("redis"), sessions.getInfrastructureDependencies());
        assertEquals("standard", sessions.getRedisFlavor());
        assertTrue(sessions.getSidecars().isEmpty());
        assertTrue(sessions.getBackendEnvOverrides().isEmpty());
        assertTrue(sessions.isFrontendPrebuildEnabled());

        Workshop search = findWorkshop(registry, "2_full_text_search");
        assertRuntimeMetadata(
            search,
            "full-text-search",
            8081,
            "java-springboot/2_full_text_search_frontend/Dockerfile",
            "full-text-search-api",
            18081,
            "java-springboot/2_full_text_search/Dockerfile"
        );
        assertEquals(java.util.List.of("redis"), search.getInfrastructureDependencies());
        assertEquals("stack", search.getRedisFlavor());
        assertTrue(search.usesRedisStack());
        assertTrue(search.getSidecars().isEmpty());
        assertTrue(search.isFrontendPrebuildEnabled());

        Workshop locks = findWorkshop(registry, "3_distributed_locks");
        assertRuntimeMetadata(
            locks,
            "distributed-locks",
            8082,
            "java-springboot/3_distributed_locks_frontend/Dockerfile",
            "distributed-locks-api",
            18082,
            "java-springboot/3_distributed_locks/Dockerfile"
        );
        assertEquals(java.util.List.of("redis", "postgres"), locks.getInfrastructureDependencies());
        assertEquals(
            java.util.Map.of(
                "SPRING_DATASOURCE_URL", "jdbc:postgresql://postgres:5432/workshop",
                "SPRING_DATASOURCE_USERNAME", "workshop",
                "SPRING_DATASOURCE_PASSWORD", "workshop"
            ),
            locks.getBackendEnvOverrides()
        );
        assertEquals("standard", locks.getRedisFlavor());
        assertTrue(locks.getSidecars().isEmpty());
        assertTrue(locks.isFrontendPrebuildEnabled());

        Workshop memory = findWorkshop(registry, "4_agent_memory");
        assertRuntimeMetadata(
            memory,
            "agent-memory",
            8083,
            "java-springboot/4_agent_memory_frontend/Dockerfile",
            "agent-memory-api",
            18083,
            "java-springboot/4_agent_memory/Dockerfile"
        );
        assertEquals(java.util.List.of("redis"), memory.getInfrastructureDependencies());
        assertEquals(
            java.util.Map.of("AGENT_MEMORY_SERVER_URL", "http://agent-memory-server:8000"),
            memory.getBackendEnvOverrides()
        );
        assertEquals("stack", memory.getRedisFlavor());
        assertTrue(memory.usesRedisStack());
        assertTrue(memory.isFrontendPrebuildEnabled());

        WorkshopSidecar sidecar = memory.getSidecars().getFirst();
        assertEquals("agent-memory-server", sidecar.getServiceName());
        assertEquals("redislabs/agent-memory-server:latest", sidecar.getImage());
        assertEquals(Integer.valueOf(8000), sidecar.getPort());
        assertEquals("redis://redis:6379", sidecar.getEnv().get("REDIS_URL"));
        assertEquals("${OPENAI_API_KEY:-}", sidecar.getEnv().get("OPENAI_API_KEY"));
        assertEquals(java.util.List.of("redis"), sidecar.getDependsOn());
        assertEquals(
            java.util.List.of("agent-memory", "api", "--host", "0.0.0.0", "--port", "8000", "--task-backend=asyncio"),
            sidecar.getCommand()
        );
        assertNotNull(sidecar.getHealthcheck());
        assertEquals(java.util.List.of("CMD", "curl", "-f", "http://localhost:8000/v1/health"), sidecar.getHealthcheck().getTest());
        assertEquals("10s", sidecar.getHealthcheck().getInterval());
        assertEquals("5s", sidecar.getHealthcheck().getTimeout());
        assertEquals(Integer.valueOf(5), sidecar.getHealthcheck().getRetries());
        assertEquals("10s", sidecar.getHealthcheck().getStartPeriod());
    }

    private void assertRuntimeMetadata(
        Workshop workshop,
        String frontendServiceName,
        int frontendPort,
        String frontendDockerfile,
        String backendServiceName,
        int backendPort,
        String backendDockerfile
    ) {
        assertEquals(frontendServiceName, workshop.getFrontendServiceName());
        assertEquals(Integer.valueOf(frontendPort), workshop.getFrontendPort());
        assertEquals(frontendDockerfile, workshop.getFrontendDockerfile());
        assertEquals(backendServiceName, workshop.getBackendServiceName());
        assertEquals(Integer.valueOf(backendPort), workshop.getBackendPort());
        assertEquals(backendDockerfile, workshop.getBackendDockerfile());
    }

    private Workshop findWorkshop(WorkshopRegistry registry, String workshopId) {
        return registry.getWorkshops().stream()
            .filter(workshop -> workshopId.equals(workshop.getId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Workshop not found: " + workshopId));
    }

    private Path findRegistryPath() {
        Path current = Path.of("").toAbsolutePath();
        while (current != null) {
            Path candidate = current.resolve("workshops.yaml");
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        throw new IllegalStateException("Unable to locate workshops.yaml from " + Path.of("").toAbsolutePath());
    }
}
