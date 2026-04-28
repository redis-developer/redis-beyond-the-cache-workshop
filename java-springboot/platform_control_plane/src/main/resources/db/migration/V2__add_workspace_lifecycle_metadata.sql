ALTER TABLE platform_sessions
    ADD COLUMN expired_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE platform_sessions
    ADD COLUMN termination_requested_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE platform_sessions
    ADD COLUMN terminated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE platform_sessions
    ADD COLUMN workspace_ref VARCHAR(255);

ALTER TABLE platform_sessions
    ADD COLUMN workspace_cleanup_state VARCHAR(32) NOT NULL DEFAULT 'NONE';

UPDATE platform_sessions
SET workspace_ref = CASE
        WHEN workspace_policy = 'EPHEMERAL' THEN 'session-workspace:' || session_id
        ELSE workspace_ref
    END,
    workspace_cleanup_state = CASE
        WHEN workspace_policy = 'EPHEMERAL' AND session_state IN ('TERMINATED') AND cleanup_completed_at IS NOT NULL THEN 'COMPLETED'
        WHEN workspace_policy = 'EPHEMERAL' AND session_state IN ('TERMINATING', 'EXPIRED', 'FAILED', 'CLEANUP_PENDING') THEN 'PENDING'
        WHEN workspace_policy = 'EPHEMERAL' THEN 'ACTIVE'
        ELSE 'NONE'
    END;

CREATE INDEX idx_platform_sessions_cleanup_state
    ON platform_sessions (workspace_cleanup_state);

CREATE INDEX idx_platform_sessions_runtime_ref_state
    ON platform_sessions (session_state, internal_runtime_ref);
