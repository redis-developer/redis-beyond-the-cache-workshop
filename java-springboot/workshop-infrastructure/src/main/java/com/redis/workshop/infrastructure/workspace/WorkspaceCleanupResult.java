package com.redis.workshop.infrastructure.workspace;

import java.nio.file.Path;
import java.util.Objects;

public record WorkspaceCleanupResult(
    String sessionId,
    Path localWorkspacePath,
    String durablePrefix,
    boolean localWorkspaceDeleted,
    boolean durableSnapshotDeleted
) {

    public WorkspaceCleanupResult {
        sessionId = Objects.requireNonNull(sessionId, "sessionId");
        localWorkspacePath = Objects.requireNonNull(localWorkspacePath, "localWorkspacePath").toAbsolutePath().normalize();
        durablePrefix = Objects.requireNonNull(durablePrefix, "durablePrefix");
    }
}
