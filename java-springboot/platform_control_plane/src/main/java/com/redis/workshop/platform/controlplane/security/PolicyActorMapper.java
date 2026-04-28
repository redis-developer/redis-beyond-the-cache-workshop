package com.redis.workshop.platform.controlplane.security;

import com.redis.workshop.platform.controlplane.policy.PolicyActorType;

public final class PolicyActorMapper {

    private PolicyActorMapper() {
    }

    public static PolicyActorType toPolicyActorType(CurrentActor currentActor) {
        if (currentActor == null) {
            return PolicyActorType.ANONYMOUS;
        }
        return switch (currentActor.actorType()) {
            case LEARNER -> PolicyActorType.LEARNER;
            case ADMIN -> PolicyActorType.ADMIN;
            case INTERNAL_SERVICE -> PolicyActorType.INTERNAL_SERVICE;
        };
    }
}
