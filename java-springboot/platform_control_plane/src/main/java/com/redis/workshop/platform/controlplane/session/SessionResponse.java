package com.redis.workshop.platform.controlplane.session;

import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.model.RouteType;

import java.time.Instant;

public record SessionResponse(
    String sessionId,
    String workshopId,
    String releaseVersion,
    SessionMode mode,
    SessionState state,
    Instant createdAt,
    Instant expiresAt,
    Instant lastActivityAt,
    Instant expiredAt,
    Instant terminationRequestedAt,
    Instant terminatedAt,
    Instant cleanupCompletedAt,
    String publicEntryUrl,
    String pollUrl,
    RouteType routeType,
    String workspaceRef,
    WorkspaceCleanupStatus workspaceCleanupStatus,
    String failureCode,
    String failureMessage,
    String terminationReason
) {

    static SessionResponse fromRecord(PlatformSessionRecord record) {
        return new SessionResponse(
            record.getSessionId(),
            record.getWorkshopId(),
            record.getReleaseVersion(),
            toSessionMode(record),
            toSessionState(record.getState()),
            record.getCreatedAt(),
            record.getExpiresAt(),
            record.getLastActivityAt(),
            record.getExpiredAt(),
            record.getTerminationRequestedAt(),
            record.getTerminatedAt(),
            record.getCleanupCompletedAt(),
            record.getPublicEntryUrl(),
            "/api/sessions/" + record.getSessionId(),
            record.getRouteType(),
            record.getWorkspaceRef(),
            toWorkspaceCleanupStatus(record),
            record.getFailureCode(),
            record.getFailureMessage(),
            record.getTerminationReason()
        );
    }

    private static SessionMode toSessionMode(PlatformSessionRecord record) {
        return SessionMode.valueOf(record.getMode().name());
    }

    private static SessionState toSessionState(PlatformSessionState state) {
        return SessionState.valueOf(state.name());
    }

    private static WorkspaceCleanupStatus toWorkspaceCleanupStatus(PlatformSessionRecord record) {
        return WorkspaceCleanupStatus.valueOf(record.getWorkspaceCleanupState().name());
    }
}
