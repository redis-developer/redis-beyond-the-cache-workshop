package com.redis.workshop.infrastructure.workspace;

public enum WorkspaceCleanupPolicy {
    LOCAL_ONLY(false),
    LOCAL_AND_DURABLE(true);

    private final boolean deleteDurableSnapshot;

    WorkspaceCleanupPolicy(boolean deleteDurableSnapshot) {
        this.deleteDurableSnapshot = deleteDurableSnapshot;
    }

    public boolean deleteDurableSnapshot() {
        return deleteDurableSnapshot;
    }
}
