package com.redis.workshop.platform.controlplane.session;

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
import org.springframework.core.task.TaskExecutor;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExecutionPlaneSessionRuntimeLifecyclePortTest {

    @Mock
    private PlatformSessionRecordRepository sessionRepository;

    @Mock
    private ExecutionPlaneClient executionPlaneClient;

    @Mock
    private SessionRuntimeReadinessProbe readinessProbe;

    private ExecutionPlaneSessionRuntimeLifecyclePort runtimeLifecyclePort;

    @BeforeEach
    void setUp() {
        TaskExecutor directExecutor = Runnable::run;
        runtimeLifecyclePort = new ExecutionPlaneSessionRuntimeLifecyclePort(
            sessionRepository,
            directExecutor,
            executionPlaneClient,
            readinessProbe
        );
        doAnswer(invocation -> invocation.getArgument(0)).when(sessionRepository).save(any(PlatformSessionRecord.class));
    }

    @Test
    void launchTransitionsSessionToReadyFromExecutionPlaneResponse() {
        PlatformSessionRecord record = sessionRecord("sess-001", PlatformSessionState.ADMITTED);
        when(sessionRepository.findById("sess-001")).thenReturn(Optional.of(record));
        when(executionPlaneClient.launchSession(any())).thenReturn(new ExecutionPlaneLaunchResult(
            cloudRunRuntimeRef("sess-001"),
            "/session/sess-001/"
        ));

        runtimeLifecyclePort.requestLaunch(new SessionLaunchRequest(
            "sess-001",
            "learner-1",
            "1_session_management",
            "current",
            SessionMode.LAB
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.READY);
        assertThat(record.getRuntimeStatus()).isEqualTo(SessionRuntimeStatuses.READY);
        assertThat(record.getInternalRuntimeRef()).isEqualTo(cloudRunRuntimeRef("sess-001"));
        assertThat(record.getPublicEntryUrl()).isEqualTo("/session/sess-001/");
        assertThat(record.getWorkspaceCleanupState()).isEqualTo(WorkspaceCleanupState.ACTIVE);
        verify(readinessProbe).awaitReady(any());
    }

    @Test
    void terminationTreatsAlreadyTerminatedAsTerminalCleanup() {
        PlatformSessionRecord record = sessionRecord("sess-002", PlatformSessionState.TERMINATING);
        record.setPublicEntryUrl("/session/sess-002/");
        record.setInternalRuntimeRef(cloudRunRuntimeRef("sess-002"));
        when(sessionRepository.findById("sess-002")).thenReturn(Optional.of(record));
        when(executionPlaneClient.terminateSession(any()))
            .thenReturn(ExecutionPlaneClient.TerminationResult.ALREADY_TERMINATED);

        runtimeLifecyclePort.requestTermination(new SessionTerminationRequest(
            "sess-002",
            "learner-1",
            cloudRunRuntimeRef("sess-002"),
            "user_requested"
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.TERMINATED);
        assertThat(record.getRuntimeStatus()).isEqualTo(SessionRuntimeStatuses.TERMINATED);
        assertThat(record.getPublicEntryUrl()).isNull();
        assertThat(record.getInternalRuntimeRef()).isNull();
        assertThat(record.getTerminatedAt()).isNotNull();
        assertThat(record.getCleanupCompletedAt()).isNotNull();
        assertThat(record.getWorkspaceCleanupState()).isEqualTo(WorkspaceCleanupState.COMPLETED);
    }

    @Test
    void restartTransitionsProvisioningSessionBackToReady() {
        PlatformSessionRecord record = sessionRecord("sess-002b", PlatformSessionState.PROVISIONING);
        record.setPublicEntryUrl("/session/sess-002b/");
        record.setInternalRuntimeRef(cloudRunRuntimeRef("sess-002b-old"));
        when(sessionRepository.findById("sess-002b")).thenReturn(Optional.of(record));
        when(executionPlaneClient.restartSession(any())).thenReturn(new ExecutionPlaneLaunchResult(
            cloudRunRuntimeRef("sess-002b"),
            "/session/sess-002b/"
        ));

        runtimeLifecyclePort.requestRestart(new SessionRestartRequest(
            "sess-002b",
            "learner-1",
            "1_session_management",
            "current",
            SessionMode.LAB,
            true
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.READY);
        assertThat(record.getRuntimeStatus()).isEqualTo(SessionRuntimeStatuses.READY);
        assertThat(record.getInternalRuntimeRef()).isEqualTo(cloudRunRuntimeRef("sess-002b"));
        assertThat(record.getPublicEntryUrl()).isEqualTo("/session/sess-002b/");
        assertThat(record.getFailureCode()).isNull();
        assertThat(record.getFailureMessage()).isNull();
        verify(executionPlaneClient).restartSession(any());
        verify(readinessProbe).awaitReady(any());
    }

    @Test
    void duplicateLaunchRequestDoesNotReinvokeExecutionPlane() {
        PlatformSessionRecord record = sessionRecord("sess-003", PlatformSessionState.ADMITTED);
        when(sessionRepository.findById("sess-003")).thenReturn(Optional.of(record));
        when(executionPlaneClient.launchSession(any())).thenReturn(new ExecutionPlaneLaunchResult(
            cloudRunRuntimeRef("sess-003"),
            "/session/sess-003/"
        ));

        SessionLaunchRequest request = new SessionLaunchRequest(
            "sess-003",
            "learner-1",
            "1_session_management",
            "current",
            SessionMode.LAB
        );

        runtimeLifecyclePort.requestLaunch(request);
        runtimeLifecyclePort.requestLaunch(request);

        assertThat(record.getState()).isEqualTo(PlatformSessionState.READY);
        verify(executionPlaneClient).launchSession(request);
    }

    @Test
    void invalidLaunchResponseFailsClosed() {
        PlatformSessionRecord record = sessionRecord("sess-004", PlatformSessionState.ADMITTED);
        when(sessionRepository.findById("sess-004")).thenReturn(Optional.of(record));
        when(executionPlaneClient.launchSession(any())).thenThrow(new ExecutionPlaneClient.ExecutionPlaneClientException(
            ExecutionPlaneClient.FailureKind.INVALID_RESPONSE,
            "Execution plane launch returned an invalid response"
        ));

        runtimeLifecyclePort.requestLaunch(new SessionLaunchRequest(
            "sess-004",
            "learner-1",
            "1_session_management",
            "current",
            SessionMode.LAB
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.FAILED);
        assertThat(record.getRuntimeStatus()).isEqualTo(SessionRuntimeStatuses.FAILED);
        assertThat(record.getFailureCode()).isEqualTo("execution_plane_invalid_response");
        assertThat(record.getFailureMessage()).isEqualTo("Execution plane launch returned an invalid response");
        assertThat(record.getPublicEntryUrl()).isNull();
        assertThat(record.getWorkspaceCleanupState()).isEqualTo(WorkspaceCleanupState.PENDING);
    }

    @Test
    void restartFailureKeepsWorkspaceActiveWhileMarkingSessionFailed() {
        PlatformSessionRecord record = sessionRecord("sess-004b", PlatformSessionState.PROVISIONING);
        record.setPublicEntryUrl("/session/sess-004b/");
        record.setInternalRuntimeRef(cloudRunRuntimeRef("sess-004b"));
        when(sessionRepository.findById("sess-004b")).thenReturn(Optional.of(record));
        when(executionPlaneClient.restartSession(any())).thenThrow(new ExecutionPlaneClient.ExecutionPlaneClientException(
            ExecutionPlaneClient.FailureKind.INVALID_RESPONSE,
            "Execution plane restart returned an invalid response"
        ));

        runtimeLifecyclePort.requestRestart(new SessionRestartRequest(
            "sess-004b",
            "learner-1",
            "1_session_management",
            "current",
            SessionMode.LAB,
            false
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.FAILED);
        assertThat(record.getRuntimeStatus()).isEqualTo(SessionRuntimeStatuses.RESTART_FAILED);
        assertThat(record.getFailureCode()).isEqualTo("execution_plane_restart_invalid_response");
        assertThat(record.getPublicEntryUrl()).isEqualTo("/session/sess-004b/");
        assertThat(record.getWorkspaceCleanupState()).isEqualTo(WorkspaceCleanupState.ACTIVE);
    }

    @Test
    void launchKeepsCloudRunCreationInProvisioningUntilExecutionPlaneReturns() {
        PlatformSessionRecord record = sessionRecord("sess-004c", PlatformSessionState.ADMITTED);
        when(sessionRepository.findById("sess-004c")).thenReturn(Optional.of(record));
        when(executionPlaneClient.launchSession(any())).thenAnswer(invocation -> {
            assertThat(record.getState()).isEqualTo(PlatformSessionState.PROVISIONING);
            assertThat(record.getRuntimeStatus()).isEqualTo(SessionRuntimeStatuses.EXECUTION_LAUNCH_REQUESTED);
            return new ExecutionPlaneLaunchResult(
                cloudRunRuntimeRef("sess-004c"),
                "/session/sess-004c/"
            );
        });
        doAnswer(invocation -> {
            assertThat(record.getState()).isEqualTo(PlatformSessionState.INITIALIZING);
            assertThat(record.getRuntimeStatus()).isEqualTo(SessionRuntimeStatuses.SESSION_RUNNER_INITIALIZING);
            assertThat(record.getPublicEntryUrl()).isEqualTo("/session/sess-004c/");
            return null;
        }).when(readinessProbe).awaitReady(any());

        runtimeLifecyclePort.requestLaunch(new SessionLaunchRequest(
            "sess-004c",
            "learner-1",
            "1_session_management",
            "current",
            SessionMode.LAB
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.READY);
        assertThat(record.getRuntimeStatus()).isEqualTo(SessionRuntimeStatuses.READY);
    }

    @Test
    void restartKeepsCloudRunManagerRequestInProvisioningUntilExecutionPlaneReturns() {
        PlatformSessionRecord record = sessionRecord("sess-004r", PlatformSessionState.PROVISIONING);
        record.setPublicEntryUrl("/session/sess-004r/");
        record.setInternalRuntimeRef(cloudRunRuntimeRef("sess-004r"));
        when(sessionRepository.findById("sess-004r")).thenReturn(Optional.of(record));
        when(executionPlaneClient.restartSession(any())).thenAnswer(invocation -> {
            assertThat(record.getState()).isEqualTo(PlatformSessionState.PROVISIONING);
            return new ExecutionPlaneLaunchResult(
                cloudRunRuntimeRef("sess-004r"),
                "/session/sess-004r/"
            );
        });
        doAnswer(invocation -> {
            assertThat(record.getState()).isEqualTo(PlatformSessionState.INITIALIZING);
            assertThat(record.getRuntimeStatus()).isEqualTo(SessionRuntimeStatuses.RESTART_REBUILD_INITIALIZING);
            assertThat(record.getPublicEntryUrl()).isEqualTo("/session/sess-004r/");
            return null;
        }).when(readinessProbe).awaitReady(any());

        runtimeLifecyclePort.requestRestart(new SessionRestartRequest(
            "sess-004r",
            "learner-1",
            "1_session_management",
            "current",
            SessionMode.LAB,
            true
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.READY);
        assertThat(record.getRuntimeStatus()).isEqualTo(SessionRuntimeStatuses.READY);
    }

    @Test
    void readinessFailureFailsLaunchAfterRouteBindingIsCaptured() {
        PlatformSessionRecord record = sessionRecord("sess-004d", PlatformSessionState.ADMITTED);
        when(sessionRepository.findById("sess-004d")).thenReturn(Optional.of(record));
        when(executionPlaneClient.launchSession(any())).thenReturn(new ExecutionPlaneLaunchResult(
            cloudRunRuntimeRef("sess-004d"),
            "/session/sess-004d/"
        ));
        doAnswer(invocation -> {
            throw new IllegalStateException("Session runner did not become ready");
        }).when(readinessProbe).awaitReady(any());

        runtimeLifecyclePort.requestLaunch(new SessionLaunchRequest(
            "sess-004d",
            "learner-1",
            "1_session_management",
            "current",
            SessionMode.LAB
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.FAILED);
        assertThat(record.getRuntimeStatus()).isEqualTo(SessionRuntimeStatuses.FAILED);
        assertThat(record.getFailureCode()).isEqualTo("execution_plane_launch_failed");
        assertThat(record.getFailureMessage()).isEqualTo("Session runner did not become ready");
        assertThat(record.getPublicEntryUrl()).isNull();
        assertThat(record.getWorkspaceCleanupState()).isEqualTo(WorkspaceCleanupState.PENDING);
    }

    @Test
    void unauthorizedTerminationFailsInsteadOfStickingInTerminating() {
        PlatformSessionRecord record = sessionRecord("sess-005", PlatformSessionState.TERMINATING);
        record.setPublicEntryUrl("/session/sess-005/");
        when(sessionRepository.findById("sess-005")).thenReturn(Optional.of(record));
        when(executionPlaneClient.terminateSession(any())).thenThrow(new ExecutionPlaneClient.ExecutionPlaneClientException(
            ExecutionPlaneClient.FailureKind.UNAUTHORIZED,
            "Execution plane termination request was not authorized"
        ));

        runtimeLifecyclePort.requestTermination(new SessionTerminationRequest(
            "sess-005",
            "learner-1",
            "user_requested"
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.FAILED);
        assertThat(record.getRuntimeStatus()).isEqualTo(SessionRuntimeStatuses.FAILED);
        assertThat(record.getFailureCode()).isEqualTo("execution_plane_termination_unauthorized");
        assertThat(record.getFailureMessage()).isEqualTo("Execution plane termination request was not authorized");
        assertThat(record.getPublicEntryUrl()).isNull();
        assertThat(record.getWorkspaceCleanupState()).isEqualTo(WorkspaceCleanupState.FAILED);
    }

    @Test
    void repeatedTerminationRequestIsIdempotentFromControlPlanePerspective() {
        PlatformSessionRecord record = sessionRecord("sess-006", PlatformSessionState.TERMINATING);
        record.setPublicEntryUrl("/session/sess-006/");
        when(sessionRepository.findById("sess-006")).thenReturn(Optional.of(record));
        when(executionPlaneClient.terminateSession(any())).thenReturn(ExecutionPlaneClient.TerminationResult.TERMINATED);

        SessionTerminationRequest request = new SessionTerminationRequest(
            "sess-006",
            "learner-1",
            "user_requested"
        );

        runtimeLifecyclePort.requestTermination(request);
        runtimeLifecyclePort.requestTermination(request);

        assertThat(record.getState()).isEqualTo(PlatformSessionState.TERMINATED);
        assertThat(record.getRuntimeStatus()).isEqualTo(SessionRuntimeStatuses.TERMINATED);
        verify(executionPlaneClient).terminateSession(request);
    }

    @Test
    void terminationRetryPreservesExpiredStateWhileClearingRuntimeRef() {
        PlatformSessionRecord record = sessionRecord("sess-007", PlatformSessionState.EXPIRED);
        record.setInternalRuntimeRef(cloudRunRuntimeRef("sess-007"));
        record.setPublicEntryUrl("/session/sess-007/");
        record.setWorkspaceCleanupState(WorkspaceCleanupState.FAILED);
        when(sessionRepository.findById("sess-007")).thenReturn(Optional.of(record));
        when(executionPlaneClient.terminateSession(any())).thenReturn(ExecutionPlaneClient.TerminationResult.TERMINATED);

        runtimeLifecyclePort.requestTerminationRetry(new SessionTerminationRequest(
            "sess-007",
            "learner-1",
            "expired"
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.EXPIRED);
        assertThat(record.getRuntimeStatus()).isEqualTo(SessionRuntimeStatuses.TERMINATION_RECONCILED);
        assertThat(record.getInternalRuntimeRef()).isNull();
        assertThat(record.getPublicEntryUrl()).isNull();
        assertThat(record.getWorkspaceCleanupState()).isEqualTo(WorkspaceCleanupState.PENDING);
        verify(executionPlaneClient).terminateSession(any());
    }

    @Test
    void terminationRetryFailureMarksCleanupFailedWithoutChangingLogicalState() {
        PlatformSessionRecord record = sessionRecord("sess-008", PlatformSessionState.EXPIRED);
        record.setInternalRuntimeRef(cloudRunRuntimeRef("sess-008"));
        when(sessionRepository.findById("sess-008")).thenReturn(Optional.of(record));
        when(executionPlaneClient.terminateSession(any())).thenThrow(new ExecutionPlaneClient.ExecutionPlaneClientException(
            ExecutionPlaneClient.FailureKind.UNAUTHORIZED,
            "Execution plane termination request was not authorized"
        ));

        runtimeLifecyclePort.requestTerminationRetry(new SessionTerminationRequest(
            "sess-008",
            "learner-1",
            "expired"
        ));

        assertThat(record.getState()).isEqualTo(PlatformSessionState.EXPIRED);
        assertThat(record.getRuntimeStatus()).isEqualTo(SessionRuntimeStatuses.TERMINATION_RETRY_FAILED);
        assertThat(record.getWorkspaceCleanupState()).isEqualTo(WorkspaceCleanupState.FAILED);
        assertThat(record.getInternalRuntimeRef()).isEqualTo(cloudRunRuntimeRef("sess-008"));
    }

    private PlatformSessionRecord sessionRecord(String sessionId, PlatformSessionState state) {
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
        record.setLastActivityAt(Instant.parse("2026-04-21T10:05:00Z"));
        record.setRouteType(RouteType.PATH);
        record.setWorkspacePolicy(WorkspacePolicy.EPHEMERAL);
        record.setWorkspaceRef("session-workspace:" + sessionId);
        record.setWorkspaceCleanupState(WorkspaceCleanupState.ACTIVE);
        return record;
    }

    private String cloudRunRuntimeRef(String sessionId) {
        return "cloud-run:projects/workshop-prod/locations/europe-west1/services/ws-" + sessionId;
    }
}
