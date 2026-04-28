package com.redis.workshop.platform.controlplane.admin;

public record AdminSessionCountResponse(
    String workshopId,
    String mode,
    String state,
    long count
) {
}
