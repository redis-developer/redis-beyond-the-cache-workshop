package com.redis.workshop.platform.contracts.executionplane;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExecutionPlaneTerminateAccepted(
    @NotBlank String sessionId,
    @Valid @NotNull ExecutionPlaneRuntimeRef runtimeRef,
    @NotNull ExecutionPlaneTerminationMode mode
) {
}
