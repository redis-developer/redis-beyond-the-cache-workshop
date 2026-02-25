package com.redis.workshop.locks.infrastructure;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA Controller for Distributed Locks workshop.
 * Forwards routes to Vue.js frontend.
 */
@Controller
public class LocksSpaController {

    @GetMapping("/")
    public String home() {
        return "forward:/index.html";
    }

    @GetMapping("/demo")
    public String demo() {
        return "forward:/index.html";
    }
}
