package com.redis.workshop.platform.controlplane.policy;

import java.util.LinkedHashSet;
import java.util.Set;

public record AdminActionPolicyRequest(
    String actorId,
    PolicyActorType actorType,
    Set<String> actorAuthorities,
    String action,
    String targetResourceType,
    String targetResourceId
) {

    public AdminActionPolicyRequest {
        actorAuthorities = actorAuthorities == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(actorAuthorities));
    }
}
