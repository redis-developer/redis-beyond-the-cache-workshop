package com.redis.workshop.platform.controlplane.session;

import java.util.Map;

public record SessionRestartRequest(
    String sessionId,
    String ownerUserId,
    String workshopId,
    String releaseVersion,
    SessionMode mode,
    boolean rebuild,
    String releaseId,
    Map<String, String> artifacts,
    SessionResourcePolicy resourcePolicy,
    SessionWorkspacePolicy workspacePolicy,
    Map<String, String> runtimeConfig
) {

    public SessionRestartRequest(
        String sessionId,
        String ownerUserId,
        String workshopId,
        String releaseVersion,
        SessionMode mode,
        boolean rebuild
    ) {
        this(
            sessionId,
            ownerUserId,
            workshopId,
            releaseVersion,
            mode,
            rebuild,
            releaseVersion,
            Map.of(),
            SessionResourcePolicy.defaultFor("small", 60),
            SessionWorkspacePolicy.defaultFor(releaseVersion, mode),
            Map.of()
        );
    }

    public SessionRestartRequest {
        sessionId = requireText(sessionId, "sessionId");
        ownerUserId = requireText(ownerUserId, "ownerUserId");
        workshopId = requireText(workshopId, "workshopId");
        releaseVersion = requireText(releaseVersion, "releaseVersion");
        releaseId = requireText(releaseId, "releaseId");
        if (mode == null) {
            throw new IllegalArgumentException("mode is required");
        }
        artifacts = artifacts == null ? Map.of() : Map.copyOf(artifacts);
        if (resourcePolicy == null) {
            throw new IllegalArgumentException("resourcePolicy is required");
        }
        if (workspacePolicy == null) {
            throw new IllegalArgumentException("workspacePolicy is required");
        }
        runtimeConfig = runtimeConfig == null ? Map.of() : Map.copyOf(runtimeConfig);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
