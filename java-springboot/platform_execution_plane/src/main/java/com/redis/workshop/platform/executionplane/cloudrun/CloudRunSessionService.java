package com.redis.workshop.platform.executionplane.cloudrun;

public record CloudRunSessionService(
    String projectId,
    String region,
    String serviceName,
    String uri
) {
}
