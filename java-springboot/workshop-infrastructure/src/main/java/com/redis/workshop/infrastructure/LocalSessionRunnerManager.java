package com.redis.workshop.infrastructure;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class LocalSessionRunnerManager {

    enum RunnerState {
        DISABLED,
        STOPPED,
        CHILD_STARTING,
        CHILD_READY,
        CHILD_FAILED,
        RESTARTING,
        STOPPING
    }

    private static final int MAX_RECENT_LOG_LINES = 80;
    private static final Logger logger = LoggerFactory.getLogger(LocalSessionRunnerManager.class);

    private final SessionRunnerProperties properties;
    private final HttpClient httpClient;
    private final ExecutorService logExecutor;
    private final Object lifecycleLock = new Object();
    private final ArrayDeque<String> recentLogs = new ArrayDeque<>();

    private RunnerState state = RunnerState.STOPPED;
    private Process childProcess;
    private Process redisProcess;
    private Process redisInsightProcess;
    private long generation;
    private Integer lastExitCode;
    private String lastError;
    private Instant lastStartedAt;

    @Autowired
    public LocalSessionRunnerManager(SessionRunnerProperties properties) {
        this(properties, HttpClient.newHttpClient(), Executors.newCachedThreadPool());
    }

    LocalSessionRunnerManager(SessionRunnerProperties properties, HttpClient httpClient, ExecutorService logExecutor) {
        this.properties = properties;
        this.httpClient = httpClient;
        this.logExecutor = logExecutor;
        this.state = properties.isEnabled() ? RunnerState.STOPPED : RunnerState.DISABLED;
    }

    @PostConstruct
    void startOnBoot() {
        if (properties.isEnabled() && properties.isAutoStart()) {
            startChild();
        }
    }

    @PreDestroy
    void shutdown() {
        synchronized (lifecycleLock) {
            stopChildLocked();
            stopDependencyProcessesLocked();
        }
        logExecutor.shutdownNow();
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    public boolean isChildReady() {
        synchronized (lifecycleLock) {
            return state == RunnerState.CHILD_READY && childProcess != null && childProcess.isAlive();
        }
    }

    public SessionRunnerStatus status() {
        synchronized (lifecycleLock) {
            return statusLocked();
        }
    }

    public SessionRunnerStatus restart(boolean rebuild) {
        synchronized (lifecycleLock) {
            if (!properties.isEnabled()) {
                state = RunnerState.DISABLED;
                lastError = "Session runner is disabled";
                return statusLocked();
            }
            state = RunnerState.RESTARTING;
            lastError = null;
            stopChildLocked();
            if (rebuild && !rebuildChildLocked()) {
                state = RunnerState.CHILD_FAILED;
                return statusLocked();
            }
            startChildLocked();
            return statusLocked();
        }
    }

    public SessionRunnerStatus startChild() {
        synchronized (lifecycleLock) {
            if (!properties.isEnabled()) {
                state = RunnerState.DISABLED;
                lastError = "Session runner is disabled";
                return statusLocked();
            }
            if (childProcess != null && childProcess.isAlive()) {
                return statusLocked();
            }
            startChildLocked();
            return statusLocked();
        }
    }

    public SessionRunnerStatus stopChild() {
        synchronized (lifecycleLock) {
            stopChildLocked();
            return statusLocked();
        }
    }

    private void startChildLocked() {
        lastError = null;
        lastExitCode = null;
        if (!properties.hasRunnableChildCommand()) {
            state = RunnerState.CHILD_FAILED;
            lastError = "Child command is not configured";
            return;
        }

        startDependencyProcessesLocked();

        ProcessBuilder processBuilder = new ProcessBuilder(
            properties.resolveShellExecutable(),
            properties.resolveShellArgument(),
            properties.resolveChildCommand()
        );
        properties.resolveChildWorkingDirectory().ifPresent(path -> processBuilder.directory(path.toFile()));
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().putAll(properties.getEnvironment());
        processBuilder.environment().put("SERVER_PORT", String.valueOf(properties.resolveChildPort()));
        processBuilder.environment().put("WORKSHOP_CHILD_PORT", String.valueOf(properties.resolveChildPort()));
        if (properties.resolveLocalRedisUri().isPresent()) {
            processBuilder.environment().put("SPRING_DATA_REDIS_HOST", "127.0.0.1");
            processBuilder.environment().put("SPRING_DATA_REDIS_PORT", String.valueOf(properties.resolveLocalRedisPort()));
            processBuilder.environment().put("SPRING_REDIS_HOST", "127.0.0.1");
            processBuilder.environment().put("SPRING_REDIS_PORT", String.valueOf(properties.resolveLocalRedisPort()));
        }

        try {
            state = RunnerState.CHILD_STARTING;
            childProcess = processBuilder.start();
            generation++;
            lastStartedAt = Instant.now();
            pumpLogs(childProcess, generation);
            waitForReadinessLocked(childProcess);
        } catch (IOException exception) {
            state = RunnerState.CHILD_FAILED;
            lastError = exception.getMessage();
        }
    }

    private void startDependencyProcessesLocked() {
        if (properties.hasLocalRedisCommand() && !isAlive(redisProcess)) {
            redisProcess = startDependencyProcess("redis", properties.resolveLocalRedisCommand());
        }
        if (properties.hasLocalRedisInsightCommand() && !isAlive(redisInsightProcess)) {
            redisInsightProcess = startDependencyProcess("redis-insight", properties.resolveLocalRedisInsightCommand());
        }
    }

    private Process startDependencyProcess(String name, String command) {
        ProcessBuilder processBuilder = new ProcessBuilder(
            properties.resolveShellExecutable(),
            properties.resolveShellArgument(),
            command
        );
        properties.resolveChildWorkingDirectory().ifPresent(path -> processBuilder.directory(path.toFile()));
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().putAll(properties.getEnvironment());
        processBuilder.environment().put("WORKSHOP_LOCAL_REDIS_PORT", String.valueOf(properties.resolveLocalRedisPort()));
        processBuilder.environment().put("WORKSHOP_REDIS_INSIGHT_PORT", String.valueOf(properties.resolveLocalRedisInsightPort()));

        try {
            Process process = processBuilder.start();
            pumpLogs(process, name, generation);
            return process;
        } catch (IOException exception) {
            appendLog(name + " start failed: " + exception.getMessage());
            lastError = name + " start failed: " + exception.getMessage();
            return null;
        }
    }

    private boolean rebuildChildLocked() {
        if (!properties.hasChildRebuildCommand()) {
            lastError = "Child rebuild command is not configured";
            appendLog(lastError);
            return false;
        }

        properties.resolveRebuiltChildJar().ifPresent(path -> {
            try {
                java.nio.file.Files.deleteIfExists(path);
            } catch (IOException exception) {
                appendLog("Unable to delete previous rebuilt child jar: " + exception.getMessage());
            }
        });

        ProcessBuilder processBuilder = new ProcessBuilder(
            properties.resolveShellExecutable(),
            properties.resolveShellArgument(),
            properties.resolveChildRebuildCommand()
        );
        properties.resolveChildWorkingDirectory().ifPresent(path -> processBuilder.directory(path.toFile()));
        processBuilder.redirectErrorStream(true);
        processBuilder.environment().putAll(properties.getEnvironment());
        processBuilder.environment().put("SERVER_PORT", String.valueOf(properties.resolveChildPort()));
        processBuilder.environment().put("WORKSHOP_CHILD_PORT", String.valueOf(properties.resolveChildPort()));

        java.nio.file.Path rebuildLog = null;
        try {
            appendLog("rebuild started");
            rebuildLog = java.nio.file.Files.createTempFile("workshop-child-rebuild-", ".log");
            processBuilder.redirectOutput(ProcessBuilder.Redirect.appendTo(rebuildLog.toFile()));
            Process rebuildProcess = processBuilder.start();
            Duration timeout = safeDuration(properties.getRebuildTimeout(), Duration.ofMinutes(3));
            boolean completed = rebuildProcess.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!completed) {
                rebuildProcess.destroyForcibly();
                appendRebuildLog(rebuildLog);
                lastError = "Child rebuild command timed out";
                appendLog(lastError);
                return false;
            }
            appendRebuildLog(rebuildLog);
            if (rebuildProcess.exitValue() != 0) {
                lastError = "Child rebuild command exited with code " + rebuildProcess.exitValue();
                appendLog(lastError);
                return false;
            }
            appendLog("rebuild completed");
            return true;
        } catch (IOException exception) {
            lastError = "Child rebuild command failed: " + exception.getMessage();
            appendLog(lastError);
            return false;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            lastError = "Interrupted while rebuilding child process";
            appendLog(lastError);
            return false;
        } finally {
            if (rebuildLog != null) {
                try {
                    java.nio.file.Files.deleteIfExists(rebuildLog);
                } catch (IOException ignored) {
                    // Best effort cleanup of the temporary rebuild log.
                }
            }
        }
    }

    private void appendRebuildLog(java.nio.file.Path rebuildLog) {
        try {
            for (String line : java.nio.file.Files.readAllLines(rebuildLog, StandardCharsets.UTF_8)) {
                appendLog("rebuild: " + line);
            }
        } catch (IOException exception) {
            appendLog("rebuild log capture failed: " + exception.getMessage());
        }
    }

    private void waitForReadinessLocked(Process process) {
        if (!process.isAlive()) {
            state = RunnerState.CHILD_FAILED;
            lastExitCode = process.exitValue();
            lastError = "Child process exited before becoming ready";
            return;
        }
        if (!properties.hasChildHealthPath()) {
            state = RunnerState.CHILD_READY;
            return;
        }

        URI healthUri = URI.create(
            "http://127.0.0.1:" + properties.resolveChildPort() + properties.normalizedChildHealthPath()
        );
        Instant deadline = Instant.now().plus(safeDuration(properties.getStartupTimeout(), Duration.ofSeconds(30)));
        while (Instant.now().isBefore(deadline)) {
            if (!process.isAlive()) {
                state = RunnerState.CHILD_FAILED;
                lastExitCode = process.exitValue();
                lastError = "Child process exited before health check passed";
                return;
            }
            try {
                HttpRequest request = HttpRequest.newBuilder(healthUri)
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build();
                HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() < 500) {
                    state = RunnerState.CHILD_READY;
                    return;
                }
            } catch (IOException ignored) {
                // Retry until startup timeout.
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                state = RunnerState.CHILD_FAILED;
                lastError = "Interrupted while waiting for child readiness";
                return;
            }

            try {
                Thread.sleep(250);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                state = RunnerState.CHILD_FAILED;
                lastError = "Interrupted while waiting for child readiness";
                return;
            }
        }
        state = RunnerState.CHILD_FAILED;
        lastError = "Child health check did not pass before timeout";
    }

    private void stopChildLocked() {
        Process process = childProcess;
        if (process == null) {
            state = properties.isEnabled() ? RunnerState.STOPPED : RunnerState.DISABLED;
            return;
        }

        if (process.isAlive()) {
            state = RunnerState.STOPPING;
            process.destroy();
            try {
                if (!process.waitFor(safeDuration(properties.getStopTimeout(), Duration.ofSeconds(5)).toMillis(), TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }

        if (!process.isAlive()) {
            lastExitCode = process.exitValue();
        }
        childProcess = null;
        state = properties.isEnabled() ? RunnerState.STOPPED : RunnerState.DISABLED;
    }

    private void stopDependencyProcessesLocked() {
        redisInsightProcess = stopDependencyProcess("redis-insight", redisInsightProcess);
        redisProcess = stopDependencyProcess("redis", redisProcess);
    }

    private Process stopDependencyProcess(String name, Process process) {
        if (process == null) {
            return null;
        }
        if (process.isAlive()) {
            process.destroy();
            try {
                if (!process.waitFor(safeDuration(properties.getStopTimeout(), Duration.ofSeconds(5)).toMillis(), TimeUnit.MILLISECONDS)) {
                    process.destroyForcibly();
                    process.waitFor(2, TimeUnit.SECONDS);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                process.destroyForcibly();
            }
        }
        appendLog(name + " stopped");
        return null;
    }

    private void pumpLogs(Process process, long processGeneration) {
        logExecutor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendLog(line);
                }
            } catch (IOException exception) {
                appendLog("log capture failed: " + exception.getMessage());
            } finally {
                int exitCode = waitForExit(process);
                synchronized (lifecycleLock) {
                    if (generation == processGeneration && childProcess == process) {
                        lastExitCode = exitCode;
                        if (state == RunnerState.CHILD_READY || state == RunnerState.CHILD_STARTING) {
                            state = RunnerState.CHILD_FAILED;
                            lastError = "Child process exited with code " + exitCode;
                        }
                    }
                }
            }
        });
    }

    private void pumpLogs(Process process, String prefix, long processGeneration) {
        logExecutor.submit(() -> {
            try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
            )) {
                String line;
                while ((line = reader.readLine()) != null) {
                    appendLog(prefix + ": " + line);
                }
            } catch (IOException exception) {
                appendLog(prefix + " log capture failed: " + exception.getMessage());
            } finally {
                int exitCode = waitForExit(process);
                synchronized (lifecycleLock) {
                    appendLog(prefix + " exited with code " + exitCode);
                }
            }
        });
    }

    private int waitForExit(Process process) {
        try {
            return process.waitFor();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return -1;
        }
    }

    private void appendLog(String line) {
        synchronized (lifecycleLock) {
            if (recentLogs.size() == MAX_RECENT_LOG_LINES) {
                recentLogs.removeFirst();
            }
            recentLogs.addLast(line);
        }
        logger.info("session runner: {}", line);
    }

    private SessionRunnerStatus statusLocked() {
        Optional<URI> backendUri = properties.resolveChildBackendUri();
        return new SessionRunnerStatus(
            properties.isEnabled(),
            state.name(),
            generation,
            childProcess != null && childProcess.isAlive(),
            lastExitCode,
            lastError,
            lastStartedAt,
            backendUri.map(URI::toString).orElse(""),
            dependencyStatuses(),
            new ArrayList<>(recentLogs)
        );
    }

    private List<SessionRunnerDependencyStatus> dependencyStatuses() {
        return List.of(
            new SessionRunnerDependencyStatus(
                "redis",
                properties.resolveRedisTenancyMode(),
                properties.hasLocalRedisCommand(),
                isAlive(redisProcess),
                properties.resolveLocalRedisUri().map(URI::toString).orElse(""),
                properties.resolveLocalRedisHealthCommand()
            ),
            new SessionRunnerDependencyStatus(
                "redis-insight",
                properties.resolveRedisTenancyMode(),
                properties.hasLocalRedisInsightCommand(),
                isAlive(redisInsightProcess),
                properties.resolveLocalRedisInsightUri().map(URI::toString).orElse(""),
                properties.normalizedLocalRedisInsightHealthPath()
            )
        );
    }

    private Duration safeDuration(Duration configured, Duration fallback) {
        if (configured == null || configured.isZero() || configured.isNegative()) {
            return fallback;
        }
        return configured;
    }

    private boolean isAlive(Process process) {
        return process != null && process.isAlive();
    }

    public record SessionRunnerStatus(
        boolean enabled,
        String state,
        long generation,
        boolean childAlive,
        Integer lastExitCode,
        String lastError,
        Instant lastStartedAt,
        String backendUri,
        List<SessionRunnerDependencyStatus> dependencies,
        List<String> recentLogs
    ) {
    }

    public record SessionRunnerDependencyStatus(
        String name,
        String mode,
        boolean configured,
        boolean running,
        String localUri,
        String healthProbe
    ) {
    }
}
