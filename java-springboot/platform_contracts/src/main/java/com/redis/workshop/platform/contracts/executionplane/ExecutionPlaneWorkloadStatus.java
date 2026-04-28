package com.redis.workshop.platform.contracts.executionplane;

public enum ExecutionPlaneWorkloadStatus {
    ACCEPTED,
    PROVISIONING,
    READY,
    DEGRADED,
    TERMINATING,
    TERMINATED,
    FAILED
}
