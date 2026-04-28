package com.redis.workshop.platform.executionplane.cloudrun;

import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneArtifactReference;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneLaunchAccepted;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneLaunchRequest;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneRestartRequest;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneRouteBinding;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneRuntimeRef;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneTerminateAccepted;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneTerminateRequest;
import com.redis.workshop.platform.executionplane.session.ExecutionPlaneRuntimeAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
@ConditionalOnProperty(
    prefix = "platform.execution-plane",
    name = "runtime-provider",
    havingValue = "cloud-run",
    matchIfMissing = true
)
public class CloudRunSessionRuntimeAdapter implements ExecutionPlaneRuntimeAdapter {

    private static final String PROVIDER = "cloud-run";

    private final CloudRunRuntimeProperties properties;
    private final CloudRunSessionRuntimeClient cloudRunClient;

    public CloudRunSessionRuntimeAdapter(
        CloudRunRuntimeProperties properties,
        CloudRunSessionRuntimeClient cloudRunClient
    ) {
        this.properties = properties;
        this.cloudRunClient = cloudRunClient;
    }

    public ExecutionPlaneLaunchAccepted launch(ExecutionPlaneLaunchRequest request) {
        CloudRunServiceSpec serviceSpec = serviceSpecFor(request);
        CloudRunSessionService service = cloudRunClient.createOrReplace(serviceSpec);
        return accepted(request.sessionId(), service);
    }

    public ExecutionPlaneLaunchAccepted restart(ExecutionPlaneRestartRequest request) {
        String serviceName = serviceNameFor(request.sessionId());
        CloudRunSessionService service = cloudRunClient.restartManager(
            properties.getProjectId(),
            properties.getRegion(),
            serviceName,
            request.rebuild()
        );
        return accepted(request.sessionId(), service);
    }

    public ExecutionPlaneTerminateAccepted terminate(ExecutionPlaneTerminateRequest request) {
        String serviceName = serviceNameFrom(request.runtimeRef());
        cloudRunClient.delete(properties.getProjectId(), properties.getRegion(), serviceName);
        return new ExecutionPlaneTerminateAccepted(
            request.sessionId(),
            request.runtimeRef(),
            request.mode()
        );
    }

    CloudRunServiceSpec serviceSpecFor(ExecutionPlaneLaunchRequest request) {
        return serviceSpec(
            request.sessionId(),
            request.workshopId(),
            request.releaseId(),
            request.releaseVersion(),
            request.mode(),
            request.artifacts(),
            request.resourcePolicy().resourceClass(),
            request.resourcePolicy().cpuMillis(),
            request.resourcePolicy().memoryMiB(),
            request.runtimeConfig(),
            request.redisPolicy() == null ? Map.of() : request.redisPolicy().toRuntimeConfig()
        );
    }

    CloudRunServiceSpec serviceSpecFor(ExecutionPlaneRestartRequest request) {
        return serviceSpec(
            request.sessionId(),
            request.workshopId(),
            request.releaseId(),
            request.releaseVersion(),
            request.mode(),
            request.artifacts(),
            request.resourcePolicy().resourceClass(),
            request.resourcePolicy().cpuMillis(),
            request.resourcePolicy().memoryMiB(),
            request.runtimeConfig(),
            request.redisPolicy() == null ? Map.of() : request.redisPolicy().toRuntimeConfig()
        );
    }

    private CloudRunServiceSpec serviceSpec(
        String sessionId,
        String workshopId,
        String releaseId,
        String releaseVersion,
        String mode,
        Map<String, ExecutionPlaneArtifactReference> artifacts,
        String resourceClass,
        int cpuMillis,
        int memoryMiB,
        Map<String, String> runtimeConfig,
        Map<String, String> redisRuntimeConfig
    ) {
        Map<String, String> environment = new LinkedHashMap<>();
        putIfText(environment, "WORKSHOP_SESSION_ID", sessionId);
        putIfText(environment, "WORKSHOP_ID", workshopId);
        putIfText(environment, "WORKSHOP_RELEASE_ID", releaseId);
        putIfText(environment, "WORKSHOP_RELEASE_VERSION", releaseVersion);
        putIfText(environment, "WORKSHOP_SESSION_MODE", mode);
        putIfText(environment, "WORKSHOP_PUBLIC_BASE_PATH", publicBasePath(sessionId));
        putIfText(environment, "PORT", String.valueOf(properties.getManagerPort()));
        putIfText(environment, "SERVER_PORT", String.valueOf(properties.getManagerPort()));

        Map<String, String> secretEnvironment = new LinkedHashMap<>();
        copyRuntimeConfig(environment, secretEnvironment, runtimeConfig);
        copyRuntimeConfig(environment, secretEnvironment, redisRuntimeConfig);

        return new CloudRunServiceSpec(
            properties.getProjectId(),
            properties.getRegion(),
            serviceNameFor(sessionId),
            imageFrom(artifacts),
            properties.getServiceAccount(),
            properties.getIngress(),
            properties.getVpcConnector(),
            properties.getManagerPort(),
            cpuMillis,
            memoryMiB,
            Math.max(1, properties.getConcurrency()),
            Math.max(0, properties.getMinInstances()),
            Math.max(1, properties.getMaxInstances()),
            labelsFor(sessionId, workshopId, releaseId, resourceClass, runtimeConfig, redisRuntimeConfig),
            Map.copyOf(environment),
            Map.copyOf(secretEnvironment)
        );
    }

    private void copyRuntimeConfig(
        Map<String, String> environment,
        Map<String, String> secretEnvironment,
        Map<String, String> runtimeConfig
    ) {
        if (runtimeConfig == null) {
            return;
        }
        runtimeConfig.forEach((key, value) -> {
            if (!StringUtils.hasText(key) || !StringUtils.hasText(value)) {
                return;
            }
            String normalizedKey = key.trim();
            if (isSecretRef(normalizedKey)) {
                secretEnvironment.put(normalizedKey, value.trim());
            } else {
                environment.put(normalizedKey, value.trim());
            }
        });
    }

    private boolean isSecretRef(String key) {
        String normalized = key.toUpperCase(Locale.ROOT);
        return normalized.endsWith("_SECRET_REF") || normalized.endsWith("_PASSWORD_SECRET_REF");
    }

    private String imageFrom(Map<String, ExecutionPlaneArtifactReference> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            throw new IllegalArgumentException("at least one artifact is required");
        }
        for (String role : java.util.List.of("runner", "combined", "frontend", "backend")) {
            ExecutionPlaneArtifactReference reference = artifacts.get(role);
            if (reference != null && StringUtils.hasText(reference.value())) {
                return reference.value();
            }
        }
        return artifacts.values().iterator().next().value();
    }

    private Map<String, String> labelsFor(
        String sessionId,
        String workshopId,
        String releaseId,
        String resourceClass,
        Map<String, String> runtimeConfig,
        Map<String, String> redisRuntimeConfig
    ) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("workshop-session-id", labelValue(sessionId));
        labels.put("workshop-id", labelValue(workshopId));
        labels.put("release-id", labelValue(releaseId));
        labels.put("environment", labelValue(valueFrom(runtimeConfig, "WORKSHOP_ENVIRONMENT", properties.getEnvironment())));
        labels.put("event-id", labelValue(valueFrom(runtimeConfig, "WORKSHOP_EVENT_ID", properties.getEventId())));
        labels.put("resource-class", labelValue(resourceClass));
        String redisMode = valueFrom(
            redisRuntimeConfig,
            "WORKSHOP_REDIS_MODE",
            valueFrom(runtimeConfig, "WORKSHOP_REDIS_MODE", "unknown")
        );
        labels.put(
            "redis-mode",
            labelValue(redisMode)
        );
        labels.put("managed-by", "workshop-execution-plane");
        return Map.copyOf(labels);
    }

    private ExecutionPlaneLaunchAccepted accepted(String sessionId, CloudRunSessionService service) {
        return new ExecutionPlaneLaunchAccepted(
            sessionId,
            runtimeRefFor(service.serviceName()),
            new ExecutionPlaneRouteBinding(
                publicBasePath(sessionId),
                properties.getGatewayHost(),
                service.serviceName(),
                properties.getRegion(),
                service.uri()
            )
        );
    }

    private ExecutionPlaneRuntimeRef runtimeRefFor(String serviceName) {
        return new ExecutionPlaneRuntimeRef(
            PROVIDER,
            "projects/" + properties.getProjectId()
                + "/locations/" + properties.getRegion()
                + "/services/" + serviceName
        );
    }

    private String serviceNameFrom(ExecutionPlaneRuntimeRef runtimeRef) {
        if (runtimeRef == null || !PROVIDER.equals(runtimeRef.provider())) {
            throw new IllegalArgumentException("runtimeRef must use cloud-run provider");
        }
        String handle = runtimeRef.handle();
        int index = handle.lastIndexOf("/services/");
        if (index < 0 || index + "/services/".length() >= handle.length()) {
            throw new IllegalArgumentException("runtimeRef handle must include /services/{serviceName}");
        }
        return handle.substring(index + "/services/".length());
    }

    private String serviceNameFor(String sessionId) {
        String normalized = labelValue("ws-" + sessionId);
        if (normalized.length() <= 63) {
            return normalized;
        }
        return normalized.substring(0, 63).replaceAll("-+$", "");
    }

    private String publicBasePath(String sessionId) {
        return "/session/" + sessionId + "/";
    }

    private void putIfText(Map<String, String> values, String key, String value) {
        if (StringUtils.hasText(value)) {
            values.put(key, value.trim());
        }
    }

    private String valueFrom(Map<String, String> values, String key, String fallback) {
        if (values != null && StringUtils.hasText(values.get(key))) {
            return values.get(key).trim();
        }
        return fallback;
    }

    private String labelValue(String value) {
        String normalized = value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9-]", "-");
        normalized = normalized.replaceAll("-+", "-").replaceAll("^-|-$", "");
        if (normalized.length() > 63) {
            normalized = normalized.substring(0, 63).replaceAll("-+$", "");
        }
        return normalized.isBlank() ? "unknown" : normalized;
    }
}
