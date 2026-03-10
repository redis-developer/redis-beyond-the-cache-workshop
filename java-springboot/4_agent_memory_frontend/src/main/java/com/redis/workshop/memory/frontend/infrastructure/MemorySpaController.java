package com.redis.workshop.memory.frontend.infrastructure;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA Controller for Agent Memory workshop.
 * Forwards routes to Vue.js frontend.
 */
@Controller
public class MemorySpaController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    @GetMapping("/demo")
    public String demo() {
        return "forward:/index.html";
    }

    @GetMapping("/learn")
    public String learn() {
        return "forward:/index.html";
    }
}
