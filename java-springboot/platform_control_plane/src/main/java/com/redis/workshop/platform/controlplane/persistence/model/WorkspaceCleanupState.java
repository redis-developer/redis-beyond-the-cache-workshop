package com.redis.workshop.platform.controlplane.persistence.model;

public enum WorkspaceCleanupState {
    NONE,
    ACTIVE,
    PENDING,
    COMPLETED,
    FAILED
}
