package com.redis.workshop.platform.controlplane.session;

import com.redis.workshop.platform.controlplane.release.ReleaseCatalogEntry;
import com.redis.workshop.platform.controlplane.release.ReleaseImageReferences;
import com.redis.workshop.platform.controlplane.redis.RedisSessionRuntimeConfig;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

record SessionLaunchDescriptor(
    String releaseId,
    Map<String, String> artifacts,
    SessionResourcePolicy resourcePolicy,
    SessionWorkspacePolicy workspacePolicy,
    Map<String, String> runtimeConfig
) {

    SessionLaunchDescriptor {
        releaseId = requireText(releaseId, "releaseId");
        artifacts = Map.copyOf(artifacts == null ? Map.of() : artifacts);
        if (artifacts.isEmpty()) {
            throw new IllegalArgumentException("at least one release artifact is required");
        }
        if (resourcePolicy == null) {
            throw new IllegalArgumentException("resourcePolicy is required");
        }
        if (workspacePolicy == null) {
            throw new IllegalArgumentException("workspacePolicy is required");
        }
        runtimeConfig = Map.copyOf(runtimeConfig == null ? Map.of() : runtimeConfig);
    }

    static SessionLaunchDescriptor from(String sessionId, ReleaseCatalogEntry release, Duration ttl, SessionMode mode) {
        if (release == null) {
            throw new IllegalArgumentException("release is required");
        }
        return new SessionLaunchDescriptor(
            release.releaseId(),
            artifactsFrom(release.images()),
            SessionResourcePolicy.defaultFor(release.resourceClass(), ttl.toMinutes()),
            SessionWorkspacePolicy.defaultFor(release.releaseId(), mode),
            runtimeConfigFrom(sessionId, release)
        );
    }

    private static Map<String, String> artifactsFrom(ReleaseImageReferences images) {
        if (images == null) {
            return Map.of();
        }
        Map<String, String> artifacts = new LinkedHashMap<>();
        putIfPresent(artifacts, "frontend", images.frontend());
        putIfPresent(artifacts, "backend", images.backend());
        putIfPresent(artifacts, "combined", images.combined());
        putIfPresent(artifacts, "init", images.init());
        return artifacts;
    }

    private static Map<String, String> runtimeConfigFrom(String sessionId, ReleaseCatalogEntry release) {
        Map<String, String> runtimeConfig = new LinkedHashMap<>();
        runtimeConfig.put("releaseId", release.releaseId());
        runtimeConfig.put("mutableDependencies", String.join(",", release.mutableDependencies()));
        runtimeConfig.putAll(RedisSessionRuntimeConfig.fromRelease(sessionId, release));
        return runtimeConfig;
    }

    private static void putIfPresent(Map<String, String> artifacts, String role, String value) {
        if (value != null && !value.isBlank()) {
            artifacts.put(role, value.trim());
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
