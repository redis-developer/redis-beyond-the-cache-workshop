package com.redis.workshop.platform.controlplane.release;

import com.redis.workshop.platform.controlplane.session.SessionMode;

public record WorkshopLaunchDefaults(
    String defaultReleaseVersion,
    SessionMode defaultMode,
    boolean releaseBacked
) {

    public static WorkshopLaunchDefaults current() {
        return new WorkshopLaunchDefaults("current", SessionMode.LAB, false);
    }

    public static WorkshopLaunchDefaults releaseBacked(ReleaseCatalogEntry release) {
        return new WorkshopLaunchDefaults(release.releaseVersion(), release.mode(), true);
    }
}
