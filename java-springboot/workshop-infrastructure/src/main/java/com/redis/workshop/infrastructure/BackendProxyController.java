package com.redis.workshop.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@ConditionalOnExpression(
    "T(org.springframework.util.StringUtils).hasText('${workshop.frontend.session-backend-url:}')"
        + " || T(org.springframework.util.StringUtils).hasText('${workshop.frontend.backend-url:}')"
        + " || T(org.springframework.util.StringUtils).hasText('${workshop.backend.url:}')"
        + " || T(org.springframework.util.StringUtils).hasText('${WORKSHOP_SESSION_BACKEND_URL:}')"
        + " || T(org.springframework.util.StringUtils).hasText('${WORKSHOP_BACKEND_URL:}')"
        + " || '${workshop.session-runner.enabled:false}' == 'true'"
)
public class BackendProxyController {

    private static final String REDIS_INSIGHT_BASE_PATH = "/redis-insight";
    private static final String CODE_EDITOR_BASE_PATH = "/code";
    private static final Pattern ROOT_ATTRIBUTE_REFERENCE = Pattern.compile(
        "(?i)(\\b(?:href|src|action|content)=([\"']))/(?!/)"
    );
    private static final Pattern ROOT_CSS_URL_REFERENCE = Pattern.compile("(?i)(url\\((?:[\"']?))/(?!/)");

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
        "content-length",
        "accept-encoding",
        "x-frame-options"
    );

    private final SessionRuntimeResolver runtimeResolver;
    private final HttpClient httpClient;
    private final SessionRunnerProperties runnerProperties;
    private final Optional<LocalSessionRunnerManager> runnerManager;

    @Autowired
    public BackendProxyController(
        FrontendRuntimeProperties runtimeProperties,
        SessionRunnerProperties runnerProperties,
        Optional<LocalSessionRunnerManager> runnerManager
    ) {
        this(
            new SessionRuntimeResolver(runtimeProperties, runnerProperties),
            HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .version(HttpClient.Version.HTTP_1_1)
                .build(),
            runnerProperties,
            runnerManager
        );
    }

    BackendProxyController(FrontendRuntimeProperties runtimeProperties, HttpClient httpClient) {
        this(new SessionRuntimeResolver(runtimeProperties), httpClient, null, Optional.empty());
    }

    BackendProxyController(SessionRuntimeResolver runtimeResolver, HttpClient httpClient) {
        this(runtimeResolver, httpClient, null, Optional.empty());
    }

    BackendProxyController(
        SessionRuntimeResolver runtimeResolver,
        HttpClient httpClient,
        SessionRunnerProperties runnerProperties,
        Optional<LocalSessionRunnerManager> runnerManager
    ) {
        this.runtimeResolver = runtimeResolver;
        this.httpClient = httpClient;
        this.runnerProperties = runnerProperties;
        this.runnerManager = runnerManager == null ? Optional.empty() : runnerManager;
    }

    @RequestMapping(
        value = {
            "/api/**",
            "/app",
            "/app/",
            "/app/**",
            "/login",
            "/login/**",
            "/logout",
            "/logout/**"
        },
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

        Optional<URI> backendUri = runtimeResolver.resolveBackendUri();
        if (backendUri.isEmpty()) {
            byte[] errorBody = "{\"error\":\"Backend URL is not configured\"}".getBytes(StandardCharsets.UTF_8);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody);
        }

        Optional<ResponseEntity<byte[]>> runnerUnavailable = runnerUnavailableResponse(backendUri.get());
        if (runnerUnavailable.isPresent()) {
            return runnerUnavailable.get();
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

    @RequestMapping(
        value = {
            "/redis-insight",
            "/redis-insight/",
            "/redis-insight/**"
        },
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
    public ResponseEntity<byte[]> proxyRedisInsight(
        HttpServletRequest request,
        @RequestBody(required = false) byte[] body
    ) {
        if (runnerProperties == null || !runnerProperties.isEnabled()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Optional<URI> redisInsightUri = runnerProperties.resolveLocalRedisInsightUri();
        if (redisInsightUri.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        URI targetUri = buildTargetUri(redisInsightUri.get(), request, extractPath(request));
        HttpRequest outboundRequest = buildOutboundRequest(request, targetUri, body);

        try {
            HttpResponse<byte[]> redisInsightResponse = httpClient.send(
                outboundRequest,
                HttpResponse.BodyHandlers.ofByteArray()
            );
            return toRedisInsightResponse(redisInsightResponse, request);
        } catch (IOException e) {
            return proxyFailureResponse();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return proxyFailureResponse();
        }
    }

    @RequestMapping(
        value = {
            "/code",
            "/code/",
            "/code/**"
        },
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
    public ResponseEntity<byte[]> proxyCodeEditor(
        HttpServletRequest request,
        @RequestBody(required = false) byte[] body
    ) {
        if (runnerProperties == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Optional<URI> codeEditorUri = runnerProperties.resolveLocalCodeEditorUri();
        if (codeEditorUri.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        URI targetUri = buildTargetUri(
            codeEditorUri.get(),
            request,
            stripBasePath(extractPath(request), CODE_EDITOR_BASE_PATH)
        );
        HttpRequest outboundRequest = buildOutboundRequest(
            request,
            targetUri,
            body,
            null,
            false
        );

        try {
            HttpResponse<byte[]> codeEditorResponse = httpClient.send(
                outboundRequest,
                HttpResponse.BodyHandlers.ofByteArray()
            );
            return toCodeEditorResponse(codeEditorResponse, codeEditorUri.get(), request);
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
        return buildOutboundRequest(request, targetUri, body, null);
    }

    private HttpRequest buildOutboundRequest(
        HttpServletRequest request,
        URI targetUri,
        byte[] body,
        String forwardedPrefixOverride
    ) {
        return buildOutboundRequest(request, targetUri, body, forwardedPrefixOverride, true);
    }

    private HttpRequest buildOutboundRequest(
        HttpServletRequest request,
        URI targetUri,
        byte[] body,
        String forwardedPrefixOverride,
        boolean includeForwardedAuthority
    ) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(targetUri)
            .method(request.getMethod(), bodyPublisherFor(body));

        copyInboundHeaders(request, builder, forwardedPrefixOverride, includeForwardedAuthority);
        return builder.build();
    }

    private void copyInboundHeaders(
        HttpServletRequest request,
        HttpRequest.Builder builder,
        String forwardedPrefixOverride,
        boolean includeForwardedAuthority
    ) {
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (isExcludedHeader(headerName)) {
                continue;
            }
            if (!includeForwardedAuthority && isForwardedAuthorityHeader(headerName)) {
                continue;
            }
            if (StringUtils.hasText(forwardedPrefixOverride)
                && "X-Forwarded-Prefix".equalsIgnoreCase(headerName)) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(headerName);
            while (values.hasMoreElements()) {
                builder.header(headerName, values.nextElement());
            }
        }

        if (includeForwardedAuthority) {
            builder.header("X-Forwarded-Proto", request.getScheme());
            String forwardedHost = request.getHeader("Host");
            if (!StringUtils.hasText(forwardedHost)) {
                forwardedHost = request.getServerPort() > 0
                    ? request.getServerName() + ":" + request.getServerPort()
                    : request.getServerName();
            }
            builder.header("X-Forwarded-Host", forwardedHost);
            builder.header("X-Forwarded-Port", String.valueOf(request.getServerPort()));
        }
        if (StringUtils.hasText(forwardedPrefixOverride)) {
            builder.header("X-Forwarded-Prefix", forwardedPrefixOverride);
        } else if (StringUtils.hasText(request.getContextPath())) {
            builder.header("X-Forwarded-Prefix", request.getContextPath());
        }

        if (includeForwardedAuthority) {
            String existingForwardedFor = request.getHeader("X-Forwarded-For");
            String forwardedFor = StringUtils.hasText(existingForwardedFor)
                ? existingForwardedFor + ", " + request.getRemoteAddr()
                : request.getRemoteAddr();
            builder.header("X-Forwarded-For", forwardedFor);
        }
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

    private ResponseEntity<byte[]> toRedisInsightResponse(
        HttpResponse<byte[]> redisInsightResponse,
        HttpServletRequest request
    ) {
        HttpHeaders responseHeaders = new HttpHeaders();
        for (Map.Entry<String, List<String>> entry : redisInsightResponse.headers().map().entrySet()) {
            String headerName = entry.getKey();
            if (isExcludedHeader(headerName)) {
                continue;
            }
            if (HttpHeaders.LOCATION.equalsIgnoreCase(headerName)) {
                for (String value : entry.getValue()) {
                    responseHeaders.add(HttpHeaders.LOCATION, rewriteRedisInsightLocation(value, request));
                }
                continue;
            }
            if (HttpHeaders.SET_COOKIE.equalsIgnoreCase(headerName)) {
                for (String value : entry.getValue()) {
                    responseHeaders.add(
                        HttpHeaders.SET_COOKIE,
                        rewriteCookie(value, externalRedisInsightBasePath(request))
                    );
                }
                continue;
            }
            for (String value : entry.getValue()) {
                responseHeaders.add(headerName, value);
            }
        }

        byte[] responseBody = rewriteRedisInsightBody(redisInsightResponse.body(), responseHeaders, request);
        HttpStatus status = HttpStatus.resolve(redisInsightResponse.statusCode());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        return new ResponseEntity<>(responseBody, responseHeaders, status);
    }

    private ResponseEntity<byte[]> toCodeEditorResponse(
        HttpResponse<byte[]> codeEditorResponse,
        URI codeEditorBaseUri,
        HttpServletRequest request
    ) {
        HttpHeaders responseHeaders = new HttpHeaders();
        for (Map.Entry<String, List<String>> entry : codeEditorResponse.headers().map().entrySet()) {
            String headerName = entry.getKey();
            if (isExcludedHeader(headerName)) {
                continue;
            }
            if (HttpHeaders.LOCATION.equalsIgnoreCase(headerName)) {
                for (String value : entry.getValue()) {
                    responseHeaders.add(
                        HttpHeaders.LOCATION,
                        rewriteCodeEditorLocation(value, codeEditorBaseUri, request)
                    );
                }
                continue;
            }
            if (HttpHeaders.SET_COOKIE.equalsIgnoreCase(headerName)) {
                for (String value : entry.getValue()) {
                    responseHeaders.add(
                        HttpHeaders.SET_COOKIE,
                        rewriteCookie(value, externalCodeEditorBasePath(request))
                    );
                }
                continue;
            }
            for (String value : entry.getValue()) {
                responseHeaders.add(headerName, value);
            }
        }

        byte[] responseBody = rewriteCodeEditorBody(codeEditorResponse.body(), responseHeaders, request);
        HttpStatus status = HttpStatus.resolve(codeEditorResponse.statusCode());
        if (status == null) {
            status = HttpStatus.BAD_GATEWAY;
        }
        return new ResponseEntity<>(responseBody, responseHeaders, status);
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
        return rewriteCookie(setCookieHeader, "/");
    }

    private String rewriteCookie(String setCookieHeader, String path) {
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
                rewritten.add("Path=" + pathWithTrailingSlash(path));
                hasPath = true;
                continue;
            }
            rewritten.add(part);
        }

        if (!hasPath) {
            rewritten.add("Path=" + pathWithTrailingSlash(path));
        }
        return String.join("; ", rewritten);
    }

    private String rewriteRedisInsightLocation(String location, HttpServletRequest request) {
        if (!StringUtils.hasText(location)) {
            return location;
        }
        String externalBasePath = externalRedisInsightBasePath(request);
        if (location.startsWith(REDIS_INSIGHT_BASE_PATH) && !location.startsWith(externalBasePath)) {
            return externalBasePath + location.substring(REDIS_INSIGHT_BASE_PATH.length());
        }
        return location;
    }

    private String rewriteCodeEditorLocation(String location, URI codeEditorBaseUri, HttpServletRequest request) {
        if (!StringUtils.hasText(location)) {
            return location;
        }

        URI locationUri;
        try {
            locationUri = URI.create(location);
        } catch (IllegalArgumentException e) {
            return location;
        }

        if (locationUri.isAbsolute() && !isSameAuthority(locationUri, codeEditorBaseUri)) {
            return location;
        }
        if (!locationUri.isAbsolute() && !location.startsWith("/")) {
            return location;
        }

        return externalCodeEditorPath(
            locationUri.getRawPath(),
            locationUri.getRawQuery(),
            locationUri.getRawFragment(),
            request
        );
    }

    private byte[] rewriteRedisInsightBody(byte[] body, HttpHeaders responseHeaders, HttpServletRequest request) {
        if (body == null) {
            return null;
        }
        String contentType = responseHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType == null
            || !(contentType.contains("text/html")
            || contentType.contains("javascript")
            || contentType.contains("application/json"))) {
            return body;
        }
        String externalBasePath = externalRedisInsightBasePath(request);
        if (REDIS_INSIGHT_BASE_PATH.equals(externalBasePath)) {
            return body;
        }
        String rewritten = new String(body, StandardCharsets.UTF_8)
            .replace(REDIS_INSIGHT_BASE_PATH, externalBasePath);
        byte[] bytes = rewritten.getBytes(StandardCharsets.UTF_8);
        responseHeaders.setContentLength(bytes.length);
        return bytes;
    }

    private byte[] rewriteCodeEditorBody(byte[] body, HttpHeaders responseHeaders, HttpServletRequest request) {
        if (body == null) {
            return null;
        }
        String contentType = responseHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
        if (!isRewritableTextContent(contentType)) {
            return body;
        }

        String externalBasePath = externalCodeEditorBasePath(request);
        String rewritten = new String(body, StandardCharsets.UTF_8);
        if (!CODE_EDITOR_BASE_PATH.equals(externalBasePath)) {
            rewritten = rewritten.replace(CODE_EDITOR_BASE_PATH, externalBasePath);
        }
        rewritten = prefixRootAttributeReferences(rewritten, externalBasePath);
        rewritten = prefixRootCssUrlReferences(rewritten, externalBasePath);
        byte[] bytes = rewritten.getBytes(StandardCharsets.UTF_8);
        responseHeaders.setContentLength(bytes.length);
        return bytes;
    }

    private String externalRedisInsightBasePath(HttpServletRequest request) {
        String forwardedPrefix = request.getHeader("X-Forwarded-Prefix");
        if (!StringUtils.hasText(forwardedPrefix) || "/".equals(forwardedPrefix.trim())) {
            return REDIS_INSIGHT_BASE_PATH;
        }
        return trimTrailingSlash(forwardedPrefix.trim()) + REDIS_INSIGHT_BASE_PATH;
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

    private String externalCodeEditorPath(
        String path,
        String query,
        String fragment,
        HttpServletRequest request
    ) {
        String normalizedPath = StringUtils.hasText(path) ? path : "/";
        normalizedPath = stripBasePath(normalizedPath, CODE_EDITOR_BASE_PATH);
        String result = joinBasePath(externalCodeEditorBasePath(request), normalizedPath);
        if (StringUtils.hasText(query)) {
            result += "?" + query;
        }
        if (StringUtils.hasText(fragment)) {
            result += "#" + fragment;
        }
        return result;
    }

    private String joinBasePath(String basePath, String path) {
        if (!StringUtils.hasText(path) || "/".equals(path)) {
            return pathWithTrailingSlash(basePath);
        }
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return trimTrailingSlash(basePath) + normalizedPath;
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

    private boolean isRewritableTextContent(String contentType) {
        if (!StringUtils.hasText(contentType)) {
            return false;
        }
        String normalized = contentType.toLowerCase(Locale.ROOT);
        return normalized.contains("text/html")
            || normalized.contains("text/css")
            || normalized.contains("javascript")
            || normalized.contains("application/json")
            || normalized.contains("application/xml")
            || normalized.contains("text/xml")
            || normalized.contains("image/svg+xml");
    }

    private String prefixRootAttributeReferences(String value, String externalBasePath) {
        Matcher matcher = ROOT_ATTRIBUTE_REFERENCE.matcher(value);
        StringBuffer rewritten = new StringBuffer();
        while (matcher.find()) {
            if (remainingStartsWithPath(value.substring(matcher.end()), externalBasePath)
                || remainingStartsWithPath(value.substring(matcher.end()), CODE_EDITOR_BASE_PATH)) {
                matcher.appendReplacement(rewritten, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            matcher.appendReplacement(
                rewritten,
                Matcher.quoteReplacement(matcher.group(1) + pathWithTrailingSlash(externalBasePath))
            );
        }
        matcher.appendTail(rewritten);
        return rewritten.toString();
    }

    private String prefixRootCssUrlReferences(String value, String externalBasePath) {
        Matcher matcher = ROOT_CSS_URL_REFERENCE.matcher(value);
        StringBuffer rewritten = new StringBuffer();
        while (matcher.find()) {
            if (remainingStartsWithPath(value.substring(matcher.end()), externalBasePath)
                || remainingStartsWithPath(value.substring(matcher.end()), CODE_EDITOR_BASE_PATH)) {
                matcher.appendReplacement(rewritten, Matcher.quoteReplacement(matcher.group()));
                continue;
            }
            matcher.appendReplacement(
                rewritten,
                Matcher.quoteReplacement(matcher.group(1) + stripLeadingSlash(pathWithTrailingSlash(externalBasePath)))
            );
        }
        matcher.appendTail(rewritten);
        return rewritten.toString();
    }

    private boolean remainingStartsWithPath(String remaining, String path) {
        if (!StringUtils.hasText(remaining) || !StringUtils.hasText(path)) {
            return false;
        }
        String normalized = stripLeadingSlash(trimTrailingSlash(path));
        return remaining.equals(normalized)
            || remaining.startsWith(normalized + "/")
            || remaining.startsWith(normalized + "?")
            || remaining.startsWith(normalized + "#")
            || remaining.startsWith(normalized + "\"")
            || remaining.startsWith(normalized + "'");
    }

    private String stripLeadingSlash(String value) {
        String normalized = value;
        while (normalized.startsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String pathWithTrailingSlash(String path) {
        if (!StringUtils.hasText(path)) {
            return "/";
        }
        return path.endsWith("/") ? path : path + "/";
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

    private Optional<ResponseEntity<byte[]>> runnerUnavailableResponse(URI backendUri) {
        if (runnerProperties == null || !runnerProperties.isEnabled()) {
            return Optional.empty();
        }
        Optional<URI> childBackendUri = runnerProperties.resolveChildBackendUri();
        if (childBackendUri.isEmpty() || !isSameAuthority(backendUri, childBackendUri.get())) {
            return Optional.empty();
        }
        if (runnerManager.map(LocalSessionRunnerManager::isChildReady).orElse(false)) {
            return Optional.empty();
        }
        byte[] errorBody = "{\"error\":\"Session runner child process is not ready\"}".getBytes(StandardCharsets.UTF_8);
        return Optional.of(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
            .contentType(MediaType.APPLICATION_JSON)
            .body(errorBody));
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

    private boolean isForwardedAuthorityHeader(String headerName) {
        String normalized = headerName.toLowerCase(Locale.ROOT);
        return normalized.equals("x-forwarded-for")
            || normalized.equals("x-forwarded-host")
            || normalized.equals("x-forwarded-port")
            || normalized.equals("x-forwarded-prefix")
            || normalized.equals("x-forwarded-proto");
    }

    private String trimTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
