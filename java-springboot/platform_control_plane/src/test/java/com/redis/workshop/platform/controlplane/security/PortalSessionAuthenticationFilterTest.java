package com.redis.workshop.platform.controlplane.security;

import com.redis.workshop.platform.controlplane.portal.PortalSession;
import com.redis.workshop.platform.controlplane.portal.PortalSessionProperties;
import com.redis.workshop.platform.controlplane.portal.PortalSessionService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PortalSessionAuthenticationFilterTest {

    private static final PortalSessionProperties PROPERTIES = new PortalSessionProperties(
        "portal_session",
        Duration.ofHours(8),
        false,
        "test:portal:",
        "test:portal:marketing:"
    );

    private final PortalSessionService portalSessionService = mock(PortalSessionService.class);
    private final PortalSessionAuthenticationFilter filter = new PortalSessionAuthenticationFilter(
        () -> portalSessionService,
        PROPERTIES
    );

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesCurrentActorFromValidPortalCookie() throws Exception {
        when(portalSessionService.findByToken("opaque-token")).thenReturn(Optional.of(new PortalSession(
            "learner@example.com",
            Instant.parse("2026-04-21T10:00:00Z"),
            Instant.parse("2026-04-21T18:00:00Z"),
            List.of("sess-001")
        )));
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/sessions");
        request.setCookies(new Cookie("portal_session", "opaque-token"));

        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        assertThat(authentication).isNotNull();
        assertThat(authentication.getPrincipal()).isInstanceOf(CurrentActor.class);
        CurrentActor currentActor = (CurrentActor) authentication.getPrincipal();
        assertThat(currentActor.actorId()).isEqualTo("learner@example.com");
        assertThat(currentActor.actorType()).isEqualTo(CurrentActorType.LEARNER);
        assertThat(currentActor.authorities()).containsExactly(PlatformAuthorities.LEARNER);
    }

    @Test
    void ignoresRequestsWithoutPortalCookie() throws Exception {
        filter.doFilter(
            new MockHttpServletRequest("GET", "/api/sessions"),
            new MockHttpServletResponse(),
            new MockFilterChain()
        );

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(portalSessionService);
    }
}
