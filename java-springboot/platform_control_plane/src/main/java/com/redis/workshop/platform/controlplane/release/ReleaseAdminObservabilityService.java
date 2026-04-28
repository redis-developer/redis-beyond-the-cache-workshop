package com.redis.workshop.platform.controlplane.release;

import com.redis.workshop.platform.controlplane.audit.AuditEventPayload;
import com.redis.workshop.platform.controlplane.audit.AuditEventResponse;
import com.redis.workshop.platform.controlplane.audit.AuditTrailService;
import com.redis.workshop.platform.controlplane.observability.ReleaseCatalogMetricsRecorder;
import com.redis.workshop.platform.controlplane.observability.ReleaseCatalogMetricsSnapshot;
import com.redis.workshop.platform.controlplane.persistence.model.AuditActionType;
import com.redis.workshop.platform.controlplane.persistence.model.AuditResult;
import com.redis.workshop.platform.controlplane.persistence.model.AuditTargetType;
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

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class ReleaseAdminObservabilityService {

    private final ReleaseCatalogService releaseCatalogService;
    private final ReleaseCatalogMetricsRecorder releaseCatalogMetricsRecorder;
    private final CurrentActorProvider currentActorProvider;
    private final AdminActionPolicy adminActionPolicy;
    private final AuditTrailService auditTrailService;
    private final ObjectProvider<HttpServletRequest> httpServletRequestProvider;

    public ReleaseAdminObservabilityService(
        ReleaseCatalogService releaseCatalogService,
        ReleaseCatalogMetricsRecorder releaseCatalogMetricsRecorder,
        CurrentActorProvider currentActorProvider,
        AdminActionPolicy adminActionPolicy,
        AuditTrailService auditTrailService,
        ObjectProvider<HttpServletRequest> httpServletRequestProvider
    ) {
        this.releaseCatalogService = releaseCatalogService;
        this.releaseCatalogMetricsRecorder = releaseCatalogMetricsRecorder;
        this.currentActorProvider = currentActorProvider;
        this.adminActionPolicy = adminActionPolicy;
        this.auditTrailService = auditTrailService;
        this.httpServletRequestProvider = httpServletRequestProvider;
    }

    public ReleaseAdminOverviewResponse getOverview() {
        CurrentActor actor = requireAdminAccess("read_release_overview", "observability:release-overview");
        ReleaseCatalogInspection inspection = releaseCatalogService.inspectCatalog();
        ReleaseCatalogMetricsSnapshot metrics = releaseCatalogMetricsRecorder.snapshot();
        List<ReleaseAdminCatalogEntryResponse> releases = inspection.releases().stream()
            .map(ReleaseAdminCatalogEntryResponse::fromEntry)
            .sorted(Comparator
                .comparing(ReleaseAdminCatalogEntryResponse::workshopId)
                .thenComparing(ReleaseAdminCatalogEntryResponse::releaseId))
            .toList();
        List<AuditEventResponse> recentReleaseAuditEvents = auditTrailService
            .getRecentEventsByTargetPrefix("release:", 20);

        recordAdminRead(actor, "observability:release-overview", "release_overview");

        return new ReleaseAdminOverviewResponse(
            Instant.now(),
            inspection.available(),
            inspection.failureReason(),
            metrics,
            releases,
            recentReleaseAuditEvents
        );
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
}
