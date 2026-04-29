package com.redis.workshop.platform.controlplane.release;

import com.redis.workshop.platform.controlplane.audit.ReleaseCatalogAuditHooks;
import com.redis.workshop.platform.controlplane.observability.ReleaseCatalogMetricsRecorder;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class ReleaseCatalogServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesDefaultReleaseForWorkshopFromCatalog() throws IOException {
        Path registry = tempDir.resolve("workshops.yaml");
        Files.writeString(registry, """
            version: 1
            workshops:
              - id: 1_session_management
                releases:
                  - releaseId: session-management-2026.04.1
                    releaseVersion: 2026.04.1
                    mode: LAB
                    defaultForWorkshop: true
                    enabled: true
                    environments:
                      - local
                    images:
                      frontend: registry.example.com/workshops/session-management-frontend@sha256:1111111111111111111111111111111111111111111111111111111111111111
                      backend: registry.example.com/workshops/session-management-backend@sha256:2222222222222222222222222222222222222222222222222222222222222222
                    resourceClass: small
                    sessionTtlMinutes: 60
                    mutableDependencies:
                      - redis
            """);

        ReleaseCatalogService service = new ReleaseCatalogService(
            new ReleaseCatalogLoader(new DefaultResourceLoader(), registry.toString()),
            mock(ReleaseCatalogAuditHooks.class),
            new ReleaseCatalogMetricsRecorder(new SimpleMeterRegistry())
        );

        ReleaseCatalogEntry release = service.findDefaultRelease("1_session_management").orElseThrow();

        assertThat(release.releaseVersion()).isEqualTo("2026.04.1");
        assertThat(release.images().frontend()).contains("@sha256:");
        assertThat(release.mutableDependencies()).containsExactly("redis");
    }

    @Test
    void classpathRegistryIncludesRemainingWorkshopReleases() {
        ReleaseCatalogService service = new ReleaseCatalogService(
            new ReleaseCatalogLoader(new DefaultResourceLoader(), "missing-workshops.yaml"),
            mock(ReleaseCatalogAuditHooks.class),
            new ReleaseCatalogMetricsRecorder(new SimpleMeterRegistry())
        );

        assertThat(service.findDefaultRelease("3_distributed_locks")).hasValueSatisfying(release -> {
            assertThat(release.releaseId()).isEqualTo("distributed-locks-2026.04.1");
            assertThat(release.releaseVersion()).isEqualTo("2026.04.1");
            assertThat(release.resourceClass()).isEqualTo("medium");
            assertThat(release.sessionTtlMinutes()).isEqualTo(180);
            assertThat(release.mutableDependencies()).containsExactly("redis", "postgres");
            assertThat(release.images().frontend()).contains("@sha256:");
            assertThat(release.images().backend()).contains("@sha256:");
        });
        assertThat(service.findDefaultRelease("4_agent_memory")).hasValueSatisfying(release -> {
            assertThat(release.releaseId()).isEqualTo("agent-memory-2026.04.1");
            assertThat(release.releaseVersion()).isEqualTo("2026.04.1");
            assertThat(release.resourceClass()).isEqualTo("medium");
            assertThat(release.sessionTtlMinutes()).isEqualTo(180);
            assertThat(release.mutableDependencies()).containsExactly("redis");
            assertThat(release.images().frontend()).contains("@sha256:");
            assertThat(release.images().backend()).contains("@sha256:");
        });
    }

    @Test
    void rejectsMutableImageReferences() throws IOException {
        Path registry = tempDir.resolve("workshops.yaml");
        Files.writeString(registry, """
            version: 1
            workshops:
              - id: 1_session_management
                releases:
                  - releaseId: session-management-current
                    releaseVersion: current
                    mode: LAB
                    defaultForWorkshop: true
                    enabled: true
                    environments:
                      - local
                    images:
                      frontend: registry.example.com/workshops/session-management-frontend:latest
                    resourceClass: small
                    sessionTtlMinutes: 60
            """);

        ReleaseCatalogService service = new ReleaseCatalogService(
            new ReleaseCatalogLoader(new DefaultResourceLoader(), registry.toString()),
            mock(ReleaseCatalogAuditHooks.class),
            new ReleaseCatalogMetricsRecorder(new SimpleMeterRegistry())
        );

        assertThatThrownBy(() -> service.findDefaultRelease("1_session_management"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Invalid release catalog")
            .hasMessageContaining("immutable");
    }

    @Test
    void rejectsAmbiguousDefaultReleaseSelection() throws IOException {
        Path registry = tempDir.resolve("workshops.yaml");
        Files.writeString(registry, """
            version: 1
            workshops:
              - id: 1_session_management
                releases:
                  - releaseId: session-management-2026.04.1
                    releaseVersion: 2026.04.1
                    mode: LAB
                    enabled: true
                    environments:
                      - local
                    images:
                      frontend: registry.example.com/workshops/session-management-frontend@sha256:1111111111111111111111111111111111111111111111111111111111111111
                    resourceClass: small
                    sessionTtlMinutes: 60
                  - releaseId: session-management-2026.04.2
                    releaseVersion: 2026.04.2
                    mode: LAB
                    enabled: true
                    environments:
                      - local
                    images:
                      frontend: registry.example.com/workshops/session-management-frontend@sha256:2222222222222222222222222222222222222222222222222222222222222222
                    resourceClass: small
                    sessionTtlMinutes: 60
            """);

        ReleaseCatalogService service = new ReleaseCatalogService(
            new ReleaseCatalogLoader(new DefaultResourceLoader(), registry.toString()),
            mock(ReleaseCatalogAuditHooks.class),
            new ReleaseCatalogMetricsRecorder(new SimpleMeterRegistry())
        );

        assertThatThrownBy(() -> service.findDefaultRelease("1_session_management"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("exactly one default release");
    }
}
