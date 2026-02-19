package com.redis.workshop.infrastructure;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Shared SPA Controller for Vue.js frontend routing.
 * 
 * This controller handles the common SPA routes that all workshops need.
 * Workshops can extend this or use it directly if they don't need custom routes.
 */
@Controller
public class SpaController {

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
     * Welcome page - served by Vue.js SPA (requires authentication via Spring Security).
     */
    @GetMapping("/welcome")
    public String welcome() {
        return "forward:/index.html";
    }

    /**
     * Editor page - served by Vue.js SPA (requires authentication via Spring Security).
     */
    @GetMapping("/editor")
    public String editor() {
        return "forward:/index.html";
    }
}

