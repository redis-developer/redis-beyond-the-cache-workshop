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
     * Root path sends learners to the stable workshop shell.
     */
    @GetMapping("/")
    public String root() {
        return "redirect:/0";
    }

    /**
     * Numbered workshop pages served by Vue.js SPA.
     */
    @GetMapping({"/0", "/1", "/2", "/3", "/4", "/redis-insight-view"})
    public String numberedPage() {
        return "forward:/index.html";
    }

    @GetMapping("/welcome")
    public String legacyWelcome() {
        return "redirect:/0";
    }

    @GetMapping("/editor")
    public String legacyEditor() {
        return "redirect:/4";
    }
}
