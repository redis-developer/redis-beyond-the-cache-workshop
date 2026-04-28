CREATE TABLE platform_sessions (
    session_id VARCHAR(64) PRIMARY KEY,
    owner_user_id VARCHAR(128) NOT NULL,
    team_id VARCHAR(128),
    workshop_id VARCHAR(128) NOT NULL,
    release_version VARCHAR(64) NOT NULL,
    session_mode VARCHAR(32) NOT NULL,
    session_state VARCHAR(32) NOT NULL,
    quota_class VARCHAR(64) NOT NULL,
    resource_class VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_activity_at TIMESTAMP WITH TIME ZONE NOT NULL,
    cleanup_completed_at TIMESTAMP WITH TIME ZONE,
    route_type VARCHAR(32) NOT NULL,
    public_entry_url VARCHAR(1024),
    internal_runtime_ref VARCHAR(255),
    workspace_policy VARCHAR(32) NOT NULL,
    failure_code VARCHAR(64),
    failure_message TEXT,
    termination_reason VARCHAR(255),
    runtime_status VARCHAR(64)
);

CREATE INDEX idx_platform_sessions_owner_user_id
    ON platform_sessions (owner_user_id);

CREATE INDEX idx_platform_sessions_workshop_state
    ON platform_sessions (workshop_id, session_state);

CREATE INDEX idx_platform_sessions_expires_at
    ON platform_sessions (expires_at);

CREATE TABLE audit_events (
    event_id UUID PRIMARY KEY,
    actor_id VARCHAR(128) NOT NULL,
    actor_type VARCHAR(32) NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    target_resource_type VARCHAR(64) NOT NULL,
    target_resource_id VARCHAR(128) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    result VARCHAR(32) NOT NULL,
    reason VARCHAR(255),
    request_id VARCHAR(128),
    session_id VARCHAR(64),
    workshop_id VARCHAR(128),
    release_version VARCHAR(64),
    runtime_ref VARCHAR(255)
);

CREATE INDEX idx_audit_events_created_at
    ON audit_events (created_at);

CREATE INDEX idx_audit_events_session_id
    ON audit_events (session_id);

CREATE INDEX idx_audit_events_target_resource
    ON audit_events (target_resource_type, target_resource_id);
