package com.redis.workshop.platform.controlplane.redis;

import com.redis.workshop.platform.controlplane.release.ReleaseCatalogEntry;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RedisSessionRuntimeConfig {

    private static final String REDIS_DEPENDENCY = "redis";

    private RedisSessionRuntimeConfig() {
    }

    public static Map<String, String> fromRelease(String sessionId, ReleaseCatalogEntry release) {
        if (release == null || !requiresRedis(release.mutableDependencies())) {
            return Map.of();
        }

        Map<String, String> config = new LinkedHashMap<>();
        config.put("WORKSHOP_REDIS_MODE", "local-process");
        config.put("WORKSHOP_REDIS_KEY_PREFIX", "session:" + requireText(sessionId, "sessionId") + ":");
        config.put("WORKSHOP_REDIS_FLAVOR", "standard");
        return Map.copyOf(config);
    }

    private static boolean requiresRedis(List<String> mutableDependencies) {
        if (mutableDependencies == null) {
            return false;
        }
        return mutableDependencies.stream().anyMatch(dependency -> REDIS_DEPENDENCY.equalsIgnoreCase(dependency));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
