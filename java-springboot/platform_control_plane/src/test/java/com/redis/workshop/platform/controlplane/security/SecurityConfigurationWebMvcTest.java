package com.redis.workshop.platform.controlplane.security;

import com.redis.workshop.platform.controlplane.catalog.CatalogController;
import com.redis.workshop.platform.controlplane.catalog.CatalogWorkshopResponse;
import com.redis.workshop.platform.controlplane.catalog.WorkshopCatalogService;
import com.redis.workshop.platform.controlplane.console.ControlPlaneConsoleController;
import com.redis.workshop.platform.controlplane.audit.AuditTrailService;
import com.redis.workshop.platform.controlplane.audit.ReleaseCatalogAuditHooks;
import com.redis.workshop.platform.controlplane.observability.SessionLifecycleMetricsRecorder;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogService;
import com.redis.workshop.platform.controlplane.persistence.model.RouteType;
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
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {CatalogController.class, SessionController.class, ControlPlaneConsoleController.class})
@Import(SecurityConfiguration.class)
class SecurityConfigurationWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkshopCatalogService workshopCatalogService;

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

    @Test
    void allowsAnonymousControlPlaneConsole() throws Exception {
        mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andExpect(forwardedUrl("/index.html"));
    }

    @Test
    void allowsAnonymousHubStaticAssets() throws Exception {
        mockMvc.perform(get("/js/app.js"))
            .andExpect(status().isNotFound());
    }

    @Test
    void allowsAnonymousCatalogReads() throws Exception {
        given(workshopCatalogService.getVisibleWorkshops()).willReturn(List.of(
            new CatalogWorkshopResponse(
                "1_session_management",
                "Distributed Session Management",
                "Learn Redis backed sessions",
                "Beginner",
                30,
                "/workshop/session-management/",
                "current",
                SessionMode.LAB,
                List.of(SessionMode.LAB),
                List.of("Sessions")
            )
        ));

        mockMvc.perform(get("/api/catalog/workshops"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].workshopId").value("1_session_management"));
    }

    @Test
    void requiresAuthenticationForSessionCreation() throws Exception {
        mockMvc.perform(post("/api/sessions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"workshopId\":\"1_session_management\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void requiresAuthenticationForSessionRestart() throws Exception {
        mockMvc.perform(post("/api/sessions/sess-001/restart")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rebuild\":true}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void acceptsLearnerIdentityOnProtectedSessionRoutes() throws Exception {
        given(sessionService.listSessions()).willReturn(List.of(sampleSession("sess-001")));

        mockMvc.perform(get("/api/sessions")
                .header(HeaderAuthenticatedActorFilter.ACTOR_ID_HEADER, "learner-1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].sessionId").value("sess-001"));
    }

    @Test
    void allowsAnonymousSessionProxyRoutesThroughSecurity() throws Exception {
        mockMvc.perform(get("/session/sess-001/"))
            .andExpect(status().isNotFound());
    }

    @Test
    void exposesInternalServiceIdentityWithinProtectedRoutes() throws Exception {
        given(sessionService.listSessions()).willReturn(List.of(sampleSession("sess-001")));

        mockMvc.perform(get("/api/sessions")
                .header(HeaderAuthenticatedActorFilter.ACTOR_ID_HEADER, "svc-launcher")
                .header(HeaderAuthenticatedActorFilter.ACTOR_TYPE_HEADER, "INTERNAL_SERVICE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].state").value("READY"));
    }

    @Test
    void deniesUnexpectedRoutesByDefault() throws Exception {
        mockMvc.perform(get("/api/admin/ping")
                .header(HeaderAuthenticatedActorFilter.ACTOR_ID_HEADER, "admin-1")
                .header(HeaderAuthenticatedActorFilter.ACTOR_TYPE_HEADER, "ADMIN"))
            .andExpect(status().isForbidden());
    }

    private SessionResponse sampleSession(String sessionId) {
        return new SessionResponse(
            sessionId,
            "1_session_management",
            "current",
            SessionMode.LAB,
            SessionState.READY,
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
            null
        );
    }
}
