package com.redis.workshop.platform.controlplane.release;

import com.redis.workshop.platform.controlplane.session.SessionMode;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PilotLaunchRuleServiceTest {

    @Test
    void resolvesReleaseBackedDefaultsForEnabledWorkshopInCurrentEnvironment() {
        ReleaseCatalogService releaseCatalogService = mock(ReleaseCatalogService.class);
        when(releaseCatalogService.findDefaultRelease("1_session_management")).thenReturn(Optional.of(sampleRelease(true, List.of("local"))));

        PilotLaunchRuleService service = new PilotLaunchRuleService(
            releaseCatalogService,
            "local"
        );

        WorkshopLaunchDefaults defaults = service.resolveLaunchDefaults("1_session_management");

        assertThat(defaults.releaseBacked()).isTrue();
        assertThat(defaults.defaultReleaseVersion()).isEqualTo("2026.04.1");
        assertThat(defaults.defaultMode()).isEqualTo(SessionMode.LAB);
    }

    @Test
    void usesCurrentLocalDevelopmentPathWhenReleaseIsDisabled() {
        ReleaseCatalogEntry disabledRelease = sampleRelease(false, List.of("local"));
        ReleaseCatalogService releaseCatalogService = mock(ReleaseCatalogService.class);
        when(releaseCatalogService.findDefaultRelease("1_session_management")).thenReturn(Optional.of(disabledRelease));
        when(releaseCatalogService.findRelease("1_session_management", "2026.04.1")).thenReturn(Optional.of(disabledRelease));

        PilotLaunchRuleService service = new PilotLaunchRuleService(
            releaseCatalogService,
            "local"
        );

        assertThat(service.resolveLaunchDefaults("1_session_management")).isEqualTo(WorkshopLaunchDefaults.current());
        assertThatThrownBy(() -> service.resolveRequestedReleaseVersion("1_session_management", "2026.04.1", "current"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("disabled or inactive");
    }

    @Test
    void usesCurrentLocalDevelopmentPathWhenEnvironmentDoesNotMatch() {
        ReleaseCatalogService releaseCatalogService = mock(ReleaseCatalogService.class);
        when(releaseCatalogService.findDefaultRelease("4_agent_memory"))
            .thenReturn(Optional.of(sampleWorkshopFourRelease(true, List.of("cloud-run"))));

        PilotLaunchRuleService service = new PilotLaunchRuleService(
            releaseCatalogService,
            "local"
        );

        assertThat(service.resolveLaunchDefaults("4_agent_memory")).isEqualTo(WorkshopLaunchDefaults.current());
    }

    @Test
    void resolvesRemainingWorkshopsFromClasspathRegistry() {
        ReleaseCatalogService releaseCatalogService = new ReleaseCatalogService(
            new ReleaseCatalogLoader(new DefaultResourceLoader(), "missing-workshops.yaml"),
            mock(com.redis.workshop.platform.controlplane.audit.ReleaseCatalogAuditHooks.class),
            new com.redis.workshop.platform.controlplane.observability.ReleaseCatalogMetricsRecorder(
                new io.micrometer.core.instrument.simple.SimpleMeterRegistry()
            )
        );

        PilotLaunchRuleService service = new PilotLaunchRuleService(
            releaseCatalogService,
            "local"
        );

        assertThat(service.resolveLaunchDefaults("3_distributed_locks"))
            .isEqualTo(new WorkshopLaunchDefaults("2026.04.1", SessionMode.LAB, true));
        assertThat(service.resolveLaunchDefaults("4_agent_memory"))
            .isEqualTo(new WorkshopLaunchDefaults("2026.04.1", SessionMode.LAB, true));
        assertThat(service.resolveRequestedReleaseVersion("3_distributed_locks", "2026.04.1", "2026.04.1"))
            .isEqualTo("2026.04.1");
        assertThat(service.resolveRequestedReleaseVersion("4_agent_memory", "2026.04.1", "2026.04.1"))
            .isEqualTo("2026.04.1");
    }

    @Test
    void keepsNonReleaseBackedWorkshopOnCurrentWhenNoReleaseExists() {
        ReleaseCatalogService releaseCatalogService = mock(ReleaseCatalogService.class);
        when(releaseCatalogService.findDefaultRelease("99_unknown")).thenReturn(Optional.empty());
        when(releaseCatalogService.findRelease("99_unknown", "2026.04.1")).thenReturn(Optional.empty());

        PilotLaunchRuleService service = new PilotLaunchRuleService(
            releaseCatalogService,
            "local"
        );

        assertThat(service.resolveLaunchDefaults("99_unknown")).isEqualTo(WorkshopLaunchDefaults.current());
        assertThat(service.resolveRequestedReleaseVersion("99_unknown", null, "current")).isEqualTo("current");
    }

    private ReleaseCatalogEntry sampleRelease(boolean enabled, List<String> environments) {
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
            List.of("redis"),
            enabled,
            environments
        );
    }

    private ReleaseCatalogEntry sampleWorkshopFourRelease(boolean enabled, List<String> environments) {
        return new ReleaseCatalogEntry(
            "agent-memory-2026.04.1",
            "4_agent_memory",
            "2026.04.1",
            SessionMode.LAB,
            true,
            new ReleaseImageReferences(
                "registry.example.com/workshops/agent-memory-frontend@sha256:7777777777777777777777777777777777777777777777777777777777777777",
                "registry.example.com/workshops/agent-memory-backend@sha256:8888888888888888888888888888888888888888888888888888888888888888",
                null,
                null
            ),
            "medium",
            60,
            List.of("redis"),
            enabled,
            environments
        );
    }
}
