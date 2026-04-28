package com.redis.workshop.platform.controlplane.session;

public enum SessionState {
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
