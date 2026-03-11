package com.redis.workshop.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@RestController
@ConditionalOnProperty(name = "workshop.backend.url")
public class BackendProxyController {

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
        "content-length"
    );

    private final FrontendRuntimeProperties runtimeProperties;
    private final HttpClient httpClient;

    @Autowired
    public BackendProxyController(FrontendRuntimeProperties runtimeProperties) {
        this(runtimeProperties, HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build());
    }

    BackendProxyController(FrontendRuntimeProperties runtimeProperties, HttpClient httpClient) {
        this.runtimeProperties = runtimeProperties;
        this.httpClient = httpClient;
    }

    @RequestMapping(
        value = "/api/**",
        method = {
            RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.PATCH,
            RequestMethod.DELETE,
            RequestMethod.OPTIONS,
            RequestMethod.HEAD
        }
    )
    public ResponseEntity<byte[]> proxyRequest(HttpServletRequest request, @RequestBody(required = false) byte[] body) {
        String requestPath = extractPath(request);
        if (requestPath.equals("/api/editor")
            || requestPath.startsWith("/api/editor/")
            || requestPath.equals("/api/content")
            || requestPath.startsWith("/api/content/")) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        Optional<URI> backendUri = runtimeProperties.resolveBackendUri();
        if (backendUri.isEmpty()) {
            byte[] errorBody = "{\"error\":\"Backend URL is not configured\"}".getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody);
        }

        URI targetUri = buildTargetUri(backendUri.get(), request, requestPath);
        HttpRequest outboundRequest = buildOutboundRequest(request, targetUri, body);

        try {
            HttpResponse<byte[]> backendResponse = httpClient.send(outboundRequest, HttpResponse.BodyHandlers.ofByteArray());
            return toResponseEntity(backendResponse, backendUri.get(), request);
        } catch (IOException e) {
            return proxyFailureResponse();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return proxyFailureResponse();
        }
    }

    private URI buildTargetUri(URI backendBaseUri, HttpServletRequest request, String requestPath) {
        return UriComponentsBuilder.fromUri(backendBaseUri)
            .path(requestPath)
            .replaceQuery(request.getQueryString())
            .build(true)
            .toUri();
    }

    private HttpRequest buildOutboundRequest(HttpServletRequest request, URI targetUri, byte[] body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(targetUri)
            .method(request.getMethod(), bodyPublisherFor(body));

        copyInboundHeaders(request, builder);
        return builder.build();
    }

    private void copyInboundHeaders(HttpServletRequest request, HttpRequest.Builder builder) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (isExcludedHeader(headerName)) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(headerName);
            while (values.hasMoreElements()) {
                builder.header(headerName, values.nextElement());
            }
        }

        builder.header("X-Forwarded-Proto", request.getScheme());
        String forwardedHost = request.getHeader("Host");
        if (!StringUtils.hasText(forwardedHost)) {
            forwardedHost = request.getServerPort() > 0
                ? request.getServerName() + ":" + request.getServerPort()
                : request.getServerName();
        }
        builder.header("X-Forwarded-Host", forwardedHost);
        builder.header("X-Forwarded-Port", String.valueOf(request.getServerPort()));
        if (StringUtils.hasText(request.getContextPath())) {
            builder.header("X-Forwarded-Prefix", request.getContextPath());
        }

        String existingForwardedFor = request.getHeader("X-Forwarded-For");
        String forwardedFor = StringUtils.hasText(existingForwardedFor)
            ? existingForwardedFor + ", " + request.getRemoteAddr()
            : request.getRemoteAddr();
        builder.header("X-Forwarded-For", forwardedFor);
    }

    private HttpRequest.BodyPublisher bodyPublisherFor(byte[] body) {
        if (body == null || body.length == 0) {
            return HttpRequest.BodyPublishers.noBody();
        }
        return HttpRequest.BodyPublishers.ofByteArray(body);
    }

    private ResponseEntity<byte[]> toResponseEntity(
        HttpResponse<byte[]> backendResponse,
        URI backendBaseUri,
        HttpServletRequest request
    ) {
        HttpHeaders responseHeaders = new HttpHeaders();
        for (Map.Entry<String, List<String>> entry : backendResponse.headers().map().entrySet()) {
            String headerName = entry.getKey();
            if (isExcludedHeader(headerName)) {
                continue;
            }
            if (HttpHeaders.LOCATION.equalsIgnoreCase(headerName)) {
                for (String value : entry.getValue()) {
                    responseHeaders.add(HttpHeaders.LOCATION, rewriteLocation(value, backendBaseUri, request));
                }
                continue;
            }
            if (HttpHeaders.SET_COOKIE.equalsIgnoreCase(headerName)) {
                for (String value : entry.getValue()) {
                    responseHeaders.add(HttpHeaders.SET_COOKIE, rewriteSetCookie(value));
                }
                continue;
            }
            for (String value : entry.getValue()) {
                responseHeaders.add(headerName, value);
            }
        }

        HttpStatus status = HttpStatus.resolve(backendResponse.statusCode());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        return new ResponseEntity<>(backendResponse.body(), responseHeaders, status);
    }

    private String rewriteLocation(String location, URI backendBaseUri, HttpServletRequest request) {
        if (!StringUtils.hasText(location)) {
            return location;
        }

        URI locationUri;
        try {
            locationUri = URI.create(location);
        } catch (IllegalArgumentException e) {
            return location;
        }

        if (!locationUri.isAbsolute() || !isSameAuthority(locationUri, backendBaseUri)) {
            return location;
        }

        return ServletUriComponentsBuilder.fromRequestUri(request)
            .replacePath(locationUri.getPath())
            .replaceQuery(locationUri.getRawQuery())
            .fragment(locationUri.getRawFragment())
            .build(true)
            .toUriString();
    }

    private String rewriteSetCookie(String setCookieHeader) {
        String[] parts = setCookieHeader.split(";");
        List<String> rewritten = new ArrayList<>();
        boolean hasPath = false;

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i].trim();
            if (i == 0) {
                rewritten.add(part);
                continue;
            }

            String lowerPart = part.toLowerCase(Locale.ROOT);
            if (lowerPart.startsWith("domain=")) {
                continue;
            }
            if (lowerPart.startsWith("path=")) {
                rewritten.add("Path=/");
                hasPath = true;
                continue;
            }
            rewritten.add(part);
        }

        if (!hasPath) {
            rewritten.add("Path=/");
        }
        return String.join("; ", rewritten);
    }

    private boolean isSameAuthority(URI candidate, URI backendBaseUri) {
        if (!StringUtils.hasText(candidate.getHost()) || !StringUtils.hasText(backendBaseUri.getHost())) {
            return false;
        }
        return candidate.getHost().equalsIgnoreCase(backendBaseUri.getHost())
            && resolvePort(candidate) == resolvePort(backendBaseUri);
    }

    private int resolvePort(URI uri) {
        if (uri.getPort() > 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private ResponseEntity<byte[]> proxyFailureResponse() {
        byte[] errorBody = "{\"error\":\"Failed to reach backend service\"}".getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
            .contentType(MediaType.APPLICATION_JSON)
            .body(errorBody);
    }

    private String extractPath(HttpServletRequest request) {
        String requestUri = request.getRequestURI();
        if (!StringUtils.hasText(request.getContextPath())) {
            return requestUri;
        }
        return requestUri.substring(request.getContextPath().length());
    }

    private boolean isExcludedHeader(String headerName) {
        return EXCLUDED_HEADERS.contains(headerName.toLowerCase(Locale.ROOT));
    }
}
