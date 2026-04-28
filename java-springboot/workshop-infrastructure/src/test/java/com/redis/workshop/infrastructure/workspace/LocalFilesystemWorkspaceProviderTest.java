package com.redis.workshop.infrastructure.workspace;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFilesystemWorkspaceProviderTest {

    @TempDir
    Path tempDir;

    @Test
    void initializeCopiesBaselineToLocalWorkspace() throws IOException {
        Path baseline = baselineSource();
        Path workspace = tempDir.resolve("workspaces/session-1");
        LocalFilesystemWorkspaceProvider provider = provider();

        WorkspaceHandle handle = provider.initialize(metadata("session-1", baseline, workspace));

        assertThat(handle.workspacePath()).isEqualTo(workspace.toAbsolutePath().normalize());
        assertThat(handle.restoredFromDurableSnapshot()).isFalse();
        assertThat(Files.readString(workspace.resolve("src/main/resources/application.properties")))
            .isEqualTo("spring.application.name=baseline\n");
        assertThat(Files.readString(baseline.resolve("src/main/resources/application.properties")))
            .isEqualTo("spring.application.name=baseline\n");
    }

    @Test
    void snapshotsAndRestoresChangedWorkspaceFiles() throws IOException {
        Path baseline = baselineSource();
        Path workspace = tempDir.resolve("workspaces/session-2");
        LocalFilesystemWorkspaceProvider provider = provider();
        WorkspaceMetadata metadata = metadata("session-2", baseline, workspace);

        provider.initialize(metadata);
        Files.writeString(
            workspace.resolve("src/main/resources/application.properties"),
            "spring.application.name=changed\n"
        );
        Files.createDirectories(workspace.resolve("src/main/java/demo"));
        Files.writeString(workspace.resolve("src/main/java/demo/NewFile.java"), "class NewFile {}\n");

        WorkspaceSnapshot snapshot = provider.snapshot(metadata);
        assertThat(snapshot.fileCount()).isEqualTo(2);

        provider.cleanup(metadata);
        assertThat(Files.exists(workspace)).isFalse();

        WorkspaceHandle restored = provider.restore(metadata);

        assertThat(restored.restoredFromDurableSnapshot()).isTrue();
        assertThat(Files.readString(workspace.resolve("src/main/resources/application.properties")))
            .isEqualTo("spring.application.name=changed\n");
        assertThat(Files.readString(workspace.resolve("src/main/java/demo/NewFile.java")))
            .isEqualTo("class NewFile {}\n");
    }

    @Test
    void cleanupDeletesLocalWorkspaceWithoutTouchingBaseline() throws IOException {
        Path baseline = baselineSource();
        Path workspace = tempDir.resolve("workspaces/session-3");
        LocalFilesystemWorkspaceProvider provider = provider();
        WorkspaceMetadata metadata = metadata("session-3", baseline, workspace);

        provider.initialize(metadata);
        WorkspaceCleanupResult result = provider.cleanup(metadata);

        assertThat(result.localWorkspaceDeleted()).isTrue();
        assertThat(result.durableSnapshotDeleted()).isFalse();
        assertThat(Files.exists(workspace)).isFalse();
        assertThat(Files.readString(baseline.resolve("src/main/resources/application.properties")))
            .isEqualTo("spring.application.name=baseline\n");
    }

    @Test
    void cleanupRefusesWorkspaceThatOverlapsBaseline() throws IOException {
        Path baseline = baselineSource();
        LocalFilesystemWorkspaceProvider provider = provider();
        WorkspaceMetadata metadata = metadata("session-4", baseline, baseline);

        assertThatThrownBy(() -> provider.cleanup(metadata))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("must not overlap");
        assertThat(Files.readString(baseline.resolve("src/main/resources/application.properties")))
            .isEqualTo("spring.application.name=baseline\n");
    }

    @Test
    void cleanupCanRemoveDurableSnapshotWhenRetentionPolicyRequiresIt() throws IOException {
        Path baseline = baselineSource();
        Path workspace = tempDir.resolve("workspaces/session-5");
        LocalFilesystemWorkspaceProvider provider = provider();
        WorkspaceMetadata metadata = metadata("session-5", baseline, workspace);

        provider.initialize(metadata);
        provider.snapshot(metadata);

        WorkspaceCleanupResult result = provider.cleanup(metadata, WorkspaceCleanupPolicy.LOCAL_AND_DURABLE);

        assertThat(result.localWorkspaceDeleted()).isTrue();
        assertThat(result.durableSnapshotDeleted()).isTrue();
        assertThat(provider.restore(metadata).restoredFromDurableSnapshot()).isFalse();
    }

    private LocalFilesystemWorkspaceProvider provider() {
        return LocalFilesystemWorkspaceProvider.withLocalDurableRoot(tempDir.resolve("durable"));
    }

    private WorkspaceMetadata metadata(String sessionId, Path baseline, Path workspace) {
        return new WorkspaceMetadata(sessionId, baseline, workspace, WorkspaceMetadata.defaultDurablePrefix(sessionId));
    }

    private Path baselineSource() throws IOException {
        Path baseline = tempDir.resolve("baseline-" + java.util.UUID.randomUUID());
        Files.createDirectories(baseline.resolve("src/main/resources"));
        Files.writeString(
            baseline.resolve("src/main/resources/application.properties"),
            "spring.application.name=baseline\n"
        );
        return baseline;
    }
}
