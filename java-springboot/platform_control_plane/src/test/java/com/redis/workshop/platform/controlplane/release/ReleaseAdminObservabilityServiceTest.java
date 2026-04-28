package com.redis.workshop.platform.controlplane.release;

import com.redis.workshop.platform.controlplane.audit.AuditEventResponse;
import com.redis.workshop.platform.controlplane.audit.AuditTrailService;
import com.redis.workshop.platform.controlplane.observability.ReleaseCatalogMetricsRecorder;
import com.redis.workshop.platform.controlplane.observability.ReleaseCatalogMetricsSnapshot;
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

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReleaseAdminObservabilityServiceTest {

    @Mock
    private ReleaseCatalogService releaseCatalogService;

    @Mock
    private ReleaseCatalogMetricsRecorder releaseCatalogMetricsRecorder;

    @Mock
    private CurrentActorProvider currentActorProvider;

    @Mock
    private AdminActionPolicy adminActionPolicy;

    @Mock
    private AuditTrailService auditTrailService;

    @Mock
    private ObjectProvider<jakarta.servlet.http.HttpServletRequest> httpServletRequestProvider;

    private ReleaseAdminObservabilityService service;

    @BeforeEach
    void setUp() {
        service = new ReleaseAdminObservabilityService(
            releaseCatalogService,
            releaseCatalogMetricsRecorder,
            currentActorProvider,
            adminActionPolicy,
            auditTrailService,
            httpServletRequestProvider
        );
    }

    @Test
    void returnsReleaseAdminOverview() {
        when(currentActorProvider.requireCurrentActor()).thenReturn(new CurrentActor(
            "admin-1",
            CurrentActorType.ADMIN,
            Set.of(PlatformAuthorities.ADMIN)
        ));
        when(adminActionPolicy.canPerform(any())).thenReturn(PolicyDecision.allow("admin_override"));
        when(releaseCatalogService.inspectCatalog()).thenReturn(ReleaseCatalogInspection.available(List.of(sampleRelease())));
        when(releaseCatalogMetricsRecorder.snapshot()).thenReturn(new ReleaseCatalogMetricsSnapshot(
            3.0,
            1.0,
            0.0,
            2.0,
            1,
            15,
            Instant.parse("2026-04-22T10:00:00Z"),
            Instant.parse("2026-04-22T10:00:05Z"),
            null,
            null
        ));
        when(auditTrailService.getRecentEventsByTargetPrefix("release:", 20)).thenReturn(List.of(
            new AuditEventResponse(
                UUID.randomUUID(),
                "release-catalog",
                "INTERNAL_SERVICE",
                "ADMIN_OBSERVABILITY_READ",
                "ADMIN_ACTION",
                "release:catalog-activated",
                Instant.parse("2026-04-22T10:00:06Z"),
                "SUCCESS",
                "release_count=1",
                null,
                null,
                null,
                null,
                null
            )
        ));

        ReleaseAdminOverviewResponse overview = service.getOverview();

        assertThat(overview.catalogAvailable()).isTrue();
        assertThat(overview.releases()).hasSize(1);
        assertThat(overview.metrics().catalogLoadSuccessCount()).isEqualTo(3.0);
        verify(auditTrailService).record(any());
    }

    private ReleaseCatalogEntry sampleRelease() {
        return new ReleaseCatalogEntry(
            "session-management-2026.04.1",
            "1_session_management",
            "2026.04.1",
            com.redis.workshop.platform.controlplane.session.SessionMode.LAB,
            true,
            new ReleaseImageReferences(
                "registry.example.com/workshops/session-management-frontend@sha256:1111111111111111111111111111111111111111111111111111111111111111",
                "registry.example.com/workshops/session-management-backend@sha256:2222222222222222222222222222222222222222222222222222222222222222",
                null,
                null
            ),
            "small",
            60,
            List.of("redis")
        );
    }
}
