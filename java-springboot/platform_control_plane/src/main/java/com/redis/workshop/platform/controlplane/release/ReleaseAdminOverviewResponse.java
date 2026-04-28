package com.redis.workshop.platform.controlplane.release;

import com.redis.workshop.platform.controlplane.audit.AuditEventResponse;
import com.redis.workshop.platform.controlplane.observability.ReleaseCatalogMetricsSnapshot;

import java.time.Instant;
import java.util.List;

public record ReleaseAdminOverviewResponse(
    Instant observedAt,
    boolean catalogAvailable,
    String currentFailureReason,
    ReleaseCatalogMetricsSnapshot metrics,
    List<ReleaseAdminCatalogEntryResponse> releases,
    List<AuditEventResponse> recentReleaseAuditEvents
) {
}
