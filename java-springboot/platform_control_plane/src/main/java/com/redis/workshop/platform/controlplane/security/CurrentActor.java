package com.redis.workshop.platform.controlplane.security;

import java.util.LinkedHashSet;
import java.util.Set;

public record CurrentActor(
    String actorId,
    CurrentActorType actorType,
    Set<String> authorities
) {

    public CurrentActor {
        authorities = authorities == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(authorities));
    }

    public boolean hasAuthority(String authority) {
        return authorities.contains(authority);
    }

    public boolean isLearner() {
        return actorType == CurrentActorType.LEARNER || hasAuthority(PlatformAuthorities.LEARNER);
    }

    public boolean isAdmin() {
        return actorType == CurrentActorType.ADMIN || hasAuthority(PlatformAuthorities.ADMIN);
    }

    public boolean isInternalService() {
        return actorType == CurrentActorType.INTERNAL_SERVICE
            || hasAuthority(PlatformAuthorities.INTERNAL_SERVICE);
    }
}
