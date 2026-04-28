package com.redis.workshop.platform.controlplane.policy;

public interface SessionProvisioningPolicy {

    SessionProvisioningProfile resolve(SessionProvisioningRequest request);
}
