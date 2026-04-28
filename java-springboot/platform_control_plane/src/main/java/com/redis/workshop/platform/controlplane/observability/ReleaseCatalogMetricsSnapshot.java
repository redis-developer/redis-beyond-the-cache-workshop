package com.redis.workshop.platform.controlplane.observability;

import java.time.Instant;

public record ReleaseCatalogMetricsSnapshot(
    double catalogLoadSuccessCount,
    double catalogLoadFailureCount,
    double releaseLookupMissCount,
    double releaseLaunchAttemptCount,
    long catalogEntryCount,
    long catalogStalenessSeconds,
    Instant lastCatalogLoadAttemptAt,
    Instant lastSuccessfulCatalogLoadAt,
    Instant lastCatalogLoadFailureAt,
    String lastCatalogLoadFailureReason
) {
}
