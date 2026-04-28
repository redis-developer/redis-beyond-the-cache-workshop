package com.redis.workshop.platform.controlplane.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HttpSessionRuntimeReadinessProbeTest {

    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void reportsReadyOnlyWhenRunnerIsEnabledChildReadyAndChildAlive() throws Exception {
        startServer("""
            {"enabled":true,"state":"CHILD_READY","childAlive":true}
            """);

        HttpSessionRuntimeReadinessProbe probe = readinessProbe();

        assertThatCode(() -> probe.awaitReady(routeBinding()))
            .doesNotThrowAnyException();
    }

    @Test
    void rejectsChildReadyWhenChildProcessIsNotAlive() throws Exception {
        startServer("""
            {"enabled":true,"state":"CHILD_READY","childAlive":false,"lastError":"child exited"}
            """);

        HttpSessionRuntimeReadinessProbe probe = readinessProbe();

        assertThatThrownBy(() -> probe.awaitReady(routeBinding()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("state=CHILD_READY")
            .hasMessageContaining("childAlive=false")
            .hasMessageContaining("lastError=child exited");
    }

    @Test
    void rejectsAliveChildUntilStateIsChildReady() throws Exception {
        startServer("""
            {"enabled":true,"state":"CHILD_STARTING","childAlive":true}
            """);

        HttpSessionRuntimeReadinessProbe probe = readinessProbe();

        assertThatThrownBy(() -> probe.awaitReady(routeBinding()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("state=CHILD_STARTING")
            .hasMessageContaining("childAlive=true");
    }

    private HttpSessionRuntimeReadinessProbe readinessProbe() {
        return new HttpSessionRuntimeReadinessProbe(
            new ObjectMapper(),
            HttpClient.newHttpClient(),
            Duration.ofMillis(250),
            Duration.ofMillis(10),
            Duration.ofMillis(100)
        );
    }

    private ExecutionPlaneRouteBinding routeBinding() {
        return new ExecutionPlaneRouteBinding(
            "/session/sess-001/",
            "workshops.example.com",
            "ws-sess-001",
            "europe-west4",
            "http://127.0.0.1:" + server.getAddress().getPort()
        );
    }

    private void startServer(String responseBody) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/session-runner/status", exchange -> {
            byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }
}
