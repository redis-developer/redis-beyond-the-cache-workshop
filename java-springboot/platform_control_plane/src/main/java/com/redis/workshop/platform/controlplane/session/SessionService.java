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
import com.redis.workshop.platform.controlplane.policy.PolicyDecision;
import com.redis.workshop.platform.controlplane.policy.SessionAccessPolicy;
import com.redis.workshop.platform.controlplane.policy.SessionAccessPolicyRequest;
import com.redis.workshop.platform.controlplane.policy.SessionCreatePolicyRequest;
import com.redis.workshop.platform.controlplane.policy.SessionListPolicyRequest;
import com.redis.workshop.platform.controlplane.policy.SessionProvisioningPolicy;
import com.redis.workshop.platform.controlplane.policy.SessionProvisioningProfile;
import com.redis.workshop.platform.controlplane.policy.SessionProvisioningRequest;
import com.redis.workshop.platform.controlplane.release.PilotLaunchRuleService;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogEntry;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogService;
import com.redis.workshop.platform.controlplane.security.CurrentActor;
import com.redis.workshop.platform.controlplane.security.CurrentActorProvider;
import com.redis.workshop.platform.controlplane.security.PolicyActorMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.ErrorResponseException;
import org.springframework.http.ProblemDetail;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class SessionService {

    private static final EnumSet<PlatformSessionState> EXPIRABLE_STATES = EnumSet.of(
        PlatformSessionState.REQUESTED,
        PlatformSessionState.ADMITTED,
        PlatformSessionState.PROVISIONING,
        PlatformSessionState.INITIALIZING,
        PlatformSessionState.READY,
        PlatformSessionState.DEGRADED
    );
    private static final EnumSet<PlatformSessionState> LOGICALLY_ENDED_STATES = EnumSet.of(
        PlatformSessionState.TERMINATED,
        PlatformSessionState.EXPIRED,
        PlatformSessionState.FAILED,
        PlatformSessionState.CLEANUP_PENDING
    );
    private static final EnumSet<PlatformSessionState> RESTARTABLE_STATES = EnumSet.of(
        PlatformSessionState.READY,
        PlatformSessionState.DEGRADED,
        PlatformSessionState.FAILED
    );
    private static final EnumSet<PlatformSessionState> RESTART_IN_PROGRESS_STATES = EnumSet.of(
        PlatformSessionState.PROVISIONING,
        PlatformSessionState.INITIALIZING
    );
    private static final EnumSet<PlatformSessionState> ACTIVE_SESSION_CONFLICT_STATES = EnumSet.of(
        PlatformSessionState.REQUESTED,
        PlatformSessionState.ADMITTED,
        PlatformSessionState.PROVISIONING,
        PlatformSessionState.INITIALIZING,
        PlatformSessionState.READY,
        PlatformSessionState.DEGRADED,
        PlatformSessionState.TERMINATING,
        PlatformSessionState.CLEANUP_PENDING
    );
    private static final Pattern SESSION_ENVIRONMENT_KEY_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final String SESSION_ENVIRONMENT_KEYS_CONFIG = "WORKSHOP_SESSION_ENVIRONMENT_KEYS";
    private static final Set<String> RESERVED_SESSION_ENVIRONMENT_KEYS = Set.of(
        "PORT",
        "SERVER_PORT",
        "JAVA_OPTS",
        "JAVA_TOOL_OPTIONS",
        "GRADLE_USER_HOME",
        "PATH",
        "HOME",
        "SHELL"
    );
    private static final List<String> RESERVED_SESSION_ENVIRONMENT_PREFIXES = List.of(
        "WORKSHOP_",
        "RI_",
        "REDIS_",
        "PLATFORM_"
    );

    private final PlatformSessionRecordRepository sessionRepository;
    private final WorkshopCatalogService workshopCatalogService;
    private final SessionAccessPolicy sessionAccessPolicy;
    private final SessionProvisioningPolicy sessionProvisioningPolicy;
    private final CurrentActorProvider currentActorProvider;
    private final PilotLaunchRuleService pilotLaunchRuleService;
    private final ReleaseCatalogService releaseCatalogService;
    private final SessionRuntimeLifecyclePort sessionRuntimeLifecyclePort;

    public SessionService(
        PlatformSessionRecordRepository sessionRepository,
        WorkshopCatalogService workshopCatalogService,
        SessionAccessPolicy sessionAccessPolicy,
        SessionProvisioningPolicy sessionProvisioningPolicy,
        CurrentActorProvider currentActorProvider,
        PilotLaunchRuleService pilotLaunchRuleService,
        ReleaseCatalogService releaseCatalogService,
        SessionRuntimeLifecyclePort sessionRuntimeLifecyclePort
    ) {
        this.sessionRepository = sessionRepository;
        this.workshopCatalogService = workshopCatalogService;
        this.sessionAccessPolicy = sessionAccessPolicy;
        this.sessionProvisioningPolicy = sessionProvisioningPolicy;
        this.currentActorProvider = currentActorProvider;
        this.pilotLaunchRuleService = pilotLaunchRuleService;
        this.releaseCatalogService = releaseCatalogService;
        this.sessionRuntimeLifecyclePort = sessionRuntimeLifecyclePort;
    }

    @Transactional
    public SessionResponse createSession(CreateSessionRequest request) {
        CurrentActor actor = currentActorProvider.requireCurrentActor();
        WorkshopCatalogEntry workshop = workshopCatalogService.getWorkshopEntry(request.workshopId());
        SessionMode requestedMode = request.mode() == null ? workshop.defaultMode() : request.mode();
        String resolvedReleaseVersion = pilotLaunchRuleService.resolveRequestedReleaseVersion(
            workshop.workshopId(),
            request.releaseVersion(),
            workshop.defaultReleaseVersion()
        );
        if (!workshop.supportedModes().contains(requestedMode)) {
            throw new ResponseStatusException(FORBIDDEN, "Requested session mode is not supported");
        }

        ensureAllowed(sessionAccessPolicy.canCreate(new SessionCreatePolicyRequest(
            actor.actorId(),
            PolicyActorMapper.toPolicyActorType(actor),
            actor.authorities(),
            actor.actorId(),
            request.workshopId()
        )));

        Map<String, String> sessionEnvironment = normalizeSessionEnvironment(request.sessionEnvironment());
        PlatformSessionRecord existingSession = findActiveSessionConflict(actor.actorId());
        if (existingSession != null) {
            throw activeSessionConflict(existingSession);
        }

        Instant now = Instant.now();
        SessionProvisioningProfile provisioningProfile = sessionProvisioningPolicy.resolve(new SessionProvisioningRequest(
            workshop.workshopId(),
            resolvedReleaseVersion,
            requestedMode
        ));
        String sessionId = UUID.randomUUID().toString();
        SessionLaunchDescriptor launchDescriptor = launchDescriptorFor(
            sessionId,
            workshop.workshopId(),
            resolvedReleaseVersion,
            requestedMode,
            provisioningProfile
        );
        Map<String, String> runtimeConfig = mergeRuntimeConfig(launchDescriptor.runtimeConfig(), sessionEnvironment);
        PlatformSessionRecord record = new PlatformSessionRecord();
        record.setSessionId(sessionId);
        record.setOwnerUserId(actor.actorId());
        record.setWorkshopId(workshop.workshopId());
        record.setReleaseVersion(resolvedReleaseVersion);
        record.setMode(toPersistenceMode(requestedMode));
        record.setState(PlatformSessionState.REQUESTED);
        record.setQuotaClass(provisioningProfile.quotaClass());
        record.setResourceClass(provisioningProfile.resourceClass());
        record.setCreatedAt(now);
        record.setLastActivityAt(now);
        record.setExpiresAt(now.plus(provisioningProfile.ttl()));
        record.setRouteType(RouteType.PATH);
        record.setWorkspacePolicy(requestedMode == SessionMode.LAB ? WorkspacePolicy.EPHEMERAL : WorkspacePolicy.NONE);
        record.setWorkspaceRef(workspaceRefFor(record));
        record.setWorkspaceCleanupState(initialWorkspaceCleanupState(requestedMode));
        record.setRuntimeStatus("requested");
        sessionRepository.save(record);

        record.setState(PlatformSessionState.ADMITTED);
        record.setRuntimeStatus("admitted");
        record.setLastActivityAt(Instant.now());
        sessionRepository.save(record);

        requestLaunchAfterCommit(new SessionLaunchRequest(
            record.getSessionId(),
            record.getOwnerUserId(),
            record.getWorkshopId(),
            record.getReleaseVersion(),
            requestedMode,
            launchDescriptor.releaseId(),
            launchDescriptor.artifacts(),
            launchDescriptor.resourcePolicy(),
            launchDescriptor.workspacePolicy(),
            runtimeConfig
        ));

        return SessionResponse.fromRecord(record);
    }

    @Transactional
    public List<SessionResponse> listSessions() {
        CurrentActor actor = currentActorProvider.requireCurrentActor();
        ensureAllowed(sessionAccessPolicy.canList(new SessionListPolicyRequest(
            actor.actorId(),
            PolicyActorMapper.toPolicyActorType(actor),
            actor.authorities(),
            actor.actorId()
        )));

        return sessionRepository.findAllByOwnerUserIdOrderByCreatedAtDesc(actor.actorId()).stream()
            .map(this::alignLifecycleState)
            .map(SessionResponse::fromRecord)
            .toList();
    }

    @Transactional
    public SessionResponse getSession(String sessionId) {
        return SessionResponse.fromRecord(requireReadableSession(sessionId));
    }

    @Transactional
    public SessionShellResponse getSessionShell(String sessionId) {
        return SessionShellResponse.fromRecord(requireReadableSession(sessionId));
    }

    private PlatformSessionRecord requireReadableSession(String sessionId) {
        CurrentActor actor = currentActorProvider.requireCurrentActor();
        PlatformSessionRecord record = alignLifecycleState(sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Session not found")));

        ensureAllowed(sessionAccessPolicy.canRead(new SessionAccessPolicyRequest(
            actor.actorId(),
            PolicyActorMapper.toPolicyActorType(actor),
            actor.authorities(),
            record.getOwnerUserId(),
            record.getSessionId()
        )));

        return record;
    }

    @Transactional
    public SessionResponse restartSession(String sessionId, RestartSessionRequest request) {
        CurrentActor actor = currentActorProvider.requireCurrentActor();
        PlatformSessionRecord record = alignLifecycleState(sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Session not found")));

        ensureAllowed(sessionAccessPolicy.canRestart(new SessionAccessPolicyRequest(
            actor.actorId(),
            PolicyActorMapper.toPolicyActorType(actor),
            actor.authorities(),
            record.getOwnerUserId(),
            record.getSessionId()
        )));

        if (RESTART_IN_PROGRESS_STATES.contains(record.getState())) {
            return SessionResponse.fromRecord(record);
        }
        if (!canRestart(record)) {
            throw new ResponseStatusException(CONFLICT, "Session restart is not available from the current state");
        }

        Instant now = Instant.now();
        SessionMode mode = SessionMode.valueOf(record.getMode().name());
        SessionLaunchDescriptor launchDescriptor = launchDescriptorFor(
            record.getSessionId(),
            record.getWorkshopId(),
            record.getReleaseVersion(),
            mode,
            new SessionProvisioningProfile(record.getQuotaClass(), record.getResourceClass(), Duration.between(now, record.getExpiresAt()))
        );
        record.setState(PlatformSessionState.PROVISIONING);
        record.setRuntimeStatus(request.rebuild() ? "restart_requested_rebuild" : "restart_requested");
        record.setLastActivityAt(now);
        record.setFailureCode(null);
        record.setFailureMessage(null);
        record.setWorkspaceCleanupState(activeCleanupState(record));
        sessionRepository.save(record);

        requestRestartAfterCommit(new SessionRestartRequest(
            record.getSessionId(),
            record.getOwnerUserId(),
            record.getWorkshopId(),
            record.getReleaseVersion(),
            mode,
            request.rebuild(),
            launchDescriptor.releaseId(),
            launchDescriptor.artifacts(),
            launchDescriptor.resourcePolicy(),
            launchDescriptor.workspacePolicy(),
            launchDescriptor.runtimeConfig()
        ));

        return SessionResponse.fromRecord(record);
    }

    @Transactional
    public SessionResponse terminateSession(String sessionId) {
        CurrentActor actor = currentActorProvider.requireCurrentActor();
        PlatformSessionRecord record = alignLifecycleState(sessionRepository.findById(sessionId)
            .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Session not found")));

        ensureAllowed(sessionAccessPolicy.canTerminate(new SessionAccessPolicyRequest(
            actor.actorId(),
            PolicyActorMapper.toPolicyActorType(actor),
            actor.authorities(),
            record.getOwnerUserId(),
            record.getSessionId()
        )));

        if (!isTerminationRequested(record.getState())) {
            Instant now = Instant.now();
            record.setState(PlatformSessionState.TERMINATING);
            record.setTerminationReason(actor.isAdmin() ? "admin_requested" : "user_requested");
            record.setRuntimeStatus("termination_requested");
            record.setLastActivityAt(now);
            record.setTerminationRequestedAt(now);
            markCleanupPending(record);
            sessionRepository.save(record);
            requestTerminationAfterCommit(new SessionTerminationRequest(
                record.getSessionId(),
                record.getOwnerUserId(),
                record.getInternalRuntimeRef(),
                record.getTerminationReason()
            ));
        }

        return SessionResponse.fromRecord(record);
    }

    @Transactional
    List<SessionResponse> findExpiredSessions(Instant cutoff) {
        Instant effectiveCutoff = cutoff == null ? Instant.now() : cutoff;
        return sessionRepository.findAllByExpiresAtBeforeAndStateInOrderByExpiresAtAsc(effectiveCutoff, EXPIRABLE_STATES).stream()
            .map(this::alignLifecycleState)
            .map(SessionResponse::fromRecord)
            .toList();
    }

    @Transactional(readOnly = true)
    List<SessionResponse> findCleanupPendingSessions() {
        return sessionRepository.findAllByWorkspaceCleanupStateInOrderByLastActivityAtAsc(List.of(
                WorkspaceCleanupState.PENDING,
                WorkspaceCleanupState.FAILED
            )).stream()
            .map(SessionResponse::fromRecord)
            .toList();
    }

    @Transactional(readOnly = true)
    List<SessionResponse> findLogicalEndedSessionsWithRuntimeRefs() {
        return sessionRepository.findAllByStateInAndInternalRuntimeRefIsNotNullOrderByLastActivityAtAsc(LOGICALLY_ENDED_STATES).stream()
            .map(SessionResponse::fromRecord)
            .toList();
    }

    @Transactional
    int reconcileExpiredSessions(Instant cutoff) {
        Instant effectiveCutoff = cutoff == null ? Instant.now() : cutoff;
        int reconciled = 0;
        for (PlatformSessionRecord record : sessionRepository.findAllByExpiresAtBeforeAndStateInOrderByExpiresAtAsc(
            effectiveCutoff,
            EXPIRABLE_STATES
        )) {
            if (expireSession(record, effectiveCutoff)) {
                sessionRepository.save(record);
                requestTerminationRetryAfterCommit(new SessionTerminationRequest(
                    record.getSessionId(),
                    record.getOwnerUserId(),
                    record.getInternalRuntimeRef(),
                    record.getTerminationReason()
                ));
                reconciled++;
            }
        }
        return reconciled;
    }

    @Transactional
    int reconcileLogicalEndedRuntimeTerminationRetries() {
        int retried = 0;
        for (PlatformSessionRecord record : sessionRepository.findAllByStateInAndInternalRuntimeRefIsNotNullOrderByLastActivityAtAsc(
            LOGICALLY_ENDED_STATES
        )) {
            if (record.getTerminationReason() == null || record.getTerminationReason().isBlank()) {
                record.setTerminationReason(defaultTerminationReason(record.getState()));
                sessionRepository.save(record);
            }
            requestTerminationRetryAfterCommit(new SessionTerminationRequest(
                record.getSessionId(),
                record.getOwnerUserId(),
                record.getInternalRuntimeRef(),
                record.getTerminationReason()
            ));
            retried++;
        }
        return retried;
    }

    @Transactional
    int reconcileCompletedCleanupMetadata() {
        Instant now = Instant.now();
        int completed = 0;
        for (PlatformSessionRecord record : sessionRepository.findAllByWorkspaceCleanupStateInOrderByLastActivityAtAsc(List.of(
            WorkspaceCleanupState.PENDING,
            WorkspaceCleanupState.FAILED
        ))) {
            if (record.getInternalRuntimeRef() != null) {
                continue;
            }
            if (record.getWorkspaceCleanupState() == WorkspaceCleanupState.NONE
                || record.getWorkspaceCleanupState() == WorkspaceCleanupState.COMPLETED) {
                continue;
            }
            record.setWorkspaceCleanupState(terminalCleanupState(record));
            if (record.getCleanupCompletedAt() == null) {
                record.setCleanupCompletedAt(now);
            }
            record.setLastActivityAt(now);
            sessionRepository.save(record);
            completed++;
        }
        return completed;
    }

    private void ensureAllowed(PolicyDecision decision) {
        if (!decision.allowed()) {
            throw new ResponseStatusException(FORBIDDEN, decision.reason());
        }
    }

    private PlatformSessionRecord findActiveSessionConflict(String ownerUserId) {
        for (PlatformSessionRecord existingSession : sessionRepository.findAllByOwnerUserIdAndStateInOrderByCreatedAtDesc(
            ownerUserId,
            ACTIVE_SESSION_CONFLICT_STATES
        )) {
            PlatformSessionRecord alignedSession = alignLifecycleState(existingSession);
            if (ACTIVE_SESSION_CONFLICT_STATES.contains(alignedSession.getState())) {
                return alignedSession;
            }
        }
        return null;
    }

    private Map<String, String> normalizeSessionEnvironment(Map<String, String> submittedEnvironment) {
        if (submittedEnvironment == null || submittedEnvironment.isEmpty()) {
            return Map.of();
        }

        Map<String, String> normalizedEnvironment = new LinkedHashMap<>();
        submittedEnvironment.forEach((key, value) -> {
            String normalizedKey = key == null ? "" : key.trim();
            if (!SESSION_ENVIRONMENT_KEY_PATTERN.matcher(normalizedKey).matches()) {
                throw new ResponseStatusException(BAD_REQUEST, "sessionEnvironment contains an invalid variable name");
            }
            if (isReservedSessionEnvironmentKey(normalizedKey)) {
                throw new ResponseStatusException(BAD_REQUEST, "sessionEnvironment contains a reserved variable name");
            }
            if (value == null) {
                throw new ResponseStatusException(BAD_REQUEST, "sessionEnvironment contains a null variable value");
            }
            normalizedEnvironment.put(normalizedKey, value.trim());
        });
        return Map.copyOf(normalizedEnvironment);
    }

    private boolean isReservedSessionEnvironmentKey(String key) {
        if (RESERVED_SESSION_ENVIRONMENT_KEYS.contains(key)) {
            return true;
        }
        return RESERVED_SESSION_ENVIRONMENT_PREFIXES.stream().anyMatch(key::startsWith);
    }

    private Map<String, String> mergeRuntimeConfig(
        Map<String, String> releaseRuntimeConfig,
        Map<String, String> sessionEnvironment
    ) {
        if (sessionEnvironment.isEmpty()) {
            return releaseRuntimeConfig;
        }

        Map<String, String> runtimeConfig = new LinkedHashMap<>(releaseRuntimeConfig);
        sessionEnvironment.forEach((key, value) -> {
            if (runtimeConfig.containsKey(key)) {
                throw new ResponseStatusException(BAD_REQUEST, "sessionEnvironment cannot override runtime config");
            }
            runtimeConfig.put(key, value);
        });
        runtimeConfig.put(SESSION_ENVIRONMENT_KEYS_CONFIG, String.join(",", sessionEnvironment.keySet()));
        return Map.copyOf(runtimeConfig);
    }

    private ErrorResponseException activeSessionConflict(PlatformSessionRecord existingSession) {
        SessionResponse existingSessionResponse = SessionResponse.fromRecord(existingSession);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
            CONFLICT,
            "You already have an active workshop. Stop it before deploying another one."
        );
        problemDetail.setTitle("Conflict");
        problemDetail.setProperty("message", "You already have an active workshop. Stop it before deploying another one.");
        problemDetail.setProperty("code", "active_session_exists");
        problemDetail.setProperty("existingSessionId", existingSessionResponse.sessionId());
        problemDetail.setProperty("existingSession", existingSessionResponse);
        return new ErrorResponseException(CONFLICT, problemDetail, null);
    }

    private boolean isTerminationRequested(PlatformSessionState state) {
        return state == PlatformSessionState.TERMINATING
            || state == PlatformSessionState.TERMINATED
            || state == PlatformSessionState.CLEANUP_PENDING
            || state == PlatformSessionState.EXPIRED
            || state == PlatformSessionState.FAILED;
    }

    private boolean canRestart(PlatformSessionRecord record) {
        if (!RESTARTABLE_STATES.contains(record.getState())) {
            return false;
        }
        return record.getState() != PlatformSessionState.FAILED
            || record.getTerminationReason() == null
            || record.getTerminationReason().isBlank();
    }

    private PlatformSessionRecord alignLifecycleState(PlatformSessionRecord record) {
        if (record == null) {
            return null;
        }
        if (!EXPIRABLE_STATES.contains(record.getState())) {
            return record;
        }
        Instant now = Instant.now();
        if (!record.getExpiresAt().isAfter(now) && expireSession(record, now)) {
            return sessionRepository.save(record);
        }
        return record;
    }

    private PlatformSessionMode toPersistenceMode(SessionMode mode) {
        return PlatformSessionMode.valueOf(mode.name());
    }

    private WorkspaceCleanupState initialWorkspaceCleanupState(SessionMode mode) {
        return mode == SessionMode.LAB ? WorkspaceCleanupState.ACTIVE : WorkspaceCleanupState.NONE;
    }

    private SessionLaunchDescriptor launchDescriptorFor(
        String sessionId,
        String workshopId,
        String releaseVersion,
        SessionMode mode,
        SessionProvisioningProfile provisioningProfile
    ) {
        ReleaseCatalogEntry release = releaseCatalogService.findRelease(workshopId, releaseVersion)
            .orElseThrow(() -> new ResponseStatusException(
                INTERNAL_SERVER_ERROR,
                "Release backed session launch is missing catalog artifacts for workshop "
                    + workshopId + " and releaseVersion " + releaseVersion
            ));
        if (release.mode() != mode) {
            throw new ResponseStatusException(
                INTERNAL_SERVER_ERROR,
                "Release backed session launch mode mismatch for workshop "
                    + workshopId + " and releaseVersion " + releaseVersion
            );
        }
        try {
            return SessionLaunchDescriptor.from(sessionId, release, provisioningProfile.ttl(), mode);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                INTERNAL_SERVER_ERROR,
                "Release backed session launch has invalid execution plane artifacts for workshop "
                    + workshopId + " and releaseVersion " + releaseVersion,
                exception
            );
        }
    }

    private boolean expireSession(PlatformSessionRecord record, Instant expiredAt) {
        if (record == null || !EXPIRABLE_STATES.contains(record.getState())) {
            return false;
        }
        record.setState(PlatformSessionState.EXPIRED);
        if (record.getExpiredAt() == null) {
            record.setExpiredAt(expiredAt);
        }
        record.setRuntimeStatus("expired");
        record.setLastActivityAt(expiredAt);
        if (record.getTerminationReason() == null || record.getTerminationReason().isBlank()) {
            record.setTerminationReason("expired");
        }
        if (record.getTerminationRequestedAt() == null) {
            record.setTerminationRequestedAt(expiredAt);
        }
        markCleanupPending(record);
        return true;
    }

    private String workspaceRefFor(PlatformSessionRecord record) {
        if (record.getWorkspacePolicy() != WorkspacePolicy.EPHEMERAL) {
            return null;
        }
        return "session-workspace:" + record.getSessionId();
    }

    private void markCleanupPending(PlatformSessionRecord record) {
        if (record.getWorkspacePolicy() == WorkspacePolicy.EPHEMERAL
            && record.getWorkspaceCleanupState() != WorkspaceCleanupState.COMPLETED) {
            record.setWorkspaceCleanupState(WorkspaceCleanupState.PENDING);
        }
    }

    private WorkspaceCleanupState activeCleanupState(PlatformSessionRecord record) {
        if (record.getWorkspacePolicy() == WorkspacePolicy.NONE
            || record.getWorkspaceCleanupState() == WorkspaceCleanupState.NONE) {
            return WorkspaceCleanupState.NONE;
        }
        return WorkspaceCleanupState.ACTIVE;
    }

    private void requestLaunchAfterCommit(SessionLaunchRequest request) {
        runAfterCommit(() -> sessionRuntimeLifecyclePort.requestLaunch(request));
    }

    private void requestRestartAfterCommit(SessionRestartRequest request) {
        runAfterCommit(() -> sessionRuntimeLifecyclePort.requestRestart(request));
    }

    private void requestTerminationAfterCommit(SessionTerminationRequest request) {
        runAfterCommit(() -> sessionRuntimeLifecyclePort.requestTermination(request));
    }

    private void requestTerminationRetryAfterCommit(SessionTerminationRequest request) {
        runAfterCommit(() -> sessionRuntimeLifecyclePort.requestTerminationRetry(request));
    }

    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()
            || !TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    private WorkspaceCleanupState terminalCleanupState(PlatformSessionRecord record) {
        if (record.getWorkspacePolicy() == WorkspacePolicy.NONE
            || record.getWorkspaceCleanupState() == WorkspaceCleanupState.NONE) {
            return WorkspaceCleanupState.NONE;
        }
        return WorkspaceCleanupState.COMPLETED;
    }

    private String defaultTerminationReason(PlatformSessionState state) {
        if (state == PlatformSessionState.EXPIRED) {
            return "expired";
        }
        if (state == PlatformSessionState.FAILED) {
            return "runtime_failed";
        }
        return "cleanup_reconciler";
    }
}
