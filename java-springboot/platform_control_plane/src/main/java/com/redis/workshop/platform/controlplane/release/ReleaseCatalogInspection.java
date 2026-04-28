package com.redis.workshop.platform.controlplane.release;

import java.util.List;

public record ReleaseCatalogInspection(
    boolean available,
    String failureReason,
    List<ReleaseCatalogEntry> releases
) {

    static ReleaseCatalogInspection available(List<ReleaseCatalogEntry> releases) {
        return new ReleaseCatalogInspection(true, null, List.copyOf(releases));
    }

    static ReleaseCatalogInspection unavailable(String failureReason) {
        return new ReleaseCatalogInspection(false, failureReason, List.of());
    }
}
