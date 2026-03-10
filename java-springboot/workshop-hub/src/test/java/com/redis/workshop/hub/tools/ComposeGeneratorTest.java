package com.redis.workshop.hub.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComposeGeneratorTest {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

    @TempDir
    Path tempDir;

    @Test
    void generatesDedicatedFrontendAndBackendServicesForSplitWorkshop() throws Exception {
        GeneratedComposeFiles compose = generateCompose(
            """
                version: 1
                workshops:
                  - id: demo_workshop
                    title: Demo Workshop
                    description: Demo
                    difficulty: Beginner
                    estimatedMinutes: 10
                    serviceName: demo-workshop
                    port: 8089
                    url: /workshop/demo-workshop/
                    dockerfile: java-springboot/demo_workshop/Dockerfile
                    frontendServiceName: demo-workshop
                    frontendPort: 8089
                    frontendDockerfile: java-springboot/demo_workshop_frontend/Dockerfile
                    backendServiceName: demo-workshop-api
                    backendPort: 18089
                    backendDockerfile: java-springboot/demo_workshop/Dockerfile
                    topics: [Demo]
                """
        );

        assertSplitWorkshop(compose.local(), "${WORKSHOP_ROOT_PATH:-.}");
        assertSplitWorkshop(compose.internal(), "${WORKSHOP_ROOT_PATH:-/workshops}");
        assertLocalWorkshopProfileDependenciesResolvable(compose.local(), "workshop-demo_workshop");
    }

    @Test
    void generatesPostgresDependenciesAndRoleSpecificEnvOverridesFromRegistry() throws Exception {
        GeneratedComposeFiles compose = generateCompose(
            """
                version: 1
                workshops:
                  - id: data_workshop
                    title: Data Workshop
                    description: Demo
                    difficulty: Intermediate
                    estimatedMinutes: 20
                    serviceName: data-workshop
                    port: 8087
                    url: /workshop/data-workshop/
                    dockerfile: java-springboot/data_workshop/Dockerfile
                    frontendServiceName: data-workshop
                    frontendPort: 8087
                    frontendDockerfile: java-springboot/data_workshop_frontend/Dockerfile
                    backendServiceName: data-workshop-api
                    backendPort: 18087
                    backendDockerfile: java-springboot/data_workshop/Dockerfile
                    infrastructureDependencies: [redis, postgres]
                    envOverrides:
                      common:
                        LOG_LEVEL: debug
                      backend:
                        SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/workshop
                        SPRING_DATASOURCE_USERNAME: workshop
                        SPRING_DATASOURCE_PASSWORD: workshop
                    topics: [Data]
                """
        );

        Map<String, Object> localBackend = service(compose.local(), "data-workshop-api");
        Map<String, Object> internalBackend = service(compose.internal(), "data-workshop-api");
        Map<String, String> localBackendEnv = environmentMap(localBackend);
        Map<String, String> localFrontendEnv = environmentMap(service(compose.local(), "data-workshop"));

        assertEquals(List.of("redis", "postgres"), stringList(localBackend.get("depends_on")));
        assertEquals(List.of("redis", "postgres"), stringList(internalBackend.get("depends_on")));
        assertEquals("debug", localBackendEnv.get("LOG_LEVEL"));
        assertEquals("jdbc:postgresql://postgres:5432/workshop", localBackendEnv.get("SPRING_DATASOURCE_URL"));
        assertEquals("workshop", localBackendEnv.get("SPRING_DATASOURCE_USERNAME"));
        assertEquals("workshop", localBackendEnv.get("SPRING_DATASOURCE_PASSWORD"));
        assertEquals("debug", localFrontendEnv.get("LOG_LEVEL"));
        assertFalse(localFrontendEnv.containsKey("SPRING_DATASOURCE_URL"));

        Map<String, Object> localPostgres = service(compose.local(), "postgres");
        Map<String, Object> internalPostgres = service(compose.internal(), "postgres");
        assertEquals(List.of("workshops", "workshop-data_workshop"), stringList(localPostgres.get("profiles")));
        assertFalse(internalPostgres.containsKey("profiles"));
        assertLocalWorkshopProfileDependenciesResolvable(compose.local(), "workshop-data_workshop");
    }

    @Test
    void generatesSidecarsRedisStackAndFrontendPrebuildBehaviorFromRegistry() throws Exception {
        GeneratedComposeFiles compose = generateCompose(
            """
                version: 1
                workshops:
                  - id: memory_workshop
                    title: Memory Workshop
                    description: Demo
                    difficulty: Intermediate
                    estimatedMinutes: 30
                    serviceName: memory-ui
                    port: 8086
                    url: /workshop/memory-ui/
                    dockerfile: java-springboot/memory_workshop/Dockerfile
                    frontendServiceName: memory-ui
                    frontendPort: 8086
                    frontendDockerfile: java-springboot/memory_workshop_frontend/Dockerfile
                    backendServiceName: memory-api
                    backendPort: 18086
                    backendDockerfile: java-springboot/memory_workshop/Dockerfile
                    infrastructureDependencies: [redis]
                    redisFlavor: stack
                    frontendPrebuild: false
                    envOverrides:
                      common:
                        LOG_LEVEL: info
                      frontend:
                        VUE_APP_EXPERIMENT: enabled
                      backend:
                        AGENT_MEMORY_SERVER_URL: http://memory-sidecar:9000
                    sidecars:
                      - serviceName: memory-sidecar
                        image: demo/memory-sidecar:latest
                        port: 9000
                        env:
                          REDIS_URL: redis://redis:6379
                        dependsOn: [redis]
                        command: [memory-sidecar, serve]
                        healthcheck:
                          test: [CMD, curl, -f, http://localhost:9000/health]
                          interval: 10s
                          timeout: 5s
                          retries: 3
                          startPeriod: 15s
                    topics: [Memory]
                """
        );

        assertEquals("redis/redis-stack-server:latest", service(compose.local(), "redis").get("image"));
        assertEquals("redis/redis-stack-server:latest", service(compose.internal(), "redis").get("image"));

        Map<String, Object> sidecar = service(compose.local(), "memory-sidecar");
        Map<String, Object> sidecarHealthcheck = map(sidecar.get("healthcheck"));
        assertEquals(List.of("9000:9000"), stringList(sidecar.get("ports")));
        assertEquals(List.of("redis"), stringList(sidecar.get("depends_on")));
        assertEquals(List.of("memory-sidecar", "serve"), stringList(sidecar.get("command")));
        assertEquals("redis://redis:6379", environmentMap(sidecar).get("REDIS_URL"));
        assertEquals(List.of("CMD", "curl", "-f", "http://localhost:9000/health"), stringList(sidecarHealthcheck.get("test")));
        assertEquals("10s", sidecarHealthcheck.get("interval"));
        assertEquals("5s", sidecarHealthcheck.get("timeout"));
        assertEquals(3, sidecarHealthcheck.get("retries"));
        assertEquals("15s", sidecarHealthcheck.get("start_period"));
        assertEquals(List.of("workshops", "workshop-memory_workshop"), stringList(sidecar.get("profiles")));

        Map<String, Object> backend = service(compose.local(), "memory-api");
        Map<String, String> backendEnv = environmentMap(backend);
        Map<String, String> frontendEnv = environmentMap(service(compose.local(), "memory-ui"));
        assertEquals(List.of("redis", "memory-sidecar"), stringList(backend.get("depends_on")));
        assertEquals("http://memory-sidecar:9000", backendEnv.get("AGENT_MEMORY_SERVER_URL"));
        assertEquals("info", backendEnv.get("LOG_LEVEL"));
        assertEquals("enabled", frontendEnv.get("VUE_APP_EXPERIMENT"));
        assertFalse(frontendEnv.containsKey("AGENT_MEMORY_SERVER_URL"));

        Map<String, Object> internalFrontendBuild = map(service(compose.internal(), "memory-ui").get("build"));
        assertEquals(
            List.of("VUE_APP_BASE_PATH=/", "SKIP_FRONTEND_BUILD=false"),
            stringList(internalFrontendBuild.get("args"))
        );
        assertLocalWorkshopProfileDependenciesResolvable(compose.local(), "workshop-memory_workshop");
    }

    @SuppressWarnings("unchecked")
    private void assertSplitWorkshop(Map<String, Object> compose, String volumeRoot) {
        Map<String, Object> services = (Map<String, Object>) compose.get("services");
        assertTrue(services.containsKey("demo-workshop"));
        assertTrue(services.containsKey("demo-workshop-api"));

        Map<String, Object> frontend = service(compose, "demo-workshop");
        Map<String, Object> backend = service(compose, "demo-workshop-api");

        Map<String, Object> frontendBuild = map(frontend.get("build"));
        Map<String, Object> backendBuild = map(backend.get("build"));

        assertEquals("java-springboot/demo_workshop_frontend/Dockerfile", frontendBuild.get("dockerfile"));
        assertEquals("java-springboot/demo_workshop/Dockerfile", backendBuild.get("dockerfile"));
        assertEquals(List.of(volumeRoot + "/java-springboot/demo_workshop:/workshop-sources"), stringList(frontend.get("volumes")));
        assertEquals(List.of(volumeRoot + "/java-springboot/demo_workshop:/workshop-sources"), stringList(backend.get("volumes")));
        assertEquals("http://demo-workshop-api:18089", environmentMap(frontend).get("WORKSHOP_BACKEND_URL"));
        assertEquals(List.of("redis"), stringList(backend.get("depends_on")));
        assertEquals(List.of("demo-workshop-api"), stringList(frontend.get("depends_on")));
        assertEquals(List.of("18089:18089"), stringList(backend.get("ports")));
        assertEquals(List.of("8089:8089"), stringList(frontend.get("ports")));
    }

    private GeneratedComposeFiles generateCompose(String yaml) throws Exception {
        Files.writeString(tempDir.resolve("workshops.yaml"), yaml);
        ComposeGenerator.main(new String[]{tempDir.toString(), "workshops.yaml"});
        return new GeneratedComposeFiles(
            readYaml(tempDir.resolve("java-springboot/workshop-hub/docker-compose.local.yml")),
            readYaml(tempDir.resolve("java-springboot/workshop-hub/docker-compose.internal.yml"))
        );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> service(Map<String, Object> compose, String name) {
        return map(map(compose.get("services")).get(name));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<String> stringList(Object value) {
        if (value == null) {
            return List.of();
        }
        return ((List<Object>) value).stream()
            .map(String::valueOf)
            .toList();
    }

    private Map<String, String> environmentMap(Map<String, Object> service) {
        Map<String, String> env = new LinkedHashMap<>();
        for (String entry : stringList(service.get("environment"))) {
            int separatorIndex = entry.indexOf('=');
            if (separatorIndex < 0) {
                env.put(entry, "");
            } else {
                env.put(entry.substring(0, separatorIndex), entry.substring(separatorIndex + 1));
            }
        }
        return env;
    }

    private void assertLocalWorkshopProfileDependenciesResolvable(Map<String, Object> compose, String profile) {
        Map<String, Object> services = map(compose.get("services"));
        for (Map.Entry<String, Object> entry : services.entrySet()) {
            Map<String, Object> service = map(entry.getValue());
            if (!isServiceActiveForProfile(service, profile)) {
                continue;
            }

            for (String dependency : stringList(service.get("depends_on"))) {
                Map<String, Object> dependencyService = service(compose, dependency);
                assertTrue(
                    isServiceActiveForProfile(dependencyService, profile),
                    () -> "Service '" + entry.getKey() + "' depends on '" + dependency + "' outside profile '" + profile + "'"
                );
            }
        }
    }

    private boolean isServiceActiveForProfile(Map<String, Object> service, String profile) {
        List<String> profiles = stringList(service.get("profiles"));
        return profiles.isEmpty() || new LinkedHashSet<>(profiles).contains(profile);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readYaml(Path path) throws Exception {
        return mapper.readValue(Files.newInputStream(path), Map.class);
    }

    private record GeneratedComposeFiles(Map<String, Object> local, Map<String, Object> internal) {
    }
}
