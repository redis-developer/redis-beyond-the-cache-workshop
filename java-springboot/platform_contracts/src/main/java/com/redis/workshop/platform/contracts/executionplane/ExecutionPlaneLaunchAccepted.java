package com.redis.workshop.platform.contracts.executionplane;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ExecutionPlaneLaunchAccepted(
    @NotBlank String sessionId,
    @Valid @NotNull ExecutionPlaneRuntimeRef runtimeRef,
    @Valid @NotNull ExecutionPlaneRouteBinding routeBinding
) {
}
