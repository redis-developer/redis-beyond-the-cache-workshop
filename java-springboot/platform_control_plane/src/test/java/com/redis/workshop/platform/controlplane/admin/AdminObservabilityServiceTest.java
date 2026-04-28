package com.redis.workshop.platform.controlplane.admin;

import com.redis.workshop.platform.controlplane.audit.AuditEventResponse;
import com.redis.workshop.platform.controlplane.audit.AuditTrailService;
import com.redis.workshop.platform.controlplane.observability.SessionLifecycleMetricsRecorder;
import com.redis.workshop.platform.controlplane.observability.SessionMetricsSnapshot;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionMode;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.model.RouteType;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspaceCleanupState;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspacePolicy;
import com.redis.workshop.platform.controlplane.persistence.repository.PlatformSessionRecordRepository;
import com.redis.workshop.platform.controlplane.policy.AdminActionPolicy;
import com.redis.workshop.platform.controlplane.policy.PolicyDecision;
import com.redis.workshop.platform.controlplane.security.CurrentActor;
import com.redis.workshop.platform.controlplane.security.CurrentActorProvider;
import com.redis.workshop.platform.controlplane.security.CurrentActorType;
import com.redis.workshop.platform.controlplane.security.PlatformAuthorities;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminObservabilityServiceTest {

    @Mock
    private PlatformSessionRecordRepository platformSessionRecordRepository;

    @Mock
    private CurrentActorProvider currentActorProvider;

    @Mock
    private AdminActionPolicy adminActionPolicy;

    @Mock
    private AuditTrailService auditTrailService;

    @Mock
    private SessionLifecycleMetricsRecorder sessionLifecycleMetricsRecorder;

    @Mock
    private ObjectProvider<jakarta.servlet.http.HttpServletRequest> httpServletRequestProvider;

    private AdminObservabilityService adminObservabilityService;

    @BeforeEach
    void setUp() {
        adminObservabilityService = new AdminObservabilityService(
            platformSessionRecordRepository,
            currentActorProvider,
            adminActionPolicy,
            auditTrailService,
            sessionLifecycleMetricsRecorder,
            httpServletRequestProvider
        );
    }

    @Test
    void buildsAdminOverviewFromSessionAndAuditData() {
        when(currentActorProvider.requireCurrentActor()).thenReturn(new CurrentActor(
            "admin-1",
            CurrentActorType.ADMIN,
            java.util.Set.of(PlatformAuthorities.ADMIN)
        ));
        when(adminActionPolicy.canPerform(any())).thenReturn(PolicyDecision.allow("admin_override"));
        when(platformSessionRecordRepository.findAll()).thenReturn(List.of(
            session("sess-ready", PlatformSessionState.READY, null, Instant.parse("2026-04-21T10:10:00Z")),
            session("sess-failed", PlatformSessionState.FAILED, "startup_timeout", Instant.parse("2026-04-21T10:20:00Z")),
            session("sess-pending", PlatformSessionState.PROVISIONING, null, Instant.parse("2026-04-21T10:30:00Z"))
        ));
        when(auditTrailService.getRecentEvents(20)).thenReturn(List.of());
        when(sessionLifecycleMetricsRecorder.snapshot()).thenReturn(new SessionMetricsSnapshot(3, 2, 1, 120));

        AdminObservabilityOverviewResponse overview = adminObservabilityService.getOverview();

        assertThat(overview.activeSessions()).hasSize(2);
        assertThat(overview.recentProvisioningFailures()).extracting(AdminProvisioningFailureSummaryResponse::failureCode)
            .containsExactly("startup_timeout");
        assertThat(overview.pendingSessions()).extracting(AdminPendingSessionResponse::sessionId)
            .contains("sess-pending");
        verify(auditTrailService).record(any());
    }

    @Test
    void rejectsLearnerAccessToAdminOverview() {
        when(currentActorProvider.requireCurrentActor()).thenReturn(new CurrentActor(
            "learner-1",
            CurrentActorType.LEARNER,
            java.util.Set.of(PlatformAuthorities.LEARNER)
        ));
        when(adminActionPolicy.canPerform(any())).thenReturn(PolicyDecision.deny("admin_action_not_authorized"));

        assertThatThrownBy(() -> adminObservabilityService.getOverview())
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("403 FORBIDDEN");
    }

    private PlatformSessionRecord session(String sessionId, PlatformSessionState state, String failureCode, Instant lastActivityAt) {
        PlatformSessionRecord record = new PlatformSessionRecord();
        record.setSessionId(sessionId);
        record.setOwnerUserId("learner-1");
        record.setWorkshopId("1_session_management");
        record.setReleaseVersion("current");
        record.setMode(PlatformSessionMode.LAB);
        record.setState(state);
        record.setQuotaClass("standard");
        record.setResourceClass("small");
        record.setCreatedAt(Instant.parse("2026-04-21T10:00:00Z"));
        record.setExpiresAt(Instant.parse("2026-04-21T11:00:00Z"));
        record.setLastActivityAt(lastActivityAt);
        record.setRouteType(RouteType.SUBDOMAIN);
        record.setWorkspacePolicy(WorkspacePolicy.EPHEMERAL);
        record.setWorkspaceRef("session-workspace:" + sessionId);
        record.setWorkspaceCleanupState(WorkspaceCleanupState.ACTIVE);
        record.setFailureCode(failureCode);
        return record;
    }
}
