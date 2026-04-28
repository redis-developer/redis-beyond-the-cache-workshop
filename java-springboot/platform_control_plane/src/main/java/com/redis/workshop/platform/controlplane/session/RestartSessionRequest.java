package com.redis.workshop.platform.controlplane.session;

public record RestartSessionRequest(
    boolean rebuild
) {
}
