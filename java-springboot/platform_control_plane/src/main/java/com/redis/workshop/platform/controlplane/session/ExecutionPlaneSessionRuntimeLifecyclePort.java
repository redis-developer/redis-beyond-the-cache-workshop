package com.redis.workshop.platform.controlplane.session;

import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspaceCleanupState;
import com.redis.workshop.platform.controlplane.persistence.repository.PlatformSessionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

@Component
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${platform.controlplane.execution.base-url:}')")
class ExecutionPlaneSessionRuntimeLifecyclePort implements SessionRuntimeLifecyclePort {

    private static final Logger logger = LoggerFactory.getLogger(ExecutionPlaneSessionRuntimeLifecyclePort.class);

    private static final Set<PlatformSessionState> IMMUTABLE_FAILURE_STATES = EnumSet.of(
        PlatformSessionState.TERMINATED,
        PlatformSessionState.CLEANUP_PENDING,
        PlatformSessionState.EXPIRED,
        PlatformSessionState.FAILED
    );
    private static final Set<PlatformSessionState> TERMINATION_RETRY_STATES = EnumSet.of(
        PlatformSessionState.TERMINATED,
        PlatformSessionState.CLEANUP_PENDING,
        PlatformSessionState.EXPIRED,
        PlatformSessionState.FAILED
    );

    private final PlatformSessionRecordRepository sessionRepository;
    private final TaskExecutor taskExecutor;
    private final ExecutionPlaneClient executionPlaneClient;
    private final SessionRuntimeReadinessProbe readinessProbe;

    ExecutionPlaneSessionRuntimeLifecyclePort(
        PlatformSessionRecordRepository sessionRepository,
        @Qualifier("applicationTaskExecutor")
        TaskExecutor taskExecutor,
        ExecutionPlaneClient executionPlaneClient,
        SessionRuntimeReadinessProbe readinessProbe
    ) {
        this.sessionRepository = sessionRepository;
        this.taskExecutor = taskExecutor;
        this.executionPlaneClient = executionPlaneClient;
        this.readinessProbe = readinessProbe;
    }

    @Override
    public void requestLaunch(SessionLaunchRequest request) {
        taskExecutor.execute(() -> runLaunch(request));
    }

    @Override
    public void requestRestart(SessionRestartRequest request) {
        taskExecutor.execute(() -> runRestart(request));
    }

    @Override
    public void requestTermination(SessionTerminationRequest request) {
        taskExecutor.execute(() -> runTermination(request));
    }

    @Override
    public void requestTerminationRetry(SessionTerminationRequest request) {
        taskExecutor.execute(() -> runTerminationRetry(request));
    }

    private void runLaunch(SessionLaunchRequest request) {
        try {
            if (!transition(
                request.sessionId(),
                EnumSet.of(PlatformSessionState.ADMITTED),
                PlatformSessionState.PROVISIONING,
                SessionRuntimeStatuses.EXECUTION_LAUNCH_REQUESTED,
                null
            )) {
                return;
            }

            ExecutionPlaneLaunchResult result = executionPlaneClient.launchSession(request);
            if (!transition(
                request.sessionId(),
                EnumSet.of(PlatformSessionState.PROVISIONING),
                PlatformSessionState.INITIALIZING,
                SessionRuntimeStatuses.SESSION_RUNNER_INITIALIZING,
                record -> {
                    record.setInternalRuntimeRef(result.runtimeRef());
                    applyRouteBinding(record, result);
                    record.setWorkspaceCleanupState(activeCleanupState(record));
                }
            )) {
                return;
            }

            readinessProbe.awaitReady(result.routeBinding());
            transition(
                request.sessionId(),
                EnumSet.of(PlatformSessionState.INITIALIZING),
                PlatformSessionState.READY,
                SessionRuntimeStatuses.READY,
                record -> {
                    record.setWorkspaceCleanupState(activeCleanupState(record));
                    record.setFailureCode(null);
                    record.setFailureMessage(null);
                }
            );
        } catch (ExecutionPlaneClient.ExecutionPlaneClientException exception) {
            logger.error("Execution plane launch failed for session {}", request.sessionId(), exception);
            failSession(request.sessionId(), failureCodeForLaunch(exception), exception.getMessage(), false);
        } catch (Exception exception) {
            logger.error("Execution plane launch failed for session {}", request.sessionId(), exception);
            failSession(request.sessionId(), "execution_plane_launch_failed", exception.getMessage(), false);
        }
    }

    private void runRestart(SessionRestartRequest request) {
        try {
            ExecutionPlaneLaunchResult result = executionPlaneClient.restartSession(request);
            if (!transition(
                request.sessionId(),
                EnumSet.of(PlatformSessionState.PROVISIONING),
                PlatformSessionState.INITIALIZING,
                request.rebuild()
                    ? SessionRuntimeStatuses.RESTART_REBUILD_INITIALIZING
                    : SessionRuntimeStatuses.RESTART_INITIALIZING,
                record -> {
                    record.setInternalRuntimeRef(result.runtimeRef());
                    applyRouteBinding(record, result);
                    record.setWorkspaceCleanupState(activeCleanupState(record));
                }
            )) {
                return;
            }

            readinessProbe.awaitReady(result.routeBinding());
            transition(
                request.sessionId(),
                EnumSet.of(PlatformSessionState.INITIALIZING),
                PlatformSessionState.READY,
                SessionRuntimeStatuses.READY,
                record -> {
                    record.setWorkspaceCleanupState(activeCleanupState(record));
                    record.setFailureCode(null);
                    record.setFailureMessage(null);
                }
            );
        } catch (ExecutionPlaneClient.ExecutionPlaneClientException exception) {
            logger.error("Execution plane restart failed for session {}", request.sessionId(), exception);
            failRestart(request.sessionId(), failureCodeForRestart(exception), exception.getMessage());
        } catch (Exception exception) {
            logger.error("Execution plane restart failed for session {}", request.sessionId(), exception);
            failRestart(request.sessionId(), "execution_plane_restart_failed", exception.getMessage());
        }
    }

    private void runTermination(SessionTerminationRequest request) {
        PlatformSessionRecord record = sessionRepository.findById(request.sessionId()).orElse(null);
        if (record == null || record.getState() != PlatformSessionState.TERMINATING) {
            return;
        }

        try {
            executionPlaneClient.terminateSession(request);
            transition(
                request.sessionId(),
                EnumSet.of(PlatformSessionState.TERMINATING),
                PlatformSessionState.TERMINATED,
                SessionRuntimeStatuses.TERMINATED,
                sessionRecord -> {
                    sessionRecord.setPublicEntryUrl(null);
                    sessionRecord.setInternalRuntimeRef(null);
                    clearRouteBinding(sessionRecord);
                    sessionRecord.setTerminatedAt(Instant.now());
                    sessionRecord.setCleanupCompletedAt(Instant.now());
                    sessionRecord.setWorkspaceCleanupState(terminalCleanupState(sessionRecord));
                }
            );
        } catch (ExecutionPlaneClient.ExecutionPlaneClientException exception) {
            logger.error("Execution plane termination failed for session {}", request.sessionId(), exception);
            failSession(request.sessionId(), failureCodeForTermination(exception), exception.getMessage(), true);
        } catch (Exception exception) {
            logger.error("Execution plane termination failed for session {}", request.sessionId(), exception);
            failSession(request.sessionId(), "execution_plane_termination_failed", exception.getMessage(), true);
        }
    }

    private void runTerminationRetry(SessionTerminationRequest request) {
        PlatformSessionRecord record = sessionRepository.findById(request.sessionId()).orElse(null);
        if (record == null || !TERMINATION_RETRY_STATES.contains(record.getState())) {
            return;
        }

        try {
            executionPlaneClient.terminateSession(request);
            finalizeTerminationRetry(request.sessionId());
        } catch (ExecutionPlaneClient.ExecutionPlaneClientException exception) {
            logger.error("Execution plane cleanup termination retry failed for session {}", request.sessionId(), exception);
            failTerminationRetry(request.sessionId(), failureCodeForTermination(exception), exception.getMessage());
        } catch (Exception exception) {
            logger.error("Execution plane cleanup termination retry failed for session {}", request.sessionId(), exception);
            failTerminationRetry(request.sessionId(), "execution_plane_termination_retry_failed", exception.getMessage());
        }
    }

    private boolean transition(
        String sessionId,
        Set<PlatformSessionState> allowedCurrentStates,
        PlatformSessionState targetState,
        String runtimeStatus,
        Consumer<PlatformSessionRecord> mutation
    ) {
        PlatformSessionRecord record = sessionRepository.findById(sessionId).orElse(null);
        if (record == null || !allowedCurrentStates.contains(record.getState())) {
            return false;
        }

        record.setState(targetState);
        record.setRuntimeStatus(runtimeStatus);
        record.setLastActivityAt(Instant.now());
        if (mutation != null) {
            mutation.accept(record);
        }
        sessionRepository.save(record);
        return true;
    }

    private String failureCodeForLaunch(ExecutionPlaneClient.ExecutionPlaneClientException exception) {
        return switch (exception.failureKind()) {
            case UNAUTHORIZED -> "execution_plane_unauthorized";
            case INVALID_RESPONSE -> "execution_plane_invalid_response";
            case REMOTE_FAILURE, COMMUNICATION_FAILURE -> "execution_plane_launch_failed";
        };
    }

    private String failureCodeForTermination(ExecutionPlaneClient.ExecutionPlaneClientException exception) {
        return switch (exception.failureKind()) {
            case UNAUTHORIZED -> "execution_plane_termination_unauthorized";
            case INVALID_RESPONSE -> "execution_plane_termination_invalid_response";
            case REMOTE_FAILURE, COMMUNICATION_FAILURE -> "execution_plane_termination_failed";
        };
    }

    private String failureCodeForRestart(ExecutionPlaneClient.ExecutionPlaneClientException exception) {
        return switch (exception.failureKind()) {
            case UNAUTHORIZED -> "execution_plane_restart_unauthorized";
            case INVALID_RESPONSE -> "execution_plane_restart_invalid_response";
            case REMOTE_FAILURE, COMMUNICATION_FAILURE -> "execution_plane_restart_failed";
        };
    }

    private void failSession(String sessionId, String failureCode, String failureMessage, boolean allowFromTerminating) {
        PlatformSessionRecord record = sessionRepository.findById(sessionId).orElse(null);
        if (record == null || IMMUTABLE_FAILURE_STATES.contains(record.getState())) {
            return;
        }
        if (!allowFromTerminating && record.getState() == PlatformSessionState.TERMINATING) {
            return;
        }

        record.setState(PlatformSessionState.FAILED);
        record.setRuntimeStatus(SessionRuntimeStatuses.FAILED);
        record.setFailureCode(failureCode);
        record.setFailureMessage(failureMessage == null ? "Unknown execution plane error" : failureMessage);
        record.setLastActivityAt(Instant.now());
        record.setPublicEntryUrl(null);
        clearRouteBinding(record);
        if (allowFromTerminating) {
            record.setWorkspaceCleanupState(WorkspaceCleanupState.FAILED);
        } else if (record.getWorkspaceCleanupState() == WorkspaceCleanupState.ACTIVE) {
            record.setWorkspaceCleanupState(WorkspaceCleanupState.PENDING);
        }
        sessionRepository.save(record);
    }

    private void failRestart(String sessionId, String failureCode, String failureMessage) {
        PlatformSessionRecord record = sessionRepository.findById(sessionId).orElse(null);
        if (record == null
            || record.getState() == PlatformSessionState.TERMINATED
            || record.getState() == PlatformSessionState.CLEANUP_PENDING
            || record.getState() == PlatformSessionState.EXPIRED) {
            return;
        }

        record.setState(PlatformSessionState.FAILED);
        record.setRuntimeStatus(SessionRuntimeStatuses.RESTART_FAILED);
        record.setFailureCode(failureCode);
        record.setFailureMessage(failureMessage == null ? "Unknown execution plane error" : failureMessage);
        record.setLastActivityAt(Instant.now());
        record.setWorkspaceCleanupState(activeCleanupState(record));
        sessionRepository.save(record);
    }

    private void finalizeTerminationRetry(String sessionId) {
        PlatformSessionRecord record = sessionRepository.findById(sessionId).orElse(null);
        if (record == null || !TERMINATION_RETRY_STATES.contains(record.getState())) {
            return;
        }
        Instant now = Instant.now();
        record.setPublicEntryUrl(null);
        record.setInternalRuntimeRef(null);
        if (record.getTerminatedAt() == null) {
            record.setTerminatedAt(now);
        }
        record.setRuntimeStatus(SessionRuntimeStatuses.TERMINATION_RECONCILED);
        record.setLastActivityAt(now);
        clearRouteBinding(record);
        if (record.getWorkspaceCleanupState() != WorkspaceCleanupState.NONE) {
            record.setWorkspaceCleanupState(WorkspaceCleanupState.PENDING);
        }
        sessionRepository.save(record);
    }

    private void applyRouteBinding(PlatformSessionRecord record, ExecutionPlaneLaunchResult result) {
        ExecutionPlaneRouteBinding routeBinding = result.routeBinding();
        record.setPublicEntryUrl(routeBinding.publicBasePath());
        record.setRouteGatewayHost(routeBinding.gatewayHost());
        record.setRouteServiceName(routeBinding.serviceName());
        record.setRouteServiceNamespace(routeBinding.serviceNamespace());
        record.setRouteUpstreamBaseUrl(routeBinding.upstreamBaseUrl());
    }

    private void clearRouteBinding(PlatformSessionRecord record) {
        record.setRouteGatewayHost(null);
        record.setRouteServiceName(null);
        record.setRouteServiceNamespace(null);
        record.setRouteUpstreamBaseUrl(null);
    }

    private void failTerminationRetry(String sessionId, String failureCode, String failureMessage) {
        PlatformSessionRecord record = sessionRepository.findById(sessionId).orElse(null);
        if (record == null || !TERMINATION_RETRY_STATES.contains(record.getState())) {
            return;
        }
        record.setRuntimeStatus(SessionRuntimeStatuses.TERMINATION_RETRY_FAILED);
        record.setFailureCode(failureCode);
        record.setFailureMessage(failureMessage == null ? "Unknown execution plane error" : failureMessage);
        record.setLastActivityAt(Instant.now());
        if (record.getWorkspaceCleanupState() != WorkspaceCleanupState.NONE) {
            record.setWorkspaceCleanupState(WorkspaceCleanupState.FAILED);
        }
        sessionRepository.save(record);
    }

    private WorkspaceCleanupState activeCleanupState(PlatformSessionRecord record) {
        if (record.getWorkspaceCleanupState() == WorkspaceCleanupState.NONE) {
            return WorkspaceCleanupState.NONE;
        }
        return WorkspaceCleanupState.ACTIVE;
    }

    private WorkspaceCleanupState terminalCleanupState(PlatformSessionRecord record) {
        if (record.getWorkspaceCleanupState() == WorkspaceCleanupState.NONE) {
            return WorkspaceCleanupState.NONE;
        }
        return WorkspaceCleanupState.COMPLETED;
    }
}
