package com.redis.workshop.hub.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WorkshopRegistryParsingTest {

    private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).findAndRegisterModules();

    @Test
    void parsesDualServiceFields() throws Exception {
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
    }

    @Test
    void fallsBackToLegacyFieldsWhenDualFieldsAreMissing() throws Exception {
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

        assertNull(workshop.getFrontendServiceName());
        assertNull(workshop.getFrontendPort());
        assertNull(workshop.getFrontendDockerfile());
        assertNull(workshop.getBackendServiceName());
        assertNull(workshop.getBackendPort());
        assertNull(workshop.getBackendDockerfile());

        assertEquals("legacy-service", workshop.getEffectiveFrontendServiceName());
        assertEquals(8090, workshop.getEffectiveFrontendPort());
        assertEquals("java-springboot/legacy/Dockerfile", workshop.getEffectiveFrontendDockerfile());
        assertEquals("legacy-service", workshop.getEffectiveBackendServiceName());
        assertEquals(8090, workshop.getEffectiveBackendPort());
        assertEquals("java-springboot/legacy/Dockerfile", workshop.getEffectiveBackendDockerfile());

        // Canonical workshop URL remains the frontend URL field.
        assertEquals("/workshop/legacy-service/", workshop.getUrl());
    }
}

