package com.redis.workshop.hub.tools;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ComposeGeneratorTest {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

    @TempDir
    Path tempDir;

    @Test
    void generatesDedicatedFrontendAndBackendServicesForSplitWorkshop() throws Exception {
        Files.writeString(
            tempDir.resolve("workshops.yaml"),
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

        ComposeGenerator.main(new String[]{tempDir.toString(), "workshops.yaml"});

        Map<String, Object> localCompose = readYaml(tempDir.resolve("java-springboot/workshop-hub/docker-compose.local.yml"));
        Map<String, Object> internalCompose = readYaml(tempDir.resolve("java-springboot/workshop-hub/docker-compose.internal.yml"));

        assertSplitWorkshop(localCompose, "${WORKSHOP_ROOT_PATH:-.}");
        assertSplitWorkshop(internalCompose, "${WORKSHOP_ROOT_PATH:-/workshops}");
    }

    @SuppressWarnings("unchecked")
    private void assertSplitWorkshop(Map<String, Object> compose, String volumeRoot) {
        Map<String, Object> services = (Map<String, Object>) compose.get("services");
        assertTrue(services.containsKey("demo-workshop"));
        assertTrue(services.containsKey("demo-workshop-api"));

        Map<String, Object> frontend = (Map<String, Object>) services.get("demo-workshop");
        Map<String, Object> backend = (Map<String, Object>) services.get("demo-workshop-api");

        Map<String, Object> frontendBuild = (Map<String, Object>) frontend.get("build");
        Map<String, Object> backendBuild = (Map<String, Object>) backend.get("build");

        assertEquals("java-springboot/demo_workshop_frontend/Dockerfile", frontendBuild.get("dockerfile"));
        assertEquals("java-springboot/demo_workshop/Dockerfile", backendBuild.get("dockerfile"));
        assertEquals(List.of(volumeRoot + "/java-springboot/demo_workshop:/workshop-sources"), frontend.get("volumes"));
        assertEquals(List.of(volumeRoot + "/java-springboot/demo_workshop:/workshop-sources"), backend.get("volumes"));
        assertTrue(((List<String>) frontend.get("environment")).contains("WORKSHOP_BACKEND_URL=http://demo-workshop-api:18089"));
        assertEquals(List.of("18089:18089"), backend.get("ports"));
        assertEquals(List.of("8089:8089"), frontend.get("ports"));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readYaml(Path path) throws Exception {
        return mapper.readValue(Files.newInputStream(path), Map.class);
    }
}
