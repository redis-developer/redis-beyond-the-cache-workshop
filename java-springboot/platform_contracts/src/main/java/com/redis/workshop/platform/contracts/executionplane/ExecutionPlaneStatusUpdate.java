package com.redis.workshop.platform.contracts.executionplane;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ExecutionPlaneStatusUpdate(
    @NotBlank String sessionId,
    @Valid @NotNull ExecutionPlaneRuntimeRef runtimeRef,
    @NotNull ExecutionPlaneWorkloadStatus status,
    String reason,
    String message,
    @NotNull Instant observedAt
) {
}
