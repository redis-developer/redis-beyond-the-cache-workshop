package com.redis.workshop.platform.controlplane.session;

import com.redis.workshop.platform.controlplane.catalog.WorkshopCatalogEntry;
import com.redis.workshop.platform.controlplane.catalog.WorkshopCatalogService;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspaceCleanupState;
import com.redis.workshop.platform.controlplane.persistence.repository.PlatformSessionRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Profile;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.function.Consumer;

@Component
@Profile("local")
@ConditionalOnExpression("!T(org.springframework.util.StringUtils).hasText('${platform.controlplane.execution.base-url:}')")
class LocalSessionRuntimeLifecyclePort implements SessionRuntimeLifecyclePort {

    private static final Logger logger = LoggerFactory.getLogger(LocalSessionRuntimeLifecyclePort.class);

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
    private final WorkshopCatalogService workshopCatalogService;
    private final TaskExecutor taskExecutor;
    private final String publicBaseUrl;
    private final Duration provisioningDelay;
    private final Duration initializationDelay;
    private final Duration terminationDelay;

    LocalSessionRuntimeLifecyclePort(
        PlatformSessionRecordRepository sessionRepository,
        WorkshopCatalogService workshopCatalogService,
        @Qualifier("applicationTaskExecutor")
        TaskExecutor taskExecutor,
        @Value("${platform.controlplane.runtime.public-base-url:}") String publicBaseUrl,
        @Value("${platform.controlplane.runtime.provisioning-delay:250ms}") Duration provisioningDelay,
        @Value("${platform.controlplane.runtime.initialization-delay:250ms}") Duration initializationDelay,
        @Value("${platform.controlplane.runtime.termination-delay:100ms}") Duration terminationDelay
    ) {
        this.sessionRepository = sessionRepository;
        this.workshopCatalogService = workshopCatalogService;
        this.taskExecutor = taskExecutor;
        this.publicBaseUrl = publicBaseUrl == null ? "" : publicBaseUrl.trim();
        this.provisioningDelay = provisioningDelay;
        this.initializationDelay = initializationDelay;
        this.terminationDelay = terminationDelay;
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
                SessionRuntimeStatuses.PROVISIONING,
                record -> {
                    record.setInternalRuntimeRef("local:" + request.workshopId() + ":" + request.sessionId());
                    record.setWorkspaceCleanupState(activeCleanupState(record));
                }
            )) {
                return;
            }

            pause(provisioningDelay);

            if (!transition(
                request.sessionId(),
                EnumSet.of(PlatformSessionState.PROVISIONING),
                PlatformSessionState.INITIALIZING,
                SessionRuntimeStatuses.INITIALIZING,
                null
            )) {
                return;
            }

            pause(initializationDelay);

            WorkshopCatalogEntry workshop = workshopCatalogService.getWorkshopEntry(request.workshopId());
            String publicEntryUrl = toPublicEntryUrl(workshop.path());
            if (publicEntryUrl == null) {
                failSession(request.sessionId(), "entry_route_missing", "No public entry route is configured for the workshop");
                return;
            }
            ExecutionPlaneLaunchResult launchResult;
            try {
                launchResult = new ExecutionPlaneLaunchResult("local:" + request.workshopId() + ":" + request.sessionId(), publicEntryUrl);
            } catch (IllegalArgumentException exception) {
                failSession(request.sessionId(), "entry_route_invalid", exception.getMessage());
                return;
            }

            transition(
                request.sessionId(),
                EnumSet.of(PlatformSessionState.INITIALIZING),
                PlatformSessionState.READY,
                SessionRuntimeStatuses.READY,
                record -> {
                    record.setInternalRuntimeRef(launchResult.runtimeRef());
                    record.setPublicEntryUrl(launchResult.publicEntryUrl());
                    record.setWorkspaceCleanupState(activeCleanupState(record));
                    record.setFailureCode(null);
                    record.setFailureMessage(null);
                }
            );
        } catch (Exception exception) {
            logger.error("Launch failed for session {}", request.sessionId(), exception);
            failSession(request.sessionId(), "launch_failed", exception.getMessage());
        }
    }

    private void runRestart(SessionRestartRequest request) {
        try {
            if (!transition(
                request.sessionId(),
                EnumSet.of(PlatformSessionState.PROVISIONING),
                PlatformSessionState.INITIALIZING,
                request.rebuild()
                    ? SessionRuntimeStatuses.RESTART_REBUILD_IN_PROGRESS
                    : SessionRuntimeStatuses.RESTART_IN_PROGRESS,
                null
            )) {
                return;
            }

            pause(initializationDelay);

            WorkshopCatalogEntry workshop = workshopCatalogService.getWorkshopEntry(request.workshopId());
            String publicEntryUrl = toPublicEntryUrl(workshop.path());
            if (publicEntryUrl == null) {
                failRestart(request.sessionId(), "entry_route_missing", "No public entry route is configured for the workshop");
                return;
            }
            ExecutionPlaneLaunchResult restartResult;
            try {
                restartResult = new ExecutionPlaneLaunchResult("local:" + request.workshopId() + ":" + request.sessionId(), publicEntryUrl);
            } catch (IllegalArgumentException exception) {
                failRestart(request.sessionId(), "entry_route_invalid", exception.getMessage());
                return;
            }

            transition(
                request.sessionId(),
                EnumSet.of(PlatformSessionState.INITIALIZING),
                PlatformSessionState.READY,
                SessionRuntimeStatuses.READY,
                record -> {
                    record.setInternalRuntimeRef(restartResult.runtimeRef());
                    record.setPublicEntryUrl(restartResult.publicEntryUrl());
                    record.setWorkspaceCleanupState(activeCleanupState(record));
                    record.setFailureCode(null);
                    record.setFailureMessage(null);
                }
            );
        } catch (Exception exception) {
            logger.error("Restart failed for session {}", request.sessionId(), exception);
            failRestart(request.sessionId(), SessionRuntimeStatuses.RESTART_FAILED, exception.getMessage());
        }
    }

    private void runTermination(SessionTerminationRequest request) {
        PlatformSessionRecord record = sessionRepository.findById(request.sessionId()).orElse(null);
        if (record == null || record.getState() != PlatformSessionState.TERMINATING) {
            return;
        }

        try {
            pause(terminationDelay);
            transition(
                request.sessionId(),
                EnumSet.of(PlatformSessionState.TERMINATING),
                PlatformSessionState.TERMINATED,
                SessionRuntimeStatuses.TERMINATED,
                sessionRecord -> {
                    sessionRecord.setPublicEntryUrl(null);
                    sessionRecord.setInternalRuntimeRef(null);
                    sessionRecord.setTerminatedAt(Instant.now());
                    sessionRecord.setCleanupCompletedAt(Instant.now());
                    sessionRecord.setWorkspaceCleanupState(terminalCleanupState(sessionRecord));
                }
            );
        } catch (Exception exception) {
            logger.error("Termination failed for session {}", request.sessionId(), exception);
            failSession(request.sessionId(), "termination_failed", exception.getMessage());
        }
    }

    private void runTerminationRetry(SessionTerminationRequest request) {
        PlatformSessionRecord record = sessionRepository.findById(request.sessionId()).orElse(null);
        if (record == null || !TERMINATION_RETRY_STATES.contains(record.getState())) {
            return;
        }

        try {
            pause(terminationDelay);
            finalizeTerminationRetry(request.sessionId());
        } catch (Exception exception) {
            logger.error("Cleanup termination retry failed for session {}", request.sessionId(), exception);
            failTerminationRetry(request.sessionId(), SessionRuntimeStatuses.TERMINATION_RETRY_FAILED, exception.getMessage());
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
        if (record == null) {
            logger.warn("Session {} not found while applying runtime transition {}", sessionId, targetState);
            return false;
        }
        if (!allowedCurrentStates.contains(record.getState())) {
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

    private void failSession(String sessionId, String failureCode, String failureMessage) {
        PlatformSessionRecord record = sessionRepository.findById(sessionId).orElse(null);
        if (record == null || IMMUTABLE_FAILURE_STATES.contains(record.getState())) {
            return;
        }

        record.setState(PlatformSessionState.FAILED);
        record.setRuntimeStatus(SessionRuntimeStatuses.FAILED);
        record.setFailureCode(failureCode);
        record.setFailureMessage(failureMessage == null ? "Unknown runtime error" : failureMessage);
        record.setLastActivityAt(Instant.now());
        record.setPublicEntryUrl(null);
        if (record.getWorkspaceCleanupState() == WorkspaceCleanupState.ACTIVE) {
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
        record.setFailureMessage(failureMessage == null ? "Unknown runtime error" : failureMessage);
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
        if (record.getWorkspaceCleanupState() != WorkspaceCleanupState.NONE) {
            record.setWorkspaceCleanupState(WorkspaceCleanupState.PENDING);
        }
        sessionRepository.save(record);
    }

    private void failTerminationRetry(String sessionId, String failureCode, String failureMessage) {
        PlatformSessionRecord record = sessionRepository.findById(sessionId).orElse(null);
        if (record == null || !TERMINATION_RETRY_STATES.contains(record.getState())) {
            return;
        }
        record.setRuntimeStatus(SessionRuntimeStatuses.TERMINATION_RETRY_FAILED);
        record.setFailureCode(failureCode);
        record.setFailureMessage(failureMessage == null ? "Unknown runtime error" : failureMessage);
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

    private String toPublicEntryUrl(String workshopPath) {
        if (workshopPath == null || workshopPath.isBlank()) {
            return null;
        }
        if (workshopPath.startsWith("http://") || workshopPath.startsWith("https://")) {
            return workshopPath;
        }
        if (publicBaseUrl.isBlank()) {
            return normalizePath(workshopPath);
        }
        return trimTrailingSlash(publicBaseUrl) + normalizePath(workshopPath);
    }

    private String normalizePath(String workshopPath) {
        return workshopPath.startsWith("/") ? workshopPath : "/" + workshopPath;
    }

    private String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private void pause(Duration duration) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            return;
        }

        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Runtime transition interrupted", exception);
        }
    }
}
