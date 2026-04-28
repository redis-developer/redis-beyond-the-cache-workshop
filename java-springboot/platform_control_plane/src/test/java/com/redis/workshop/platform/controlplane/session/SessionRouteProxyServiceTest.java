package com.redis.workshop.platform.controlplane.session;

import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionMode;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.model.RouteType;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspaceCleanupState;
import com.redis.workshop.platform.controlplane.persistence.model.WorkspacePolicy;
import com.redis.workshop.platform.controlplane.persistence.repository.PlatformSessionRecordRepository;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionRouteProxyServiceTest {

    @Mock
    private PlatformSessionRecordRepository sessionRepository;

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void proxiesReadySessionToStoredUpstreamAndStripsStableBasePath() throws Exception {
        AtomicReference<String> proxiedPath = new AtomicReference<>();
        server = startServer(exchange -> {
            proxiedPath.set(exchange.getRequestURI().toString());
            send(exchange, 200, "text/html", "<html><script src=\"/js/app.js\"></script></html>");
        });
        PlatformSessionRecord record = readySession("sess-001");
        record.setRouteUpstreamBaseUrl(serverBaseUrl());
        when(sessionRepository.findById("sess-001")).thenReturn(Optional.of(record));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/session/sess-001/demo");
        request.setQueryString("tab=redis");

        ResponseEntity<byte[]> response = new SessionRouteProxyService(sessionRepository).proxy("sess-001", request, null);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(proxiedPath.get()).isEqualTo("/demo?tab=redis");
        assertThat(new String(response.getBody(), StandardCharsets.UTF_8))
            .contains("src=\"/session/sess-001/js/app.js\"");
    }

    @Test
    void returnsNotFoundForNonRoutableSession() {
        PlatformSessionRecord record = readySession("sess-002");
        record.setState(PlatformSessionState.TERMINATED);
        when(sessionRepository.findById("sess-002")).thenReturn(Optional.of(record));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/session/sess-002/");

        ResponseEntity<byte[]> response = new SessionRouteProxyService(sessionRepository).proxy("sess-002", request, null);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void rewritesAbsoluteRedirectsFromUpstreamHost() throws Exception {
        server = startServer(exchange -> {
            exchange.getResponseHeaders().add("Location", serverBaseUrl().replace("127.0.0.1", "localhost") + "/login?next=%2F");
            exchange.sendResponseHeaders(302, -1);
        });
        PlatformSessionRecord record = readySession("sess-003");
        record.setRouteUpstreamBaseUrl(serverBaseUrl().replace("127.0.0.1", "localhost"));
        when(sessionRepository.findById("sess-003")).thenReturn(Optional.of(record));

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/session/sess-003/");

        ResponseEntity<byte[]> response = new SessionRouteProxyService(sessionRepository).proxy("sess-003", request, null);

        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getFirst("Location")).isEqualTo("/session/sess-003/login?next=%2F");
    }

    @Test
    void doesNotDoublePrefixAbsoluteRedirectsThatAlreadyIncludeStableBasePath() throws Exception {
        server = startServer(exchange -> {
            exchange.getResponseHeaders().add("Location", serverBaseUrl() + "/session/sess-004/welcome");
            exchange.sendResponseHeaders(302, -1);
        });
        PlatformSessionRecord record = readySession("sess-004");
        record.setRouteUpstreamBaseUrl(serverBaseUrl());
        when(sessionRepository.findById("sess-004")).thenReturn(Optional.of(record));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/session/sess-004/login");

        ResponseEntity<byte[]> response = new SessionRouteProxyService(sessionRepository).proxy("sess-004", request, null);

        assertThat(response.getStatusCode().value()).isEqualTo(302);
        assertThat(response.getHeaders().getFirst("Location")).isEqualTo("/session/sess-004/welcome");
    }

    private PlatformSessionRecord readySession(String sessionId) {
        PlatformSessionRecord record = new PlatformSessionRecord();
        record.setSessionId(sessionId);
        record.setOwnerUserId("learner-1");
        record.setWorkshopId("1_session_management");
        record.setReleaseVersion("current");
        record.setMode(PlatformSessionMode.LAB);
        record.setState(PlatformSessionState.READY);
        record.setQuotaClass("standard");
        record.setResourceClass("small");
        record.setCreatedAt(Instant.parse("2026-04-21T10:00:00Z"));
        record.setExpiresAt(Instant.parse("2026-04-21T11:00:00Z"));
        record.setLastActivityAt(Instant.parse("2026-04-21T10:05:00Z"));
        record.setRouteType(RouteType.PATH);
        record.setWorkspacePolicy(WorkspacePolicy.EPHEMERAL);
        record.setWorkspaceRef("session-workspace:" + sessionId);
        record.setWorkspaceCleanupState(WorkspaceCleanupState.ACTIVE);
        return record;
    }

    private HttpServer startServer(ExchangeHandler handler) throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress(0), 0);
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

    private String serverBaseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void send(HttpExchange exchange, int statusCode, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", contentType);
        exchange.sendResponseHeaders(statusCode, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @FunctionalInterface
    private interface ExchangeHandler {

        void handle(HttpExchange exchange) throws IOException;
    }
}
