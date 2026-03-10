package com.redis.workshop.session.frontend.infrastructure;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA Controller for Session Management workshop.
 * Handles SPA routing for the Vue.js frontend.
 */
@Controller
public class SessionSpaController {

    /**
     * Root path - send learners to the login route.
     */
    @GetMapping("/")
    public String root() {
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
