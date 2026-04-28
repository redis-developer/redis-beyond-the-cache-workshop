package com.redis.workshop.platform.controlplane.session;

record SessionResourcePolicy(
    String resourceClass,
    int cpuMillis,
    int memoryMiB,
    int storageMiB,
    int ttlMinutes
) {

    SessionResourcePolicy {
        resourceClass = requireText(resourceClass, "resourceClass");
        if (cpuMillis <= 0 || memoryMiB <= 0 || storageMiB <= 0 || ttlMinutes <= 0) {
            throw new IllegalArgumentException("resource policy values must be positive");
        }
    }

    static SessionResourcePolicy defaultFor(String resourceClass, long ttlMinutes) {
        String normalized = requireText(resourceClass, "resourceClass");
        int effectiveTtl = Math.toIntExact(Math.max(1, ttlMinutes));
        return switch (normalized) {
            case "medium" -> new SessionResourcePolicy(normalized, 1000, 2048, 2048, effectiveTtl);
            case "large" -> new SessionResourcePolicy(normalized, 2000, 4096, 4096, effectiveTtl);
            default -> new SessionResourcePolicy(normalized, 1000, 2048, 1024, effectiveTtl);
        };
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
