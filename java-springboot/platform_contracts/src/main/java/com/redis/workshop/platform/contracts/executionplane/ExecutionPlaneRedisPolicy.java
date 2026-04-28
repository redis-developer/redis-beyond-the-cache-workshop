package com.redis.workshop.platform.contracts.executionplane;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

public record ExecutionPlaneRedisPolicy(
    @NotNull ExecutionPlaneRedisMode mode,
    @NotBlank String keyPrefix,
    String flavor,
    @Valid ExecutionPlaneLocalRedisBinding local
) {

    public ExecutionPlaneRedisPolicy {
        if (mode == null) {
            throw new IllegalArgumentException("mode is required");
        }
        keyPrefix = requireText(keyPrefix, "keyPrefix");
        flavor = normalizeOptional(flavor, "standard");
        if (mode != ExecutionPlaneRedisMode.LOCAL_PROCESS) {
            throw new IllegalArgumentException("only local Redis mode is supported");
        }
        if (local == null) {
            throw new IllegalArgumentException("local binding is required for local Redis mode");
        }
    }

    public Map<String, String> toRuntimeConfig() {
        Map<String, String> config = new LinkedHashMap<>();
        config.put("WORKSHOP_REDIS_MODE", runtimeMode(mode));
        config.put("WORKSHOP_REDIS_KEY_PREFIX", keyPrefix);
        config.put("WORKSHOP_REDIS_FLAVOR", flavor);

        config.put("WORKSHOP_LOCAL_REDIS_COMMAND", local.redisCommand());
        config.put("WORKSHOP_LOCAL_REDIS_PORT", String.valueOf(local.redisPort()));
        config.put("WORKSHOP_LOCAL_REDIS_DATA_PATH", local.redisDataPath());
        config.put("WORKSHOP_LOCAL_REDIS_HEALTH_COMMAND", local.redisHealthCommand());
        config.put("WORKSHOP_REDIS_INSIGHT_COMMAND", local.redisInsightCommand());
        config.put("WORKSHOP_REDIS_INSIGHT_PORT", String.valueOf(local.redisInsightPort()));
        config.put("WORKSHOP_REDIS_INSIGHT_BASE_PATH", local.redisInsightBasePath());
        config.put("WORKSHOP_REDIS_INSIGHT_HEALTH_PATH", local.redisInsightHealthPath());

        return Map.copyOf(config);
    }

    private static String runtimeMode(ExecutionPlaneRedisMode mode) {
        return "local-process";
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }
}
