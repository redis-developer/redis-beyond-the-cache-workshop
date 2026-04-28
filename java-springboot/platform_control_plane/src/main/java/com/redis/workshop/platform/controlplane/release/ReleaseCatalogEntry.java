package com.redis.workshop.platform.controlplane.release;

import com.redis.workshop.platform.controlplane.session.SessionMode;

import java.util.List;

public record ReleaseCatalogEntry(
    String releaseId,
    String workshopId,
    String releaseVersion,
    SessionMode mode,
    boolean defaultForWorkshop,
    ReleaseImageReferences images,
    String resourceClass,
    int sessionTtlMinutes,
    List<String> mutableDependencies,
    boolean enabled,
    List<String> environments
) {
    public ReleaseCatalogEntry(
        String releaseId,
        String workshopId,
        String releaseVersion,
        SessionMode mode,
        boolean defaultForWorkshop,
        ReleaseImageReferences images,
        String resourceClass,
        int sessionTtlMinutes,
        List<String> mutableDependencies
    ) {
        this(
            releaseId,
            workshopId,
            releaseVersion,
            mode,
            defaultForWorkshop,
            images,
            resourceClass,
            sessionTtlMinutes,
            mutableDependencies,
            true,
            List.of("local", "cloud-run")
        );
    }
}
