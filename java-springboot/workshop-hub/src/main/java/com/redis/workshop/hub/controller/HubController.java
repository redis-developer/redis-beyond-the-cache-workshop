package com.redis.workshop.hub.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Hub Controller - Serves the Vue.js SPA
 *
 * The Vue.js frontend is served from /static/vue/ directory.
 * All routes are handled by Vue Router, with fallback to index.html
 * configured in WebConfig.java
 */
@Controller
public class HubController {

    /**
     * Serve the Vue.js SPA for the root path
     * The actual routing is handled by Vue Router on the frontend
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }
}

