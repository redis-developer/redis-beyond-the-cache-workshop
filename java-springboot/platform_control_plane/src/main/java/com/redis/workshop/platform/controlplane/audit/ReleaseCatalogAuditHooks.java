package com.redis.workshop.platform.controlplane.audit;

import com.redis.workshop.platform.controlplane.persistence.model.AuditActionType;
import com.redis.workshop.platform.controlplane.persistence.model.AuditResult;
import com.redis.workshop.platform.controlplane.persistence.model.AuditTargetType;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogEntry;
import com.redis.workshop.platform.controlplane.security.CurrentActor;
import com.redis.workshop.platform.controlplane.security.CurrentActorType;
import com.redis.workshop.platform.controlplane.session.SessionResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class ReleaseCatalogAuditHooks {

    private final AuditTrailService auditTrailService;
    private final AtomicReference<String> lastCatalogFingerprint = new AtomicReference<>();

    public ReleaseCatalogAuditHooks(AuditTrailService auditTrailService) {
        this.auditTrailService = auditTrailService;
    }

    public void recordCatalogActivation(List<ReleaseCatalogEntry> releases) {
        String fingerprint = fingerprint(releases);
        String previous = lastCatalogFingerprint.getAndSet(fingerprint);
        if (fingerprint.equals(previous)) {
            return;
        }

        auditTrailService.record(new AuditEventPayload(
            "release-catalog",
            CurrentActorType.INTERNAL_SERVICE,
            AuditActionType.ADMIN_OBSERVABILITY_READ,
            AuditTargetType.ADMIN_ACTION,
            "release:catalog-activated",
            AuditResult.SUCCESS,
            "release_count=" + releases.size() + ", fingerprint=" + fingerprint,
            null,
            null,
            null,
            null,
            null,
            Instant.now()
        ));
    }

    public void recordCatalogLoadFailure(String reason) {
        auditTrailService.record(new AuditEventPayload(
            "release-catalog",
            CurrentActorType.INTERNAL_SERVICE,
            AuditActionType.ADMIN_OBSERVABILITY_READ,
            AuditTargetType.ADMIN_ACTION,
            "release:catalog-load-failure",
            AuditResult.FAILURE,
            reason,
            null,
            null,
            null,
            null,
            null,
            Instant.now()
        ));
    }

    public void recordReleaseSelection(
        CurrentActor actor,
        AuditActionType actionType,
        SessionResponse sessionResponse,
        String requestId,
        String launchAction,
        ReleaseCatalogEntry release
    ) {
        auditTrailService.record(new AuditEventPayload(
            actor.actorId(),
            actor.actorType(),
            actionType,
            AuditTargetType.ADMIN_ACTION,
            "release:selection:" + release.releaseId(),
            AuditResult.SUCCESS,
            "launch_action=" + launchAction,
            requestId,
            sessionResponse.sessionId(),
            sessionResponse.workshopId(),
            sessionResponse.releaseVersion(),
            null,
            Instant.now()
        ));
    }

    public void recordReleaseSelectionLookupMiss(
        CurrentActor actor,
        AuditActionType actionType,
        SessionResponse sessionResponse,
        String requestId,
        String launchAction
    ) {
        auditTrailService.record(new AuditEventPayload(
            actor.actorId(),
            actor.actorType(),
            actionType,
            AuditTargetType.ADMIN_ACTION,
            "release:lookup-miss",
            AuditResult.FAILURE,
            "launch_action=" + launchAction,
            requestId,
            sessionResponse.sessionId(),
            sessionResponse.workshopId(),
            sessionResponse.releaseVersion(),
            null,
            Instant.now()
        ));
    }

    private String fingerprint(List<ReleaseCatalogEntry> releases) {
        return Integer.toHexString(releases.stream()
            .sorted(Comparator.comparing(ReleaseCatalogEntry::releaseId))
            .map(entry -> entry.releaseId() + ":" + entry.releaseVersion() + ":" + entry.mode().name())
            .reduce("", (left, right) -> left + "|" + right)
            .hashCode());
    }
}
