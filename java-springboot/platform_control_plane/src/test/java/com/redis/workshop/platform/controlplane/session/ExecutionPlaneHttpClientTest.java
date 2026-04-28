package com.redis.workshop.platform.controlplane.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExecutionPlaneHttpClientTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void launchSessionSendsSharedSecretAndContractPayload() throws Exception {
        AtomicReference<String> secretHeader = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = startServer(exchange -> {
            secretHeader.set(exchange.getRequestHeaders().getFirst("X-Execution-Plane-Key"));
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, 200, launchAccepted("sess-001"));
        });

        ExecutionPlaneHttpClient client = client();

        ExecutionPlaneLaunchResult result = client.launchSession(launchRequest("sess-001"));

        assertThat(secretHeader.get()).isEqualTo("test-secret");
        assertThat(requestBody.get()).contains("\"sessionId\":\"sess-001\"");
        assertThat(requestBody.get()).contains("\"releaseId\":\"session-management-2026.04.1\"");
        assertThat(requestBody.get()).contains("\"artifacts\"");
        assertThat(requestBody.get()).contains("\"resourcePolicy\"");
        assertThat(requestBody.get()).contains("\"workspacePolicy\"");
        assertThat(result.runtimeRef()).isEqualTo(
            "cloud-run:projects/workshop-prod/locations/europe-west1/services/ws-sess-001"
        );
        assertThat(result.routeBinding().publicBasePath()).isEqualTo("/session/sess-001/");
        assertThat(result.routeBinding().upstreamBaseUrl()).isEqualTo("https://ws-sess-001.run.app");
        assertThat(result.publicEntryUrl()).isEqualTo("/session/sess-001/");
    }

    @Test
    void launchSessionRejectsDirectCloudRunPublicBasePath() throws Exception {
        server = startServer(exchange -> sendJson(exchange, 200, """
            {
              "sessionId": "sess-002",
              "runtimeRef": {
                "provider": "cloud-run",
                "handle": "projects/workshop-prod/locations/europe-west1/services/ws-sess-002"
              },
              "routeBinding": {
                "publicBasePath": "https://ws-sess-002-europe-west1.run.app/",
                "gatewayHost": "workshops.example.com",
                "serviceName": "ws-sess-002",
                "serviceNamespace": "europe-west1",
                "upstreamBaseUrl": "https://ws-sess-002.run.app"
              }
            }
            """));

        assertThatThrownBy(() -> client().launchSession(launchRequest("sess-002")))
            .isInstanceOf(ExecutionPlaneClient.ExecutionPlaneClientException.class)
            .satisfies(exception -> assertThat(((ExecutionPlaneClient.ExecutionPlaneClientException) exception).failureKind())
                .isEqualTo(ExecutionPlaneClient.FailureKind.INVALID_RESPONSE));
    }

    @Test
    void launchSessionRejectsUnauthorizedResponse() throws Exception {
        server = startServer(exchange -> sendJson(exchange, 403, """
            {
              "message": "forbidden"
            }
            """));

        assertThatThrownBy(() -> client().launchSession(launchRequest("sess-003")))
            .isInstanceOf(ExecutionPlaneClient.ExecutionPlaneClientException.class)
            .satisfies(exception -> assertThat(((ExecutionPlaneClient.ExecutionPlaneClientException) exception).failureKind())
                .isEqualTo(ExecutionPlaneClient.FailureKind.UNAUTHORIZED));
    }

    @Test
    void launchSessionRetriesOnceAfterConnectionFailure() throws Exception {
        AtomicInteger attempts = new AtomicInteger();
        server = startServer(exchange -> {
            if (attempts.incrementAndGet() == 1) {
                return;
            }
            sendJson(exchange, 200, launchAccepted("sess-retry"));
        });

        ExecutionPlaneLaunchResult result = client().launchSession(launchRequest("sess-retry"));

        assertThat(attempts.get()).isEqualTo(2);
        assertThat(result.publicEntryUrl()).isEqualTo("/session/sess-retry/");
    }

    @Test
    void launchSessionReportsTimeoutClearly() throws Exception {
        server = startServer(exchange -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IOException("sleep interrupted", exception);
            }
            sendJson(exchange, 200, launchAccepted("sess-timeout"));
        });

        ExecutionPlaneHttpClient client = new ExecutionPlaneHttpClient(
            new ObjectMapper(),
            serverBaseUrl(),
            "test-secret",
            Duration.ofMillis(50),
            Duration.ofSeconds(5),
            Duration.ofSeconds(5)
        );

        assertThatThrownBy(() -> client.launchSession(launchRequest("sess-timeout")))
            .isInstanceOf(ExecutionPlaneClient.ExecutionPlaneClientException.class)
            .hasMessageContaining("Execution plane launch request timed out after PT0.05S")
            .satisfies(exception -> assertThat(((ExecutionPlaneClient.ExecutionPlaneClientException) exception).failureKind())
                .isEqualTo(ExecutionPlaneClient.FailureKind.COMMUNICATION_FAILURE));
    }

    @Test
    void terminateSessionSendsRuntimeRefAndTreatsNotFoundAsAlreadyTerminated() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, 404, """
                {
                  "message": "missing"
                }
                """);
        }, "/internal/execution-plane/sessions/sess-004");

        ExecutionPlaneClient.TerminationResult result = client().terminateSession(new SessionTerminationRequest(
            "sess-004",
            "learner-1",
            "cloud-run:projects/workshop-prod/locations/europe-west1/services/ws-sess-004",
            "user_requested"
        ));

        assertThat(result).isEqualTo(ExecutionPlaneClient.TerminationResult.ALREADY_TERMINATED);
        assertThat(requestBody.get()).contains("\"provider\":\"cloud-run\"");
        assertThat(requestBody.get()).contains("\"handle\":\"projects/workshop-prod/locations/europe-west1/services/ws-sess-004\"");
        assertThat(requestBody.get()).contains("\"mode\":\"USER_REQUESTED\"");
    }

    @Test
    void restartSessionSendsFullContractPayloadAndRebuildFlag() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, 200, launchAccepted("sess-005"));
        }, "/internal/execution-plane/sessions/sess-005/restart");

        ExecutionPlaneLaunchResult result = client().restartSession(restartRequest("sess-005"));

        assertThat(requestBody.get()).contains("\"releaseId\":\"session-management-2026.04.1\"");
        assertThat(requestBody.get()).contains("\"rebuild\":true");
        assertThat(requestBody.get()).contains("\"artifacts\"");
        assertThat(requestBody.get()).contains("\"workspacePolicy\"");
        assertThat(result.runtimeRef()).isEqualTo(
            "cloud-run:projects/workshop-prod/locations/europe-west1/services/ws-sess-005"
        );
        assertThat(result.publicEntryUrl()).isEqualTo("/session/sess-005/");
    }

    @Test
    void restartSessionSendsRebuildFalseForManagerRestart() throws Exception {
        AtomicReference<String> requestBody = new AtomicReference<>();
        server = startServer(exchange -> {
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            sendJson(exchange, 200, launchAccepted("sess-006"));
        }, "/internal/execution-plane/sessions/sess-006/restart");

        ExecutionPlaneLaunchResult result = client().restartSession(restartRequest("sess-006", false));

        assertThat(requestBody.get()).contains("\"rebuild\":false");
        assertThat(requestBody.get()).contains("\"combined\"");
        assertThat(result.runtimeRef()).isEqualTo(
            "cloud-run:projects/workshop-prod/locations/europe-west1/services/ws-sess-006"
        );
        assertThat(result.publicEntryUrl()).isEqualTo("/session/sess-006/");
    }

    @Test
    void constructorRequiresSharedSecretWhenExecutionBaseUrlIsConfigured() {
        assertThatThrownBy(() -> new ExecutionPlaneHttpClient(
            new ObjectMapper(),
            "http://localhost:8080",
            " ",
            Duration.ofSeconds(5),
            Duration.ofSeconds(5),
            Duration.ofSeconds(5)
        )).isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("platform.controlplane.execution.shared-secret is required");
    }

    private ExecutionPlaneHttpClient client() {
        return new ExecutionPlaneHttpClient(
            new ObjectMapper(),
            serverBaseUrl(),
            "test-secret",
            Duration.ofSeconds(5),
            Duration.ofSeconds(5),
            Duration.ofSeconds(5)
        );
    }

    private SessionLaunchRequest launchRequest(String sessionId) {
        return new SessionLaunchRequest(
            sessionId,
            "learner-1",
            "1_session_management",
            "2026.04.1",
            SessionMode.LAB,
            "session-management-2026.04.1",
            artifacts(),
            resourcePolicy(),
            workspacePolicy(),
            Map.of("releaseId", "session-management-2026.04.1")
        );
    }

    private SessionRestartRequest restartRequest(String sessionId) {
        return restartRequest(sessionId, true);
    }

    private SessionRestartRequest restartRequest(String sessionId, boolean rebuild) {
        return new SessionRestartRequest(
            sessionId,
            "learner-1",
            "1_session_management",
            "2026.04.1",
            SessionMode.LAB,
            rebuild,
            "session-management-2026.04.1",
            artifacts(),
            resourcePolicy(),
            workspacePolicy(),
            Map.of("releaseId", "session-management-2026.04.1")
        );
    }

    private Map<String, String> artifacts() {
        return Map.of(
            "frontend",
            "registry.example.com/workshops/session-management-frontend@sha256:1111111111111111111111111111111111111111111111111111111111111111",
            "backend",
            "registry.example.com/workshops/session-management-backend@sha256:2222222222222222222222222222222222222222222222222222222222222222",
            "combined",
            "registry.example.com/workshops/session-management-runner@sha256:9999999999999999999999999999999999999999999999999999999999999999"
        );
    }

    private SessionResourcePolicy resourcePolicy() {
        return new SessionResourcePolicy("small", 500, 512, 1024, 60);
    }

    private SessionWorkspacePolicy workspacePolicy() {
        return new SessionWorkspacePolicy(
            "/workshop-sources",
            "release:session-management-2026.04.1",
            true,
            true
        );
    }

    private String launchAccepted(String sessionId) {
        return """
            {
              "sessionId": "%s",
              "runtimeRef": {
                "provider": "cloud-run",
                "handle": "projects/workshop-prod/locations/europe-west1/services/ws-%s"
              },
              "routeBinding": {
                "publicBasePath": "/session/%s/",
                "gatewayHost": "workshops.example.com",
                "serviceName": "ws-%s",
                "serviceNamespace": "europe-west1",
                "upstreamBaseUrl": "https://ws-%s.run.app"
              }
            }
            """.formatted(sessionId, sessionId, sessionId, sessionId, sessionId);
    }

    private HttpServer startServer(ExchangeHandler handler) throws IOException {
        return startServer(handler, "/internal/execution-plane/sessions");
    }

    private HttpServer startServer(ExchangeHandler handler, String path) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
        httpServer.createContext(path, exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        httpServer.start();
        return httpServer;
    }

    private String serverBaseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void sendJson(HttpExchange exchange, int statusCode, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @FunctionalInterface
    private interface ExchangeHandler {

        void handle(HttpExchange exchange) throws IOException;
    }
}
