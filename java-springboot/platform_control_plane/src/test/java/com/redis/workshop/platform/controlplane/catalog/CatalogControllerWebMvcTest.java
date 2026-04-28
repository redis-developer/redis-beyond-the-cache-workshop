package com.redis.workshop.platform.controlplane.catalog;

import com.redis.workshop.platform.controlplane.audit.AuditTrailService;
import com.redis.workshop.platform.controlplane.audit.ReleaseCatalogAuditHooks;
import com.redis.workshop.platform.controlplane.observability.SessionLifecycleMetricsRecorder;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogService;
import com.redis.workshop.platform.controlplane.security.SecurityConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CatalogController.class)
@Import(SecurityConfiguration.class)
class CatalogControllerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private WorkshopCatalogService workshopCatalogService;

    @MockitoBean
    private AuditTrailService auditTrailService;

    @MockitoBean
    private SessionLifecycleMetricsRecorder sessionLifecycleMetricsRecorder;

    @MockitoBean
    private ReleaseCatalogService releaseCatalogService;

    @MockitoBean
    private ReleaseCatalogAuditHooks releaseCatalogAuditHooks;

    @Test
    void listsCatalogEntriesWithoutAuthentication() throws Exception {
        given(workshopCatalogService.getVisibleWorkshops()).willReturn(List.of(
            new CatalogWorkshopResponse(
                "1_session_management",
                "Distributed Session Management",
                "Learn Redis backed sessions",
                "Beginner",
                30,
                "/workshop/session-management/",
                "2026.04.1",
                com.redis.workshop.platform.controlplane.session.SessionMode.LAB,
                List.of(com.redis.workshop.platform.controlplane.session.SessionMode.LAB),
                List.of("Sessions", "Redis")
            )
        ));

        mockMvc.perform(get("/api/catalog/workshops"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].workshopId").value("1_session_management"))
            .andExpect(jsonPath("$[0].defaultMode").value("LAB"));
    }

    @Test
    void returnsWorkshopDetailsForSingleEntry() throws Exception {
        given(workshopCatalogService.getVisibleWorkshop("3_distributed_locks")).willReturn(
            new CatalogWorkshopResponse(
                "3_distributed_locks",
                "Distributed Locks",
                "Protect inventory updates",
                "Intermediate",
                35,
                "/workshop/distributed-locks/",
                "2026.04.1",
                com.redis.workshop.platform.controlplane.session.SessionMode.LAB,
                List.of(com.redis.workshop.platform.controlplane.session.SessionMode.LAB),
                List.of("Locks")
            )
        );

        mockMvc.perform(get("/api/catalog/workshops/3_distributed_locks"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.workshopId").value("3_distributed_locks"))
            .andExpect(jsonPath("$.title").value("Distributed Locks"));
    }
}
