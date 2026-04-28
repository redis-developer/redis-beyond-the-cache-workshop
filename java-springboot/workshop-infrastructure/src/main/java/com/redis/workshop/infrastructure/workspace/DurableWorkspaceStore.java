package com.redis.workshop.infrastructure.workspace;

import java.io.IOException;
import java.nio.file.Path;

public interface DurableWorkspaceStore {

    boolean hasSnapshot(WorkspaceMetadata metadata) throws IOException;

    int saveSnapshot(WorkspaceMetadata metadata, Path sourceDirectory) throws IOException;

    int restoreSnapshot(WorkspaceMetadata metadata, Path targetDirectory) throws IOException;

    boolean deleteSnapshot(WorkspaceMetadata metadata) throws IOException;
}
