package com.redis.workshop.platform.controlplane.portal;

import java.time.Instant;

public record PortalContactPreference(
    String email,
    boolean allowMarketingContact,
    Instant updatedAt
) {
}
