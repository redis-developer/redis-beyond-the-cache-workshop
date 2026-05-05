package com.redis.workshop.platform.controlplane.session;

import jakarta.validation.constraints.NotBlank;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record CreateSessionRequest(
    @NotBlank String workshopId,
    SessionMode mode,
    String releaseVersion,
    Map<String, String> sessionEnvironment
) {

    public CreateSessionRequest(String workshopId, SessionMode mode, String releaseVersion) {
        this(workshopId, mode, releaseVersion, Map.of());
    }

    public CreateSessionRequest {
        sessionEnvironment = sessionEnvironment == null
            ? Map.of()
            : Collections.unmodifiableMap(new LinkedHashMap<>(sessionEnvironment));
    }
}
