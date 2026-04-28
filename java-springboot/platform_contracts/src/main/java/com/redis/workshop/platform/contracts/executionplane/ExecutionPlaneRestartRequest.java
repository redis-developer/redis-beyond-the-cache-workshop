package com.redis.workshop.platform.contracts.executionplane;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ExecutionPlaneRestartRequest(
    @NotBlank String sessionId,
    @NotBlank String workshopId,
    @NotBlank String releaseId,
    @NotBlank String releaseVersion,
    @NotBlank String mode,
    boolean rebuild,
    @NotEmpty Map<String, @Valid @NotNull ExecutionPlaneArtifactReference> artifacts,
    @Valid @NotNull ExecutionPlaneResourcePolicy resourcePolicy,
    @Valid @NotNull ExecutionPlaneWorkspacePolicy workspacePolicy,
    @Valid ExecutionPlaneRedisPolicy redisPolicy,
    Map<String, String> runtimeConfig
) {

    public ExecutionPlaneRestartRequest {
        artifacts = artifacts == null ? Map.of() : Map.copyOf(artifacts);
        runtimeConfig = runtimeConfig == null ? Map.of() : Map.copyOf(runtimeConfig);
    }
}
