package com.redis.workshop.platform.controlplane.admin;

import com.redis.workshop.platform.controlplane.audit.AuditEventResponse;
import com.redis.workshop.platform.controlplane.observability.SessionMetricsSnapshot;

import java.time.Instant;
import java.util.List;

public record AdminObservabilityOverviewResponse(
    Instant generatedAt,
    List<AdminSessionCountResponse> activeSessions,
    List<AdminProvisioningFailureSummaryResponse> recentProvisioningFailures,
    List<AdminPendingSessionResponse> pendingSessions,
    List<AuditEventResponse> recentAuditEvents,
    SessionMetricsSnapshot metrics
) {
}
