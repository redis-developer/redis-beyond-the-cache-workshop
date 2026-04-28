package com.redis.workshop.platform.controlplane.observability;

import com.redis.workshop.platform.controlplane.release.ReleaseCatalogEntry;
import com.redis.workshop.platform.controlplane.release.ReleaseImageReferences;
import com.redis.workshop.platform.controlplane.session.SessionMode;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReleaseCatalogMetricsRecorderTest {

    @Test
    void recordsCatalogLoadAndLaunchMetrics() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        ReleaseCatalogMetricsRecorder recorder = new ReleaseCatalogMetricsRecorder(meterRegistry);

        recorder.recordCatalogLoadSuccess(4);
        recorder.recordLookupMiss("1_session_management", "missing");
        recorder.recordReleaseLaunchAttempt(sampleRelease(), "create");
        recorder.refreshCatalogStaleness();

        ReleaseCatalogMetricsSnapshot snapshot = recorder.snapshot();
        assertThat(snapshot.catalogLoadSuccessCount()).isEqualTo(1.0);
        assertThat(snapshot.releaseLookupMissCount()).isEqualTo(1.0);
        assertThat(snapshot.releaseLaunchAttemptCount()).isEqualTo(1.0);
        assertThat(snapshot.catalogEntryCount()).isEqualTo(4);
        assertThat(meterRegistry.get("platform.controlplane.release.launch.attempt.by_release").counter().count()).isEqualTo(1.0);
    }

    private ReleaseCatalogEntry sampleRelease() {
        return new ReleaseCatalogEntry(
            "session-management-2026.04.1",
            "1_session_management",
            "2026.04.1",
            SessionMode.LAB,
            true,
            new ReleaseImageReferences(
                "registry.example.com/workshops/session-management-frontend@sha256:1111111111111111111111111111111111111111111111111111111111111111",
                "registry.example.com/workshops/session-management-backend@sha256:2222222222222222222222222222222222222222222222222222222222222222",
                null,
                null
            ),
            "small",
            60,
            java.util.List.of("redis")
        );
    }
}
