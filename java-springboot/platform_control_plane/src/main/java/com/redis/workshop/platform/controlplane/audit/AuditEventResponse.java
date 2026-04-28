package com.redis.workshop.platform.controlplane.audit;

import com.redis.workshop.platform.controlplane.persistence.model.AuditEventRecord;

import java.time.Instant;
import java.util.UUID;

public record AuditEventResponse(
    UUID eventId,
    String actorId,
    String actorType,
    String actionType,
    String targetResourceType,
    String targetResourceId,
    Instant createdAt,
    String result,
    String reason,
    String requestId,
    String sessionId,
    String workshopId,
    String releaseVersion,
    String runtimeRef
) {

    public static AuditEventResponse fromRecord(AuditEventRecord record) {
        return new AuditEventResponse(
            record.getEventId(),
            record.getActorId(),
            record.getActorType().name(),
            record.getActionType().name(),
            record.getTargetResourceType().name(),
            record.getTargetResourceId(),
            record.getCreatedAt(),
            record.getResult().name(),
            record.getReason(),
            record.getRequestId(),
            record.getSessionId(),
            record.getWorkshopId(),
            record.getReleaseVersion(),
            record.getRuntimeRef()
        );
    }
}
