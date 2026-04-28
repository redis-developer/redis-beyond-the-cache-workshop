package com.redis.workshop.platform.controlplane.admin;

import java.time.Instant;

public record AdminPendingSessionResponse(
    String sessionId,
    String workshopId,
    String mode,
    String state,
    long pendingAgeSeconds,
    Instant createdAt,
    Instant expiresAt
) {
}
