package com.redis.workshop.infrastructure;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Shared SPA Controller for Vue.js frontend routing.
 *
 * This controller forwards SPA routes to index.html so Vue Router can handle them.
 * Each workshop can add additional routes as needed.
 */
@Controller
public class SpaController {

    /**
     * Editor page - served by Vue.js SPA.
     */
    @GetMapping("/editor")
    public String editor() {
        return "forward:/index.html";
    }
}

