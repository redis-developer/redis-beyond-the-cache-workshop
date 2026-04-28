package com.redis.workshop.platform.controlplane.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

public class SecurityContextCurrentActorProvider implements CurrentActorProvider {

    @Override
    public Optional<CurrentActor> findCurrentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof CurrentActor currentActor) {
            return Optional.of(currentActor);
        }
        return Optional.empty();
    }

    @Override
    public CurrentActor requireCurrentActor() {
        return findCurrentActor().orElseThrow(
            () -> new AuthenticationCredentialsNotFoundException("Authenticated actor is required")
        );
    }
}
