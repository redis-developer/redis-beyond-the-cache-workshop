package com.redis.workshop.platform.controlplane.security;

import com.redis.workshop.platform.controlplane.portal.PortalSession;
import com.redis.workshop.platform.controlplane.portal.PortalSessionProperties;
import com.redis.workshop.platform.controlplane.portal.PortalSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

public class PortalSessionAuthenticationFilter extends OncePerRequestFilter {

    private final Supplier<PortalSessionService> portalSessionService;
    private final PortalSessionProperties properties;

    public PortalSessionAuthenticationFilter(
        Supplier<PortalSessionService> portalSessionService,
        PortalSessionProperties properties
    ) {
        this.portalSessionService = portalSessionService;
        this.properties = properties;
    }

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

        Cookie cookie = WebUtils.getCookie(request, properties.cookieName());
        if (cookie == null) {
            filterChain.doFilter(request, response);
            return;
        }

        PortalSessionService service = portalSessionService.get();
        if (service == null) {
            filterChain.doFilter(request, response);
            return;
        }

        service.findByToken(cookie.getValue()).ifPresent(this::authenticate);
        filterChain.doFilter(request, response);
    }

    private void authenticate(PortalSession session) {
        Set<String> authorities = Set.of(PlatformAuthorities.LEARNER);
        CurrentActor currentActor = new CurrentActor(
            session.email(),
            CurrentActorType.LEARNER,
            authorities
        );
        List<SimpleGrantedAuthority> grantedAuthorities = authorities.stream()
            .map(SimpleGrantedAuthority::new)
            .toList();
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(currentActor, null, grantedAuthorities);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
