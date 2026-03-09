package com.redis.workshop.search.infrastructure;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA Controller for Full-Text Search workshop.
 * Forwards routes to Vue.js frontend.
 */
@Controller
public class SearchSpaController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    @GetMapping("/search")
    public String search() {
        return "forward:/index.html";
    }
}
