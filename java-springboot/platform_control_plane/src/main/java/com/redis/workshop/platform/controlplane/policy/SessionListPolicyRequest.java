package com.redis.workshop.platform.controlplane.policy;

import java.util.LinkedHashSet;
import java.util.Set;

public record SessionListPolicyRequest(
    String actorId,
    PolicyActorType actorType,
    Set<String> actorAuthorities,
    String ownerUserId
) {

    public SessionListPolicyRequest {
        actorAuthorities = actorAuthorities == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(actorAuthorities));
    }
}
