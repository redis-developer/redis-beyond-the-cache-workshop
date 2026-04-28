package com.redis.workshop.platform.controlplane.persistence.model;

public enum PlatformSessionState {
    REQUESTED,
    ADMITTED,
    PROVISIONING,
    INITIALIZING,
    READY,
    DEGRADED,
    TERMINATING,
    TERMINATED,
    CLEANUP_PENDING,
    EXPIRED,
    FAILED
}
