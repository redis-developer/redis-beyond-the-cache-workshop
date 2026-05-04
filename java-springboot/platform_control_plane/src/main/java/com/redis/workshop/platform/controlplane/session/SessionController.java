package com.redis.workshop.platform.controlplane.session;

import com.redis.workshop.platform.controlplane.portal.PortalSessionProperties;
import com.redis.workshop.platform.controlplane.portal.PortalSessionService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;
    private final PortalSessionService portalSessionService;
    private final PortalSessionProperties portalSessionProperties;

    public SessionController(
        SessionService sessionService,
        PortalSessionService portalSessionService,
        PortalSessionProperties portalSessionProperties
    ) {
        this.sessionService = sessionService;
        this.portalSessionService = portalSessionService;
        this.portalSessionProperties = portalSessionProperties;
    }

    @PostMapping
    ResponseEntity<SessionResponse> createSession(
        @Valid @RequestBody CreateSessionRequest request,
        HttpServletRequest servletRequest
    ) {
        SessionResponse response = sessionService.createSession(request);
        attachPortalSession(servletRequest, response.sessionId());
        return ResponseEntity.accepted()
            .header("Location", response.pollUrl())
            .body(response);
    }

    @GetMapping
    List<SessionResponse> listSessions() {
        return sessionService.listSessions();
    }

    @GetMapping("/{sessionId}")
    SessionResponse getSession(@PathVariable String sessionId) {
        return sessionService.getSession(sessionId);
    }

    @GetMapping("/{sessionId}/shell")
    SessionShellResponse getSessionShell(@PathVariable String sessionId) {
        return sessionService.getSessionShell(sessionId);
    }

    @PostMapping("/{sessionId}/restart")
    ResponseEntity<SessionResponse> restartSession(
        @PathVariable String sessionId,
        @RequestBody(required = false) RestartSessionRequest request
    ) {
        SessionResponse response = sessionService.restartSession(
            sessionId,
            request == null ? new RestartSessionRequest(false) : request
        );
        return ResponseEntity.accepted().body(response);
    }

    @DeleteMapping("/{sessionId}")
    ResponseEntity<SessionResponse> terminateSession(@PathVariable String sessionId, HttpServletRequest request) {
        SessionResponse response = sessionService.terminateSession(sessionId);
        detachPortalSession(request, response.sessionId());
        return ResponseEntity.accepted().body(response);
    }

    private void attachPortalSession(HttpServletRequest request, String sessionId) {
        String token = readPortalSessionToken(request);
        if (StringUtils.hasText(token)) {
            portalSessionService.attachSession(token, sessionId);
        }
    }

    private void detachPortalSession(HttpServletRequest request, String sessionId) {
        String token = readPortalSessionToken(request);
        if (StringUtils.hasText(token)) {
            portalSessionService.detachSession(token, sessionId);
        }
    }

    private String readPortalSessionToken(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, portalSessionProperties.cookieName());
        return cookie == null ? null : cookie.getValue();
    }
}
