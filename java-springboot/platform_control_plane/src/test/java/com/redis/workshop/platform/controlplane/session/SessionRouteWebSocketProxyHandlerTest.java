package com.redis.workshop.platform.controlplane.session;

import com.redis.workshop.platform.controlplane.persistence.repository.PlatformSessionRecordRepository;
import jakarta.websocket.WebSocketContainer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SessionRouteWebSocketProxyHandlerTest {

    @Test
    void detectsWebSocketUpgradeRequestsUnderSessionRoutes() {
        SessionRouteWebSocketProxyHandler handler = handler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/session/sess-001/code/");
        request.addHeader(HttpHeaders.UPGRADE, "websocket");
        request.addHeader(HttpHeaders.CONNECTION, "keep-alive, Upgrade");

        assertThat(handler.canHandle(request)).isTrue();
        assertThat(handler.extractSessionId(request)).contains("sess-001");
    }

    @Test
    void ignoresNonWebSocketRequestsUnderSessionRoutes() {
        SessionRouteWebSocketProxyHandler handler = handler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/session/sess-001/code/");

        assertThat(handler.canHandle(request)).isFalse();
    }

    @Test
    void convertsResolvedHttpTargetsToWebSocketUrisWithoutRewritingRawQuery() {
        SessionRouteWebSocketProxyHandler handler = handler();

        assertThat(handler.webSocketUri("http://runner.example.dev/code/?folder=%2Fworkspace").toString())
            .isEqualTo("ws://runner.example.dev/code/?folder=%2Fworkspace");
        assertThat(handler.webSocketUri("https://runner.example.dev/code/?folder=%2Fworkspace").toString())
            .isEqualTo("wss://runner.example.dev/code/?folder=%2Fworkspace");
    }

    @Test
    void forwardsProxyHeadersWithoutClientWebSocketHandshakeHeaders() {
        SessionRouteWebSocketProxyHandler handler = handler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/session/sess-001/code/");
        request.setScheme("https");
        request.addHeader(HttpHeaders.HOST, "hub.example.dev");
        request.addHeader(HttpHeaders.COOKIE, "sid=abc");
        request.addHeader(HttpHeaders.CONNECTION, "Upgrade");
        request.addHeader(HttpHeaders.UPGRADE, "websocket");
        request.addHeader("Sec-WebSocket-Key", "client-key");
        request.addHeader("Sec-WebSocket-Protocol", "vscode");
        request.addHeader("Sec-WebSocket-Version", "13");

        Map<String, List<String>> headers = handler.upstreamRequestHeaders(request, "/session/sess-001");

        assertThat(headers).containsEntry(HttpHeaders.COOKIE, List.of("sid=abc"));
        assertThat(headers).doesNotContainKey("X-Forwarded-Prefix");
        assertThat(headers).containsEntry("X-Forwarded-Host", List.of("hub.example.dev"));
        assertThat(headers).containsEntry("X-Forwarded-Proto", List.of("https"));
        assertThat(headers.keySet())
            .noneMatch(name -> name.equalsIgnoreCase(HttpHeaders.CONNECTION))
            .noneMatch(name -> name.equalsIgnoreCase(HttpHeaders.UPGRADE))
            .noneMatch(name -> name.equalsIgnoreCase(HttpHeaders.HOST))
            .noneMatch(name -> name.equalsIgnoreCase("Sec-WebSocket-Key"))
            .noneMatch(name -> name.equalsIgnoreCase("Sec-WebSocket-Protocol"))
            .noneMatch(name -> name.equalsIgnoreCase("Sec-WebSocket-Version"));
    }

    private SessionRouteWebSocketProxyHandler handler() {
        return new SessionRouteWebSocketProxyHandler(
            new SessionRouteProxyService(mock(PlatformSessionRecordRepository.class)),
            mock(WebSocketContainer.class)
        );
    }
}
