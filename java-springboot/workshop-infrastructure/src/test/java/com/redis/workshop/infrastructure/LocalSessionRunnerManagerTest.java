package com.redis.workshop.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class LocalSessionRunnerManagerTest {

    @TempDir
    Path tempDir;

    @Test
    void startsAndStopsChildProcess() {
        SessionRunnerProperties properties = enabledRunnerProperties();
        LocalSessionRunnerManager manager = new LocalSessionRunnerManager(properties);

        try {
            LocalSessionRunnerManager.SessionRunnerStatus started = manager.startChild();

            assertThat(started.state()).isEqualTo("CHILD_READY");
            assertThat(started.childAlive()).isTrue();
            assertThat(started.backendUri()).isEqualTo("http://127.0.0.1:19191");
            assertThat(manager.isChildReady()).isTrue();

            LocalSessionRunnerManager.SessionRunnerStatus stopped = manager.stopChild();

            assertThat(stopped.state()).isEqualTo("STOPPED");
            assertThat(stopped.childAlive()).isFalse();
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void restartReplacesChildProcessWithoutRebuild() {
        SessionRunnerProperties properties = enabledRunnerProperties();
        LocalSessionRunnerManager manager = new LocalSessionRunnerManager(properties);

        try {
            LocalSessionRunnerManager.SessionRunnerStatus first = manager.startChild();
            LocalSessionRunnerManager.SessionRunnerStatus restarted = manager.restart(false);

            assertThat(first.state()).isEqualTo("CHILD_READY");
            assertThat(restarted.state()).isEqualTo("CHILD_READY");
            assertThat(restarted.generation()).isGreaterThan(first.generation());
            assertThat(restarted.childAlive()).isTrue();
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void restartWithRebuildRunsConfiguredRebuildCommand() throws Exception {
        Path marker = tempDir.resolve("rebuilt.txt");
        SessionRunnerProperties properties = enabledRunnerProperties();
        properties.setChildRebuildCommand("printf rebuilt > " + shellQuote(marker.toString()));
        LocalSessionRunnerManager manager = new LocalSessionRunnerManager(properties);

        try {
            manager.startChild();
            LocalSessionRunnerManager.SessionRunnerStatus restarted = manager.restart(true);

            assertThat(restarted.state()).isEqualTo("CHILD_READY");
            assertThat(restarted.childAlive()).isTrue();
            assertThat(Files.readString(marker)).isEqualTo("rebuilt");
            assertThat(restarted.recentLogs()).contains("rebuild started", "rebuild completed");
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void requestRestartWithRebuildReturnsWhileRebuildIsRunning() throws Exception {
        Path gate = tempDir.resolve("continue-rebuild");
        Path marker = tempDir.resolve("rebuilt.txt");
        SessionRunnerProperties properties = enabledRunnerProperties();
        properties.setChildRebuildCommand(
            "while [ ! -f " + shellQuote(gate.toString()) + " ]; do sleep 0.1; done; "
                + "printf rebuilt > " + shellQuote(marker.toString())
        );
        LocalSessionRunnerManager manager = new LocalSessionRunnerManager(properties);

        try {
            manager.startChild();
            LocalSessionRunnerManager.SessionRunnerStatus requested = manager.requestRestart(true);

            assertThat(requested.state()).isEqualTo("RESTARTING");
            assertThat(manager.status().state()).isEqualTo("RESTARTING");

            Files.writeString(gate, "continue");
            LocalSessionRunnerManager.SessionRunnerStatus ready = waitForState(manager, "CHILD_READY");

            assertThat(ready.childAlive()).isTrue();
            assertThat(Files.readString(marker)).isEqualTo("rebuilt");
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void restartWithRebuildFailsWhenRebuildCommandFails() {
        SessionRunnerProperties properties = enabledRunnerProperties();
        properties.setChildRebuildCommand("exit 7");
        LocalSessionRunnerManager manager = new LocalSessionRunnerManager(properties);

        try {
            manager.startChild();
            LocalSessionRunnerManager.SessionRunnerStatus restarted = manager.restart(true);

            assertThat(restarted.state()).isEqualTo("CHILD_FAILED");
            assertThat(restarted.childAlive()).isFalse();
            assertThat(restarted.lastError()).isEqualTo("Child rebuild command exited with code 7");
        } finally {
            manager.shutdown();
        }
    }

    @Test
    void reportsFailureWhenChildCommandIsMissing() {
        SessionRunnerProperties properties = new SessionRunnerProperties();
        properties.setEnabled(true);
        LocalSessionRunnerManager manager = new LocalSessionRunnerManager(properties);

        LocalSessionRunnerManager.SessionRunnerStatus status = manager.startChild();

        assertThat(status.state()).isEqualTo("CHILD_FAILED");
        assertThat(status.lastError()).isEqualTo("Child command is not configured");
        assertThat(status.childAlive()).isFalse();
    }

    @Test
    void reportsConfiguredLocalRedisDependencies() {
        SessionRunnerProperties properties = enabledRunnerProperties();
        properties.setRedisCommand("redis-server --port 6379");
        properties.setRedisHealthCommand("redis-cli -p 6379 ping");
        properties.setRedisInsightCommand("redisinsight");
        properties.setRedisInsightHealthPath("/api/health");
        LocalSessionRunnerManager manager = new LocalSessionRunnerManager(properties);

        LocalSessionRunnerManager.SessionRunnerStatus status = manager.status();

        assertThat(status.dependencies())
            .extracting(LocalSessionRunnerManager.SessionRunnerDependencyStatus::name)
            .containsExactly("redis", "redis-insight");
        assertThat(status.dependencies())
            .extracting(LocalSessionRunnerManager.SessionRunnerDependencyStatus::mode)
            .containsExactly("local-process", "local-process");
        assertThat(status.dependencies())
            .extracting(LocalSessionRunnerManager.SessionRunnerDependencyStatus::configured)
            .containsExactly(true, true);
        assertThat(status.dependencies())
            .extracting(LocalSessionRunnerManager.SessionRunnerDependencyStatus::running)
            .containsExactly(false, false);
        assertThat(status.dependencies())
            .extracting(LocalSessionRunnerManager.SessionRunnerDependencyStatus::healthProbe)
            .containsExactly("redis-cli -p 6379 ping", "/api/health");
    }

    @Test
    void startsLocalRedisDependenciesWithChildProcess() {
        SessionRunnerProperties properties = enabledRunnerProperties();
        properties.setRedisCommand("while true; do sleep 1; done");
        properties.setRedisInsightCommand("while true; do sleep 1; done");
        LocalSessionRunnerManager manager = new LocalSessionRunnerManager(properties);

        try {
            LocalSessionRunnerManager.SessionRunnerStatus status = manager.startChild();

            assertThat(status.state()).isEqualTo("CHILD_READY");
            assertThat(status.dependencies())
                .extracting(LocalSessionRunnerManager.SessionRunnerDependencyStatus::running)
                .containsExactly(true, true);
        } finally {
            manager.shutdown();
        }
    }

    private SessionRunnerProperties enabledRunnerProperties() {
        SessionRunnerProperties properties = new SessionRunnerProperties();
        properties.setEnabled(true);
        properties.setAutoStart(false);
        properties.setChildPort(19191);
        properties.setChildWorkingDirectory(tempDir.toString());
        properties.setChildCommand("while true; do sleep 1; done");
        return properties;
    }

    private LocalSessionRunnerManager.SessionRunnerStatus waitForState(
        LocalSessionRunnerManager manager,
        String expectedState
    ) throws InterruptedException {
        long deadline = System.nanoTime() + java.util.concurrent.TimeUnit.SECONDS.toNanos(5);
        LocalSessionRunnerManager.SessionRunnerStatus status = manager.status();
        while (System.nanoTime() < deadline) {
            status = manager.status();
            if (expectedState.equals(status.state())) {
                return status;
            }
            Thread.sleep(50);
        }
        return status;
    }

    private String shellQuote(String value) {
        return "'" + value.replace("'", "'\\''") + "'";
    }
}
