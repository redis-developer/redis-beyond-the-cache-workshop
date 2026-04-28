package com.redis.workshop.infrastructure.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CloudStorageDurableWorkspaceStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void delegatesCloudStorageOperationsThroughClientSurface() throws IOException {
        RecordingCloudStorageClient client = new RecordingCloudStorageClient();
        CloudStorageDurableWorkspaceStore store = new CloudStorageDurableWorkspaceStore("runner-workspaces", client);
        WorkspaceMetadata metadata = new WorkspaceMetadata(
            "session-1",
            tempDir.resolve("baseline"),
            tempDir.resolve("workspace"),
            "sessions/session-1/workspace"
        );

        assertThat(store.hasSnapshot(metadata)).isTrue();
        assertThat(store.saveSnapshot(metadata, metadata.localWorkspacePath())).isEqualTo(3);
        assertThat(store.restoreSnapshot(metadata, metadata.localWorkspacePath())).isEqualTo(3);
        assertThat(store.deleteSnapshot(metadata)).isTrue();

        assertThat(client.operations()).containsExactly(
            "exists runner-workspaces sessions/session-1/workspace",
            "upload runner-workspaces sessions/session-1/workspace",
            "download runner-workspaces sessions/session-1/workspace",
            "delete runner-workspaces sessions/session-1/workspace"
        );
    }

    private static class RecordingCloudStorageClient implements CloudStorageWorkspaceClient {
        private final List<String> operations = new ArrayList<>();

        @Override
        public boolean prefixExists(String bucket, String prefix) {
            operations.add("exists " + bucket + " " + prefix);
            return true;
        }

        @Override
        public int uploadDirectory(Path sourceDirectory, String bucket, String prefix) {
            operations.add("upload " + bucket + " " + prefix);
            return 3;
        }

        @Override
        public int downloadPrefix(String bucket, String prefix, Path targetDirectory) {
            operations.add("download " + bucket + " " + prefix);
            return 3;
        }

        @Override
        public boolean deletePrefix(String bucket, String prefix) {
            operations.add("delete " + bucket + " " + prefix);
            return true;
        }

        private List<String> operations() {
            return operations;
        }
    }
}
