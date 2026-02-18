package com.redis.workshop.session.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * WORKSHOP TEACHING CONTENT - Login Controller
 *
 * Simple login page controller for the workshop.
 * Authentication is handled by Spring Security (see SecurityConfig).
 *
 * This demonstrates how sessions are created upon login and
 * how they persist (or don't) depending on the storage mechanism.
 */
@Controller
public class LoginController {

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}

