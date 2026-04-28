package com.redis.workshop.platform.controlplane.persistence.model;

public enum AuditActionType {
    SESSION_CREATED,
    SESSION_RESTARTED_BY_USER,
    SESSION_RESTARTED_BY_ADMIN,
    SESSION_TERMINATED_BY_USER,
    SESSION_TERMINATED_BY_ADMIN,
    ADMIN_OBSERVABILITY_READ
}
