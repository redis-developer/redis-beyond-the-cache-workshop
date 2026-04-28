package com.redis.workshop.infrastructure.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.time.Instant;
import java.util.Objects;

public class LocalFilesystemWorkspaceProvider implements WorkspaceProvider {

    private final DurableWorkspaceStore durableWorkspaceStore;

    public LocalFilesystemWorkspaceProvider(DurableWorkspaceStore durableWorkspaceStore) {
        this.durableWorkspaceStore = Objects.requireNonNull(durableWorkspaceStore, "durableWorkspaceStore");
    }

    public static LocalFilesystemWorkspaceProvider withLocalDurableRoot(java.nio.file.Path durableRoot) {
        return new LocalFilesystemWorkspaceProvider(new LocalFilesystemDurableWorkspaceStore(durableRoot));
    }

    @Override
    public WorkspaceHandle initialize(WorkspaceMetadata metadata) throws IOException {
        return materialize(metadata);
    }

    @Override
    public WorkspaceSnapshot snapshot(WorkspaceMetadata metadata) throws IOException {
        requireSafeWorkspace(metadata);
        if (!Files.isDirectory(metadata.localWorkspacePath())) {
            throw new IOException("Workspace does not exist: " + metadata.localWorkspacePath());
        }

        int fileCount = durableWorkspaceStore.saveSnapshot(metadata, metadata.localWorkspacePath());
        return new WorkspaceSnapshot(metadata, metadata.durablePrefix(), Instant.now(), fileCount);
    }

    @Override
    public WorkspaceHandle restore(WorkspaceMetadata metadata) throws IOException {
        return materialize(metadata);
    }

    @Override
    public WorkspaceCleanupResult cleanup(WorkspaceMetadata metadata, WorkspaceCleanupPolicy policy) throws IOException {
        requireSafeWorkspace(metadata);
        WorkspaceCleanupPolicy resolvedPolicy = policy == null ? WorkspaceCleanupPolicy.LOCAL_ONLY : policy;

        boolean localDeleted = WorkspaceFileOperations.deleteRecursively(metadata.localWorkspacePath());
        boolean durableDeleted = false;
        if (resolvedPolicy.deleteDurableSnapshot()) {
            durableDeleted = durableWorkspaceStore.deleteSnapshot(metadata);
        }

        return new WorkspaceCleanupResult(
            metadata.sessionId(),
            metadata.localWorkspacePath(),
            metadata.durablePrefix(),
            localDeleted,
            durableDeleted
        );
    }

    private WorkspaceHandle materialize(WorkspaceMetadata metadata) throws IOException {
        requireSafeWorkspace(metadata);
        if (!Files.isDirectory(metadata.baselinePath())) {
            throw new IOException("Baseline does not exist: " + metadata.baselinePath());
        }

        boolean restoredFromDurable = durableWorkspaceStore.hasSnapshot(metadata);
        WorkspaceFileOperations.deleteRecursively(metadata.localWorkspacePath());
        if (restoredFromDurable) {
            durableWorkspaceStore.restoreSnapshot(metadata, metadata.localWorkspacePath());
        } else {
            WorkspaceFileOperations.copyDirectory(metadata.baselinePath(), metadata.localWorkspacePath());
        }
        return new WorkspaceHandle(metadata, metadata.localWorkspacePath(), restoredFromDurable);
    }

    private void requireSafeWorkspace(WorkspaceMetadata metadata) {
        Objects.requireNonNull(metadata, "metadata");
        WorkspaceFileOperations.assertNotFilesystemRoot(metadata.localWorkspacePath(), "localWorkspacePath");
        WorkspaceFileOperations.assertNoPathOverlap(
            metadata.baselinePath(),
            metadata.localWorkspacePath(),
            "baselinePath and localWorkspacePath must not overlap"
        );
    }
}
