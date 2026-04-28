package com.redis.workshop.platform.executionplane.docker;

import org.springframework.util.StringUtils;

public record DockerCommandResult(
    int exitCode,
    String stdout,
    String stderr
) {

    public static DockerCommandResult success(String stdout) {
        return new DockerCommandResult(0, stdout, "");
    }

    public void requireSuccess(String action) {
        if (exitCode == 0) {
            return;
        }
        throw new IllegalStateException("Failed to " + action + " with exit code " + exitCode + ": " + output());
    }

    public String trimmedStdout() {
        return stdout == null ? "" : stdout.trim();
    }

    private String output() {
        if (StringUtils.hasText(stderr)) {
            return stderr.trim();
        }
        if (StringUtils.hasText(stdout)) {
            return stdout.trim();
        }
        return "no output";
    }
}
