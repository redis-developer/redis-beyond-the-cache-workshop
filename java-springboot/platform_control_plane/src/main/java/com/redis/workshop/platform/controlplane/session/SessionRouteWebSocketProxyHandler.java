package com.redis.workshop.platform.controlplane.session;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

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

@Component
public class SessionRouteWebSocketProxyHandler {

    private static final String SESSION_ROUTE_PREFIX = "/session/";
    private static final int MAX_MESSAGE_BUFFER_SIZE = 1024 * 1024;
    private static final Set<String> WEBSOCKET_HANDSHAKE_HEADERS = Set.of(
        "sec-websocket-accept",
        "sec-websocket-extensions",
        "sec-websocket-key",
        "sec-websocket-protocol",
        "sec-websocket-version"
    );

    private final SessionRouteProxyService sessionRouteProxyService;
    private final WebSocketContainer clientContainer;

    @Autowired
    public SessionRouteWebSocketProxyHandler(SessionRouteProxyService sessionRouteProxyService) {
        this(sessionRouteProxyService, ContainerProvider.getWebSocketContainer());
    }

    SessionRouteWebSocketProxyHandler(
        SessionRouteProxyService sessionRouteProxyService,
        WebSocketContainer clientContainer
    ) {
        this.sessionRouteProxyService = sessionRouteProxyService;
        this.clientContainer = clientContainer;
        this.clientContainer.setDefaultMaxTextMessageBufferSize(MAX_MESSAGE_BUFFER_SIZE);
        this.clientContainer.setDefaultMaxBinaryMessageBufferSize(MAX_MESSAGE_BUFFER_SIZE);
    }

    public boolean canHandle(HttpServletRequest request) {
        return isWebSocketUpgradeRequest(request) && extractSessionId(request).isPresent();
    }

    public void handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        Optional<String> sessionId = extractSessionId(request);
        if (sessionId.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        SessionRouteProxyService.RouteResolution routeResolution =
            sessionRouteProxyService.resolveRoute(sessionId.get(), request);
        if (!routeResolution.routable()) {
            writeRejection(response, routeResolution.rejection());
            return;
        }

        SessionRouteProxyService.RouteTarget routeTarget = routeResolution.target();
        URI upstreamUri;
        try {
            upstreamUri = webSocketUri(routeTarget.targetUrl());
        } catch (IllegalArgumentException exception) {
            writeBadGateway(response, "Session route has an unsupported WebSocket upstream target");
            return;
        }

        ServerContainer serverContainer = serverContainer(request);
        if (serverContainer == null) {
            writeBadGateway(response, "WebSocket server container unavailable");
            return;
        }

        ProxyRequest proxyRequest = new ProxyRequest(
            upstreamUri,
            upstreamRequestHeaders(request, routeTarget.basePath()),
            requestedSubprotocols(request)
        );

        ServerEndpointConfig endpointConfig = ServerEndpointConfig.Builder
            .create(ProxyEndpoint.class, "/session/{sessionId}")
            .subprotocols(proxyRequest.subprotocols())
            .configurator(new ProxyEndpointConfigurator(proxyRequest, clientContainer))
            .build();
        try {
            serverContainer.upgradeHttpToWebSocket(
                request,
                response,
                endpointConfig,
                Map.of("sessionId", sessionId.get())
            );
        } catch (DeploymentException exception) {
            writeBadGateway(response, "WebSocket route unavailable: " + exception.getMessage());
        }
    }

    boolean isWebSocketUpgradeRequest(HttpServletRequest request) {
        return "GET".equalsIgnoreCase(request.getMethod())
            && headerContainsToken(request, HttpHeaders.UPGRADE, "websocket")
            && headerContainsToken(request, HttpHeaders.CONNECTION, "upgrade");
    }

    Optional<String> extractSessionId(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (requestUri == null || !requestUri.startsWith(SESSION_ROUTE_PREFIX)) {
            return Optional.empty();
        }

        int start = SESSION_ROUTE_PREFIX.length();
        int slash = requestUri.indexOf('/', start);
        String sessionId = slash == -1 ? requestUri.substring(start) : requestUri.substring(start, slash);
        return StringUtils.hasText(sessionId) ? Optional.of(sessionId) : Optional.empty();
    }

    URI webSocketUri(String targetUrl) {
        URI targetUri = URI.create(targetUrl);
        String scheme = targetUri.getScheme();
        if ("ws".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme)) {
            return targetUri;
        }
        if ("http".equalsIgnoreCase(scheme)) {
            return replaceScheme(targetUrl, scheme, "ws");
        }
        if ("https".equalsIgnoreCase(scheme)) {
            return replaceScheme(targetUrl, scheme, "wss");
        }
        throw new IllegalArgumentException("Unsupported WebSocket upstream scheme: " + scheme);
    }

    Map<String, List<String>> upstreamRequestHeaders(HttpServletRequest request, String basePath) {
        HttpHeaders httpHeaders = sessionRouteProxyService.requestHeaders(request, basePath);
        Map<String, List<String>> headers = new LinkedHashMap<>();
        httpHeaders.forEach((name, values) -> {
            if (!isWebSocketHandshakeHeader(name)) {
                headers.put(name, List.copyOf(values));
            }
        });
        return headers;
    }

    private URI replaceScheme(String targetUrl, String existingScheme, String replacementScheme) {
        return URI.create(replacementScheme + targetUrl.substring(existingScheme.length()));
    }

    private ServerContainer serverContainer(HttpServletRequest request) {
        Object container = request.getServletContext().getAttribute(ServerContainer.class.getName());
        return container instanceof ServerContainer serverContainer ? serverContainer : null;
    }

    private void writeRejection(HttpServletResponse response, ResponseEntity<byte[]> rejection) throws IOException {
        response.setStatus(rejection.getStatusCode().value());
        rejection.getHeaders().forEach((name, values) -> values.forEach(value -> response.addHeader(name, value)));
        byte[] body = rejection.getBody();
        if (body != null && body.length > 0) {
            response.getOutputStream().write(body);
        }
    }

    private void writeBadGateway(HttpServletResponse response, String message) throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(HttpStatus.BAD_GATEWAY.value());
        response.getOutputStream().write(message.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    private boolean isWebSocketHandshakeHeader(String headerName) {
        return headerName != null && WEBSOCKET_HANDSHAKE_HEADERS.contains(headerName.toLowerCase(Locale.ROOT));
    }

    private boolean headerContainsToken(HttpServletRequest request, String headerName, String token) {
        Enumeration<String> headers = request.getHeaders(headerName);
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            for (String value : header.split(",")) {
                if (token.equalsIgnoreCase(value.trim())) {
                    return true;
                }
            }
        }
        return false;
    }

    private List<String> requestedSubprotocols(HttpServletRequest request) {
        List<String> subprotocols = new ArrayList<>();
        Enumeration<String> headers = request.getHeaders("Sec-WebSocket-Protocol");
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            for (String value : header.split(",")) {
                String subprotocol = value.trim();
                if (StringUtils.hasText(subprotocol)) {
                    subprotocols.add(subprotocol);
                }
            }
        }
        return subprotocols;
    }

    private record ProxyRequest(
        URI upstreamUri,
        Map<String, List<String>> headers,
        List<String> subprotocols
    ) {
    }

    public static final class ProxyEndpoint extends Endpoint {

        private final ProxyRequest proxyRequest;
        private final WebSocketContainer clientContainer;
        private ProxyBridge bridge;

        private ProxyEndpoint(ProxyRequest proxyRequest, WebSocketContainer clientContainer) {
            this.proxyRequest = proxyRequest;
            this.clientContainer = clientContainer;
        }

        @Override
        public void onOpen(Session downstreamSession, EndpointConfig config) {
            configureSessionBuffers(downstreamSession);
            bridge = new ProxyBridge(downstreamSession, proxyRequest, clientContainer);
            bridge.open();
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            if (bridge != null) {
                bridge.closeFromDownstream(closeReason);
            }
        }

        @Override
        public void onError(Session session, Throwable throwable) {
            if (bridge != null) {
                bridge.closeBoth(unexpectedCloseReason());
            }
        }
    }

    private static final class ProxyEndpointConfigurator extends ServerEndpointConfig.Configurator {

        private final ProxyRequest proxyRequest;
        private final WebSocketContainer clientContainer;

        private ProxyEndpointConfigurator(ProxyRequest proxyRequest, WebSocketContainer clientContainer) {
            this.proxyRequest = proxyRequest;
            this.clientContainer = clientContainer;
        }

        @Override
        public <T> T getEndpointInstance(Class<T> endpointClass) throws InstantiationException {
            if (endpointClass == ProxyEndpoint.class) {
                return endpointClass.cast(new ProxyEndpoint(proxyRequest, clientContainer));
            }
            return super.getEndpointInstance(endpointClass);
        }
    }

    private static final class HeaderForwardingConfigurator extends ClientEndpointConfig.Configurator {

        private final Map<String, List<String>> headers;

        private HeaderForwardingConfigurator(Map<String, List<String>> headers) {
            this.headers = headers;
        }

        @Override
        public void beforeRequest(Map<String, List<String>> requestHeaders) {
            requestHeaders.putAll(headers);
        }
    }

    private static final class ProxyBridge {

        private final Session downstreamSession;
        private final ProxyRequest proxyRequest;
        private final WebSocketContainer clientContainer;
        private final AtomicBoolean closing = new AtomicBoolean(false);
        private final List<PendingMessage> pendingDownstreamMessages = new ArrayList<>();
        private volatile Session upstreamSession;

        private ProxyBridge(
            Session downstreamSession,
            ProxyRequest proxyRequest,
            WebSocketContainer clientContainer
        ) {
            this.downstreamSession = downstreamSession;
            this.proxyRequest = proxyRequest;
            this.clientContainer = clientContainer;
        }

        private void open() {
            registerDownstreamHandlers();
            ClientEndpointConfig clientConfig = ClientEndpointConfig.Builder.create()
                .preferredSubprotocols(proxyRequest.subprotocols())
                .configurator(new HeaderForwardingConfigurator(proxyRequest.headers()))
                .build();
            try {
                Session connectedUpstreamSession = clientContainer.connectToServer(
                    new UpstreamEndpoint(this),
                    clientConfig,
                    proxyRequest.upstreamUri()
                );
                configureSessionBuffers(connectedUpstreamSession);
                upstreamSession = connectedUpstreamSession;
                flushPendingDownstreamMessages(connectedUpstreamSession);
            } catch (DeploymentException | IOException exception) {
                closeBoth(unavailableCloseReason());
            }
        }

        private void registerDownstreamHandlers() {
            downstreamSession.addMessageHandler(
                String.class,
                (MessageHandler.Whole<String>) this::sendTextToUpstream
            );
            downstreamSession.addMessageHandler(
                ByteBuffer.class,
                (MessageHandler.Whole<ByteBuffer>) this::sendBinaryToUpstream
            );
            downstreamSession.addMessageHandler(PongMessage.class, (MessageHandler.Whole<PongMessage>) message ->
                sendPongToUpstream(message.getApplicationData())
            );
        }

        private void registerUpstreamHandlers(Session connectedUpstreamSession) {
            connectedUpstreamSession.addMessageHandler(
                String.class,
                (MessageHandler.Whole<String>) message -> sendText(downstreamSession, message)
            );
            connectedUpstreamSession.addMessageHandler(
                ByteBuffer.class,
                (MessageHandler.Whole<ByteBuffer>) message -> sendBinary(downstreamSession, message)
            );
            connectedUpstreamSession.addMessageHandler(PongMessage.class, (MessageHandler.Whole<PongMessage>) message ->
                sendPong(downstreamSession, message.getApplicationData())
            );
        }

        private void closeFromDownstream(CloseReason closeReason) {
            if (closing.compareAndSet(false, true)) {
                closePeer(upstreamSession, closeReason);
            }
        }

        private void closeFromUpstream(CloseReason closeReason) {
            if (closing.compareAndSet(false, true)) {
                closePeer(downstreamSession, closeReason);
            }
        }

        private void closeBoth(CloseReason closeReason) {
            if (closing.compareAndSet(false, true)) {
                closePeer(upstreamSession, closeReason);
                closePeer(downstreamSession, closeReason);
            }
        }

        private void sendTextToUpstream(String message) {
            sendOrQueue(PendingMessage.text(message));
        }

        private void sendBinaryToUpstream(ByteBuffer message) {
            sendOrQueue(PendingMessage.binary(copy(message)));
        }

        private void sendPongToUpstream(ByteBuffer applicationData) {
            sendOrQueue(PendingMessage.pong(copy(applicationData)));
        }

        private void sendOrQueue(PendingMessage message) {
            if (closing.get()) {
                return;
            }

            Session target = upstreamSession;
            if (target != null && target.isOpen()) {
                message.send(this, target);
                return;
            }

            synchronized (pendingDownstreamMessages) {
                target = upstreamSession;
                if (target != null && target.isOpen()) {
                    message.send(this, target);
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
            messages.forEach(message -> message.send(this, target));
        }

        private void sendText(Session target, String message) {
            if (target == null || !target.isOpen()) {
                return;
            }
            try {
                synchronized (target) {
                    target.getBasicRemote().sendText(message);
                }
            } catch (IOException | RuntimeException exception) {
                closeBoth(unexpectedCloseReason());
            }
        }

        private void sendBinary(Session target, ByteBuffer message) {
            if (target == null || !target.isOpen()) {
                return;
            }
            try {
                synchronized (target) {
                    target.getBasicRemote().sendBinary(copy(message));
                }
            } catch (IOException | RuntimeException exception) {
                closeBoth(unexpectedCloseReason());
            }
        }

        private void sendPong(Session target, ByteBuffer applicationData) {
            if (target == null || !target.isOpen()) {
                return;
            }
            try {
                synchronized (target) {
                    target.getBasicRemote().sendPong(copy(applicationData));
                }
            } catch (IOException | RuntimeException exception) {
                closeBoth(unexpectedCloseReason());
            }
        }

        private ByteBuffer copy(ByteBuffer source) {
            ByteBuffer sourceCopy = source.asReadOnlyBuffer();
            ByteBuffer copy = ByteBuffer.allocate(sourceCopy.remaining());
            copy.put(sourceCopy);
            copy.flip();
            return copy;
        }

        private void closePeer(Session session, CloseReason closeReason) {
            if (session == null || !session.isOpen()) {
                return;
            }
            try {
                session.close(closeReason);
            } catch (IOException ignored) {
            }
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

        private void send(ProxyBridge bridge, Session target) {
            if (type == Type.TEXT) {
                bridge.sendText(target, text);
            } else if (type == Type.BINARY) {
                bridge.sendBinary(target, data.asReadOnlyBuffer());
            } else {
                bridge.sendPong(target, data.asReadOnlyBuffer());
            }
        }
    }

    private enum Type {
        TEXT,
        BINARY,
        PONG
    }

    private static final class UpstreamEndpoint extends Endpoint {

        private final ProxyBridge bridge;

        private UpstreamEndpoint(ProxyBridge bridge) {
            this.bridge = bridge;
        }

        @Override
        public void onOpen(Session upstreamSession, EndpointConfig config) {
            configureSessionBuffers(upstreamSession);
            bridge.registerUpstreamHandlers(upstreamSession);
        }

        @Override
        public void onClose(Session session, CloseReason closeReason) {
            bridge.closeFromUpstream(closeReason);
        }

        @Override
        public void onError(Session session, Throwable throwable) {
            bridge.closeBoth(unexpectedCloseReason());
        }
    }

    private static CloseReason unavailableCloseReason() {
        return new CloseReason(CloseCodes.TRY_AGAIN_LATER, "Upstream WebSocket unavailable");
    }

    private static void configureSessionBuffers(Session session) {
        if (session == null) {
            return;
        }
        session.setMaxTextMessageBufferSize(MAX_MESSAGE_BUFFER_SIZE);
        session.setMaxBinaryMessageBufferSize(MAX_MESSAGE_BUFFER_SIZE);
    }

    private static CloseReason unexpectedCloseReason() {
        return new CloseReason(CloseCodes.UNEXPECTED_CONDITION, "WebSocket proxy failure");
    }
}
