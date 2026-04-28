package com.redis.workshop.platform.controlplane.policy;

import java.util.LinkedHashSet;
import java.util.Set;

public record SessionCreatePolicyRequest(
    String actorId,
    PolicyActorType actorType,
    Set<String> actorAuthorities,
    String ownerUserId,
    String workshopId
) {

    public SessionCreatePolicyRequest {
        actorAuthorities = actorAuthorities == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(actorAuthorities));
    }
}
