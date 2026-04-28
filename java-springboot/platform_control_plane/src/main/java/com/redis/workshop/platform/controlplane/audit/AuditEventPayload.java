package com.redis.workshop.platform.controlplane.audit;

import com.redis.workshop.platform.controlplane.persistence.model.AuditActionType;
import com.redis.workshop.platform.controlplane.persistence.model.AuditResult;
import com.redis.workshop.platform.controlplane.persistence.model.AuditTargetType;
import com.redis.workshop.platform.controlplane.security.CurrentActorType;

import java.time.Instant;

public record AuditEventPayload(
    String actorId,
    CurrentActorType actorType,
    AuditActionType actionType,
    AuditTargetType targetResourceType,
    String targetResourceId,
    AuditResult result,
    String reason,
    String requestId,
    String sessionId,
    String workshopId,
    String releaseVersion,
    String runtimeRef,
    Instant createdAt
) {
}
