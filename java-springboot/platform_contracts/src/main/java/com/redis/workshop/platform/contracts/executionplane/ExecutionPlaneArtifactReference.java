package com.redis.workshop.platform.contracts.executionplane;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ExecutionPlaneArtifactReference(
    @NotBlank
    @Pattern(regexp = "^[^\\s@]+@sha256:[0-9a-f]{64}$")
    String value
) {

    @JsonCreator
    public ExecutionPlaneArtifactReference {
    }

    @JsonValue
    @Override
    public String value() {
        return value;
    }
}
