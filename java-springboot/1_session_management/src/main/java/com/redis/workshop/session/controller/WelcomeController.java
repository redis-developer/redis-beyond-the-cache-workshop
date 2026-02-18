package com.redis.workshop.session.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * WORKSHOP TEACHING CONTENT - Main Workshop Controller
 *
 * This controller demonstrates the 3-stage workshop flow:
 * - Stage 1: Experience the problem (in-memory sessions lost on restart)
 * - Stage 2: Learn the solution (enable Redis session management)
 * - Stage 3: Verify it works (sessions persist across restarts)
 *
 * Key Learning Points:
 * - How to detect session storage type
 * - How sessions behave with in-memory vs Redis storage
 * - How to track workshop progress using session attributes
 */
@Controller
public class WelcomeController {

    @Value("${spring.session.store-type:none}")
    private String sessionStoreType;

    @GetMapping({"/", "/welcome"})
    public String welcome(HttpSession session, Principal principal, Model model) {
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

        // Check if Stage 1 has been completed (user has experienced session loss)
        Boolean stage1Completed = (Boolean) session.getAttribute("stage1Completed");
        boolean showStage2 = stage1Completed != null && stage1Completed;

        model.addAttribute("sessionInfo", sessionInfo);
        model.addAttribute("redisEnabled", redisSessionEnabled);
        model.addAttribute("stage1Completed", showStage2);
        return "welcome";
    }

    @PostMapping("/mark-stage1-complete")
    @ResponseBody
    public Map<String, Boolean> markStage1Complete(HttpSession session) {
        session.setAttribute("stage1Completed", true);
        return Map.of("success", true);
    }
}

