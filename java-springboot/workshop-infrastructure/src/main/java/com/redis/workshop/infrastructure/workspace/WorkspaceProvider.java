package com.redis.workshop.infrastructure.workspace;

import java.io.IOException;

public interface WorkspaceProvider {

    WorkspaceHandle initialize(WorkspaceMetadata metadata) throws IOException;

    WorkspaceSnapshot snapshot(WorkspaceMetadata metadata) throws IOException;

    WorkspaceHandle restore(WorkspaceMetadata metadata) throws IOException;

    WorkspaceCleanupResult cleanup(WorkspaceMetadata metadata, WorkspaceCleanupPolicy policy) throws IOException;

    default WorkspaceCleanupResult cleanup(WorkspaceMetadata metadata) throws IOException {
        return cleanup(metadata, WorkspaceCleanupPolicy.LOCAL_ONLY);
    }
}
