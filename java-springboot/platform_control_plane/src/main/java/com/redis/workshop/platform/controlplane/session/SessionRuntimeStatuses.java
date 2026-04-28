package com.redis.workshop.platform.controlplane.session;

final class SessionRuntimeStatuses {

    static final String EXECUTION_LAUNCH_REQUESTED = "execution_launch_requested";
    static final String PROVISIONING = "provisioning";
    static final String INITIALIZING = "initializing";
    static final String SESSION_RUNNER_INITIALIZING = "session_runner_initializing";
    static final String RESTART_INITIALIZING = "restart_initializing";
    static final String RESTART_REBUILD_INITIALIZING = "restart_rebuild_initializing";
    static final String RESTART_IN_PROGRESS = "restart_in_progress";
    static final String RESTART_REBUILD_IN_PROGRESS = "restart_rebuild_in_progress";
    static final String READY = "ready";
    static final String FAILED = "failed";
    static final String RESTART_FAILED = "restart_failed";
    static final String TERMINATED = "terminated";
    static final String TERMINATION_RECONCILED = "termination_reconciled";
    static final String TERMINATION_RETRY_FAILED = "termination_retry_failed";

    private SessionRuntimeStatuses() {
    }
}
