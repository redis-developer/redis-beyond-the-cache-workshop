package com.redis.workshop.platform.contracts.executionplane;

import jakarta.validation.constraints.NotBlank;

public record ExecutionPlaneRouteBinding(
    @NotBlank String publicBasePath,
    @NotBlank String gatewayHost,
    @NotBlank String serviceName,
    @NotBlank String serviceNamespace,
    @NotBlank String upstreamBaseUrl
) {
}
