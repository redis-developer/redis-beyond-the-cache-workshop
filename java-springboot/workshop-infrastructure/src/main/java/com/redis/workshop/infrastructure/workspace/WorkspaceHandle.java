package com.redis.workshop.infrastructure.workspace;

import java.nio.file.Path;
import java.util.Objects;

public record WorkspaceHandle(
    WorkspaceMetadata metadata,
    Path workspacePath,
    boolean restoredFromDurableSnapshot
) {

    public WorkspaceHandle {
        metadata = Objects.requireNonNull(metadata, "metadata");
        workspacePath = Objects.requireNonNull(workspacePath, "workspacePath").toAbsolutePath().normalize();
    }
}
