package com.redis.workshop.platform.contracts.executionplane;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record ExecutionPlaneLaunchRequest(
    @NotBlank String sessionId,
    @NotBlank String workshopId,
    @NotBlank String releaseId,
    @NotBlank String releaseVersion,
    @NotBlank String mode,
    @NotEmpty Map<String, @Valid @NotNull ExecutionPlaneArtifactReference> artifacts,
    @Valid @NotNull ExecutionPlaneResourcePolicy resourcePolicy,
    @Valid @NotNull ExecutionPlaneWorkspacePolicy workspacePolicy,
    @Valid ExecutionPlaneRedisPolicy redisPolicy,
    Map<String, String> runtimeConfig
) {

    public ExecutionPlaneLaunchRequest(
        String sessionId,
        String workshopId,
        String releaseId,
        String releaseVersion,
        String mode,
        Map<String, ExecutionPlaneArtifactReference> artifacts,
        ExecutionPlaneResourcePolicy resourcePolicy,
        ExecutionPlaneWorkspacePolicy workspacePolicy,
        Map<String, String> runtimeConfig
    ) {
        this(
            sessionId,
            workshopId,
            releaseId,
            releaseVersion,
            mode,
            artifacts,
            resourcePolicy,
            workspacePolicy,
            null,
            runtimeConfig
        );
    }

    public ExecutionPlaneLaunchRequest {
        artifacts = artifacts == null ? Map.of() : Map.copyOf(artifacts);
        runtimeConfig = runtimeConfig == null ? Map.of() : Map.copyOf(runtimeConfig);
    }
}
