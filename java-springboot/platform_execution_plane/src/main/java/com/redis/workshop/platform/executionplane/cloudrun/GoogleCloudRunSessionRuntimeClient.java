package com.redis.workshop.platform.executionplane.cloudrun;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(
    prefix = "platform.execution-plane.cloud-run",
    name = "client",
    havingValue = "google"
)
public class GoogleCloudRunSessionRuntimeClient implements CloudRunSessionRuntimeClient {

    private static final String CLOUD_PLATFORM_SCOPE = "https://www.googleapis.com/auth/cloud-platform";
    private static final String UPDATE_MASK = "labels,ingress,template,traffic,invokerIamDisabled";
    private static final String CLOUD_RUN_RESERVED_PORT_ENV = "PORT";

    private final ObjectMapper objectMapper;
    private final CloudRunRuntimeProperties properties;
    private final HttpClient httpClient;
    private final AccessTokenProvider accessTokenProvider;
    private final String apiBaseUrl;

    @Autowired
    public GoogleCloudRunSessionRuntimeClient(
        ObjectMapper objectMapper,
        CloudRunRuntimeProperties properties
    ) {
        this(
            objectMapper,
            properties,
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build(),
            GoogleCloudRunSessionRuntimeClient::applicationDefaultAccessToken
        );
    }

    GoogleCloudRunSessionRuntimeClient(
        ObjectMapper objectMapper,
        CloudRunRuntimeProperties properties,
        HttpClient httpClient,
        AccessTokenProvider accessTokenProvider
    ) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = httpClient;
        this.accessTokenProvider = accessTokenProvider;
        this.apiBaseUrl = trimTrailingSlash(requireText(properties.getApiBaseUrl(), "cloud run api base url"));
    }

    @Override
    public CloudRunSessionService createOrReplace(CloudRunServiceSpec serviceSpec) {
        try {
            HttpResponse<String> response = createService(serviceSpec);
            if (response.statusCode() == HttpStatus.CONFLICT.value()) {
                response = patchService(serviceSpec);
            }
            throwIfUnexpected(response, "create or replace Cloud Run service");
            waitForOperation(response.body());
            return service(serviceSpec.projectId(), serviceSpec.region(), serviceSpec.serviceName());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to create or replace Cloud Run service " + serviceSpec.serviceName(), exception);
        }
    }

    private HttpResponse<String> createService(CloudRunServiceSpec serviceSpec) throws IOException {
        String requestBody = objectMapper.writeValueAsString(servicePayload(serviceSpec, false));
        HttpRequest request = authorizedRequest(createServiceUri(serviceSpec))
            .timeout(requestTimeout())
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build();
        return send(request, "create Cloud Run service");
    }

    private HttpResponse<String> patchService(CloudRunServiceSpec serviceSpec) throws IOException {
        String requestBody = objectMapper.writeValueAsString(servicePayload(serviceSpec, true));
        HttpRequest request = authorizedRequest(patchServiceUri(serviceSpec))
            .timeout(requestTimeout())
            .header("Content-Type", "application/json")
            .method("PATCH", HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build();
        return send(request, "patch Cloud Run service");
    }

    @Override
    public CloudRunSessionService restartManager(String projectId, String region, String serviceName, boolean rebuild) {
        CloudRunSessionService service = service(projectId, region, serviceName);
        try {
            String requestBody = objectMapper.writeValueAsString(Map.of("rebuild", rebuild));
            HttpRequest request = HttpRequest.newBuilder(managerRestartUri(service.uri()))
                .timeout(managerRestartTimeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                .build();
            HttpResponse<String> response = send(request, "restart session runner manager");
            throwIfUnexpected(response, "restart session runner manager");
            return service;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to restart session runner manager for " + serviceName, exception);
        }
    }

    @Override
    public void delete(String projectId, String region, String serviceName) {
        try {
            HttpRequest request = authorizedRequest(serviceUri(projectId, region, serviceName))
                .timeout(requestTimeout())
                .DELETE()
                .build();
            HttpResponse<String> response = send(request, "delete Cloud Run service");
            if (response.statusCode() == HttpStatus.NOT_FOUND.value()) {
                return;
            }
            throwIfUnexpected(response, "delete Cloud Run service");
            waitForOperation(response.body());
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to delete Cloud Run service " + serviceName, exception);
        }
    }

    private CloudRunSessionService service(String projectId, String region, String serviceName) {
        try {
            HttpRequest request = authorizedRequest(serviceUri(projectId, region, serviceName))
                .timeout(requestTimeout())
                .GET()
                .build();
            HttpResponse<String> response = send(request, "read Cloud Run service");
            throwIfUnexpected(response, "read Cloud Run service");
            CloudRunServiceResponse service = objectMapper.readValue(response.body(), CloudRunServiceResponse.class);
            return new CloudRunSessionService(
                projectId,
                region,
                serviceName,
                requireText(service.uri(), "Cloud Run service uri")
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to read Cloud Run service " + serviceName, exception);
        }
    }

    private ObjectNode servicePayload(CloudRunServiceSpec spec, boolean includeName) {
        ObjectNode root = objectMapper.createObjectNode();
        if (includeName) {
            root.put("name", serviceResourceName(spec.projectId(), spec.region(), spec.serviceName()));
        }
        root.put("ingress", requireText(spec.ingress(), "Cloud Run ingress"));
        root.put("invokerIamDisabled", true);

        ObjectNode labels = root.putObject("labels");
        spec.labels().forEach(labels::put);

        ObjectNode template = root.putObject("template");
        putIfText(template, "serviceAccount", spec.serviceAccount());
        template.put("maxInstanceRequestConcurrency", Math.max(1, spec.concurrency()));

        ObjectNode scaling = template.putObject("scaling");
        scaling.put("minInstanceCount", Math.max(0, spec.minInstances()));
        scaling.put("maxInstanceCount", Math.max(1, spec.maxInstances()));

        if (StringUtils.hasText(spec.vpcConnector())) {
            template.putObject("vpcAccess").put("connector", spec.vpcConnector().trim());
        }

        ObjectNode container = template.putArray("containers").addObject();
        container.put("image", requireText(spec.image(), "Cloud Run image"));
        container.putArray("ports").addObject().put("containerPort", Math.max(1, spec.managerPort()));
        appendEnvironment(container.putArray("env"), spec.environment(), spec.secretEnvironment());

        ObjectNode limits = container.putObject("resources").putObject("limits");
        limits.put("cpu", cpuLimit(spec.cpuMillis()));
        limits.put("memory", Math.max(128, spec.memoryMiB()) + "Mi");

        root.putArray("traffic")
            .addObject()
            .put("type", "TRAFFIC_TARGET_ALLOCATION_TYPE_LATEST")
            .put("percent", 100);
        return root;
    }

    private void appendEnvironment(
        ArrayNode target,
        Map<String, String> environment,
        Map<String, String> secretEnvironment
    ) {
        Map<String, String> values = new LinkedHashMap<>();
        values.putAll(environment);
        values.putAll(secretEnvironment);
        values.forEach((name, value) -> {
            String envName = StringUtils.hasText(name) ? name.trim() : "";
            if (StringUtils.hasText(envName)
                && StringUtils.hasText(value)
                && !CLOUD_RUN_RESERVED_PORT_ENV.equals(envName)) {
                target.addObject().put("name", envName).put("value", value.trim());
            }
        });
    }

    private void waitForOperation(String responseBody) throws IOException {
        OperationResponse operation = objectMapper.readValue(responseBody, OperationResponse.class);
        Instant deadline = Instant.now().plus(operationPollTimeout());
        while (!Boolean.TRUE.equals(operation.done())) {
            if (Instant.now().isAfter(deadline)) {
                throw new IllegalStateException("Timed out waiting for Cloud Run operation " + operation.name());
            }
            sleep(operationPollInterval());
            HttpRequest request = authorizedRequest(operationUri(operation.name()))
                .timeout(requestTimeout())
                .GET()
                .build();
            HttpResponse<String> response = send(request, "poll Cloud Run operation");
            throwIfUnexpected(response, "poll Cloud Run operation");
            operation = objectMapper.readValue(response.body(), OperationResponse.class);
        }
        if (operation.error() != null) {
            throw new IllegalStateException("Cloud Run operation failed: " + operation.error().message());
        }
    }

    private HttpRequest.Builder authorizedRequest(URI uri) throws IOException {
        return HttpRequest.newBuilder(uri)
            .header("Authorization", "Bearer " + accessTokenProvider.accessToken());
    }

    private HttpResponse<String> send(HttpRequest request, String action) {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while trying to " + action, exception);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to " + action, exception);
        }
    }

    private void throwIfUnexpected(HttpResponse<String> response, String action) {
        if (response.statusCode() >= 200 && response.statusCode() <= 299) {
            return;
        }
        throw new IllegalStateException(
            "Failed to " + action + " with status " + response.statusCode() + ": " + response.body()
        );
    }

    private URI patchServiceUri(CloudRunServiceSpec spec) {
        return URI.create(serviceUri(spec.projectId(), spec.region(), spec.serviceName())
            + "?updateMask=" + UPDATE_MASK);
    }

    private URI createServiceUri(CloudRunServiceSpec spec) {
        return URI.create(apiBaseUrl + "/projects/" + requireText(spec.projectId(), "Cloud Run project id")
            + "/locations/" + requireText(spec.region(), "Cloud Run region")
            + "/services?serviceId=" + encodeQueryParam(requireText(spec.serviceName(), "Cloud Run service name")));
    }

    private URI serviceUri(String projectId, String region, String serviceName) {
        return URI.create(apiBaseUrl + "/" + serviceResourceName(projectId, region, serviceName));
    }

    private URI operationUri(String operationName) {
        String value = requireText(operationName, "Cloud Run operation name");
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return URI.create(value);
        }
        return URI.create(apiBaseUrl + "/" + trimLeadingSlash(value));
    }

    private URI managerRestartUri(String serviceUri) {
        return URI.create(trimTrailingSlash(requireText(serviceUri, "Cloud Run service uri"))
            + "/internal/session-runner/restart");
    }

    private String serviceResourceName(String projectId, String region, String serviceName) {
        return "projects/" + requireText(projectId, "Cloud Run project id")
            + "/locations/" + requireText(region, "Cloud Run region")
            + "/services/" + requireText(serviceName, "Cloud Run service name");
    }

    private String encodeQueryParam(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String cpuLimit(int cpuMillis) {
        int cores = (int) Math.ceil(Math.max(1000, cpuMillis) / 1000.0);
        if (cores <= 1) {
            return "1";
        }
        if (cores <= 2) {
            return "2";
        }
        if (cores <= 4) {
            return "4";
        }
        return "8";
    }

    private void putIfText(ObjectNode node, String key, String value) {
        if (StringUtils.hasText(value)) {
            node.put(key, value.trim());
        }
    }

    private Duration requestTimeout() {
        return positiveDuration(properties.getRequestTimeout(), Duration.ofSeconds(30));
    }

    private Duration operationPollInterval() {
        return positiveDuration(properties.getOperationPollInterval(), Duration.ofSeconds(2));
    }

    private Duration operationPollTimeout() {
        return positiveDuration(properties.getOperationPollTimeout(), Duration.ofMinutes(10));
    }

    private Duration managerRestartTimeout() {
        return positiveDuration(properties.getManagerRestartTimeout(), Duration.ofSeconds(290));
    }

    private Duration positiveDuration(Duration value, Duration fallback) {
        if (value == null || value.isZero() || value.isNegative()) {
            return fallback;
        }
        return value;
    }

    private void sleep(Duration duration) {
        try {
            Thread.sleep(duration.toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for Cloud Run operation", exception);
        }
    }

    private String trimTrailingSlash(String value) {
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private String trimLeadingSlash(String value) {
        String normalized = value.trim();
        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }
        return normalized;
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }

    private static String applicationDefaultAccessToken() throws IOException {
        GoogleCredentials credentials = GoogleCredentials.getApplicationDefault()
            .createScoped(List.of(CLOUD_PLATFORM_SCOPE));
        credentials.refreshIfExpired();
        AccessToken token = credentials.getAccessToken();
        if (token == null) {
            credentials.refresh();
            token = credentials.getAccessToken();
        }
        if (token == null || !StringUtils.hasText(token.getTokenValue())) {
            throw new IOException("Application Default Credentials did not return an access token");
        }
        return token.getTokenValue();
    }

    @FunctionalInterface
    interface AccessTokenProvider {
        String accessToken() throws IOException;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OperationResponse(
        String name,
        Boolean done,
        OperationError error,
        JsonNode response
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record OperationError(
        int code,
        String message
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CloudRunServiceResponse(
        String name,
        String uri
    ) {
    }
}
