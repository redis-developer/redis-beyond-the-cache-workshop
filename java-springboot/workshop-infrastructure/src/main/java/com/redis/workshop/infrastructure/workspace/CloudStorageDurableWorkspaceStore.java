package com.redis.workshop.infrastructure.workspace;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class CloudStorageDurableWorkspaceStore implements DurableWorkspaceStore {

    private final String bucket;
    private final CloudStorageWorkspaceClient client;

    public CloudStorageDurableWorkspaceStore(String bucket, CloudStorageWorkspaceClient client) {
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("bucket must not be blank");
        }
        this.bucket = bucket.trim();
        this.client = Objects.requireNonNull(client, "client");
    }

    @Override
    public boolean hasSnapshot(WorkspaceMetadata metadata) throws IOException {
        return client.prefixExists(bucket, metadata.durablePrefix());
    }

    @Override
    public int saveSnapshot(WorkspaceMetadata metadata, Path sourceDirectory) throws IOException {
        return client.uploadDirectory(sourceDirectory, bucket, metadata.durablePrefix());
    }

    @Override
    public int restoreSnapshot(WorkspaceMetadata metadata, Path targetDirectory) throws IOException {
        return client.downloadPrefix(bucket, metadata.durablePrefix(), targetDirectory);
    }

    @Override
    public boolean deleteSnapshot(WorkspaceMetadata metadata) throws IOException {
        return client.deletePrefix(bucket, metadata.durablePrefix());
    }
}
