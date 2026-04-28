package com.redis.workshop.platform.controlplane.session;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/session")
public class SessionRouteProxyController {

    private final SessionRouteProxyService sessionRouteProxyService;

    @Autowired
    public SessionRouteProxyController(SessionRouteProxyService sessionRouteProxyService) {
        this.sessionRouteProxyService = sessionRouteProxyService;
    }

    @RequestMapping(
        value = {
            "/{sessionId}",
            "/{sessionId}/",
            "/{sessionId}/**"
        },
        method = {
            RequestMethod.GET,
            RequestMethod.POST,
            RequestMethod.PUT,
            RequestMethod.DELETE,
            RequestMethod.PATCH,
            RequestMethod.OPTIONS
        }
    )
    public ResponseEntity<byte[]> proxyRequest(
        @PathVariable String sessionId,
        HttpServletRequest request,
        @RequestBody(required = false) byte[] body
    ) {
        return sessionRouteProxyService.proxy(sessionId, request, body);
    }
}
