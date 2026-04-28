package com.redis.workshop.platform.controlplane.catalog;

import com.redis.workshop.platform.controlplane.session.SessionMode;

import java.util.List;

public record CatalogWorkshopResponse(
    String workshopId,
    String title,
    String description,
    String difficulty,
    int estimatedMinutes,
    String path,
    String defaultReleaseVersion,
    SessionMode defaultMode,
    List<SessionMode> supportedModes,
    List<String> topics
) {

    static CatalogWorkshopResponse fromEntry(WorkshopCatalogEntry entry) {
        return new CatalogWorkshopResponse(
            entry.workshopId(),
            entry.title(),
            entry.description(),
            entry.difficulty(),
            entry.estimatedMinutes(),
            entry.path(),
            entry.defaultReleaseVersion(),
            entry.defaultMode(),
            entry.supportedModes(),
            entry.topics()
        );
    }
}
