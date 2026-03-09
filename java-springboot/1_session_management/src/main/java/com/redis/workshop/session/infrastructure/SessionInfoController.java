package com.redis.workshop.session.infrastructure;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Workshop Infrastructure - Session Info Controller
 *
 * Provides API endpoints for the workshop frontend to display session information.
 * This is infrastructure code, not part of the workshop learning content.
 */
@RestController
@RequestMapping("/api")
public class SessionInfoController {

    @Value("${spring.session.store-type:none}")
    private String sessionStoreType;

    /**
     * API endpoint for Vue.js frontend to fetch session information
     */
    @GetMapping("/session-info")
    @ResponseBody
    public Map<String, Object> getSessionInfo(HttpSession session, Principal principal) {
        // If principal is null, the user is not authenticated (session is invalid)
        if (principal == null) {
            throw new org.springframework.security.access.AccessDeniedException("Not authenticated");
        }

        Map<String, Object> sessionInfo = new LinkedHashMap<>();
        sessionInfo.put("sessionId", session.getId());
        sessionInfo.put("username", principal.getName());
        sessionInfo.put("creationTime", new Date(session.getCreationTime()));
        sessionInfo.put("lastAccessedTime", new Date(session.getLastAccessedTime()));
        sessionInfo.put("maxInactiveInterval", session.getMaxInactiveInterval() + " seconds");

        // Detect if Redis session is enabled by checking the configuration property
        boolean redisSessionEnabled = "redis".equalsIgnoreCase(sessionStoreType);
        String sessionStorage = redisSessionEnabled ? "Redis (Distributed)" : "In-Memory (Default)";
        sessionInfo.put("sessionStorage", sessionStorage);
        sessionInfo.put("redisEnabled", redisSessionEnabled);

        // Check if Stage 1 has been completed (user has experienced session loss)
        Boolean stage1Completed = (Boolean) session.getAttribute("stage1Completed");
        sessionInfo.put("stage1Completed", stage1Completed != null && stage1Completed);

        return sessionInfo;
    }

    @PostMapping("/mark-stage1-complete")
    @ResponseBody
    public Map<String, Boolean> markStage1Complete(HttpSession session) {
        session.setAttribute("stage1Completed", true);
        return Map.of("success", true);
    }
}

