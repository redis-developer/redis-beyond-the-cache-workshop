package com.redis.workshop.platform.executionplane.docker;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class HttpDockerSessionRunnerManagerClientTest {

    private final List<RecordedRequest> requests = new ArrayList<>();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void restartPostsRebuildJsonToSessionRunnerEndpoint() throws Exception {
        server = startServer(exchange -> {
            requests.add(record(exchange));
            respond(exchange, 200, "{\"state\":\"RUNNING\"}");
        });
        HttpDockerSessionRunnerManagerClient client = new HttpDockerSessionRunnerManagerClient(
            new DockerRuntimeProperties(),
            HttpClient.newBuilder().build()
        );

        client.restart(baseUrl(), false);
        client.restart(baseUrl() + "/", true);

        assertThat(requests).containsExactly(
            new RecordedRequest("POST", "/internal/session-runner/restart", "{\"rebuild\":false,\"async\":true}"),
            new RecordedRequest("POST", "/internal/session-runner/restart", "{\"rebuild\":true,\"async\":true}")
        );
    }

    private HttpServer startServer(ExchangeHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        httpServer.createContext("/", exchange -> {
            try {
                handler.handle(exchange);
            } finally {
                exchange.close();
            }
        });
        httpServer.start();
        return httpServer;
    }

    private RecordedRequest record(HttpExchange exchange) throws IOException {
        return new RecordedRequest(
            exchange.getRequestMethod(),
            exchange.getRequestURI().getPath(),
            new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
        );
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] response = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private record RecordedRequest(
        String method,
        String path,
        String body
    ) {
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
