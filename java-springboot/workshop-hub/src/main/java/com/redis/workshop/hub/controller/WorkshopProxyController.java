package com.redis.workshop.hub.controller;

import com.redis.workshop.hub.model.Workshop;
import com.redis.workshop.hub.service.WorkshopRegistryService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;

/**
 * Reverse proxy controller that forwards requests to workshop containers running inside DinD.
 * Routes like /workshop/session-management/** are proxied to the internal container on port 8080.
 */
@RestController
@RequestMapping("/workshop")
public class WorkshopProxyController {

    private static final Logger logger = LoggerFactory.getLogger(WorkshopProxyController.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final WorkshopRegistryService registryService;

    private static final int REDIS_INSIGHT_PORT = 5540;

    public WorkshopProxyController(WorkshopRegistryService registryService) {
        this.registryService = registryService;
    }

    @RequestMapping(value = "/{workshopId}/**", method = {RequestMethod.GET, RequestMethod.POST,
            RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.PATCH, RequestMethod.OPTIONS})
    public ResponseEntity<byte[]> proxyRequest(
            @PathVariable String workshopId,
            HttpServletRequest request,
            @RequestBody(required = false) byte[] body) {

        Integer port = resolvePort(workshopId);
        if (port == null) {
            return ResponseEntity.notFound().build();
        }

        // Build the target URL (localhost because containers run inside DinD)
        String targetPath = request.getRequestURI().replaceFirst("/workshop/" + workshopId, "");
        if (targetPath.isEmpty()) {
            targetPath = "/";
        }
        String queryString = request.getQueryString();
        String targetUrl = "http://localhost:" + port + targetPath + (queryString != null ? "?" + queryString : "");

        logger.debug("Proxying request to: {}", targetUrl);

        try {
            // Build headers
            HttpHeaders headers = new HttpHeaders();
            Enumeration<String> headerNames = request.getHeaderNames();
            while (headerNames.hasMoreElements()) {
                String headerName = headerNames.nextElement();
                // Skip host header and connection-related headers
                if (!headerName.equalsIgnoreCase("host") &&
                    !headerName.equalsIgnoreCase("connection") &&
                    !headerName.equalsIgnoreCase("content-length")) {
                    headers.add(headerName, request.getHeader(headerName));
                }
            }

            HttpEntity<byte[]> entity = new HttpEntity<>(body, headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    URI.create(targetUrl),
                    HttpMethod.valueOf(request.getMethod()),
                    entity,
                    byte[].class
            );

            // Build response headers
            HttpHeaders responseHeaders = new HttpHeaders();
            String basePath = "/workshop/" + workshopId;
            response.getHeaders().forEach((key, values) -> {
                if (!key.equalsIgnoreCase("transfer-encoding")) {
                    // Rewrite Location header for redirects
                    if (key.equalsIgnoreCase("location")) {
                        values = values.stream()
                                .map(loc -> {
                                    // Rewrite absolute paths to include the workshop base path
                                    if (loc.startsWith("/") && !loc.startsWith("/workshop/")) {
                                        return basePath + loc;
                                    }
                                    // Rewrite localhost URLs
                                    if (loc.startsWith("http://localhost:" + port)) {
                                        return loc.replace("http://localhost:" + port, basePath);
                                    }
                                    return loc;
                                })
                                .toList();
                    }
                    responseHeaders.addAll(key, values);
                }
            });

            byte[] responseBody = response.getBody();

            // Rewrite HTML responses to fix asset paths
            String contentType = responseHeaders.getFirst(HttpHeaders.CONTENT_TYPE);
            if (contentType != null && contentType.contains("text/html") && responseBody != null) {
                String html = new String(responseBody, StandardCharsets.UTF_8);
                // Rewrite absolute paths for js, css, and other assets
                html = html.replace("src=\"/js/", "src=\"" + basePath + "/js/");
                html = html.replace("src=\"/css/", "src=\"" + basePath + "/css/");
                html = html.replace("href=\"/js/", "href=\"" + basePath + "/js/");
                html = html.replace("href=\"/css/", "href=\"" + basePath + "/css/");
                html = html.replace("href=\"/favicon.ico\"", "href=\"" + basePath + "/favicon.ico\"");
                // Rewrite form actions
                html = html.replace("action=\"/login\"", "action=\"" + basePath + "/login\"");
                html = html.replace("action=\"/logout\"", "action=\"" + basePath + "/logout\"");
                // Rewrite Redis Insight assets (uses /assets/ path)
                html = html.replace("src=\"/assets/", "src=\"" + basePath + "/assets/");
                html = html.replace("href=\"/assets/", "href=\"" + basePath + "/assets/");
                // Rewrite favicon paths (for Redis Insight)
                html = html.replace("href=\"/favicon-", "href=\"" + basePath + "/favicon-");
                html = html.replace("src=\"/favicon-", "src=\"" + basePath + "/favicon-");
                // Set Redis Insight proxy path configuration
                html = html.replace("RIPROXYPATH = '';", "RIPROXYPATH = '" + basePath + "/';");
                responseBody = html.getBytes(StandardCharsets.UTF_8);
                responseHeaders.setContentLength(responseBody.length);
            }

            // Rewrite JavaScript responses for ES module imports (Redis Insight)
            if (contentType != null && contentType.contains("javascript") && responseBody != null
                    && workshopId.equals("redis-insight")) {
                String js = new String(responseBody, StandardCharsets.UTF_8);
                // Rewrite dynamic imports like: import("/assets/...") or from "/assets/..."
                js = js.replace("from\"/assets/", "from\"" + basePath + "/assets/");
                js = js.replace("from \"/assets/", "from \"" + basePath + "/assets/");
                js = js.replace("import(\"/assets/", "import(\"" + basePath + "/assets/");
                js = js.replace("(\"/assets/", "(\"" + basePath + "/assets/");
                js = js.replace("'/assets/", "'" + basePath + "/assets/");
                // Rewrite API calls
                js = js.replace("\"/api/", "\"" + basePath + "/api/");
                js = js.replace("'/api/", "'" + basePath + "/api/");
                js = js.replace("fetch(\"/", "fetch(\"" + basePath + "/");
                js = js.replace("fetch('/", "fetch('" + basePath + "/");
                responseBody = js.getBytes(StandardCharsets.UTF_8);
                responseHeaders.setContentLength(responseBody.length);
            }

            return new ResponseEntity<>(responseBody, responseHeaders, response.getStatusCode());

        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // Handle HTTP error responses (4xx, 5xx) from the backend
            logger.warn("Backend returned error for {}: {} {}", targetUrl, e.getStatusCode(), e.getMessage());
            HttpHeaders errorHeaders = new HttpHeaders();
            e.getResponseHeaders().forEach((key, values) -> {
                if (!key.equalsIgnoreCase("transfer-encoding")) {
                    errorHeaders.addAll(key, values);
                }
            });
            return new ResponseEntity<>(e.getResponseBodyAsByteArray(), errorHeaders, e.getStatusCode());
        } catch (Exception e) {
            logger.error("Proxy error for {}: {}", targetUrl, e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(("Workshop not available: " + e.getMessage()).getBytes());
        }
    }

    private Integer resolvePort(String workshopId) {
        if ("redis-insight".equals(workshopId)) {
            return REDIS_INSIGHT_PORT;
        }

        for (Workshop workshop : registryService.getWorkshops()) {
            if (workshopId.equals(workshop.getServiceName())) {
                return workshop.getPort();
            }
            String slug = extractSlugFromUrl(workshop.getUrl());
            if (slug != null && workshopId.equals(slug)) {
                return workshop.getPort();
            }
        }
        return null;
    }

    private String extractSlugFromUrl(String url) {
        if (url == null || url.isBlank()) {
            return null;
        }
        String marker = "/workshop/";
        int markerIndex = url.indexOf(marker);
        if (markerIndex == -1) {
            return null;
        }
        int start = markerIndex + marker.length();
        int end = url.indexOf('/', start);
        if (end == -1) {
            end = url.length();
        }
        if (start >= end) {
            return null;
        }
        return url.substring(start, end);
    }
}
