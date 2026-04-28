package com.redis.workshop.platform.executionplane.docker;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Component
@ConditionalOnProperty(
    prefix = "platform.execution-plane",
    name = "runtime-provider",
    havingValue = "docker"
)
public class HttpDockerSessionRunnerManagerClient implements DockerSessionRunnerManagerClient {

    private final DockerRuntimeProperties properties;
    private final HttpClient httpClient;

    @Autowired
    public HttpDockerSessionRunnerManagerClient(DockerRuntimeProperties properties) {
        this(
            properties,
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
        );
    }

    HttpDockerSessionRunnerManagerClient(DockerRuntimeProperties properties, HttpClient httpClient) {
        this.properties = properties;
        this.httpClient = httpClient;
    }

    @Override
    public void restart(String upstreamBaseUrl, boolean rebuild) {
        String requestBody = "{\"rebuild\":" + rebuild + "}";
        HttpRequest request = HttpRequest.newBuilder(restartUri(upstreamBaseUrl))
            .timeout(managerRestartTimeout())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() >= HttpStatus.OK.value()
            && response.statusCode() <= HttpStatus.MULTIPLE_CHOICES.value() - 1) {
            return;
        }
        throw new IllegalStateException(
            "Failed to restart Docker session runner with status " + response.statusCode() + ": " + response.body()
        );
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while restarting Docker session runner", exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to restart Docker session runner", exception);
        }
    }

    private URI restartUri(String upstreamBaseUrl) {
        return URI.create(trimTrailingSlash(requireText(upstreamBaseUrl, "upstream base url"))
            + "/internal/session-runner/restart");
    }

    private Duration managerRestartTimeout() {
        Duration value = properties.getManagerRestartTimeout();
        if (value == null || value.isZero() || value.isNegative()) {
            return Duration.ofSeconds(290);
        }
        return value;
    }

    private String trimTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
