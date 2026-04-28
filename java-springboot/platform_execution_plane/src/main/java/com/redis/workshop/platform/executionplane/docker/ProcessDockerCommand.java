package com.redis.workshop.platform.executionplane.docker;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(
    prefix = "platform.execution-plane",
    name = "runtime-provider",
    havingValue = "docker"
)
public class ProcessDockerCommand implements DockerCommand {

    private final DockerRuntimeProperties properties;

    public ProcessDockerCommand(DockerRuntimeProperties properties) {
        this.properties = properties;
    }

    @Override
    public DockerCommandResult run(List<String> arguments) {
        List<String> command = new ArrayList<>();
        command.add(requireText(properties.getDockerBinary(), "docker binary"));
        command.addAll(arguments);
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            int exitCode = process.waitFor();
            return new DockerCommandResult(exitCode, output, "");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while running Docker command", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to run Docker command", exception);
        }
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
