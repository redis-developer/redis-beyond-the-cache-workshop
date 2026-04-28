package com.redis.workshop.infrastructure.workspace;

import java.time.Instant;
import java.util.Objects;

public record WorkspaceSnapshot(
    WorkspaceMetadata metadata,
    String durablePrefix,
    Instant snapshotAt,
    int fileCount
) {

    public WorkspaceSnapshot {
        metadata = Objects.requireNonNull(metadata, "metadata");
        durablePrefix = Objects.requireNonNull(durablePrefix, "durablePrefix");
        snapshotAt = Objects.requireNonNull(snapshotAt, "snapshotAt");
        if (fileCount < 0) {
            throw new IllegalArgumentException("fileCount must not be negative");
        }
    }
}
