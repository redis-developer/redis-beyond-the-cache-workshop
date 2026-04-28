package com.redis.workshop.platform.controlplane.observability;

public record SessionMetricsSnapshot(
    double sessionCreateRequestCount,
    double sessionCreateSuccessCount,
    double sessionTerminationCount,
    double pendingSessionMaxAgeSeconds
) {
}
