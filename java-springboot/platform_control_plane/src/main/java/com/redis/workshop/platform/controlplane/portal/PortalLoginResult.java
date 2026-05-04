package com.redis.workshop.platform.controlplane.portal;

public record PortalLoginResult(
    String token,
    PortalSession session
) {
}
