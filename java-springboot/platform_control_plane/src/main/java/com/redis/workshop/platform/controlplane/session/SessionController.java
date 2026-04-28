package com.redis.workshop.platform.controlplane.session;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @PostMapping
    ResponseEntity<SessionResponse> createSession(@Valid @RequestBody CreateSessionRequest request) {
        SessionResponse response = sessionService.createSession(request);
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
    ResponseEntity<SessionResponse> terminateSession(@PathVariable String sessionId) {
        SessionResponse response = sessionService.terminateSession(sessionId);
        return ResponseEntity.accepted().body(response);
    }
}
