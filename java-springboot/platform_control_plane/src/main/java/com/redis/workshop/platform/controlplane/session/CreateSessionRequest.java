package com.redis.workshop.platform.controlplane.session;

import jakarta.validation.constraints.NotBlank;

public record CreateSessionRequest(
    @NotBlank String workshopId,
    SessionMode mode,
    String releaseVersion
) {
}
