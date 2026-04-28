package com.redis.workshop.platform.controlplane.policy;

import java.time.Duration;

public record SessionProvisioningProfile(
    String quotaClass,
    String resourceClass,
    Duration ttl
) {
}
