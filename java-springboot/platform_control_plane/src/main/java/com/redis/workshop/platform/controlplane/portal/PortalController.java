package com.redis.workshop.platform.controlplane.portal;

import com.redis.workshop.platform.controlplane.security.CurrentActor;
import com.redis.workshop.platform.controlplane.security.CurrentActorProvider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

@RestController
@RequestMapping("/api/portal")
public class PortalController {

    private final PortalSessionService portalSessionService;
    private final PortalSessionProperties properties;
    private final CurrentActorProvider currentActorProvider;

    public PortalController(
        PortalSessionService portalSessionService,
        PortalSessionProperties properties,
        CurrentActorProvider currentActorProvider
    ) {
        this.portalSessionService = portalSessionService;
        this.properties = properties;
        this.currentActorProvider = currentActorProvider;
    }

    @PostMapping("/login")
    ResponseEntity<PortalUserResponse> login(@Valid @RequestBody PortalLoginRequest request) {
        PortalLoginResult result = portalSessionService.createSession(
            request.email(),
            request.allowsMarketingContact()
        );
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, sessionCookie(result.token()).toString())
            .body(PortalUserResponse.authenticated(result.session()));
    }

    @GetMapping("/me")
    PortalUserResponse currentUser(HttpServletRequest request) {
        String token = readSessionToken(request);
        if (StringUtils.hasText(token)) {
            return portalSessionService.findByToken(token)
                .map(PortalUserResponse::authenticated)
                .orElseGet(PortalUserResponse::anonymous);
        }

        return currentActorProvider.findCurrentActor()
            .filter(CurrentActor::isLearner)
            .map(actor -> PortalUserResponse.authenticated(actor.actorId()))
            .orElseGet(PortalUserResponse::anonymous);
    }

    @PostMapping("/logout")
    ResponseEntity<PortalUserResponse> logout(HttpServletRequest request) {
        String token = readSessionToken(request);
        if (StringUtils.hasText(token)) {
            portalSessionService.logout(token);
        }
        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, expiredSessionCookie().toString())
            .body(PortalUserResponse.anonymous());
    }

    private String readSessionToken(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, properties.cookieName());
        return cookie == null ? null : cookie.getValue();
    }

    private ResponseCookie sessionCookie(String token) {
        return ResponseCookie.from(properties.cookieName(), token)
            .httpOnly(true)
            .secure(properties.secureCookie())
            .sameSite("Lax")
            .path("/")
            .maxAge(properties.ttl())
            .build();
    }

    private ResponseCookie expiredSessionCookie() {
        return ResponseCookie.from(properties.cookieName(), "")
            .httpOnly(true)
            .secure(properties.secureCookie())
            .sameSite("Lax")
            .path("/")
            .maxAge(Duration.ZERO)
            .build();
    }
}
