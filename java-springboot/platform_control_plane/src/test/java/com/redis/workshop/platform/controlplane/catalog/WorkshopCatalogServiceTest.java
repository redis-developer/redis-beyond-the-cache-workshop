package com.redis.workshop.platform.controlplane.catalog;

import com.redis.workshop.platform.controlplane.policy.CatalogAccessPolicy;
import com.redis.workshop.platform.controlplane.policy.PolicyDecision;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogEntry;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogService;
import com.redis.workshop.platform.controlplane.release.ReleaseImageReferences;
import com.redis.workshop.platform.controlplane.release.PilotLaunchRuleService;
import com.redis.workshop.platform.controlplane.release.WorkshopLaunchDefaults;
import com.redis.workshop.platform.controlplane.security.CurrentActorProvider;
import com.redis.workshop.platform.controlplane.session.SessionMode;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkshopCatalogServiceTest {

    @Test
    void usesReleaseCatalogDefaultReleaseVersionWhenAvailable() {
        WorkshopCatalogLoader loader = mock(WorkshopCatalogLoader.class);
        ReleaseCatalogService releaseCatalogService = mock(ReleaseCatalogService.class);
        PilotLaunchRuleService pilotLaunchRuleService = mock(PilotLaunchRuleService.class);
        CatalogAccessPolicy catalogAccessPolicy = mock(CatalogAccessPolicy.class);
        CurrentActorProvider currentActorProvider = mock(CurrentActorProvider.class);

        when(loader.loadWorkshops()).thenReturn(List.of(workshop("1_session_management")));
        when(pilotLaunchRuleService.resolveLaunchDefaults("1_session_management")).thenReturn(
            WorkshopLaunchDefaults.releaseBacked(new ReleaseCatalogEntry(
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
                List.of("redis")
            ))
        );
        when(releaseCatalogService.findSupportedModes("1_session_management")).thenReturn(List.of(SessionMode.LAB));
        when(catalogAccessPolicy.canRead(any())).thenReturn(PolicyDecision.allow("public"));
        when(currentActorProvider.findCurrentActor()).thenReturn(Optional.empty());

        WorkshopCatalogService service = new WorkshopCatalogService(
            loader,
            releaseCatalogService,
            pilotLaunchRuleService,
            catalogAccessPolicy,
            currentActorProvider
        );

        WorkshopCatalogEntry entry = service.getWorkshopEntry("1_session_management");

        assertThat(entry.defaultReleaseVersion()).isEqualTo("2026.04.1");
        assertThat(entry.defaultMode()).isEqualTo(SessionMode.LAB);
        assertThat(entry.supportedModes()).containsExactly(SessionMode.LAB);
    }

    @Test
    void usesReleaseCatalogDefaultReleaseVersionForRemainingCutoverWorkshop() {
        WorkshopCatalogLoader loader = mock(WorkshopCatalogLoader.class);
        ReleaseCatalogService releaseCatalogService = mock(ReleaseCatalogService.class);
        PilotLaunchRuleService pilotLaunchRuleService = mock(PilotLaunchRuleService.class);
        CatalogAccessPolicy catalogAccessPolicy = mock(CatalogAccessPolicy.class);
        CurrentActorProvider currentActorProvider = mock(CurrentActorProvider.class);

        when(loader.loadWorkshops()).thenReturn(List.of(workshop("3_distributed_locks")));
        when(pilotLaunchRuleService.resolveLaunchDefaults("3_distributed_locks")).thenReturn(
            WorkshopLaunchDefaults.releaseBacked(new ReleaseCatalogEntry(
                "distributed-locks-2026.04.1",
                "3_distributed_locks",
                "2026.04.1",
                SessionMode.LAB,
                true,
                new ReleaseImageReferences(
                    "registry.example.com/workshops/distributed-locks-frontend@sha256:5555555555555555555555555555555555555555555555555555555555555555",
                    "registry.example.com/workshops/distributed-locks-backend@sha256:6666666666666666666666666666666666666666666666666666666666666666",
                    null,
                    null
                ),
                "medium",
                60,
                List.of("redis", "postgres")
            ))
        );
        when(releaseCatalogService.findSupportedModes("3_distributed_locks")).thenReturn(List.of(SessionMode.LAB));
        when(catalogAccessPolicy.canRead(any())).thenReturn(PolicyDecision.allow("public"));
        when(currentActorProvider.findCurrentActor()).thenReturn(Optional.empty());

        WorkshopCatalogService service = new WorkshopCatalogService(
            loader,
            releaseCatalogService,
            pilotLaunchRuleService,
            catalogAccessPolicy,
            currentActorProvider
        );

        WorkshopCatalogEntry entry = service.getWorkshopEntry("3_distributed_locks");

        assertThat(entry.defaultReleaseVersion()).isEqualTo("2026.04.1");
        assertThat(entry.defaultMode()).isEqualTo(SessionMode.LAB);
        assertThat(entry.supportedModes()).containsExactly(SessionMode.LAB);
    }

    @Test
    void keepsNonReleaseBackedWorkshopsOnCurrentReleaseVersion() {
        WorkshopCatalogLoader loader = mock(WorkshopCatalogLoader.class);
        ReleaseCatalogService releaseCatalogService = mock(ReleaseCatalogService.class);
        PilotLaunchRuleService pilotLaunchRuleService = mock(PilotLaunchRuleService.class);
        CatalogAccessPolicy catalogAccessPolicy = mock(CatalogAccessPolicy.class);
        CurrentActorProvider currentActorProvider = mock(CurrentActorProvider.class);

        when(loader.loadWorkshops()).thenReturn(List.of(workshop("99_unknown")));
        when(pilotLaunchRuleService.resolveLaunchDefaults("99_unknown")).thenReturn(WorkshopLaunchDefaults.current());
        when(catalogAccessPolicy.canRead(any())).thenReturn(PolicyDecision.allow("public"));
        when(currentActorProvider.findCurrentActor()).thenReturn(Optional.empty());

        WorkshopCatalogService service = new WorkshopCatalogService(
            loader,
            releaseCatalogService,
            pilotLaunchRuleService,
            catalogAccessPolicy,
            currentActorProvider
        );

        WorkshopCatalogEntry entry = service.getWorkshopEntry("99_unknown");

        assertThat(entry.defaultReleaseVersion()).isEqualTo("current");
        assertThat(entry.defaultMode()).isEqualTo(SessionMode.LAB);
        assertThat(entry.supportedModes()).containsExactly(SessionMode.LAB);
    }

    private WorkshopCatalogLoader.WorkshopCatalogDocument workshop(String workshopId) {
        WorkshopCatalogLoader.WorkshopCatalogDocument document = new WorkshopCatalogLoader.WorkshopCatalogDocument();
        document.setId(workshopId);
        document.setTitle("Distributed Session Management");
        document.setDescription("Learn Redis backed sessions");
        document.setDifficulty("Beginner");
        document.setEstimatedMinutes(30);
        document.setUrl("/workshop/session-management/");
        document.setTopics(List.of("Sessions"));
        return document;
    }
}
