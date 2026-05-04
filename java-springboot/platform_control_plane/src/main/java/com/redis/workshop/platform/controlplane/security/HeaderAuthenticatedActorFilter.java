package com.redis.workshop.platform.controlplane.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class HeaderAuthenticatedActorFilter extends OncePerRequestFilter {

    public static final String ACTOR_ID_HEADER = "X-Platform-Actor-Id";
    public static final String ACTOR_TYPE_HEADER = "X-Platform-Actor-Type";
    public static final String ACTOR_ROLES_HEADER = "X-Platform-Roles";

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication existingAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (existingAuthentication != null && existingAuthentication.isAuthenticated()) {
            filterChain.doFilter(request, response);
            return;
        }

        String actorId = request.getHeader(ACTOR_ID_HEADER);
        if (!StringUtils.hasText(actorId)) {
            filterChain.doFilter(request, response);
            return;
        }

        CurrentActorType actorType;
        try {
            actorType = parseActorType(request.getHeader(ACTOR_TYPE_HEADER));
        } catch (IllegalArgumentException ex) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, ex.getMessage());
            return;
        }

        Set<String> authorities = resolveAuthorities(actorType, request.getHeader(ACTOR_ROLES_HEADER));
        List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream()
            .map(SimpleGrantedAuthority::new)
            .toList();

        CurrentActor currentActor = new CurrentActor(actorId.trim(), actorType, authorities);
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(currentActor, null, grantedAuthorities);
        authentication.setDetails(new HttpHeaders());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    private CurrentActorType parseActorType(String value) {
        if (!StringUtils.hasText(value)) {
            return CurrentActorType.LEARNER;
        }
        return CurrentActorType.valueOf(value.trim().toUpperCase());
    }

    private Set<String> resolveAuthorities(CurrentActorType actorType, String headerValue) {
        LinkedHashSet<String> authorities = new LinkedHashSet<>();
        authorities.add(defaultAuthority(actorType));
        if (StringUtils.hasText(headerValue)) {
            authorities.addAll(Arrays.stream(headerValue.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new)));
        }
        return Set.copyOf(authorities);
    }

    private String defaultAuthority(CurrentActorType actorType) {
        return switch (actorType) {
            case LEARNER -> PlatformAuthorities.LEARNER;
            case ADMIN -> PlatformAuthorities.ADMIN;
            case INTERNAL_SERVICE -> PlatformAuthorities.INTERNAL_SERVICE;
        };
    }
}
