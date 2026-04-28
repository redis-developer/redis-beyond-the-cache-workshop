package com.redis.workshop.platform.controlplane.catalog;

import com.redis.workshop.platform.controlplane.session.SessionMode;

import java.util.List;

public record WorkshopCatalogEntry(
    String workshopId,
    String title,
    String description,
    String difficulty,
    int estimatedMinutes,
    String path,
    String defaultReleaseVersion,
    SessionMode defaultMode,
    List<SessionMode> supportedModes,
    List<String> topics,
    boolean publiclyVisible
) {
}
