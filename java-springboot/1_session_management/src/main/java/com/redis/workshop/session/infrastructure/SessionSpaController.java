package com.redis.workshop.session.infrastructure;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA Controller for Session Management workshop.
 * Handles authentication-aware routing for the Vue.js frontend.
 */
@Controller
public class SessionSpaController {

    /**
     * Root path - redirect to login or welcome based on authentication.
     */
    @GetMapping("/")
    public String root(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()) {
            return "redirect:/welcome";
        }
        return "redirect:/login";
    }

    /**
     * Login page - served by Vue.js SPA.
     */
    @GetMapping("/login")
    public String login() {
        return "forward:/index.html";
    }

    /**
     * Welcome page - served by Vue.js SPA.
     */
    @GetMapping("/welcome")
    public String welcome() {
        return "forward:/index.html";
    }
}

