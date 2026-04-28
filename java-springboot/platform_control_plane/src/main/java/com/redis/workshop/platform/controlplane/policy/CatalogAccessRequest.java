package com.redis.workshop.platform.controlplane.policy;

import java.util.LinkedHashSet;
import java.util.Set;

public record CatalogAccessRequest(
    String actorId,
    PolicyActorType actorType,
    Set<String> actorAuthorities,
    String workshopId,
    boolean publiclyVisible
) {

    public CatalogAccessRequest {
        actorAuthorities = actorAuthorities == null ? Set.of() : Set.copyOf(new LinkedHashSet<>(actorAuthorities));
    }
}
