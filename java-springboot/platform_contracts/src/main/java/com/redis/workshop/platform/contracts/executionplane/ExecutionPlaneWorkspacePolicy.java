package com.redis.workshop.platform.contracts.executionplane;

import jakarta.validation.constraints.NotBlank;

public record ExecutionPlaneWorkspacePolicy(
    @NotBlank String mountPath,
    @NotBlank String sourceRef,
    boolean writable,
    boolean deleteOnTerminate
) {
}
