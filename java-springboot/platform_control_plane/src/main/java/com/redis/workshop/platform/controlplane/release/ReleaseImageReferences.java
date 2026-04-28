package com.redis.workshop.platform.controlplane.release;

import java.util.List;
import java.util.stream.Stream;

public record ReleaseImageReferences(
    String frontend,
    String backend,
    String combined,
    String init
) {

    public List<String> declaredReferences() {
        return Stream.of(frontend, backend, combined, init)
            .filter(reference -> reference != null && !reference.isBlank())
            .toList();
    }
}
