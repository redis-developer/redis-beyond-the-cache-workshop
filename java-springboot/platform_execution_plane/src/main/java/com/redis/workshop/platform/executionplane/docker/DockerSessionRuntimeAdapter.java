package com.redis.workshop.platform.executionplane.docker;

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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@ConditionalOnProperty(
    prefix = "platform.execution-plane",
    name = "runtime-provider",
    havingValue = "docker"
)
public class DockerSessionRuntimeAdapter implements ExecutionPlaneRuntimeAdapter {

    private static final String PROVIDER = "docker";

    private final DockerRuntimeProperties properties;
    private final DockerCommand dockerCommand;
    private final DockerSessionRunnerManagerClient managerClient;

    public DockerSessionRuntimeAdapter(
        DockerRuntimeProperties properties,
        DockerCommand dockerCommand,
        DockerSessionRunnerManagerClient managerClient
    ) {
        this.properties = properties;
        this.dockerCommand = dockerCommand;
        this.managerClient = managerClient;
    }

    @Override
    public ExecutionPlaneLaunchAccepted launch(ExecutionPlaneLaunchRequest request) {
        String containerName = containerNameFor(request.sessionId());
        DockerCommandResult runResult = dockerCommand.run(runCommand(request, containerName));
        runResult.requireSuccess("launch Docker session container " + containerName);
        return accepted(request.sessionId(), containerName, managerUpstreamBaseUrl(containerName));
    }

    @Override
    public ExecutionPlaneLaunchAccepted restart(ExecutionPlaneRestartRequest request) {
        String containerName = containerNameFor(request.sessionId());
        String upstreamBaseUrl = managerUpstreamBaseUrl(containerName);
        managerClient.restart(upstreamBaseUrl, request.rebuild());
        return accepted(request.sessionId(), containerName, upstreamBaseUrl);
    }

    @Override
    public ExecutionPlaneTerminateAccepted terminate(ExecutionPlaneTerminateRequest request) {
        String containerName = containerNameFrom(request.runtimeRef());
        DockerCommandResult removeResult = dockerCommand.run(List.of("rm", "--force", "--volumes", containerName));
        if (removeResult.exitCode() != 0 && !isMissingContainer(removeResult)) {
            removeResult.requireSuccess("remove Docker session container " + containerName);
        }
        return new ExecutionPlaneTerminateAccepted(
            request.sessionId(),
            request.runtimeRef(),
            request.mode()
        );
    }

    private List<String> runCommand(ExecutionPlaneLaunchRequest request, String containerName) {
        List<String> arguments = new ArrayList<>();
        arguments.add("run");
        arguments.add("--detach");
        arguments.add("--name");
        arguments.add(containerName);

        labelsFor(request).forEach((key, value) -> {
            arguments.add("--label");
            arguments.add(key + "=" + value);
        });
        environmentFor(request).forEach((key, value) -> {
            arguments.add("--env");
            arguments.add(key + "=" + value);
        });

        arguments.add("--publish");
        arguments.add("127.0.0.1::" + managerPort() + "/tcp");
        arguments.add("--cpus");
        arguments.add(cpus(request.resourcePolicy().cpuMillis()));
        arguments.add("--memory");
        arguments.add(Math.max(128, request.resourcePolicy().memoryMiB()) + "m");
        arguments.add(imageFrom(request.workshopId(), request.releaseId(), request.artifacts()));
        return List.copyOf(arguments);
    }

    private Map<String, String> environmentFor(ExecutionPlaneLaunchRequest request) {
        Map<String, String> environment = new LinkedHashMap<>();
        putIfText(environment, "WORKSHOP_SESSION_ID", request.sessionId());
        putIfText(environment, "WORKSHOP_ID", request.workshopId());
        putIfText(environment, "WORKSHOP_RELEASE_ID", request.releaseId());
        putIfText(environment, "WORKSHOP_RELEASE_VERSION", request.releaseVersion());
        putIfText(environment, "WORKSHOP_SESSION_MODE", request.mode());
        putIfText(environment, "WORKSHOP_PUBLIC_BASE_PATH", publicBasePath(request.sessionId()));
        putIfText(environment, "PORT", String.valueOf(managerPort()));
        putIfText(environment, "SERVER_PORT", String.valueOf(managerPort()));
        copyRuntimeConfig(environment, request.runtimeConfig());
        if (request.redisPolicy() != null) {
            copyRuntimeConfig(environment, request.redisPolicy().toRuntimeConfig());
        }
        return environment;
    }

    private Map<String, String> labelsFor(ExecutionPlaneLaunchRequest request) {
        Map<String, String> labels = new LinkedHashMap<>();
        labels.put("workshop-session-id", labelValue(request.sessionId()));
        labels.put("workshop-id", labelValue(request.workshopId()));
        labels.put("release-id", labelValue(request.releaseId()));
        labels.put("managed-by", "workshop-execution-plane");
        putIfText(labels, "resource-class", request.resourcePolicy().resourceClass());
        putIfText(labels, "environment", valueFrom(request.runtimeConfig(), "WORKSHOP_ENVIRONMENT", ""));
        putIfText(labels, "event-id", valueFrom(request.runtimeConfig(), "WORKSHOP_EVENT_ID", ""));
        putIfText(labels, "redis-mode", valueFrom(request.runtimeConfig(), "WORKSHOP_REDIS_MODE", ""));
        return labels;
    }

    private void copyRuntimeConfig(Map<String, String> target, Map<String, String> runtimeConfig) {
        if (runtimeConfig == null) {
            return;
        }
        runtimeConfig.entrySet().stream()
            .sorted(Comparator.comparing(Map.Entry::getKey))
            .forEach(entry -> putIfText(target, entry.getKey(), entry.getValue()));
    }

    private String managerUpstreamBaseUrl(String containerName) {
        DockerCommandResult inspectResult = dockerCommand.run(List.of(
            "inspect",
            "--format",
            "{{(index (index .NetworkSettings.Ports \"" + managerPort() + "/tcp\") 0).HostPort}}",
            containerName
        ));
        inspectResult.requireSuccess("inspect Docker session container " + containerName);
        String hostPort = inspectResult.trimmedStdout();
        if (!StringUtils.hasText(hostPort)) {
            throw new IllegalStateException("Docker session container " + containerName + " has no published manager port");
        }
        return "http://localhost:" + hostPort;
    }

    private ExecutionPlaneLaunchAccepted accepted(String sessionId, String containerName, String upstreamBaseUrl) {
        return new ExecutionPlaneLaunchAccepted(
            sessionId,
            runtimeRefFor(containerName),
            new ExecutionPlaneRouteBinding(
                publicBasePath(sessionId),
                requireText(properties.getGatewayHost(), "gateway host"),
                containerName,
                requireText(properties.getServiceNamespace(), "service namespace"),
                upstreamBaseUrl
            )
        );
    }

    private ExecutionPlaneRuntimeRef runtimeRefFor(String containerName) {
        return new ExecutionPlaneRuntimeRef(PROVIDER, containerName);
    }

    private String containerNameFrom(ExecutionPlaneRuntimeRef runtimeRef) {
        if (runtimeRef == null || !PROVIDER.equals(runtimeRef.provider())) {
            throw new IllegalArgumentException("runtimeRef must use docker provider");
        }
        return requireText(runtimeRef.handle(), "runtimeRef handle");
    }

    private String containerNameFor(String sessionId) {
        String normalized = sessionId == null
            ? ""
            : sessionId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "-");
        normalized = normalized.replaceAll("-+", "-").replaceAll("^[^a-z0-9]+|[^a-z0-9]+$", "");
        if (!StringUtils.hasText(normalized)) {
            normalized = "unknown";
        }
        String name = "ws-" + normalized;
        if (name.length() > 63) {
            name = name.substring(0, 63).replaceAll("[_.-]+$", "");
        }
        return name;
    }

    private String imageFrom(
        String workshopId,
        String releaseId,
        Map<String, ExecutionPlaneArtifactReference> artifacts
    ) {
        String override = imageOverrideFor(workshopId, releaseId);
        if (StringUtils.hasText(override)) {
            return override.trim();
        }
        if (artifacts == null || artifacts.isEmpty()) {
            throw new IllegalArgumentException("at least one runner artifact is required");
        }
        for (String role : List.of("runner", "combined", "frontend", "backend")) {
            ExecutionPlaneArtifactReference artifact = artifacts.get(role);
            if (artifact != null && StringUtils.hasText(artifact.value())) {
                return artifact.value().trim();
            }
        }
        return artifacts.values().stream()
            .map(ExecutionPlaneArtifactReference::value)
            .filter(StringUtils::hasText)
            .map(String::trim)
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("at least one runner artifact is required"));
    }

    private String imageOverrideFor(String workshopId, String releaseId) {
        Map<String, String> overrides = properties.getImageOverrides();
        if (overrides == null || overrides.isEmpty()) {
            return "";
        }
        String byRelease = overrides.get(releaseId);
        if (StringUtils.hasText(byRelease)) {
            return byRelease.trim();
        }
        String byWorkshop = overrides.get(workshopId);
        if (StringUtils.hasText(byWorkshop)) {
            return byWorkshop.trim();
        }
        return "";
    }

    private String publicBasePath(String sessionId) {
        return "/session/" + sessionId + "/";
    }

    private int managerPort() {
        return Math.max(1, properties.getManagerPort());
    }

    private String cpus(int cpuMillis) {
        return String.format(Locale.ROOT, "%.3f", Math.max(1, cpuMillis) / 1000.0)
            .replaceAll("0+$", "")
            .replaceAll("\\.$", "");
    }

    private void putIfText(Map<String, String> values, String key, String value) {
        if (StringUtils.hasText(key) && StringUtils.hasText(value)) {
            values.put(key.trim(), value.trim());
        }
    }

    private String valueFrom(Map<String, String> values, String key, String fallback) {
        if (values != null && StringUtils.hasText(values.get(key))) {
            return values.get(key).trim();
        }
        return fallback;
    }

    private String labelValue(String value) {
        return StringUtils.hasText(value) ? value.trim() : "unknown";
    }

    private boolean isMissingContainer(DockerCommandResult result) {
        String output = ((result.stdout() == null ? "" : result.stdout())
            + "\n"
            + (result.stderr() == null ? "" : result.stderr())).toLowerCase(Locale.ROOT);
        return output.contains("no such container");
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
