package com.redis.workshop.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;

final class SessionRuntimeDiagnosticsEndpointResolver {

    private static final String DIAGNOSTICS_PATH_TEMPLATE = "/internal/execution-plane/sessions/{sessionId}/runtime/diagnostics";
    private static final String RESTORE_PATH_TEMPLATE = "/internal/execution-plane/sessions/{sessionId}/runtime/restore";

    Optional<URI> resolveDiagnosticsEndpoint(SessionRuntimeResolver runtimeResolver, HttpServletRequest request, String sessionId) {
        return resolveEndpoint(runtimeResolver, request, sessionId, DIAGNOSTICS_PATH_TEMPLATE);
    }

    Optional<URI> resolveRestoreEndpoint(SessionRuntimeResolver runtimeResolver, String sessionId) {
        return resolveEndpoint(runtimeResolver, null, sessionId, RESTORE_PATH_TEMPLATE);
    }

    private Optional<URI> resolveEndpoint(
        SessionRuntimeResolver runtimeResolver,
        HttpServletRequest request,
        String sessionId,
        String pathTemplate
    ) {
        if (!StringUtils.hasText(sessionId)) {
            return Optional.empty();
        }

        Optional<URI> publicBaseUri = runtimeResolver == null ? Optional.empty() : runtimeResolver.resolveSessionRuntimeBaseUri();
        if (publicBaseUri.isEmpty()) {
            if (request == null) {
                return Optional.empty();
            }
            publicBaseUri = resolvePublicBaseUri(request);
            if (publicBaseUri.isEmpty() || isSameAuthority(publicBaseUri.get(), request)) {
                return Optional.empty();
            }
        }

        URI diagnosticsUri = UriComponentsBuilder.fromUri(publicBaseUri.get())
            .replacePath(pathTemplate)
            .replaceQuery(null)
            .fragment(null)
            .buildAndExpand(sessionId)
            .toUri();
        return Optional.of(diagnosticsUri);
    }

    private Optional<URI> resolvePublicBaseUri(HttpServletRequest request) {
        Optional<URI> originUri = parseAbsoluteUri(request.getHeader("Origin"));
        if (originUri.isPresent()) {
            return Optional.of(stripToAuthority(originUri.get()));
        }

        Optional<URI> refererUri = parseAbsoluteUri(request.getHeader("Referer"));
        return refererUri.map(this::stripToAuthority);
    }

    private Optional<URI> parseAbsoluteUri(String rawValue) {
        if (!StringUtils.hasText(rawValue)) {
            return Optional.empty();
        }
        try {
            URI uri = URI.create(rawValue.trim());
            if (!uri.isAbsolute() || !StringUtils.hasText(uri.getHost())) {
                return Optional.empty();
            }
            return Optional.of(uri);
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    private URI stripToAuthority(URI uri) {
        return UriComponentsBuilder.newInstance()
            .scheme(uri.getScheme())
            .host(uri.getHost())
            .port(uri.getPort())
            .build()
            .toUri();
    }

    private boolean isSameAuthority(URI candidate, HttpServletRequest request) {
        String requestHost = request.getServerName();
        if (!StringUtils.hasText(requestHost) || !requestHost.equalsIgnoreCase(candidate.getHost())) {
            return false;
        }
        return resolvePort(candidate) == resolvePort(request);
    }

    private int resolvePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private int resolvePort(HttpServletRequest request) {
        int serverPort = request.getServerPort();
        if (serverPort > 0) {
            return serverPort;
        }
        return request.isSecure() ? 443 : 80;
    }
}
