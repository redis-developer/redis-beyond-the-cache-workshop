package com.redis.workshop.platform.controlplane.session;

import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.repository.PlatformSessionRecordRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SessionRouteProxyService {

    private static final Set<PlatformSessionState> ROUTABLE_STATES = Set.of(
        PlatformSessionState.READY,
        PlatformSessionState.DEGRADED
    );

    private static final Set<String> HOP_BY_HOP_HEADERS = Set.of(
        "connection",
        "content-length",
        "host",
        "keep-alive",
        "proxy-authenticate",
        "proxy-authorization",
        "te",
        "trailer",
        "transfer-encoding",
        "upgrade"
    );
    private static final Pattern CLOUD_RUN_STABLE_HOST = Pattern.compile("^.+-(\\d+)\\.([a-z]+(?:-[a-z0-9]+)+)\\.run\\.app$");

    private final PlatformSessionRecordRepository sessionRepository;
    private final RestTemplate restTemplate = new RestTemplate(
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .followRedirects(Redirect.NEVER)
                .version(Version.HTTP_1_1)
                .build()
        )
    );
    private final RestTemplate patchCapableRestTemplate = new RestTemplate(
        new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .followRedirects(Redirect.NEVER)
                .version(Version.HTTP_1_1)
                .build()
        )
    );

    @Autowired
    public SessionRouteProxyService(PlatformSessionRecordRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    public ResponseEntity<byte[]> proxy(String sessionId, HttpServletRequest request, byte[] body) {
        RouteResolution routeResolution = resolveRoute(sessionId, request);
        if (!routeResolution.routable()) {
            return routeResolution.rejection();
        }
        RouteTarget routeTarget = routeResolution.target();

        try {
            HttpEntity<byte[]> entity = new HttpEntity<>(body, requestHeaders(request, routeTarget.basePath()));
            RestTemplate activeRestTemplate = HttpMethod.PATCH.matches(request.getMethod())
                ? patchCapableRestTemplate
                : restTemplate;
            ResponseEntity<byte[]> response = activeRestTemplate.exchange(
                URI.create(routeTarget.targetUrl()),
                HttpMethod.valueOf(request.getMethod()),
                entity,
                byte[].class
            );
            return rewriteResponse(routeTarget.basePath(), routeTarget.upstreamBaseUrl(), response);
        } catch (HttpStatusCodeException exception) {
            return rewriteError(routeTarget.basePath(), routeTarget.upstreamBaseUrl(), exception);
        } catch (Exception exception) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(("Session route unavailable: " + exception.getMessage()).getBytes(StandardCharsets.UTF_8));
        }
    }

    public RouteResolution resolveRoute(String sessionId, HttpServletRequest request) {
        PlatformSessionRecord record = sessionRepository.findById(sessionId).orElse(null);
        if (record == null) {
            return resolveCloudRunSessionRunnerRoute(sessionId, request)
                .map(RouteResolution::routable)
                .orElseGet(() -> RouteResolution.rejected(ResponseEntity.notFound().build()));
        }
        if (!ROUTABLE_STATES.contains(record.getState())) {
            return RouteResolution.rejected(ResponseEntity.notFound().build());
        }
        if (!StringUtils.hasText(record.getRouteUpstreamBaseUrl())) {
            return RouteResolution.rejected(ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("Session route is missing its upstream target".getBytes(StandardCharsets.UTF_8)));
        }

        String basePath = "/session/" + sessionId;
        String upstreamBaseUrl = trimTrailingSlash(record.getRouteUpstreamBaseUrl());
        return RouteResolution.routable(new RouteTarget(basePath, upstreamBaseUrl, targetUrl(upstreamBaseUrl, basePath, request)));
    }

    private Optional<RouteTarget> resolveCloudRunSessionRunnerRoute(String sessionId, HttpServletRequest request) {
        Matcher matcher = CLOUD_RUN_STABLE_HOST.matcher(request.getServerName());
        if (!matcher.matches()) {
            return Optional.empty();
        }
        String projectNumber = matcher.group(1);
        String region = matcher.group(2);
        String basePath = "/session/" + sessionId;
        String upstreamBaseUrl = "https://ws-" + sessionId + "-" + projectNumber + "." + region + ".run.app";
        return Optional.of(new RouteTarget(basePath, upstreamBaseUrl, targetUrl(upstreamBaseUrl, basePath, request)));
    }

    String targetUrl(String upstreamBaseUrl, String basePath, HttpServletRequest request) {
        String requestPath = request.getRequestURI();
        String targetPath = requestPath.startsWith(basePath)
            ? requestPath.substring(basePath.length())
            : requestPath;
        if (!StringUtils.hasText(targetPath)) {
            targetPath = "/";
        }
        if (!targetPath.startsWith("/")) {
            targetPath = "/" + targetPath;
        }
        String queryString = request.getQueryString();
        return upstreamBaseUrl + targetPath + (queryString == null ? "" : "?" + queryString);
    }

    HttpHeaders requestHeaders(HttpServletRequest request, String basePath) {
        HttpHeaders headers = new HttpHeaders();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            if (isHopByHop(headerName)) {
                continue;
            }
            Enumeration<String> values = request.getHeaders(headerName);
            while (values.hasMoreElements()) {
                headers.add(headerName, values.nextElement());
            }
        }
        if (!isCodeEditorRoute(request, basePath)) {
            putIfText(headers, "X-Forwarded-Prefix", basePath);
        }
        putIfText(headers, "X-Forwarded-Host", request.getHeader("Host"));
        putIfText(headers, "X-Forwarded-Proto", request.getScheme());
        return headers;
    }

    private boolean isCodeEditorRoute(HttpServletRequest request, String basePath) {
        String requestPath = request.getRequestURI();
        if (requestPath == null) {
            return false;
        }
        if (requestPath.startsWith(basePath)) {
            requestPath = requestPath.substring(basePath.length());
        }
        if (!StringUtils.hasText(requestPath)) {
            requestPath = "/";
        }
        return requestPath.equals("/code") || requestPath.startsWith("/code/");
    }

    private ResponseEntity<byte[]> rewriteResponse(
        String basePath,
        String upstreamBaseUrl,
        ResponseEntity<byte[]> response
    ) {
        HttpHeaders responseHeaders = responseHeaders(basePath, upstreamBaseUrl, response.getHeaders());
        byte[] responseBody = rewriteBody(basePath, responseHeaders, response.getBody());
        return new ResponseEntity<>(responseBody, responseHeaders, response.getStatusCode());
    }

    private ResponseEntity<byte[]> rewriteError(
        String basePath,
        String upstreamBaseUrl,
        HttpStatusCodeException exception
    ) {
        HttpHeaders sourceHeaders = exception.getResponseHeaders() == null
            ? new HttpHeaders()
            : exception.getResponseHeaders();
        HttpHeaders responseHeaders = responseHeaders(basePath, upstreamBaseUrl, sourceHeaders);
        byte[] responseBody = rewriteBody(basePath, responseHeaders, exception.getResponseBodyAsByteArray());
        return new ResponseEntity<>(responseBody, responseHeaders, exception.getStatusCode());
    }

    private HttpHeaders responseHeaders(String basePath, String upstreamBaseUrl, HttpHeaders sourceHeaders) {
        HttpHeaders responseHeaders = new HttpHeaders();
        sourceHeaders.forEach((key, values) -> {
            if (isHopByHop(key)) {
                return;
            }
            if (key.equalsIgnoreCase(HttpHeaders.LOCATION)) {
                values = values.stream().map(value -> rewriteLocation(basePath, upstreamBaseUrl, value)).toList();
            }
            if (key.equalsIgnoreCase(HttpHeaders.SET_COOKIE)) {
                values = values.stream().map(value -> rewriteCookie(basePath, value)).toList();
            }
            responseHeaders.addAll(key, values);
        });
        return responseHeaders;
    }

    private byte[] rewriteBody(String basePath, HttpHeaders responseHeaders, byte[] responseBody) {
        String contentType = responseHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentType == null || !contentType.contains("text/html") || responseBody == null) {
            return responseBody;
        }

        String html = new String(responseBody, StandardCharsets.UTF_8);
        html = html.replace("src=\"/js/", "src=\"" + basePath + "/js/");
        html = html.replace("src=\"/css/", "src=\"" + basePath + "/css/");
        html = html.replace("href=\"/js/", "href=\"" + basePath + "/js/");
        html = html.replace("href=\"/css/", "href=\"" + basePath + "/css/");
        html = html.replace("src=\"/assets/", "src=\"" + basePath + "/assets/");
        html = html.replace("href=\"/assets/", "href=\"" + basePath + "/assets/");
        html = html.replace("href=\"/favicon.ico\"", "href=\"" + basePath + "/favicon.ico\"");
        html = html.replace("href=\"/favicon-", "href=\"" + basePath + "/favicon-");
        html = html.replace("src=\"/favicon-", "src=\"" + basePath + "/favicon-");
        byte[] rewritten = html.getBytes(StandardCharsets.UTF_8);
        responseHeaders.setContentLength(rewritten.length);
        return rewritten;
    }

    private String rewriteLocation(String basePath, String upstreamBaseUrl, String location) {
        if (location.startsWith("/") && !location.startsWith(basePath)) {
            return basePath + location;
        }
        if (location.startsWith(upstreamBaseUrl)) {
            try {
                URI redirect = URI.create(location);
                return rewriteRedirectPath(basePath, redirect.getRawPath(), redirect.getRawQuery());
            } catch (IllegalArgumentException ignored) {
                return location.replace(upstreamBaseUrl, basePath);
            }
        }
        try {
            URI upstream = URI.create(upstreamBaseUrl);
            URI redirect = URI.create(location);
            if (redirect.isAbsolute() && upstream.getHost() != null && upstream.getHost().equals(redirect.getHost())) {
                return rewriteRedirectPath(basePath, redirect.getRawPath(), redirect.getRawQuery());
            }
        } catch (IllegalArgumentException ignored) {
        }
        return location;
    }

    private String rewriteRedirectPath(String basePath, String rawPath, String rawQuery) {
        String path = StringUtils.hasText(rawPath) ? rawPath : "/";
        String query = rawQuery == null ? "" : "?" + rawQuery;
        if (path.equals(basePath) || path.startsWith(basePath + "/")) {
            return path + query;
        }
        return basePath + path + query;
    }

    private String rewriteCookie(String basePath, String setCookie) {
        String[] parts = setCookie.split(";");
        StringBuilder rewritten = new StringBuilder(parts[0].trim());
        boolean hasPath = false;
        for (int i = 1; i < parts.length; i++) {
            String attribute = parts[i].trim();
            String lower = attribute.toLowerCase(Locale.ROOT);
            if (lower.startsWith("path=")) {
                rewritten.append("; Path=").append(basePath).append("/");
                hasPath = true;
                continue;
            }
            rewritten.append("; ").append(attribute);
        }
        if (!hasPath) {
            rewritten.append("; Path=").append(basePath).append("/");
        }
        return rewritten.toString();
    }

    private boolean isHopByHop(String headerName) {
        return headerName != null && HOP_BY_HOP_HEADERS.contains(headerName.toLowerCase(Locale.ROOT));
    }

    private void putIfText(HttpHeaders headers, String name, String value) {
        if (StringUtils.hasText(value)) {
            headers.set(name, value);
        }
    }

    private String trimTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public record RouteTarget(String basePath, String upstreamBaseUrl, String targetUrl) {
    }

    public record RouteResolution(RouteTarget target, ResponseEntity<byte[]> rejection) {

        static RouteResolution routable(RouteTarget target) {
            return new RouteResolution(target, null);
        }

        static RouteResolution rejected(ResponseEntity<byte[]> rejection) {
            return new RouteResolution(null, rejection);
        }

        boolean routable() {
            return target != null;
        }
    }
}
