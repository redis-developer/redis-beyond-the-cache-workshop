package com.redis.workshop.platform.controlplane.release;

import com.redis.workshop.platform.controlplane.session.SessionMode;

import java.util.List;

public record ReleaseAdminCatalogEntryResponse(
    String releaseId,
    String workshopId,
    String releaseVersion,
    SessionMode mode,
    boolean defaultForWorkshop,
    String resourceClass,
    int sessionTtlMinutes,
    List<String> mutableDependencies
) {

    static ReleaseAdminCatalogEntryResponse fromEntry(ReleaseCatalogEntry entry) {
        return new ReleaseAdminCatalogEntryResponse(
            entry.releaseId(),
            entry.workshopId(),
            entry.releaseVersion(),
            entry.mode(),
            entry.defaultForWorkshop(),
            entry.resourceClass(),
            entry.sessionTtlMinutes(),
            entry.mutableDependencies()
        );
    }
}
