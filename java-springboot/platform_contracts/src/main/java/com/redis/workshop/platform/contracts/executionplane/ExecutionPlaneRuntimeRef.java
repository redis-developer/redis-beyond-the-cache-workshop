package com.redis.workshop.platform.contracts.executionplane;

import jakarta.validation.constraints.NotBlank;

public record ExecutionPlaneRuntimeRef(
    @NotBlank String provider,
    @NotBlank String handle
) {
}
