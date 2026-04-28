package com.redis.workshop.platform.controlplane.security;

public final class PlatformAuthorities {

    public static final String LEARNER = "platform:learner";
    public static final String ADMIN = "platform:admin";
    public static final String INTERNAL_SERVICE = "platform:internal-service";

    private PlatformAuthorities() {
    }
}
