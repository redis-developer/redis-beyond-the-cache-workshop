package com.redis.workshop.platform.controlplane.admin;

import java.time.Instant;

public record AdminProvisioningFailureSummaryResponse(
    String failureCode,
    long count,
    Instant mostRecentFailureAt
) {
}
