package com.redis.workshop.platform.controlplane.policy;

import java.util.LinkedHashSet;
import java.util.Set;

public record SessionAccessPolicyRequest(
    String actorId,
    PolicyActorType actorType,
    Set<String> actorAuthorities,
    String ownerUserId,
    String sessionId
) {

    public SessionAccessPolicyRequest {
        actorAuthorities = actorAuthorities == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(actorAuthorities));
    }
}
