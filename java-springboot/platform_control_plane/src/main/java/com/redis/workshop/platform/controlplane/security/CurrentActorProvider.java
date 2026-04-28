package com.redis.workshop.platform.controlplane.security;

import java.util.Optional;

public interface CurrentActorProvider {

    Optional<CurrentActor> findCurrentActor();

    CurrentActor requireCurrentActor();
}
