package com.redis.workshop.platform.controlplane.session;

public record SessionTerminationRequest(
    String sessionId,
    String ownerUserId,
    String runtimeRef,
    String reason
) {

    public SessionTerminationRequest(String sessionId, String ownerUserId, String reason) {
        this(sessionId, ownerUserId, null, reason);
    }

    public SessionTerminationRequest {
        sessionId = requireText(sessionId, "sessionId");
        ownerUserId = requireText(ownerUserId, "ownerUserId");
        runtimeRef = normalizeOptional(runtimeRef);
        reason = requireText(reason, "reason");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
