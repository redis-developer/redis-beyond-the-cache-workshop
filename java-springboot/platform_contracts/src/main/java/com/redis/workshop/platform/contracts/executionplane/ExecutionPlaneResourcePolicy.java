package com.redis.workshop.platform.contracts.executionplane;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record ExecutionPlaneResourcePolicy(
    @NotBlank String resourceClass,
    @Min(1) int cpuMillis,
    @Min(1) int memoryMiB,
    @Min(1) int storageMiB,
    @Min(1) int ttlMinutes
) {
}
