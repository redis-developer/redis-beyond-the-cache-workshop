package com.redis.workshop.platform.contracts.executionplane;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ExecutionPlaneLocalRedisBinding(
    @NotBlank String redisCommand,
    @Min(1) int redisPort,
    String redisDataPath,
    String redisHealthCommand,
    @NotBlank String redisInsightCommand,
    @Min(1) int redisInsightPort,
    String redisInsightBasePath,
    String redisInsightHealthPath
) {

    public ExecutionPlaneLocalRedisBinding {
        redisCommand = requireText(redisCommand, "redisCommand");
        redisDataPath = normalizeOptional(redisDataPath);
        redisHealthCommand = normalizeOptional(redisHealthCommand);
        redisInsightCommand = requireText(redisInsightCommand, "redisInsightCommand");
        redisInsightBasePath = normalizeOptional(redisInsightBasePath);
        redisInsightHealthPath = normalizeOptional(redisInsightHealthPath);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String normalizeOptional(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
