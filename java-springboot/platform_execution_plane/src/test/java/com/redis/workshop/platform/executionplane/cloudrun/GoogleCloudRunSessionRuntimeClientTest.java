package com.redis.workshop.platform.executionplane.cloudrun;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GoogleCloudRunSessionRuntimeClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<RecordedRequest> requests = new ArrayList<>();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void createOrReplaceCreatesServicePollsOperationAndReadsUri() throws Exception {
        server = startServer(exchange -> {
            RecordedRequest request = record(exchange);
            if (request.method().equals("POST") && request.path().endsWith("/locations/europe-west1/services")) {
                respond(exchange, 200, """
                    {"name":"projects/proj/locations/europe-west1/operations/op-1","done":false}
                    """);
                return;
            }
            if (request.method().equals("GET") && request.path().endsWith("/operations/op-1")) {
                respond(exchange, 200, """
                    {"name":"projects/proj/locations/europe-west1/operations/op-1","done":true}
                    """);
                return;
            }
            if (request.method().equals("GET") && request.path().endsWith("/services/ws-sess-001")) {
                respond(exchange, 200, """
                    {"name":"projects/proj/locations/europe-west1/services/ws-sess-001","uri":"https://ws-sess-001.run.app"}
                    """);
                return;
            }
            respond(exchange, 404, "{}");
        });

        GoogleCloudRunSessionRuntimeClient client = client();
        CloudRunSessionService service = client.createOrReplace(spec("ws-sess-001"));

        assertThat(service.uri()).isEqualTo("https://ws-sess-001.run.app");
        RecordedRequest create = requests.getFirst();
        assertThat(create.authorization()).isEqualTo("Bearer test-token");
        assertThat(create.query()).isEqualTo("serviceId=ws-sess-001");

        JsonNode payload = objectMapper.readTree(create.body());
        assertThat(payload.has("name")).isFalse();
        assertThat(payload.path("ingress").asText()).isEqualTo("INGRESS_TRAFFIC_INTERNAL_LOAD_BALANCER");
        assertThat(payload.path("invokerIamDisabled").asBoolean()).isTrue();
        assertThat(payload.path("labels").path("workshop-session-id").asText()).isEqualTo("sess-001");
        JsonNode template = payload.path("template");
        assertThat(template.path("serviceAccount").asText()).isEqualTo("runner@proj.iam.gserviceaccount.com");
        assertThat(template.path("maxInstanceRequestConcurrency").asInt()).isEqualTo(1);
        assertThat(template.path("scaling").path("minInstanceCount").asInt()).isZero();
        assertThat(template.path("scaling").path("maxInstanceCount").asInt()).isEqualTo(1);
        JsonNode container = template.path("containers").get(0);
        assertThat(container.path("image").asText()).isEqualTo("europe-west1-docker.pkg.dev/proj/runners/session@sha256:abc");
        assertThat(container.path("resources").path("limits").path("cpu").asText()).isEqualTo("1");
        assertThat(container.path("resources").path("limits").path("memory").asText()).isEqualTo("1024Mi");
        assertThat(container.path("env").findValuesAsText("name"))
            .contains("WORKSHOP_SESSION_ID", "WORKSHOP_REDIS_MODE", "WORKSHOP_STORAGE_TOKEN_SECRET_REF");
        assertThat(container.path("env").findValuesAsText("name")).doesNotContain("PORT");
    }

    @Test
    void createOrReplacePatchesExistingServiceAfterCreateConflict() throws Exception {
        server = startServer(exchange -> {
            RecordedRequest request = record(exchange);
            if (request.method().equals("POST") && request.path().endsWith("/locations/europe-west1/services")) {
                respond(exchange, 409, "{\"error\":{\"status\":\"ALREADY_EXISTS\"}}");
                return;
            }
            if (request.method().equals("PATCH") && request.path().endsWith("/services/ws-sess-001")) {
                respond(exchange, 200, """
                    {"name":"projects/proj/locations/europe-west1/operations/op-2","done":true}
                    """);
                return;
            }
            if (request.method().equals("GET") && request.path().endsWith("/services/ws-sess-001")) {
                respond(exchange, 200, """
                    {"name":"projects/proj/locations/europe-west1/services/ws-sess-001","uri":"https://ws-sess-001.run.app"}
                    """);
                return;
            }
            respond(exchange, 404, "{}");
        });

        CloudRunSessionService service = client().createOrReplace(spec("ws-sess-001"));

        assertThat(service.uri()).isEqualTo("https://ws-sess-001.run.app");
        RecordedRequest patch = requests.get(1);
        assertThat(patch.method()).isEqualTo("PATCH");
        assertThat(patch.query()).contains("updateMask=").doesNotContain("allowMissing=true");

        JsonNode payload = objectMapper.readTree(patch.body());
        assertThat(payload.path("name").asText())
            .isEqualTo("projects/proj/locations/europe-west1/services/ws-sess-001");
    }

    @Test
    void restartManagerUsesCloudRunServiceUri() throws Exception {
        server = startServer(exchange -> {
            RecordedRequest request = record(exchange);
            if (request.method().equals("GET") && request.path().endsWith("/services/ws-sess-002")) {
                respond(exchange, 200, "{\"uri\":\"" + baseUrl() + "/runner-service\"}");
                return;
            }
            if (request.method().equals("POST") && request.path().equals("/runner-service/internal/session-runner/restart")) {
                respond(exchange, 200, "{\"state\":\"RUNNING\"}");
                return;
            }
            respond(exchange, 404, "{}");
        });

        client().restartManager("proj", "europe-west1", "ws-sess-002", false);

        RecordedRequest restart = requests.get(1);
        assertThat(restart.authorization()).isEmpty();
        assertThat(objectMapper.readTree(restart.body()).path("rebuild").asBoolean()).isFalse();
    }

    @Test
    void deleteIsIdempotentWhenServiceDoesNotExist() throws Exception {
        server = startServer(exchange -> {
            record(exchange);
            respond(exchange, 404, "{}");
        });

        client().delete("proj", "europe-west1", "ws-sess-missing");

        assertThat(requests)
            .extracting(RecordedRequest::method)
            .containsExactly("DELETE");
    }

    @Test
    void defaultManagerRestartTimeoutCoversRunnerRebuildAndChildStartup() {
        assertThat(new CloudRunRuntimeProperties().getManagerRestartTimeout())
            .isEqualTo(Duration.ofSeconds(290));
    }

    private GoogleCloudRunSessionRuntimeClient client() {
        CloudRunRuntimeProperties properties = new CloudRunRuntimeProperties();
        properties.setApiBaseUrl(baseUrl() + "/v2");
        properties.setOperationPollInterval(Duration.ofMillis(1));
        properties.setOperationPollTimeout(Duration.ofSeconds(1));
        return new GoogleCloudRunSessionRuntimeClient(
            objectMapper,
            properties,
            HttpClient.newBuilder().build(),
            () -> "test-token"
        );
    }

    private CloudRunServiceSpec spec(String serviceName) {
        return new CloudRunServiceSpec(
            "proj",
            "europe-west1",
            serviceName,
            "europe-west1-docker.pkg.dev/proj/runners/session@sha256:abc",
            "runner@proj.iam.gserviceaccount.com",
            "INGRESS_TRAFFIC_INTERNAL_LOAD_BALANCER",
            "",
            8080,
            500,
            1024,
            1,
            0,
            1,
            Map.of(
                "workshop-session-id", "sess-001",
                "managed-by", "workshop-execution-plane"
            ),
            Map.of(
                "WORKSHOP_SESSION_ID", "sess-001",
                "PORT", "8080",
                "WORKSHOP_REDIS_MODE", "local-process"
            ),
            Map.of("WORKSHOP_STORAGE_TOKEN_SECRET_REF", "projects/proj/secrets/workspace-token")
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
        RecordedRequest request = new RecordedRequest(
            exchange.getRequestMethod(),
            exchange.getRequestURI().getPath(),
            exchange.getRequestURI().getRawQuery() == null ? "" : exchange.getRequestURI().getRawQuery(),
            exchange.getRequestHeaders().getFirst("Authorization") == null
                ? ""
                : exchange.getRequestHeaders().getFirst("Authorization"),
            new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)
        );
        requests.add(request);
        return request;
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
        String query,
        String authorization,
        String body
    ) {
    }

    @FunctionalInterface
    private interface ExchangeHandler {
        void handle(HttpExchange exchange) throws IOException;
    }
}
