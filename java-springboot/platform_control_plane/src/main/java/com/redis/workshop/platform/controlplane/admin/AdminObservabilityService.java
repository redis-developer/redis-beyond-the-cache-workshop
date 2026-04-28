package com.redis.workshop.platform.controlplane.admin;

import com.redis.workshop.platform.controlplane.audit.AuditEventPayload;
import com.redis.workshop.platform.controlplane.audit.AuditEventResponse;
import com.redis.workshop.platform.controlplane.audit.AuditTrailService;
import com.redis.workshop.platform.controlplane.observability.SessionLifecycleMetricsRecorder;
import com.redis.workshop.platform.controlplane.observability.SessionMetricsSnapshot;
import com.redis.workshop.platform.controlplane.persistence.model.AuditActionType;
import com.redis.workshop.platform.controlplane.persistence.model.AuditResult;
import com.redis.workshop.platform.controlplane.persistence.model.AuditTargetType;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.repository.PlatformSessionRecordRepository;
import com.redis.workshop.platform.controlplane.policy.AdminActionPolicy;
import com.redis.workshop.platform.controlplane.policy.AdminActionPolicyRequest;
import com.redis.workshop.platform.controlplane.policy.PolicyDecision;
import com.redis.workshop.platform.controlplane.security.CurrentActor;
import com.redis.workshop.platform.controlplane.security.CurrentActorProvider;
import com.redis.workshop.platform.controlplane.security.PolicyActorMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class AdminObservabilityService {

    private static final EnumSet<PlatformSessionState> ACTIVE_STATES = EnumSet.of(
        PlatformSessionState.REQUESTED,
        PlatformSessionState.ADMITTED,
        PlatformSessionState.PROVISIONING,
        PlatformSessionState.INITIALIZING,
        PlatformSessionState.READY,
        PlatformSessionState.DEGRADED,
        PlatformSessionState.TERMINATING
    );

    private static final EnumSet<PlatformSessionState> PENDING_STATES = EnumSet.of(
        PlatformSessionState.REQUESTED,
        PlatformSessionState.ADMITTED,
        PlatformSessionState.PROVISIONING,
        PlatformSessionState.INITIALIZING,
        PlatformSessionState.TERMINATING
    );

    private final PlatformSessionRecordRepository platformSessionRecordRepository;
    private final CurrentActorProvider currentActorProvider;
    private final AdminActionPolicy adminActionPolicy;
    private final AuditTrailService auditTrailService;
    private final SessionLifecycleMetricsRecorder sessionLifecycleMetricsRecorder;
    private final ObjectProvider<HttpServletRequest> httpServletRequestProvider;

    public AdminObservabilityService(
        PlatformSessionRecordRepository platformSessionRecordRepository,
        CurrentActorProvider currentActorProvider,
        AdminActionPolicy adminActionPolicy,
        AuditTrailService auditTrailService,
        SessionLifecycleMetricsRecorder sessionLifecycleMetricsRecorder,
        ObjectProvider<HttpServletRequest> httpServletRequestProvider
    ) {
        this.platformSessionRecordRepository = platformSessionRecordRepository;
        this.currentActorProvider = currentActorProvider;
        this.adminActionPolicy = adminActionPolicy;
        this.auditTrailService = auditTrailService;
        this.sessionLifecycleMetricsRecorder = sessionLifecycleMetricsRecorder;
        this.httpServletRequestProvider = httpServletRequestProvider;
    }

    public AdminObservabilityOverviewResponse getOverview() {
        CurrentActor actor = requireAdminAccess("read_overview", "observability:overview");
        Instant now = Instant.now();
        List<PlatformSessionRecord> sessions = platformSessionRecordRepository.findAll();
        List<AuditEventResponse> recentAuditEvents = auditTrailService.getRecentEvents(20);
        SessionMetricsSnapshot metricsSnapshot = sessionLifecycleMetricsRecorder.snapshot();

        AdminObservabilityOverviewResponse response = new AdminObservabilityOverviewResponse(
            now,
            buildActiveSessionCounts(sessions),
            buildFailureSummary(sessions),
            buildPendingSessions(sessions, now),
            recentAuditEvents,
            metricsSnapshot
        );

        recordAdminRead(actor, "observability:overview", "overview");
        return response;
    }

    public List<AuditEventResponse> getRecentAuditEvents(int limit) {
        CurrentActor actor = requireAdminAccess("read_audit_events", "observability:audit-events");
        List<AuditEventResponse> events = auditTrailService.getRecentEvents(limit);
        recordAdminRead(actor, "observability:audit-events", "recent_audit_events");
        return events;
    }

    private List<AdminSessionCountResponse> buildActiveSessionCounts(List<PlatformSessionRecord> sessions) {
        return sessions.stream()
            .filter(session -> ACTIVE_STATES.contains(session.getState()))
            .collect(java.util.stream.Collectors.groupingBy(
                session -> session.getWorkshopId() + "|" + session.getMode().name() + "|" + session.getState().name(),
                java.util.stream.Collectors.counting()
            ))
            .entrySet().stream()
            .map(entry -> {
                String[] parts = entry.getKey().split("\\|", 3);
                return new AdminSessionCountResponse(parts[0], parts[1], parts[2], entry.getValue());
            })
            .sorted(Comparator
                .comparing(AdminSessionCountResponse::workshopId)
                .thenComparing(AdminSessionCountResponse::mode)
                .thenComparing(AdminSessionCountResponse::state))
            .toList();
    }

    private List<AdminProvisioningFailureSummaryResponse> buildFailureSummary(List<PlatformSessionRecord> sessions) {
        Map<String, List<PlatformSessionRecord>> failuresByCode = sessions.stream()
            .filter(session -> session.getState() == PlatformSessionState.FAILED || hasText(session.getFailureCode()))
            .collect(java.util.stream.Collectors.groupingBy(
                session -> hasText(session.getFailureCode()) ? session.getFailureCode() : "unknown"
            ));

        return failuresByCode.entrySet().stream()
            .map(entry -> new AdminProvisioningFailureSummaryResponse(
                entry.getKey(),
                entry.getValue().size(),
                entry.getValue().stream()
                    .map(PlatformSessionRecord::getLastActivityAt)
                    .filter(java.util.Objects::nonNull)
                    .max(Comparator.naturalOrder())
                    .orElse(null)
            ))
            .sorted(Comparator.comparing(AdminProvisioningFailureSummaryResponse::count).reversed())
            .toList();
    }

    private List<AdminPendingSessionResponse> buildPendingSessions(List<PlatformSessionRecord> sessions, Instant now) {
        return sessions.stream()
            .filter(session -> PENDING_STATES.contains(session.getState()))
            .map(session -> new AdminPendingSessionResponse(
                session.getSessionId(),
                session.getWorkshopId(),
                session.getMode().name(),
                session.getState().name(),
                Duration.between(session.getCreatedAt(), now).getSeconds(),
                session.getCreatedAt(),
                session.getExpiresAt()
            ))
            .sorted(Comparator.comparing(AdminPendingSessionResponse::pendingAgeSeconds).reversed())
            .limit(10)
            .toList();
    }

    private CurrentActor requireAdminAccess(String action, String targetResourceId) {
        CurrentActor actor = currentActorProvider.requireCurrentActor();
        PolicyDecision decision = adminActionPolicy.canPerform(new AdminActionPolicyRequest(
            actor.actorId(),
            PolicyActorMapper.toPolicyActorType(actor),
            actor.authorities(),
            action,
            AuditTargetType.ADMIN_ACTION.name(),
            targetResourceId
        ));

        if (!decision.allowed()) {
            throw new ResponseStatusException(FORBIDDEN, decision.reason());
        }

        return actor;
    }

    private void recordAdminRead(CurrentActor actor, String targetResourceId, String reason) {
        auditTrailService.record(new AuditEventPayload(
            actor.actorId(),
            actor.actorType(),
            AuditActionType.ADMIN_OBSERVABILITY_READ,
            AuditTargetType.ADMIN_ACTION,
            targetResourceId,
            AuditResult.SUCCESS,
            reason,
            resolveRequestId(),
            null,
            null,
            null,
            null,
            null
        ));
    }

    private String resolveRequestId() {
        HttpServletRequest request = httpServletRequestProvider.getIfAvailable();
        return request == null ? null : request.getHeader("X-Request-Id");
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
