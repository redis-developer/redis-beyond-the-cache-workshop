package com.redis.workshop.platform.controlplane.observability;

import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionMode;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.model.RouteType;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspaceCleanupState;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspacePolicy;
import com.redis.workshop.platform.controlplane.persistence.repository.PlatformSessionRecordRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionLifecycleMetricsRecorderTest {

    @Mock
    private PlatformSessionRecordRepository platformSessionRecordRepository;

    @Test
    void refreshesCountersAndGaugesFromRepositoryData() {
        when(platformSessionRecordRepository.findAll()).thenReturn(List.of(
            session("sess-ready", PlatformSessionState.READY),
            session("sess-pending", PlatformSessionState.PROVISIONING),
            session("sess-ended", PlatformSessionState.TERMINATED)
        ));

        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        SessionLifecycleMetricsRecorder recorder = new SessionLifecycleMetricsRecorder(platformSessionRecordRepository, meterRegistry);
        recorder.recordSessionCreateRequest();
        recorder.recordSessionCreateSuccess(null);
        recorder.refreshSessionGauges();

        assertThat(recorder.snapshot().sessionCreateRequestCount()).isEqualTo(1.0);
        assertThat(recorder.snapshot().sessionCreateSuccessCount()).isEqualTo(1.0);
        assertThat(meterRegistry.get("platform.controlplane.active_sessions").gauges()).isNotEmpty();
        assertThat(meterRegistry.get("platform.controlplane.session_state_count").gauges()).isNotEmpty();
    }

    private PlatformSessionRecord session(String sessionId, PlatformSessionState state) {
        PlatformSessionRecord record = new PlatformSessionRecord();
        record.setSessionId(sessionId);
        record.setOwnerUserId("learner-1");
        record.setWorkshopId("1_session_management");
        record.setReleaseVersion("current");
        record.setMode(PlatformSessionMode.LAB);
        record.setState(state);
        record.setQuotaClass("standard");
        record.setCreatedAt(Instant.parse("2026-04-21T10:00:00Z"));
        record.setExpiresAt(Instant.parse("2026-04-21T11:00:00Z"));
        record.setLastActivityAt(Instant.parse("2026-04-21T10:10:00Z"));
        record.setRouteType(RouteType.SUBDOMAIN);
        record.setWorkspacePolicy(WorkspacePolicy.EPHEMERAL);
        record.setWorkspaceRef("session-workspace:" + sessionId);
        record.setWorkspaceCleanupState(WorkspaceCleanupState.ACTIVE);
        return record;
    }
}
