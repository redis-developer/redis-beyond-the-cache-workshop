package com.redis.workshop.platform.contracts.executionplane;

public enum ExecutionPlaneTerminationMode {
    USER_REQUESTED,
    EXPIRED,
    CLEANUP_RETRY,
    ADMIN_FORCED
}
