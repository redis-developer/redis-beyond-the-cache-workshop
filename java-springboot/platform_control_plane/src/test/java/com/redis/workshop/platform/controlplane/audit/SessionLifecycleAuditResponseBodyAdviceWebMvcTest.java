package com.redis.workshop.platform.controlplane.audit;

import com.redis.workshop.platform.controlplane.observability.ObservabilityConfiguration;
import com.redis.workshop.platform.controlplane.observability.SessionLifecycleMetricsRecorder;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogEntry;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogService;
import com.redis.workshop.platform.controlplane.persistence.model.RouteType;
import com.redis.workshop.platform.controlplane.portal.PortalSessionService;
import com.redis.workshop.platform.controlplane.security.HeaderAuthenticatedActorFilter;
import com.redis.workshop.platform.controlplane.security.SecurityConfiguration;
import com.redis.workshop.platform.controlplane.session.SessionController;
import com.redis.workshop.platform.controlplane.session.SessionMode;
import com.redis.workshop.platform.controlplane.session.SessionResponse;
import com.redis.workshop.platform.controlplane.session.SessionService;
import com.redis.workshop.platform.controlplane.session.SessionState;
import com.redis.workshop.platform.controlplane.session.WorkspaceCleanupStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.BDDMockito.given;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
@Import({SecurityConfiguration.class, SessionLifecycleAuditResponseBodyAdvice.class, ObservabilityConfiguration.class})
class SessionLifecycleAuditResponseBodyAdviceWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionService sessionService;

    @MockitoBean
    private AuditTrailService auditTrailService;

    @MockitoBean
    private SessionLifecycleMetricsRecorder sessionLifecycleMetricsRecorder;

    @MockitoBean
    private ReleaseCatalogService releaseCatalogService;

    @MockitoBean
    private ReleaseCatalogAuditHooks releaseCatalogAuditHooks;

    @MockitoBean
    private PortalSessionService portalSessionService;

    @Test
    void recordsSessionCreatedAuditEvent() throws Exception {
        given(sessionService.createSession(any())).willReturn(sampleSession("sess-001", SessionState.ADMITTED, null));
        given(releaseCatalogService.findRelease("1_session_management", "current")).willReturn(java.util.Optional.empty());

        mockMvc.perform(post("/api/sessions")
                .header(HeaderAuthenticatedActorFilter.ACTOR_ID_HEADER, "learner-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workshopId\":\"1_session_management\"}"))
            .andExpect(status().isAccepted());

        verify(auditTrailService).record(any());
        verify(sessionLifecycleMetricsRecorder).recordSessionCreateSuccess(any());
        verify(releaseCatalogAuditHooks).recordReleaseSelectionLookupMiss(any(), eq(com.redis.workshop.platform.controlplane.persistence.model.AuditActionType.SESSION_CREATED), any(), any(), eq("create"));
    }

    @Test
    void recordsSessionRestartAuditEvent() throws Exception {
        given(sessionService.restartSession(any(), any())).willReturn(sampleSession("sess-001", SessionState.PROVISIONING, null));
        given(releaseCatalogService.findRelease("1_session_management", "current")).willReturn(java.util.Optional.of(sampleRelease()));

        mockMvc.perform(post("/api/sessions/sess-001/restart")
                .header(HeaderAuthenticatedActorFilter.ACTOR_ID_HEADER, "learner-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rebuild\":true}"))
            .andExpect(status().isAccepted());

        verify(auditTrailService).record(any());
        verify(releaseCatalogAuditHooks).recordReleaseSelection(any(), eq(com.redis.workshop.platform.controlplane.persistence.model.AuditActionType.SESSION_RESTARTED_BY_USER), any(), any(), eq("restart"), any());
    }

    @Test
    void recordsAdminTerminationAuditEvent() throws Exception {
        given(sessionService.terminateSession("sess-001")).willReturn(sampleSession("sess-001", SessionState.TERMINATING, "admin_requested"));

        mockMvc.perform(delete("/api/sessions/sess-001")
                .header(HeaderAuthenticatedActorFilter.ACTOR_ID_HEADER, "admin-1")
                .header(HeaderAuthenticatedActorFilter.ACTOR_TYPE_HEADER, "ADMIN"))
            .andExpect(status().isAccepted());

        verify(auditTrailService).record(any());
        verify(sessionLifecycleMetricsRecorder).recordSessionTermination(any(), any());
    }

    private SessionResponse sampleSession(String sessionId, SessionState state, String terminationReason) {
        return new SessionResponse(
            sessionId,
            "1_session_management",
            "current",
            SessionMode.LAB,
            state,
            Instant.parse("2026-04-21T10:00:00Z"),
            Instant.parse("2026-04-21T11:00:00Z"),
            Instant.parse("2026-04-21T10:05:00Z"),
            null,
            null,
            null,
            null,
            "/api/sessions/" + sessionId,
            "/api/sessions/" + sessionId,
            RouteType.SUBDOMAIN,
            "session-workspace:" + sessionId,
            WorkspaceCleanupStatus.ACTIVE,
            null,
            null,
            terminationReason
        );
    }

    private ReleaseCatalogEntry sampleRelease() {
        return new ReleaseCatalogEntry(
            "session-management-2026.04.1",
            "1_session_management",
            "current",
            SessionMode.LAB,
            true,
            new com.redis.workshop.platform.controlplane.release.ReleaseImageReferences(
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
