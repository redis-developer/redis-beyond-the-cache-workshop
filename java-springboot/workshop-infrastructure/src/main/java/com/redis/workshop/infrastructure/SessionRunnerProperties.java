package com.redis.workshop.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Component
@ConfigurationProperties(prefix = "workshop.session-runner")
public class SessionRunnerProperties {

    private boolean enabled;
    private boolean autoStart = true;
    private String childCommand;
    private String childWorkingDirectory;
    private String childRebuildCommand;
    private String rebuiltChildJar;
    private int childPort = 18080;
    private String childHealthPath;
    private String redisCommand;
    private int redisPort = 6379;
    private String redisHealthCommand;
    private String redisInsightCommand;
    private int redisInsightPort = 5540;
    private String redisInsightHealthPath;
    private Duration startupTimeout = Duration.ofSeconds(30);
    private Duration stopTimeout = Duration.ofSeconds(5);
    private Duration rebuildTimeout = Duration.ofMinutes(3);
    private String shellExecutable = "bash";
    private String shellArgument = "-lc";
    private Map<String, String> environment = new LinkedHashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAutoStart() {
        return autoStart;
    }

    public void setAutoStart(boolean autoStart) {
        this.autoStart = autoStart;
    }

    public String getChildCommand() {
        return childCommand;
    }

    public void setChildCommand(String childCommand) {
        this.childCommand = childCommand;
    }

    public String getChildWorkingDirectory() {
        return childWorkingDirectory;
    }

    public void setChildWorkingDirectory(String childWorkingDirectory) {
        this.childWorkingDirectory = childWorkingDirectory;
    }

    public String getChildRebuildCommand() {
        return childRebuildCommand;
    }

    public void setChildRebuildCommand(String childRebuildCommand) {
        this.childRebuildCommand = childRebuildCommand;
    }

    public String getRebuiltChildJar() {
        return rebuiltChildJar;
    }

    public void setRebuiltChildJar(String rebuiltChildJar) {
        this.rebuiltChildJar = rebuiltChildJar;
    }

    public int getChildPort() {
        return childPort;
    }

    public void setChildPort(int childPort) {
        this.childPort = childPort;
    }

    public String getChildHealthPath() {
        return childHealthPath;
    }

    public void setChildHealthPath(String childHealthPath) {
        this.childHealthPath = childHealthPath;
    }

    public String getRedisCommand() {
        return redisCommand;
    }

    public void setRedisCommand(String redisCommand) {
        this.redisCommand = redisCommand;
    }

    public int getRedisPort() {
        return redisPort;
    }

    public void setRedisPort(int redisPort) {
        this.redisPort = redisPort;
    }

    public String getRedisHealthCommand() {
        return redisHealthCommand;
    }

    public void setRedisHealthCommand(String redisHealthCommand) {
        this.redisHealthCommand = redisHealthCommand;
    }

    public String getRedisInsightCommand() {
        return redisInsightCommand;
    }

    public void setRedisInsightCommand(String redisInsightCommand) {
        this.redisInsightCommand = redisInsightCommand;
    }

    public int getRedisInsightPort() {
        return redisInsightPort;
    }

    public void setRedisInsightPort(int redisInsightPort) {
        this.redisInsightPort = redisInsightPort;
    }

    public String getRedisInsightHealthPath() {
        return redisInsightHealthPath;
    }

    public void setRedisInsightHealthPath(String redisInsightHealthPath) {
        this.redisInsightHealthPath = redisInsightHealthPath;
    }

    public Duration getStartupTimeout() {
        return startupTimeout;
    }

    public void setStartupTimeout(Duration startupTimeout) {
        this.startupTimeout = startupTimeout;
    }

    public Duration getStopTimeout() {
        return stopTimeout;
    }

    public void setStopTimeout(Duration stopTimeout) {
        this.stopTimeout = stopTimeout;
    }

    public Duration getRebuildTimeout() {
        return rebuildTimeout;
    }

    public void setRebuildTimeout(Duration rebuildTimeout) {
        this.rebuildTimeout = rebuildTimeout;
    }

    public String getShellExecutable() {
        return shellExecutable;
    }

    public void setShellExecutable(String shellExecutable) {
        this.shellExecutable = shellExecutable;
    }

    public String getShellArgument() {
        return shellArgument;
    }

    public void setShellArgument(String shellArgument) {
        this.shellArgument = shellArgument;
    }

    public Map<String, String> getEnvironment() {
        return environment;
    }

    public void setEnvironment(Map<String, String> environment) {
        this.environment = environment == null ? new LinkedHashMap<>() : new LinkedHashMap<>(environment);
    }

    Optional<URI> resolveChildBackendUri() {
        int port = resolveChildPort();
        if (!enabled || port <= 0) {
            return Optional.empty();
        }
        return Optional.of(URI.create("http://127.0.0.1:" + port));
    }

    Optional<Path> resolveChildWorkingDirectory() {
        String value = firstNonBlank(childWorkingDirectory, System.getenv("WORKSHOP_CHILD_WORKING_DIRECTORY"));
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        return Optional.of(Path.of(value).toAbsolutePath().normalize());
    }

    String resolveChildCommand() {
        return firstNonBlank(childCommand, System.getenv("WORKSHOP_CHILD_COMMAND"));
    }

    String resolveChildRebuildCommand() {
        return firstNonBlank(childRebuildCommand, System.getenv("WORKSHOP_CHILD_REBUILD_COMMAND"));
    }

    Optional<Path> resolveRebuiltChildJar() {
        String value = firstNonBlank(rebuiltChildJar, System.getenv("WORKSHOP_REBUILT_CHILD_JAR"));
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        return Optional.of(Path.of(value).toAbsolutePath().normalize());
    }

    int resolveChildPort() {
        return firstPositiveInt(System.getenv("WORKSHOP_CHILD_PORT"), childPort);
    }

    String resolveShellExecutable() {
        return firstNonBlank(shellExecutable, "bash");
    }

    String resolveShellArgument() {
        return firstNonBlank(shellArgument, "-lc");
    }

    String resolveRedisTenancyMode() {
        return "local-process";
    }

    boolean isLocalRedisMode() {
        return true;
    }

    boolean hasLocalRedisCommand() {
        return isLocalRedisMode() && StringUtils.hasText(resolveLocalRedisCommand());
    }

    String resolveLocalRedisCommand() {
        return firstNonBlank(redisCommand, System.getenv("WORKSHOP_LOCAL_REDIS_COMMAND"));
    }

    int resolveLocalRedisPort() {
        return firstPositiveInt(System.getenv("WORKSHOP_LOCAL_REDIS_PORT"), redisPort);
    }

    Optional<URI> resolveLocalRedisUri() {
        int port = resolveLocalRedisPort();
        if (!enabled || port <= 0 || !hasLocalRedisCommand()) {
            return Optional.empty();
        }
        return Optional.of(URI.create("redis://127.0.0.1:" + port));
    }

    boolean hasLocalRedisInsightCommand() {
        return isLocalRedisMode() && StringUtils.hasText(resolveLocalRedisInsightCommand());
    }

    String resolveLocalRedisInsightCommand() {
        return firstNonBlank(
            redisInsightCommand,
            System.getenv("WORKSHOP_REDIS_INSIGHT_COMMAND"),
            System.getenv("WORKSHOP_LOCAL_REDIS_INSIGHT_COMMAND")
        );
    }

    int resolveLocalRedisInsightPort() {
        return firstPositiveInt(
            firstNonBlank(
                System.getenv("WORKSHOP_REDIS_INSIGHT_PORT"),
                System.getenv("WORKSHOP_LOCAL_REDIS_INSIGHT_PORT")
            ),
            redisInsightPort
        );
    }

    Optional<URI> resolveLocalRedisInsightUri() {
        int port = resolveLocalRedisInsightPort();
        if (!enabled || port <= 0 || !hasLocalRedisInsightCommand()) {
            return Optional.empty();
        }
        return Optional.of(URI.create("http://127.0.0.1:" + port));
    }

    String resolveLocalRedisHealthCommand() {
        return firstNonBlank(redisHealthCommand, System.getenv("WORKSHOP_LOCAL_REDIS_HEALTH_COMMAND"));
    }

    String normalizedLocalRedisInsightHealthPath() {
        String value = firstNonBlank(
            redisInsightHealthPath,
            System.getenv("WORKSHOP_REDIS_INSIGHT_HEALTH_PATH"),
            System.getenv("WORKSHOP_LOCAL_REDIS_INSIGHT_HEALTH_PATH")
        );
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.startsWith("/") ? trimmed : "/" + trimmed;
    }

    boolean hasChildHealthPath() {
        return StringUtils.hasText(resolveChildHealthPath());
    }

    String normalizedChildHealthPath() {
        if (!hasChildHealthPath()) {
            return "";
        }
        String value = resolveChildHealthPath().trim();
        return value.startsWith("/") ? value : "/" + value;
    }

    boolean hasRunnableChildCommand() {
        return StringUtils.hasText(resolveChildCommand())
            && StringUtils.hasText(resolveShellExecutable())
            && StringUtils.hasText(resolveShellArgument());
    }

    boolean hasChildRebuildCommand() {
        return StringUtils.hasText(resolveChildRebuildCommand())
            && StringUtils.hasText(resolveShellExecutable())
            && StringUtils.hasText(resolveShellArgument());
    }

    private String resolveChildHealthPath() {
        return firstNonBlank(childHealthPath, System.getenv("WORKSHOP_CHILD_HEALTH_PATH"));
    }

    private int firstPositiveInt(String rawValue, int fallback) {
        if (StringUtils.hasText(rawValue)) {
            try {
                int value = Integer.parseInt(rawValue.trim());
                if (value > 0) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
