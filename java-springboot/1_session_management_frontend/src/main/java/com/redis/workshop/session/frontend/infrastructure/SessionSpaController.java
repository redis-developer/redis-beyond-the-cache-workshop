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
     * Root path - send learners to the stable workshop shell.
     */
    @GetMapping("/")
    public String root() {
        return "redirect:/welcome";
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

    /**
     * Learner app frame - served by Vue.js SPA.
     */
    @GetMapping("/app")
    public String app() {
        return "forward:/index.html";
    }
}
