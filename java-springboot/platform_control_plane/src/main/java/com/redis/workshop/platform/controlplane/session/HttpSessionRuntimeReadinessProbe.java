package com.redis.workshop.platform.controlplane.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;

@Component
class HttpSessionRuntimeReadinessProbe implements SessionRuntimeReadinessProbe {

    private static final String SESSION_RUNNER_STATUS_PATH = "/internal/session-runner/status";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final Duration timeout;
    private final Duration pollInterval;
    private final Duration requestTimeout;

    @Autowired
    HttpSessionRuntimeReadinessProbe(
        ObjectMapper objectMapper,
        @Value("${platform.controlplane.execution.readiness-timeout:480s}")
        Duration timeout,
        @Value("${platform.controlplane.execution.readiness-poll-interval:2s}")
        Duration pollInterval,
        @Value("${platform.controlplane.execution.readiness-request-timeout:3s}")
        Duration requestTimeout
    ) {
        this(
            objectMapper,
            HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build(),
            timeout,
            pollInterval,
            requestTimeout
        );
    }

    HttpSessionRuntimeReadinessProbe(
        ObjectMapper objectMapper,
        HttpClient httpClient,
        Duration timeout,
        Duration pollInterval,
        Duration requestTimeout
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
        this.timeout = positiveDuration(timeout, Duration.ofSeconds(90));
        this.pollInterval = positiveDuration(pollInterval, Duration.ofSeconds(2));
        this.requestTimeout = positiveDuration(requestTimeout, Duration.ofSeconds(3));
    }

    @Override
    public void awaitReady(ExecutionPlaneRouteBinding routeBinding) {
        URI statusUri = statusUri(routeBinding.upstreamBaseUrl());
        Instant deadline = Instant.now().plus(timeout);
        String lastObservation = "no response";
        Exception lastFailure = null;

        while (!Instant.now().isAfter(deadline)) {
            try {
                HttpResponse<byte[]> response = httpClient.send(
                    HttpRequest.newBuilder(statusUri)
                        .timeout(requestTimeout)
                        .GET()
                        .build(),
                    HttpResponse.BodyHandlers.ofByteArray()
                );
                RunnerReadiness readiness = readinessFrom(response);
                lastObservation = readiness.observation();
                lastFailure = null;
                if (readiness.ready()) {
                    return;
                }
            } catch (IOException exception) {
                lastObservation = exception.getClass().getSimpleName() + ": " + exception.getMessage();
                lastFailure = exception;
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while waiting for session runner readiness", exception);
            }

            sleep(pollInterval);
        }

        IllegalStateException timeoutException = new IllegalStateException(
            "Session runner did not become ready before timeout at " + statusUri + ". Last observation: " + lastObservation
        );
        if (lastFailure != null) {
            timeoutException.initCause(lastFailure);
        }
        throw timeoutException;
    }

    private RunnerReadiness readinessFrom(HttpResponse<byte[]> response) throws IOException {
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            return new RunnerReadiness(false, "HTTP " + response.statusCode());
        }

        JsonNode root = objectMapper.readTree(response.body());
        boolean enabled = root.path("enabled").asBoolean(false);
        String state = root.path("state").asText("");
        boolean childAlive = root.path("childAlive").asBoolean(false);
        String lastError = root.path("lastError").asText("");
        String recentLogs = recentLogsObservation(root.path("recentLogs"));
        boolean ready = enabled && childAlive && "CHILD_READY".equals(state);
        return new RunnerReadiness(
            ready,
            "enabled=" + enabled
                + ", state=" + state
                + ", childAlive=" + childAlive
                + (StringUtils.hasText(lastError) ? ", lastError=" + lastError : "")
                + (StringUtils.hasText(recentLogs) ? ", recentLogs=[" + recentLogs + "]" : "")
        );
    }

    private String recentLogsObservation(JsonNode recentLogs) {
        if (!recentLogs.isArray() || recentLogs.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        int start = Math.max(0, recentLogs.size() - 5);
        for (int index = start; index < recentLogs.size(); index++) {
            String value = recentLogs.get(index).asText("");
            if (!StringUtils.hasText(value)) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(" | ");
            }
            builder.append(value);
        }
        return builder.toString();
    }

    private URI statusUri(String upstreamBaseUrl) {
        String baseUrl = requireText(upstreamBaseUrl, "route upstream base URL");
        return URI.create(trimTrailingSlash(baseUrl) + SESSION_RUNNER_STATUS_PATH);
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private String trimTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private Duration positiveDuration(Duration value, Duration fallback) {
        if (value == null || value.isZero() || value.isNegative()) {
            return fallback;
        }
        return value;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for session runner readiness", exception);
        }
    }

    private record RunnerReadiness(boolean ready, String observation) {
    }
}
