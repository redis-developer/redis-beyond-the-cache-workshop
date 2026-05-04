package com.redis.workshop.platform.controlplane.portal;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public record PortalSession(
    String email,
    Instant createdAt,
    Instant expiresAt,
    List<String> sessionIds
) {

    public PortalSession {
        sessionIds = sanitizeSessionIds(sessionIds);
    }

    public PortalSession withSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank() || sessionIds.contains(sessionId)) {
            return this;
        }
        List<String> updatedSessionIds = new ArrayList<>(sessionIds);
        updatedSessionIds.add(sessionId);
        return new PortalSession(email, createdAt, expiresAt, updatedSessionIds);
    }

    public PortalSession withoutSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank() || !sessionIds.contains(sessionId)) {
            return this;
        }
        return new PortalSession(
            email,
            createdAt,
            expiresAt,
            sessionIds.stream()
                .filter(candidate -> !candidate.equals(sessionId))
                .toList()
        );
    }

    private static List<String> sanitizeSessionIds(List<String> sessionIds) {
        if (sessionIds == null || sessionIds.isEmpty()) {
            return List.of();
        }
        Set<String> uniqueSessionIds = new LinkedHashSet<>();
        for (String sessionId : sessionIds) {
            if (sessionId != null && !sessionId.isBlank()) {
                uniqueSessionIds.add(sessionId);
            }
        }
        return List.copyOf(uniqueSessionIds);
    }
}
