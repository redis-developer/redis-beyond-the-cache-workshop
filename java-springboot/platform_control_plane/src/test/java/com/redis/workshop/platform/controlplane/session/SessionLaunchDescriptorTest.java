package com.redis.workshop.platform.controlplane.session;

import com.redis.workshop.platform.controlplane.release.ReleaseCatalogEntry;
import com.redis.workshop.platform.controlplane.release.ReleaseImageReferences;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionLaunchDescriptorTest {

    @Test
    void addsRedisRuntimeConfigWhenReleaseRequiresRedis() {
        SessionLaunchDescriptor descriptor = SessionLaunchDescriptor.from(
            "sess-001",
            redisRelease(),
            Duration.ofMinutes(60),
            SessionMode.LAB
        );

        assertThat(descriptor.runtimeConfig())
            .containsEntry("WORKSHOP_REDIS_MODE", "local-process")
            .containsEntry("WORKSHOP_REDIS_KEY_PREFIX", "session:sess-001:")
            .containsEntry("mutableDependencies", "redis")
            .doesNotContainKey("WORKSHOP_REDIS_PASSWORD");
    }

    @Test
    void skipsRedisRuntimeConfigWhenReleaseDoesNotRequireRedis() {
        ReleaseCatalogEntry release = new ReleaseCatalogEntry(
            "stateless-2026.04.1",
            "stateless",
            "2026.04.1",
            SessionMode.LAB,
            true,
            new ReleaseImageReferences(
                "registry.example.com/workshops/stateless-frontend@sha256:1111111111111111111111111111111111111111111111111111111111111111",
                "registry.example.com/workshops/stateless-backend@sha256:2222222222222222222222222222222222222222222222222222222222222222",
                null,
                null
            ),
            "small",
            60,
            List.of()
        );

        SessionLaunchDescriptor descriptor = SessionLaunchDescriptor.from(
            "sess-002",
            release,
            Duration.ofMinutes(60),
            SessionMode.LAB
        );

        assertThat(descriptor.runtimeConfig())
            .containsEntry("mutableDependencies", "")
            .doesNotContainKey("WORKSHOP_REDIS_MODE");
    }

    @Test
    void includesCombinedRunnerImageArtifactWhenDeclared() {
        SessionLaunchDescriptor descriptor = SessionLaunchDescriptor.from(
            "sess-003",
            redisRelease(),
            Duration.ofMinutes(60),
            SessionMode.LAB
        );

        assertThat(descriptor.artifacts())
            .containsEntry(
                "combined",
                "registry.example.com/workshops/session-management-runner@sha256:9999999999999999999999999999999999999999999999999999999999999999"
            );
    }

    @Test
    void allocatesEnoughMemoryForInContainerJavaRebuilds() {
        SessionLaunchDescriptor descriptor = SessionLaunchDescriptor.from(
            "sess-004",
            redisRelease(),
            Duration.ofMinutes(60),
            SessionMode.LAB
        );

        assertThat(descriptor.resourcePolicy().memoryMiB()).isEqualTo(2048);
    }

    private ReleaseCatalogEntry redisRelease() {
        return new ReleaseCatalogEntry(
            "session-management-2026.04.1",
            "1_session_management",
            "2026.04.1",
            SessionMode.LAB,
            true,
            new ReleaseImageReferences(
                "registry.example.com/workshops/session-management-frontend@sha256:1111111111111111111111111111111111111111111111111111111111111111",
                "registry.example.com/workshops/session-management-backend@sha256:2222222222222222222222222222222222222222222222222222222222222222",
                "registry.example.com/workshops/session-management-runner@sha256:9999999999999999999999999999999999999999999999999999999999999999",
                null
            ),
            "small",
            60,
            List.of("redis")
        );
    }
}
