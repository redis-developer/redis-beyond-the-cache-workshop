package com.redis.workshop.platform.controlplane.session;

import com.redis.workshop.platform.controlplane.catalog.WorkshopCatalogEntry;
import com.redis.workshop.platform.controlplane.catalog.WorkshopCatalogService;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionMode;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.model.RouteType;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspaceCleanupState;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspacePolicy;
import com.redis.workshop.platform.controlplane.persistence.repository.PlatformSessionRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.task.TaskExecutor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocalSessionRuntimeLifecyclePortTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withPropertyValues(
            "platform.controlplane.runtime.provisioning-delay=0ms",
            "platform.controlplane.runtime.initialization-delay=0ms",
            "platform.controlplane.runtime.termination-delay=0ms"
        )
        .withUserConfiguration(LifecyclePortSelectionConfiguration.class);

    @Mock
    private PlatformSessionRecordRepository sessionRepository;

    @Mock
    private WorkshopCatalogService workshopCatalogService;

    private LocalSessionRuntimeLifecyclePort runtimeLifecyclePort;

    @BeforeEach
    void setUp() {
        TaskExecutor directExecutor = Runnable::run;
        runtimeLifecyclePort = new LocalSessionRuntimeLifecyclePort(
            sessionRepository,
            workshopCatalogService,
            directExecutor,
            "",
            Duration.ZERO,
            Duration.ZERO,
            Duration.ZERO
        );
        lenient().doAnswer(invocation -> invocation.getArgument(0)).when(sessionRepository).save(any(PlatformSessionRecord.class));
    }

    @Test
    void launchTransitionsSessionToReadyAndPublishesWorkshopRoute() {
        PlatformSessionRecord record = admittedRecord("sess-001");
        when(sessionRepository.findById("sess-001")).thenReturn(Optional.of(record));
        when(workshopCatalogService.getWorkshopEntry("1_session_management")).thenReturn(workshop("/workshop/session-management/"));

        runtimeLifecyclePort.requestLaunch(new SessionLaunchRequest(
            "sess-001",
            "learner-1",
            "1_session_management",
            "current",
            SessionMode.LAB
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.READY);
        assertThat(record.getRuntimeStatus()).isEqualTo("ready");
        assertThat(record.getPublicEntryUrl()).isEqualTo("/workshop/session-management/");
        assertThat(record.getInternalRuntimeRef()).isEqualTo("local:1_session_management:sess-001");
        assertThat(record.getFailureCode()).isNull();
        assertThat(record.getFailureMessage()).isNull();
    }

    @Test
    void launchMarksSessionFailedWhenWorkshopRouteIsMissing() {
        PlatformSessionRecord record = admittedRecord("sess-002");
        when(sessionRepository.findById("sess-002")).thenReturn(Optional.of(record));
        when(workshopCatalogService.getWorkshopEntry("1_session_management")).thenReturn(workshop(" "));

        runtimeLifecyclePort.requestLaunch(new SessionLaunchRequest(
            "sess-002",
            "learner-1",
            "1_session_management",
            "current",
            SessionMode.LAB
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.FAILED);
        assertThat(record.getRuntimeStatus()).isEqualTo("failed");
        assertThat(record.getFailureCode()).isEqualTo("entry_route_missing");
        assertThat(record.getPublicEntryUrl()).isNull();
    }

    @Test
    void terminationTransitionsSessionToTerminatedAndClearsRoute() {
        PlatformSessionRecord record = readyRecord("sess-003");
        record.setState(PlatformSessionState.TERMINATING);
        when(sessionRepository.findById("sess-003")).thenReturn(Optional.of(record));

        runtimeLifecyclePort.requestTermination(new SessionTerminationRequest(
            "sess-003",
            "learner-1",
            "user_requested"
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.TERMINATED);
        assertThat(record.getRuntimeStatus()).isEqualTo("terminated");
        assertThat(record.getPublicEntryUrl()).isNull();
        assertThat(record.getCleanupCompletedAt()).isNotNull();
        verify(sessionRepository).save(record);
    }

    @Test
    void restartTransitionsProvisioningSessionBackToReady() {
        PlatformSessionRecord record = readyRecord("sess-003a");
        record.setState(PlatformSessionState.PROVISIONING);
        when(sessionRepository.findById("sess-003a")).thenReturn(Optional.of(record));
        when(workshopCatalogService.getWorkshopEntry("1_session_management")).thenReturn(workshop("/workshop/session-management/"));

        runtimeLifecyclePort.requestRestart(new SessionRestartRequest(
            "sess-003a",
            "learner-1",
            "1_session_management",
            "current",
            SessionMode.LAB,
            false
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.READY);
        assertThat(record.getRuntimeStatus()).isEqualTo("ready");
        assertThat(record.getPublicEntryUrl()).isEqualTo("/workshop/session-management/");
    }

    @Test
    void terminationRetryPreservesExpiredStateWhileClearingRuntimeRef() {
        PlatformSessionRecord record = readyRecord("sess-003b");
        record.setState(PlatformSessionState.EXPIRED);
        record.setWorkspaceCleanupState(WorkspaceCleanupState.FAILED);
        when(sessionRepository.findById("sess-003b")).thenReturn(Optional.of(record));

        runtimeLifecyclePort.requestTerminationRetry(new SessionTerminationRequest(
            "sess-003b",
            "learner-1",
            "expired"
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.EXPIRED);
        assertThat(record.getInternalRuntimeRef()).isNull();
        assertThat(record.getPublicEntryUrl()).isNull();
        assertThat(record.getWorkspaceCleanupState()).isEqualTo(WorkspaceCleanupState.PENDING);
        assertThat(record.getTerminatedAt()).isNotNull();
    }

    @Test
    void restartFailureKeepsExistingRouteVisible() {
        PlatformSessionRecord record = readyRecord("sess-003c");
        record.setState(PlatformSessionState.PROVISIONING);
        when(sessionRepository.findById("sess-003c")).thenReturn(Optional.of(record));
        when(workshopCatalogService.getWorkshopEntry("1_session_management")).thenReturn(workshop(" "));

        runtimeLifecyclePort.requestRestart(new SessionRestartRequest(
            "sess-003c",
            "learner-1",
            "1_session_management",
            "current",
            SessionMode.LAB,
            true
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.FAILED);
        assertThat(record.getRuntimeStatus()).isEqualTo("restart_failed");
        assertThat(record.getPublicEntryUrl()).isEqualTo("/workshop/session-management/");
        assertThat(record.getWorkspaceCleanupState()).isEqualTo(WorkspaceCleanupState.ACTIVE);
    }

    @Test
    void selectsLocalLifecyclePortOnlyWhenLocalProfileIsActiveAndExecutionBaseUrlIsBlank() {
        contextRunner.withPropertyValues("spring.profiles.active=local").run(context -> {
            assertThat(context).hasSingleBean(SessionRuntimeLifecyclePort.class);
            assertThat(context).hasSingleBean(LocalSessionRuntimeLifecyclePort.class);
            assertThat(context).doesNotHaveBean(ExecutionPlaneSessionRuntimeLifecyclePort.class);
        });
    }

    @Test
    void doesNotSelectLocalFallbackOutsideLocalProfile() {
        contextRunner.run(context -> assertThat(context).doesNotHaveBean(SessionRuntimeLifecyclePort.class));
    }

    @Test
    void selectsExecutionPlaneLifecyclePortWhenExecutionBaseUrlIsConfigured() {
        contextRunner.withPropertyValues(
            "platform.controlplane.execution.base-url=http://localhost:8181",
            "platform.controlplane.execution.shared-secret=test-secret"
        ).run(context -> {
            assertThat(context).hasSingleBean(SessionRuntimeLifecyclePort.class);
            assertThat(context).hasSingleBean(ExecutionPlaneSessionRuntimeLifecyclePort.class);
            assertThat(context).doesNotHaveBean(LocalSessionRuntimeLifecyclePort.class);
        });
    }

    private WorkshopCatalogEntry workshop(String path) {
        return new WorkshopCatalogEntry(
            "1_session_management",
            "Distributed Session Management",
            "Learn Redis backed sessions",
            "Beginner",
            30,
            path,
            "current",
            SessionMode.LAB,
            List.of(SessionMode.LAB),
            List.of("Sessions"),
            true
        );
    }

    private PlatformSessionRecord admittedRecord(String sessionId) {
        PlatformSessionRecord record = readyRecord(sessionId);
        record.setState(PlatformSessionState.ADMITTED);
        record.setRuntimeStatus("admitted");
        record.setPublicEntryUrl(null);
        record.setInternalRuntimeRef(null);
        return record;
    }

    private PlatformSessionRecord readyRecord(String sessionId) {
        PlatformSessionRecord record = new PlatformSessionRecord();
        record.setSessionId(sessionId);
        record.setOwnerUserId("learner-1");
        record.setWorkshopId("1_session_management");
        record.setReleaseVersion("current");
        record.setMode(PlatformSessionMode.LAB);
        record.setState(PlatformSessionState.READY);
        record.setQuotaClass("standard");
        record.setResourceClass("small");
        record.setCreatedAt(Instant.parse("2026-04-21T10:00:00Z"));
        record.setExpiresAt(Instant.parse("2026-04-21T11:00:00Z"));
        record.setLastActivityAt(Instant.parse("2026-04-21T10:05:00Z"));
        record.setRouteType(RouteType.SUBDOMAIN);
        record.setWorkspacePolicy(WorkspacePolicy.EPHEMERAL);
        record.setWorkspaceRef("session-workspace:" + sessionId);
        record.setWorkspaceCleanupState(WorkspaceCleanupState.ACTIVE);
        record.setPublicEntryUrl("/workshop/session-management/");
        return record;
    }

    @Configuration(proxyBeanMethods = false)
    @Import({LocalSessionRuntimeLifecyclePort.class, ExecutionPlaneSessionRuntimeLifecyclePort.class})
    static class LifecyclePortSelectionConfiguration {

        @Bean
        PlatformSessionRecordRepository platformSessionRecordRepository() {
            return mock(PlatformSessionRecordRepository.class);
        }

        @Bean(name = "conversionService")
        ConversionService conversionService() {
            return ApplicationConversionService.getSharedInstance();
        }

        @Bean
        WorkshopCatalogService workshopCatalogService() {
            return mock(WorkshopCatalogService.class);
        }

        @Bean
        ExecutionPlaneClient executionPlaneClient() {
            return mock(ExecutionPlaneClient.class);
        }

        @Bean
        SessionRuntimeReadinessProbe sessionRuntimeReadinessProbe() {
            return mock(SessionRuntimeReadinessProbe.class);
        }

        @Bean(name = "applicationTaskExecutor")
        TaskExecutor applicationTaskExecutor() {
            return Runnable::run;
        }
    }
}
