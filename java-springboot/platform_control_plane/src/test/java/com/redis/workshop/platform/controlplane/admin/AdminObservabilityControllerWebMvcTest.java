package com.redis.workshop.platform.controlplane.admin;

import com.redis.workshop.platform.controlplane.audit.AuditTrailService;
import com.redis.workshop.platform.controlplane.audit.ReleaseCatalogAuditHooks;
import com.redis.workshop.platform.controlplane.observability.SessionLifecycleMetricsRecorder;
import com.redis.workshop.platform.controlplane.observability.SessionMetricsSnapshot;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogService;
import com.redis.workshop.platform.controlplane.security.HeaderAuthenticatedActorFilter;
import com.redis.workshop.platform.controlplane.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AdminObservabilityController.class)
@Import(SecurityConfiguration.class)
class AdminObservabilityControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminObservabilityService adminObservabilityService;

    @MockitoBean
    private AuditTrailService auditTrailService;

    @MockitoBean
    private SessionLifecycleMetricsRecorder sessionLifecycleMetricsRecorder;

    @MockitoBean
    private ReleaseCatalogService releaseCatalogService;

    @MockitoBean
    private ReleaseCatalogAuditHooks releaseCatalogAuditHooks;

    @Test
    void requiresAuthenticationForAdminOverview() throws Exception {
        mockMvc.perform(get("/api/sessions/admin/overview"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsAdminOverviewForAuthenticatedAdminActors() throws Exception {
        given(adminObservabilityService.getOverview()).willReturn(new AdminObservabilityOverviewResponse(
            Instant.parse("2026-04-21T10:00:00Z"),
            List.of(new AdminSessionCountResponse("1_session_management", "LAB", "READY", 1)),
            List.of(),
            List.of(),
            List.of(),
            new SessionMetricsSnapshot(1, 1, 0, 0)
        ));

        mockMvc.perform(get("/api/sessions/admin/overview")
                .header(HeaderAuthenticatedActorFilter.ACTOR_ID_HEADER, "admin-1")
                .header(HeaderAuthenticatedActorFilter.ACTOR_TYPE_HEADER, "ADMIN"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.activeSessions[0].workshopId").value("1_session_management"))
            .andExpect(jsonPath("$.metrics.sessionCreateSuccessCount").value(1.0));
    }
}
