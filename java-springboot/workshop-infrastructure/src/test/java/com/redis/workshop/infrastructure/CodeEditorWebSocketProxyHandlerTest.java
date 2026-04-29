package com.redis.workshop.infrastructure;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.WebSocketContainer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.net.URI;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CodeEditorWebSocketProxyHandlerTest {

    @Test
    void detectsWebSocketUpgradeRequestsUnderCodePath() {
        CodeEditorWebSocketProxyHandler handler = handler();
        MockHttpServletRequest request = websocketRequest("/code/socket");

        assertThat(handler.canHandle(request)).isTrue();
    }

    @Test
    void ignoresNonWebSocketRequestsUnderCodePath() {
        CodeEditorWebSocketProxyHandler handler = handler();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/code/socket");

        assertThat(handler.canHandle(request)).isFalse();
    }

    @Test
    void convertsConfiguredHttpTargetToWebSocketUriAndStripsCodePath() {
        CodeEditorWebSocketProxyHandler handler = handler();
        MockHttpServletRequest request = websocketRequest("/code/socket");
        request.setQueryString("folder=%2Fworkspace");

        URI upstreamUri = handler.buildUpstreamWebSocketUri(request, URI.create("http://127.0.0.1:39000"));

        assertThat(upstreamUri.toString()).isEqualTo("ws://127.0.0.1:39000/socket?folder=%2Fworkspace");
    }

    @Test
    void forwardsProxyHeadersWithoutClientWebSocketHandshakeHeaders() {
        CodeEditorWebSocketProxyHandler handler = handler();
        MockHttpServletRequest request = websocketRequest("/code/socket");
        request.setScheme("https");
        request.setServerName("frontend.example");
        request.setServerPort(443);
        request.setRemoteAddr("10.0.0.7");
        request.addHeader(HttpHeaders.HOST, "frontend.example");
        request.addHeader(HttpHeaders.ORIGIN, "https://control-plane.example");
        request.addHeader("Cookie", "SESSION=abc");
        request.addHeader("X-Forwarded-Prefix", "/session/example");
        request.addHeader("X-Forwarded-For", "203.0.113.10");
        request.addHeader("Sec-WebSocket-Key", "client-key");
        request.addHeader("Sec-WebSocket-Protocol", "vscode");
        request.addHeader("Sec-WebSocket-Version", "13");

        Map<String, List<String>> headers = handler.buildProxyHeaders(request);

        assertThat(headers).containsEntry("Cookie", List.of("SESSION=abc"));
        assertThat(headers).containsEntry("X-Forwarded-Proto", List.of("https"));
        assertThat(headers).containsEntry("X-Forwarded-Host", List.of("frontend.example"));
        assertThat(headers).containsEntry("X-Forwarded-Port", List.of("443"));
        assertThat(headers).doesNotContainKey("X-Forwarded-Prefix");
        assertThat(headers).containsEntry("X-Forwarded-For", List.of("203.0.113.10, 10.0.0.7"));
        assertThat(headers).containsEntry(HttpHeaders.ORIGIN, List.of("https://frontend.example"));
        assertThat(headers.keySet())
            .noneMatch(name -> name.equalsIgnoreCase("Sec-WebSocket-Key"))
            .noneMatch(name -> name.equalsIgnoreCase("Sec-WebSocket-Protocol"))
            .noneMatch(name -> name.equalsIgnoreCase("Sec-WebSocket-Version"))
            .noneMatch(name -> name.equalsIgnoreCase(HttpHeaders.UPGRADE));
    }

    @Test
    void returnsNotFoundWhenCodeEditorPortIsNotConfigured() throws Exception {
        SessionRunnerProperties runnerProperties = new SessionRunnerProperties();
        runnerProperties.setEnabled(true);
        runnerProperties.setCodeEditorPort(0);
        CodeEditorWebSocketProxyHandler handler = new CodeEditorWebSocketProxyHandler(
            runnerProperties,
            mock(WebSocketContainer.class)
        );
        MockHttpServletRequest request = websocketRequest("/code/socket");
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.handle(request, response);

        assertThat(response.getStatus()).isEqualTo(HttpServletResponse.SC_NOT_FOUND);
        assertThat(response.getContentAsString()).contains("Code editor is not configured");
    }

    private CodeEditorWebSocketProxyHandler handler() {
        SessionRunnerProperties runnerProperties = new SessionRunnerProperties();
        runnerProperties.setEnabled(true);
        runnerProperties.setCodeEditorCommand("code-server");
        return new CodeEditorWebSocketProxyHandler(runnerProperties, mock(WebSocketContainer.class));
    }

    private MockHttpServletRequest websocketRequest(String requestUri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", requestUri);
        request.addHeader(HttpHeaders.CONNECTION, "Upgrade");
        request.addHeader(HttpHeaders.UPGRADE, "websocket");
        return request;
    }
}
