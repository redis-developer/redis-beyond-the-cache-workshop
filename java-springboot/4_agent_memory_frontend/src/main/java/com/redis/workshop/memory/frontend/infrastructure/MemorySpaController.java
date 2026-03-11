package com.redis.workshop.memory.frontend.infrastructure;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA Controller for Agent Memory workshop.
 * Forwards routes to Vue.js frontend.
 */
@Controller
public class MemorySpaController {

    @GetMapping({"/", "/challenges", "/demo", "/learn", "/lab"})
    public String app() {
        return "forward:/index.html";
    }
}
