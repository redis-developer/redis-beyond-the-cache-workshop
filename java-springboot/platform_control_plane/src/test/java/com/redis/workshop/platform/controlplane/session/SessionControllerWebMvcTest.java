package com.redis.workshop.platform.controlplane.session;

import com.redis.workshop.platform.controlplane.audit.AuditTrailService;
import com.redis.workshop.platform.controlplane.audit.ReleaseCatalogAuditHooks;
import com.redis.workshop.platform.controlplane.observability.SessionLifecycleMetricsRecorder;
import com.redis.workshop.platform.controlplane.persistence.model.RouteType;
import com.redis.workshop.platform.controlplane.portal.PortalSessionService;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogService;
import com.redis.workshop.platform.controlplane.security.HeaderAuthenticatedActorFilter;
import com.redis.workshop.platform.controlplane.security.SecurityConfiguration;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.ErrorResponseException;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SessionController.class)
@Import(SecurityConfiguration.class)
class SessionControllerWebMvcTest {

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
    void rejectsAnonymousSessionCreation() throws Exception {
        mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workshopId\":\"1_session_management\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createsSessionsAsAcceptedAsyncResources() throws Exception {
        given(sessionService.createSession(any())).willReturn(sampleSession("sess-001", SessionState.ADMITTED));

        mockMvc.perform(post("/api/sessions")
                .header(HeaderAuthenticatedActorFilter.ACTOR_ID_HEADER, "learner-1")
                .cookie(new Cookie("portal_session", "opaque-token"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workshopId\":\"1_session_management\"}"))
            .andExpect(status().isAccepted())
            .andExpect(header().string("Location", "/api/sessions/sess-001"))
            .andExpect(jsonPath("$.sessionId").value("sess-001"))
            .andExpect(jsonPath("$.state").value("ADMITTED"));

        verify(portalSessionService).attachSession("opaque-token", "sess-001");
    }

    @Test
    void returnsExistingSessionOnDuplicateActiveSessionConflict() throws Exception {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            org.springframework.http.HttpStatus.CONFLICT,
            "An active session already exists for this workshop."
        );
        problemDetail.setProperty("message", "An active session already exists for this workshop.");
        problemDetail.setProperty("code", "active_session_exists");
        problemDetail.setProperty("existingSessionId", "sess-001");
        problemDetail.setProperty("existingSession", sampleSession("sess-001", SessionState.READY));
        given(sessionService.createSession(any()))
            .willThrow(new ErrorResponseException(org.springframework.http.HttpStatus.CONFLICT, problemDetail, null));

        mockMvc.perform(post("/api/sessions")
                .header(HeaderAuthenticatedActorFilter.ACTOR_ID_HEADER, "learner-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workshopId\":\"1_session_management\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("active_session_exists"))
            .andExpect(jsonPath("$.existingSessionId").value("sess-001"))
            .andExpect(jsonPath("$.existingSession.sessionId").value("sess-001"));
    }

    @Test
    void listsSessionsForAuthenticatedLearners() throws Exception {
        given(sessionService.listSessions()).willReturn(List.of(sampleSession("sess-001", SessionState.READY)));

        mockMvc.perform(get("/api/sessions")
                .header(HeaderAuthenticatedActorFilter.ACTOR_ID_HEADER, "learner-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sessionId").value("sess-001"));
    }

    @Test
    void returnsSessionShellContract() throws Exception {
        given(sessionService.getSessionShell("sess-001")).willReturn(sampleShell("sess-001"));

        mockMvc.perform(get("/api/sessions/sess-001/shell")
                .header(HeaderAuthenticatedActorFilter.ACTOR_ID_HEADER, "learner-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sessionId").value("sess-001"))
            .andExpect(jsonPath("$.workshopId").value("1_session_management"))
            .andExpect(jsonPath("$.learnerAppUrl").value("/session/sess-001/"))
            .andExpect(jsonPath("$.redisInsightUrl").value("/session/sess-001/redis-insight/"))
            .andExpect(jsonPath("$.hubUrl").value("/"))
            .andExpect(jsonPath("$.runtime.state").value("READY"))
            .andExpect(jsonPath("$.frontend.state").value("READY"))
            .andExpect(jsonPath("$.backend.state").value("READY"))
            .andExpect(jsonPath("$.actions[2].name").value("restartRuntime"))
            .andExpect(jsonPath("$.actions[2].url").value("/api/sessions/sess-001/restart"))
            .andExpect(jsonPath("$.actions[3].name").value("rebuildRuntime"))
            .andExpect(jsonPath("$.actions[3].available").value(true))
            .andExpect(jsonPath("$.actions[3].body.rebuild").value(true));
    }

    @Test
    void restartsSessionsAsAcceptedAsyncResources() throws Exception {
        given(sessionService.restartSession(any(), any())).willReturn(sampleSession("sess-001", SessionState.PROVISIONING));

        mockMvc.perform(post("/api/sessions/sess-001/restart")
                .header(HeaderAuthenticatedActorFilter.ACTOR_ID_HEADER, "learner-1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rebuild\":true}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.state").value("PROVISIONING"));
    }

    @Test
    void terminatesSessionsAsAcceptedAsyncResources() throws Exception {
        given(sessionService.terminateSession("sess-001")).willReturn(sampleSession("sess-001", SessionState.TERMINATING));

        mockMvc.perform(delete("/api/sessions/sess-001")
                .header(HeaderAuthenticatedActorFilter.ACTOR_ID_HEADER, "learner-1")
                .cookie(new Cookie("portal_session", "opaque-token")))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.state").value("TERMINATING"));

        verify(portalSessionService).detachSession("opaque-token", "sess-001");
    }

    private SessionResponse sampleSession(String sessionId, SessionState state) {
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
            state == SessionState.TERMINATING ? "user_requested" : null
        );
    }

    private SessionShellResponse sampleShell(String sessionId) {
        return new SessionShellResponse(
            sessionId,
            "1_session_management",
            "/session/" + sessionId + "/",
            "/session/" + sessionId + "/redis-insight/",
            "/",
            new SessionShellResponse.ComponentStatus("READY", "ready", null),
            new SessionShellResponse.ComponentStatus("READY", "route_available", null),
            new SessionShellResponse.ComponentStatus("READY", "ready", null),
            List.of(
                new SessionShellResponse.Action("openLearnerApp", true, "GET", "/session/" + sessionId + "/", null, null),
                new SessionShellResponse.Action(
                    "openRedisInsight",
                    true,
                    "GET",
                    "/session/" + sessionId + "/redis-insight/",
                    null,
                    null
                ),
                new SessionShellResponse.Action(
                    "restartRuntime",
                    true,
                    "POST",
                    "/api/sessions/" + sessionId + "/restart",
                    Map.of("rebuild", false),
                    null
                ),
                new SessionShellResponse.Action(
                    "rebuildRuntime",
                    true,
                    "POST",
                    "/api/sessions/" + sessionId + "/restart",
                    Map.of("rebuild", true),
                    null
                ),
                new SessionShellResponse.Action(
                    "terminateSession",
                    true,
                    "DELETE",
                    "/api/sessions/" + sessionId,
                    null,
                    null
                )
            )
        );
    }
}
