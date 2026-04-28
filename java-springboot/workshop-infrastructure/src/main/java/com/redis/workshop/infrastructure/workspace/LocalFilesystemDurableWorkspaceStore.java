package com.redis.workshop.infrastructure.workspace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Objects;

public class LocalFilesystemDurableWorkspaceStore implements DurableWorkspaceStore {

    private final Path durableRoot;

    public LocalFilesystemDurableWorkspaceStore(Path durableRoot) {
        this.durableRoot = Objects.requireNonNull(durableRoot, "durableRoot").toAbsolutePath().normalize();
        WorkspaceFileOperations.assertNotFilesystemRoot(this.durableRoot, "durableRoot");
    }

    @Override
    public boolean hasSnapshot(WorkspaceMetadata metadata) {
        return Files.isDirectory(snapshotPath(metadata), LinkOption.NOFOLLOW_LINKS);
    }

    @Override
    public int saveSnapshot(WorkspaceMetadata metadata, Path sourceDirectory) throws IOException {
        Path source = Objects.requireNonNull(sourceDirectory, "sourceDirectory").toAbsolutePath().normalize();
        Path target = snapshotPath(metadata);
        WorkspaceFileOperations.assertNoPathOverlap(
            source,
            target,
            "sourceDirectory and durable snapshot path must not overlap"
        );
        WorkspaceFileOperations.deleteRecursively(target);
        return WorkspaceFileOperations.copyDirectory(source, target);
    }

    @Override
    public int restoreSnapshot(WorkspaceMetadata metadata, Path targetDirectory) throws IOException {
        Path source = snapshotPath(metadata);
        if (!Files.isDirectory(source, LinkOption.NOFOLLOW_LINKS)) {
            return 0;
        }

        Path target = Objects.requireNonNull(targetDirectory, "targetDirectory").toAbsolutePath().normalize();
        WorkspaceFileOperations.assertNoPathOverlap(
            source,
            target,
            "durable snapshot path and targetDirectory must not overlap"
        );
        WorkspaceFileOperations.deleteRecursively(target);
        return WorkspaceFileOperations.copyDirectory(source, target);
    }

    @Override
    public boolean deleteSnapshot(WorkspaceMetadata metadata) throws IOException {
        return WorkspaceFileOperations.deleteRecursively(snapshotPath(metadata));
    }

    Path snapshotPath(WorkspaceMetadata metadata) {
        Objects.requireNonNull(metadata, "metadata");
        Path path = durableRoot.resolve(metadata.durablePrefix()).normalize();
        if (!path.startsWith(durableRoot)) {
            throw new IllegalArgumentException("durablePrefix must stay under durableRoot");
        }
        return path;
    }
}
