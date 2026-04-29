package com.redis.workshop.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.websocket.ClientEndpointConfig;
import jakarta.websocket.CloseReason;
import jakarta.websocket.CloseReason.CloseCodes;
import jakarta.websocket.ContainerProvider;
import jakarta.websocket.DeploymentException;
import jakarta.websocket.Endpoint;
import jakarta.websocket.EndpointConfig;
import jakarta.websocket.MessageHandler;
import jakarta.websocket.PongMessage;
import jakarta.websocket.Session;
import jakarta.websocket.WebSocketContainer;
import jakarta.websocket.server.ServerContainer;
import jakarta.websocket.server.ServerEndpointConfig;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class CodeEditorWebSocketProxyHandler {

    private static final String CODE_EDITOR_BASE_PATH = "/code";
    private static final int MAX_MESSAGE_BUFFER_SIZE = 1024 * 1024;
    private static final Set<String> EXCLUDED_HEADERS = Set.of(
        "connection",
        "keep-alive",
        "proxy-authenticate",
        "proxy-authorization",
        "te",
        "trailers",
        "transfer-encoding",
        "upgrade",
        "host",
        "origin",
        "content-length"
    );
    private static final Set<String> WEB_SOCKET_HANDSHAKE_HEADERS = Set.of(
        "sec-websocket-accept",
        "sec-websocket-extensions",
        "sec-websocket-key",
        "sec-websocket-protocol",
        "sec-websocket-version"
    );
    private static final Set<String> MANAGED_FORWARDED_HEADERS = Set.of(
        "x-forwarded-for",
        "x-forwarded-host",
        "x-forwarded-port",
        "x-forwarded-prefix",
        "x-forwarded-proto"
    );

    private final SessionRunnerProperties runnerProperties;
    private final WebSocketContainer clientContainer;

    public CodeEditorWebSocketProxyHandler(SessionRunnerProperties runnerProperties) {
        this(runnerProperties, ContainerProvider.getWebSocketContainer());
    }

    CodeEditorWebSocketProxyHandler(
        SessionRunnerProperties runnerProperties,
        WebSocketContainer clientContainer
    ) {
        this.runnerProperties = runnerProperties;
        this.clientContainer = clientContainer;
        this.clientContainer.setDefaultMaxTextMessageBufferSize(MAX_MESSAGE_BUFFER_SIZE);
        this.clientContainer.setDefaultMaxBinaryMessageBufferSize(MAX_MESSAGE_BUFFER_SIZE);
    }

    boolean canHandle(HttpServletRequest request) {
        return isWebSocketUpgradeRequest(request) && isCodeEditorPath(extractPath(request));
    }

    void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<URI> codeEditorUri = runnerProperties.resolveLocalCodeEditorUri();
        if (codeEditorUri.isEmpty()) {
            writeError(response, HttpServletResponse.SC_NOT_FOUND, "Code editor is not configured");
            return;
        }

        URI upstreamUri;
        try {
            upstreamUri = buildUpstreamWebSocketUri(request, codeEditorUri.get());
        } catch (IllegalArgumentException e) {
            writeError(response, HttpServletResponse.SC_BAD_GATEWAY, "Code editor WebSocket target is invalid");
            return;
        }

        ServerContainer serverContainer = resolveServerContainer(request);
        if (serverContainer == null) {
            writeError(response, HttpServletResponse.SC_BAD_GATEWAY, "WebSocket server container unavailable");
            return;
        }

        ProxyRequest proxyRequest = new ProxyRequest(
            upstreamUri,
            buildProxyHeaders(request),
            extractRequestedSubprotocols(request)
        );
        ServerEndpointConfig endpointConfig = ServerEndpointConfig.Builder
            .create(ProxyEndpoint.class, extractPath(request))
            .configurator(new ProxyEndpointConfigurator(proxyRequest, clientContainer))
            .subprotocols(proxyRequest.subprotocols())
            .build();

        try {
            serverContainer.upgradeHttpToWebSocket(request, response, endpointConfig, Map.of());
        } catch (DeploymentException e) {
            writeError(response, HttpServletResponse.SC_BAD_GATEWAY, "Code editor WebSocket route unavailable");
        }
    }

    URI buildUpstreamWebSocketUri(HttpServletRequest request, URI codeEditorBaseUri) {
        String scheme = codeEditorBaseUri.getScheme();
        String websocketScheme;
        if ("http".equalsIgnoreCase(scheme)) {
            websocketScheme = "ws";
        } else if ("https".equalsIgnoreCase(scheme)) {
            websocketScheme = "wss";
        } else {
            throw new IllegalArgumentException("Unsupported code editor WebSocket scheme: " + scheme);
        }

        return UriComponentsBuilder.fromUri(codeEditorBaseUri)
            .scheme(websocketScheme)
            .path(stripBasePath(extractPath(request), CODE_EDITOR_BASE_PATH))
            .replaceQuery(request.getQueryString())
            .build(true)
            .toUri();
    }

    Map<String, List<String>> buildProxyHeaders(HttpServletRequest request) {
        Map<String, List<String>> headers = new LinkedHashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (shouldSkipClientHeader(headerName)) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(headerName);
            while (values.hasMoreElements()) {
                headers.computeIfAbsent(headerName, ignored -> new ArrayList<>()).add(values.nextElement());
            }
        }

        headers.put("X-Forwarded-Proto", List.of(request.getScheme()));
        headers.put("X-Forwarded-Host", List.of(resolveForwardedHost(request)));
        headers.put("X-Forwarded-Port", List.of(String.valueOf(request.getServerPort())));
        headers.put("X-Forwarded-For", List.of(resolveForwardedFor(request)));
        headers.put(HttpHeaders.ORIGIN, List.of(resolveRequestOrigin(request)));
        return headers;
    }

    private boolean isWebSocketUpgradeRequest(HttpServletRequest request) {
        return headerContainsToken(request, HttpHeaders.CONNECTION, "upgrade")
            && headerContainsToken(request, HttpHeaders.UPGRADE, "websocket");
    }

    private boolean isCodeEditorPath(String path) {
        return CODE_EDITOR_BASE_PATH.equals(path) || path.startsWith(CODE_EDITOR_BASE_PATH + "/");
    }

    private boolean headerContainsToken(HttpServletRequest request, String headerName, String expectedToken) {
        Enumeration<String> values = request.getHeaders(headerName);
        while (values.hasMoreElements()) {
            String value = values.nextElement();
            for (String token : value.split(",")) {
                if (expectedToken.equalsIgnoreCase(token.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean shouldSkipClientHeader(String headerName) {
        String normalized = headerName.toLowerCase(Locale.ROOT);
        return EXCLUDED_HEADERS.contains(normalized)
            || WEB_SOCKET_HANDSHAKE_HEADERS.contains(normalized)
            || MANAGED_FORWARDED_HEADERS.contains(normalized);
    }

    private List<String> extractRequestedSubprotocols(HttpServletRequest request) {
        List<String> subprotocols = new ArrayList<>();
        Enumeration<String> values = request.getHeaders("Sec-WebSocket-Protocol");
        while (values.hasMoreElements()) {
            String value = values.nextElement();
            for (String protocol : value.split(",")) {
                if (StringUtils.hasText(protocol)) {
                    subprotocols.add(protocol.trim());
                }
            }
        }
        return subprotocols;
    }

    private String resolveForwardedHost(HttpServletRequest request) {
        String forwardedHost = request.getHeader(HttpHeaders.HOST);
        if (StringUtils.hasText(forwardedHost)) {
            return forwardedHost;
        }
        return request.getServerPort() > 0
            ? request.getServerName() + ":" + request.getServerPort()
            : request.getServerName();
    }

    private String resolveRequestOrigin(HttpServletRequest request) {
        String forwardedProto = firstHeaderValue(request.getHeader("X-Forwarded-Proto"));
        String scheme = StringUtils.hasText(forwardedProto) ? forwardedProto : request.getScheme();
        return scheme + "://" + resolveForwardedHost(request);
    }

    private String firstHeaderValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        int comma = value.indexOf(',');
        return comma == -1 ? value.trim() : value.substring(0, comma).trim();
    }

    private String resolveForwardedFor(HttpServletRequest request) {
        String existingForwardedFor = request.getHeader("X-Forwarded-For");
        return StringUtils.hasText(existingForwardedFor)
            ? existingForwardedFor + ", " + request.getRemoteAddr()
            : request.getRemoteAddr();
    }

    private String externalCodeEditorBasePath(HttpServletRequest request) {
        String forwardedPrefix = request.getHeader("X-Forwarded-Prefix");
        String prefix = StringUtils.hasText(forwardedPrefix) ? forwardedPrefix : request.getContextPath();
        if (!StringUtils.hasText(prefix) || "/".equals(prefix.trim())) {
            return CODE_EDITOR_BASE_PATH;
        }
        String normalizedPrefix = trimTrailingSlash(prefix.trim());
        if (normalizedPrefix.equals(CODE_EDITOR_BASE_PATH) || normalizedPrefix.endsWith(CODE_EDITOR_BASE_PATH)) {
            return normalizedPrefix;
        }
        return normalizedPrefix + CODE_EDITOR_BASE_PATH;
    }

    private String extractPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (!StringUtils.hasText(request.getContextPath())) {
            return requestUri;
        }
        return requestUri.substring(request.getContextPath().length());
    }

    private String stripBasePath(String path, String basePath) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }
        if (path.equals(basePath) || path.equals(basePath + "/")) {
            return "/";
        }
        if (path.startsWith(basePath + "/")) {
            return path.substring(basePath.length());
        }
        return path;
    }

    private String trimTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private ServerContainer resolveServerContainer(HttpServletRequest request) {
        if (request.getServletContext() == null) {
            return null;
        }
        Object container = request.getServletContext().getAttribute(ServerContainer.class.getName());
        return container instanceof ServerContainer serverContainer ? serverContainer : null;
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }

    private record ProxyRequest(
        URI upstreamUri,
        Map<String, List<String>> headers,
        List<String> subprotocols
    ) {
    }

    private static class ProxyEndpointConfigurator extends ServerEndpointConfig.Configurator {

        private final ProxyRequest proxyRequest;
        private final WebSocketContainer clientContainer;

        private ProxyEndpointConfigurator(ProxyRequest proxyRequest, WebSocketContainer clientContainer) {
            this.proxyRequest = proxyRequest;
            this.clientContainer = clientContainer;
        }

        @Override
        public <T> T getEndpointInstance(Class<T> endpointClass) {
            return endpointClass.cast(new ProxyEndpoint(proxyRequest, clientContainer));
        }
    }

    private static class ProxyEndpoint extends Endpoint {

        private final ProxyRequest proxyRequest;
        private final WebSocketContainer clientContainer;
        private final AtomicBoolean closing = new AtomicBoolean(false);
        private final List<PendingMessage> pendingDownstreamMessages = new ArrayList<>();
        private volatile Session upstreamSession;

        private ProxyEndpoint(ProxyRequest proxyRequest, WebSocketContainer clientContainer) {
            this.proxyRequest = proxyRequest;
            this.clientContainer = clientContainer;
        }

        @Override
        public void onOpen(Session downstreamSession, EndpointConfig config) {
            configureSessionBuffers(downstreamSession);
            registerDownstreamHandlers(downstreamSession);
            try {
                ClientEndpointConfig clientConfig = ClientEndpointConfig.Builder.create()
                    .configurator(new ProxyClientConfigurator(proxyRequest.headers()))
                    .preferredSubprotocols(proxyRequest.subprotocols())
                    .build();
                Session connectedUpstreamSession = clientContainer.connectToServer(
                    new UpstreamEndpoint(downstreamSession),
                    clientConfig,
                    proxyRequest.upstreamUri()
                );
                configureSessionBuffers(connectedUpstreamSession);
                upstreamSession = connectedUpstreamSession;
                flushPendingDownstreamMessages(connectedUpstreamSession);
            } catch (DeploymentException | IOException e) {
                closeQuietly(downstreamSession, upstreamUnavailableCloseReason());
            }
        }

        private void registerDownstreamHandlers(Session downstreamSession) {
            downstreamSession.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    sendTextToUpstream(message);
                }
            });
            downstreamSession.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                @Override
                public void onMessage(ByteBuffer message) {
                    sendBinaryToUpstream(message);
                }
            });
            downstreamSession.addMessageHandler(PongMessage.class, this::sendPongToUpstream);
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            closeUpstream(closeReason);
        }

        @Override
        public void onError(Session session, Throwable throwable) {
            closeUpstream(proxyFailureCloseReason());
        }

        private void closeUpstream(CloseReason closeReason) {
            if (closing.compareAndSet(false, true)) {
                closeQuietly(upstreamSession, closeReason);
            }
        }

        private void sendTextToUpstream(String message) {
            sendOrQueue(PendingMessage.text(message));
        }

        private void sendBinaryToUpstream(ByteBuffer message) {
            sendOrQueue(PendingMessage.binary(copy(message)));
        }

        private void sendPongToUpstream(PongMessage message) {
            sendOrQueue(PendingMessage.pong(copy(message.getApplicationData())));
        }

        private void sendOrQueue(PendingMessage message) {
            if (closing.get()) {
                return;
            }

            Session target = upstreamSession;
            if (target != null && target.isOpen()) {
                message.send(target);
                return;
            }

            synchronized (pendingDownstreamMessages) {
                target = upstreamSession;
                if (target != null && target.isOpen()) {
                    message.send(target);
                    return;
                }
                pendingDownstreamMessages.add(message);
            }
        }

        private void flushPendingDownstreamMessages(Session target) {
            List<PendingMessage> messages;
            synchronized (pendingDownstreamMessages) {
                messages = List.copyOf(pendingDownstreamMessages);
                pendingDownstreamMessages.clear();
            }
            messages.forEach(message -> message.send(target));
        }
    }

    private record PendingMessage(Type type, String text, ByteBuffer data) {

        private static PendingMessage text(String text) {
            return new PendingMessage(Type.TEXT, text, null);
        }

        private static PendingMessage binary(ByteBuffer data) {
            return new PendingMessage(Type.BINARY, null, data);
        }

        private static PendingMessage pong(ByteBuffer data) {
            return new PendingMessage(Type.PONG, null, data);
        }

        private void send(Session target) {
            if (type == Type.TEXT) {
                sendTextMessage(target, text);
            } else if (type == Type.BINARY) {
                sendBinaryMessage(target, data.asReadOnlyBuffer());
            } else {
                sendPongData(target, data.asReadOnlyBuffer());
            }
        }
    }

    private enum Type {
        TEXT,
        BINARY,
        PONG
    }

    private static class UpstreamEndpoint extends Endpoint {

        private final Session downstreamSession;

        private UpstreamEndpoint(Session downstreamSession) {
            this.downstreamSession = downstreamSession;
        }

        @Override
        public void onOpen(Session upstreamSession, EndpointConfig config) {
            configureSessionBuffers(upstreamSession);
            upstreamSession.addMessageHandler(new MessageHandler.Whole<String>() {
                @Override
                public void onMessage(String message) {
                    sendTextMessage(downstreamSession, message);
                }
            });
            upstreamSession.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {
                @Override
                public void onMessage(ByteBuffer message) {
                    sendBinaryMessage(downstreamSession, message);
                }
            });
            upstreamSession.addMessageHandler(PongMessage.class, message -> sendPong(downstreamSession, message));
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            closeQuietly(downstreamSession, closeReason);
        }

        @Override
        public void onError(Session session, Throwable throwable) {
            closeQuietly(downstreamSession, proxyFailureCloseReason());
        }
    }

    private static class ProxyClientConfigurator extends ClientEndpointConfig.Configurator {

        private final Map<String, List<String>> headers;

        private ProxyClientConfigurator(Map<String, List<String>> headers) {
            this.headers = headers;
        }

        @Override
        public void beforeRequest(Map<String, List<String>> requestHeaders) {
            requestHeaders.putAll(headers);
        }
    }

    private static void configureSessionBuffers(Session session) {
        if (session == null) {
            return;
        }
        session.setMaxTextMessageBufferSize(MAX_MESSAGE_BUFFER_SIZE);
        session.setMaxBinaryMessageBufferSize(MAX_MESSAGE_BUFFER_SIZE);
    }

    private static void sendTextMessage(Session session, String message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.getBasicRemote().sendText(message);
            }
        } catch (IOException e) {
            closeQuietly(session, proxyFailureCloseReason());
        }
    }

    private static void sendBinaryMessage(Session session, ByteBuffer message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            synchronized (session) {
                session.getBasicRemote().sendBinary(copy(message));
            }
        } catch (IOException e) {
            closeQuietly(session, proxyFailureCloseReason());
        }
    }

    private static void sendPong(Session session, PongMessage message) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.getBasicRemote().sendPong(copy(message.getApplicationData()));
        } catch (IOException e) {
            closeQuietly(session, proxyFailureCloseReason());
        }
    }

    private static void sendPongData(Session session, ByteBuffer applicationData) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.getBasicRemote().sendPong(copy(applicationData));
        } catch (IOException e) {
            closeQuietly(session, proxyFailureCloseReason());
        }
    }

    private static ByteBuffer copy(ByteBuffer source) {
        ByteBuffer sourceCopy = source.asReadOnlyBuffer();
        ByteBuffer copy = ByteBuffer.allocate(sourceCopy.remaining());
        copy.put(sourceCopy);
        copy.flip();
        return copy;
    }

    private static void closeQuietly(Session session, CloseReason closeReason) {
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            session.close(closeReason);
        } catch (IOException ignored) {
        }
    }

    private static CloseReason upstreamUnavailableCloseReason() {
        return new CloseReason(CloseCodes.TRY_AGAIN_LATER, "Upstream WebSocket unavailable");
    }

    private static CloseReason proxyFailureCloseReason() {
        return new CloseReason(CloseCodes.UNEXPECTED_CONDITION, "WebSocket proxy failure");
    }
}
