package com.redis.workshop.platform.controlplane.session;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

@Component
@ConditionalOnExpression("T(org.springframework.util.StringUtils).hasText('${platform.controlplane.execution.base-url:}')")
class ExecutionPlaneHttpClient implements ExecutionPlaneClient {

    private static final String EXECUTION_PLANE_KEY_HEADER = "X-Execution-Plane-Key";

    private final ObjectMapper objectMapper;
    private volatile HttpClient httpClient;
    private final String baseUrl;
    private final String sharedSecret;
    private final Duration launchTimeout;
    private final Duration restartTimeout;
    private final Duration terminationTimeout;

    ExecutionPlaneHttpClient(
        ObjectMapper objectMapper,
        @org.springframework.beans.factory.annotation.Value("${platform.controlplane.execution.base-url}")
        String baseUrl,
        @org.springframework.beans.factory.annotation.Value("${platform.controlplane.execution.shared-secret:}")
        String sharedSecret,
        @org.springframework.beans.factory.annotation.Value("${platform.controlplane.execution.launch-timeout:450s}")
        Duration launchTimeout,
        @org.springframework.beans.factory.annotation.Value("${platform.controlplane.execution.restart-timeout:450s}")
        Duration restartTimeout,
        @org.springframework.beans.factory.annotation.Value("${platform.controlplane.execution.termination-timeout:30s}")
        Duration terminationTimeout
    ) {
        this.objectMapper = objectMapper;
        this.httpClient = newHttpClient();
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.sharedSecret = requireText(sharedSecret, "platform.controlplane.execution.shared-secret");
        this.launchTimeout = requirePositiveDuration(launchTimeout, "platform.controlplane.execution.launch-timeout");
        this.restartTimeout = requirePositiveDuration(restartTimeout, "platform.controlplane.execution.restart-timeout");
        this.terminationTimeout = requirePositiveDuration(terminationTimeout, "platform.controlplane.execution.termination-timeout");
    }

    @Override
    public ExecutionPlaneLaunchResult launchSession(SessionLaunchRequest request) {
        try {
            byte[] requestBody = objectMapper.writeValueAsBytes(toLaunchPayload(request));
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + "/internal/execution-plane/sessions"))
                .timeout(launchTimeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody));
            applySharedSecret(builder);

            HttpResponse<byte[]> response = sendWithConnectionRetry(builder.build());
            throwIfUnexpectedStatus(response.statusCode(), response.body(), "launch");
            return toLaunchResult(request.sessionId(), response.body(), "launch");
        } catch (HttpTimeoutException exception) {
            throw new ExecutionPlaneClientException(
                FailureKind.COMMUNICATION_FAILURE,
                "Execution plane launch request timed out after " + launchTimeout,
                exception
            );
        } catch (IOException exception) {
            throw new ExecutionPlaneClientException(
                FailureKind.COMMUNICATION_FAILURE,
                "Execution plane launch request failed",
                exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExecutionPlaneClientException(
                FailureKind.COMMUNICATION_FAILURE,
                "Execution plane launch request interrupted",
                exception
            );
        }
    }

    @Override
    public ExecutionPlaneLaunchResult restartSession(SessionRestartRequest request) {
        try {
            byte[] requestBody = objectMapper.writeValueAsBytes(toRestartPayload(request));
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + "/internal/execution-plane/sessions/" + request.sessionId() + "/restart"))
                .timeout(restartTimeout)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(requestBody));
            applySharedSecret(builder);

            HttpResponse<byte[]> response = sendWithConnectionRetry(builder.build());
            throwIfUnexpectedStatus(response.statusCode(), response.body(), "restart");
            return toLaunchResult(request.sessionId(), response.body(), "restart");
        } catch (HttpTimeoutException exception) {
            throw new ExecutionPlaneClientException(
                FailureKind.COMMUNICATION_FAILURE,
                "Execution plane restart request timed out after " + restartTimeout,
                exception
            );
        } catch (IOException exception) {
            throw new ExecutionPlaneClientException(
                FailureKind.COMMUNICATION_FAILURE,
                "Execution plane restart request failed",
                exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExecutionPlaneClientException(
                FailureKind.COMMUNICATION_FAILURE,
                "Execution plane restart request interrupted",
                exception
            );
        }
    }

    @Override
    public TerminationResult terminateSession(SessionTerminationRequest request) {
        try {
            byte[] requestBody = objectMapper.writeValueAsBytes(toTerminationPayload(request));
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + "/internal/execution-plane/sessions/" + request.sessionId()))
                .timeout(terminationTimeout)
                .header("Content-Type", "application/json")
                .method("DELETE", HttpRequest.BodyPublishers.ofByteArray(requestBody));
            applySharedSecret(builder);

            HttpResponse<byte[]> response = sendWithConnectionRetry(builder.build());
            if (response.statusCode() == HttpStatus.NOT_FOUND.value()) {
                return TerminationResult.ALREADY_TERMINATED;
            }
            throwIfUnexpectedStatus(response.statusCode(), response.body(), "termination");
            if (response.statusCode() != HttpStatus.NO_CONTENT.value() && response.body().length > 0) {
                validateTerminationResponse(request, response.body());
            }
            return TerminationResult.TERMINATED;
        } catch (IOException exception) {
            throw new ExecutionPlaneClientException(
                FailureKind.COMMUNICATION_FAILURE,
                "Execution plane termination request failed",
                exception
            );
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ExecutionPlaneClientException(
                FailureKind.COMMUNICATION_FAILURE,
                "Execution plane termination request interrupted",
                exception
            );
        }
    }

    private LaunchPayload toLaunchPayload(SessionLaunchRequest request) {
        if (request.artifacts().isEmpty()) {
            throw new ExecutionPlaneClientException(
                FailureKind.INVALID_RESPONSE,
                "Execution plane launch requires release artifacts"
            );
        }
        return new LaunchPayload(
            request.sessionId(),
            request.workshopId(),
            request.releaseId(),
            request.releaseVersion(),
            request.mode().name(),
            request.artifacts(),
            ResourcePolicyPayload.from(request.resourcePolicy()),
            WorkspacePolicyPayload.from(request.workspacePolicy()),
            request.runtimeConfig()
        );
    }

    private RestartPayload toRestartPayload(SessionRestartRequest request) {
        if (request.artifacts().isEmpty()) {
            throw new ExecutionPlaneClientException(
                FailureKind.INVALID_RESPONSE,
                "Execution plane restart requires release artifacts"
            );
        }
        return new RestartPayload(
            request.sessionId(),
            request.workshopId(),
            request.releaseId(),
            request.releaseVersion(),
            request.mode().name(),
            request.rebuild(),
            request.artifacts(),
            ResourcePolicyPayload.from(request.resourcePolicy()),
            WorkspacePolicyPayload.from(request.workspacePolicy()),
            request.runtimeConfig()
        );
    }

    private TerminationPayload toTerminationPayload(SessionTerminationRequest request) {
        return new TerminationPayload(
            request.sessionId(),
            RuntimeRefPayload.fromOpaque(request.runtimeRef()),
            terminationModeFor(request.reason())
        );
    }

    private String terminationModeFor(String reason) {
        return switch (reason) {
            case "expired" -> "EXPIRED";
            case "cleanup_reconciler", "runtime_failed" -> "CLEANUP_RETRY";
            case "admin_requested" -> "ADMIN_FORCED";
            default -> "USER_REQUESTED";
        };
    }

    private void applySharedSecret(HttpRequest.Builder builder) {
        builder.header(EXECUTION_PLANE_KEY_HEADER, sharedSecret);
    }

    private HttpResponse<byte[]> sendWithConnectionRetry(HttpRequest request) throws IOException, InterruptedException {
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException firstFailure) {
            httpClient = newHttpClient();
            try {
                return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            } catch (IOException secondFailure) {
                secondFailure.addSuppressed(firstFailure);
                throw secondFailure;
            }
        }
    }

    private HttpClient newHttpClient() {
        return HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    }

    private ExecutionPlaneLaunchResult toLaunchResult(String expectedSessionId, byte[] responseBody, String operation) {
        try {
            LaunchAccepted launchResponse = objectMapper.readValue(responseBody, LaunchAccepted.class);
            validateResponseSessionId(launchResponse.sessionId(), expectedSessionId, operation);
            RuntimeRefPayload runtimeRef = requireRuntimeRef(launchResponse.runtimeRef(), operation);
            RouteBindingPayload routeBinding = requireRouteBinding(
                launchResponse.routeBinding(),
                operation,
                expectedSessionId
            );
            return new ExecutionPlaneLaunchResult(
                runtimeRef.toOpaque(),
                new ExecutionPlaneRouteBinding(
                    routeBinding.publicBasePath(),
                    routeBinding.gatewayHost(),
                    routeBinding.serviceName(),
                    routeBinding.serviceNamespace(),
                    routeBinding.upstreamBaseUrl()
                )
            );
        } catch (IOException | IllegalArgumentException exception) {
            throw new ExecutionPlaneClientException(
                FailureKind.INVALID_RESPONSE,
                "Execution plane " + operation + " returned an invalid response",
                exception
            );
        }
    }

    private void validateTerminationResponse(SessionTerminationRequest request, byte[] responseBody) {
        try {
            TerminationAccepted terminationResponse = objectMapper.readValue(responseBody, TerminationAccepted.class);
            validateResponseSessionId(terminationResponse.sessionId(), request.sessionId(), "termination");
            requireRuntimeRef(terminationResponse.runtimeRef(), "termination");
        } catch (IOException | IllegalArgumentException exception) {
            throw new ExecutionPlaneClientException(
                FailureKind.INVALID_RESPONSE,
                "Execution plane termination returned an invalid response",
                exception
            );
        }
    }

    private void validateResponseSessionId(String responseSessionId, String expectedSessionId, String operation) {
        String normalizedSessionId = requirePayloadText(responseSessionId, operation + " response sessionId");
        if (!expectedSessionId.equals(normalizedSessionId)) {
            throw new IllegalArgumentException(operation + " response sessionId does not match the request");
        }
    }

    private RuntimeRefPayload requireRuntimeRef(RuntimeRefPayload runtimeRef, String operation) {
        if (runtimeRef == null) {
            throw new IllegalArgumentException("Execution plane " + operation + " response runtimeRef is required");
        }
        runtimeRef.provider();
        runtimeRef.handle();
        return runtimeRef;
    }

    private RouteBindingPayload requireRouteBinding(
        RouteBindingPayload routeBinding,
        String operation,
        String expectedSessionId
    ) {
        if (routeBinding == null) {
            throw new IllegalArgumentException("Execution plane " + operation + " response routeBinding is required");
        }
        routeBinding.publicBasePath();
        routeBinding.gatewayHost();
        routeBinding.serviceName();
        routeBinding.serviceNamespace();
        routeBinding.upstreamBaseUrl();
        String stablePublicBasePath = "/session/" + expectedSessionId + "/";
        if (!stablePublicBasePath.equals(routeBinding.publicBasePath())) {
            throw new IllegalArgumentException(
                "Execution plane " + operation + " response publicBasePath must use the stable Cloud Run session route "
                    + stablePublicBasePath
            );
        }
        return routeBinding;
    }

    private void throwIfUnexpectedStatus(int statusCode, byte[] responseBody, String operation) {
        if (statusCode == HttpStatus.UNAUTHORIZED.value() || statusCode == HttpStatus.FORBIDDEN.value()) {
            throw new ExecutionPlaneClientException(
                FailureKind.UNAUTHORIZED,
                "Execution plane " + operation + " request was not authorized"
            );
        }
        if (statusCode < 200 || statusCode >= 300) {
            throw new ExecutionPlaneClientException(
                FailureKind.REMOTE_FAILURE,
                "Execution plane " + operation + " failed with status " + statusCode + ": " + responseBody(responseBody)
            );
        }
    }

    private String normalizeBaseUrl(String value) {
        String normalized = trimTrailingSlash(requireText(value, "platform.controlplane.execution.base-url"));
        URI uri;
        try {
            uri = URI.create(normalized);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("platform.controlplane.execution.base-url must be a valid absolute URL", exception);
        }
        if (!uri.isAbsolute()
            || (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme()))) {
            throw new IllegalArgumentException("platform.controlplane.execution.base-url must be an absolute http URL");
        }
        return normalized;
    }

    private Duration requirePositiveDuration(Duration value, String propertyName) {
        if (value == null || value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(propertyName + " must be greater than zero");
        }
        return value;
    }

    private String requireText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalStateException(propertyName + " is required when platform.controlplane.execution.base-url is configured");
        }
        return value.trim();
    }

    private String responseBody(byte[] responseBody) {
        if (responseBody == null || responseBody.length == 0) {
            return "<empty>";
        }
        return new String(responseBody, StandardCharsets.UTF_8);
    }

    private String trimTrailingSlash(String value) {
        if (value.endsWith("/")) {
            return value.substring(0, value.length() - 1);
        }
        return value;
    }

    private record LaunchPayload(
        String sessionId,
        String workshopId,
        String releaseId,
        String releaseVersion,
        String mode,
        Map<String, String> artifacts,
        ResourcePolicyPayload resourcePolicy,
        WorkspacePolicyPayload workspacePolicy,
        Map<String, String> runtimeConfig
    ) {
    }

    private record RestartPayload(
        String sessionId,
        String workshopId,
        String releaseId,
        String releaseVersion,
        String mode,
        boolean rebuild,
        Map<String, String> artifacts,
        ResourcePolicyPayload resourcePolicy,
        WorkspacePolicyPayload workspacePolicy,
        Map<String, String> runtimeConfig
    ) {
    }

    private record ResourcePolicyPayload(
        String resourceClass,
        int cpuMillis,
        int memoryMiB,
        int storageMiB,
        int ttlMinutes
    ) {

        static ResourcePolicyPayload from(SessionResourcePolicy policy) {
            return new ResourcePolicyPayload(
                policy.resourceClass(),
                policy.cpuMillis(),
                policy.memoryMiB(),
                policy.storageMiB(),
                policy.ttlMinutes()
            );
        }
    }

    private record WorkspacePolicyPayload(
        String mountPath,
        String sourceRef,
        boolean writable,
        boolean deleteOnTerminate
    ) {

        static WorkspacePolicyPayload from(SessionWorkspacePolicy policy) {
            return new WorkspacePolicyPayload(
                policy.mountPath(),
                policy.sourceRef(),
                policy.writable(),
                policy.deleteOnTerminate()
            );
        }
    }

    private record TerminationPayload(
        String sessionId,
        RuntimeRefPayload runtimeRef,
        String mode
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record LaunchAccepted(
        String sessionId,
        RuntimeRefPayload runtimeRef,
        RouteBindingPayload routeBinding
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record TerminationAccepted(
        String sessionId,
        RuntimeRefPayload runtimeRef,
        String mode
    ) {
    }

    private record RuntimeRefPayload(
        String provider,
        String handle
    ) {

        RuntimeRefPayload {
            provider = requirePayloadText(provider, "runtimeRef.provider");
            handle = requirePayloadText(handle, "runtimeRef.handle");
        }

        static RuntimeRefPayload fromOpaque(String runtimeRef) {
            String normalized = requirePayloadText(runtimeRef, "runtimeRef");
            int delimiter = normalized.indexOf(':');
            if (delimiter <= 0 || delimiter == normalized.length() - 1) {
                throw new IllegalArgumentException("runtimeRef must use provider:handle format");
            }
            return new RuntimeRefPayload(
                normalized.substring(0, delimiter),
                normalized.substring(delimiter + 1)
            );
        }

        String toOpaque() {
            return provider + ":" + handle;
        }
    }

    private record RouteBindingPayload(
        String publicBasePath,
        String gatewayHost,
        String serviceName,
        String serviceNamespace,
        String upstreamBaseUrl
    ) {

        RouteBindingPayload {
            publicBasePath = requirePayloadText(publicBasePath, "routeBinding.publicBasePath");
            gatewayHost = requirePayloadText(gatewayHost, "routeBinding.gatewayHost");
            serviceName = requirePayloadText(serviceName, "routeBinding.serviceName");
            serviceNamespace = requirePayloadText(serviceNamespace, "routeBinding.serviceNamespace");
            upstreamBaseUrl = requirePayloadText(upstreamBaseUrl, "routeBinding.upstreamBaseUrl");
        }
    }

    private static String requirePayloadText(String value, String propertyName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(propertyName + " is required");
        }
        return value.trim();
    }
}
