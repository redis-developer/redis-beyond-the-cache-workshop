package com.redis.workshop.locks.frontend.infrastructure;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * SPA Controller for Distributed Locks workshop.
 * Forwards routes to Vue.js frontend.
 */
@Controller
public class LocksSpaController {

    @GetMapping({
        "/",
        "/reentrant",
        "/reentrant/implement",
        "/reentrant/editor",
        "/reentrant/demo",
        "/demo",
        "/implement",
        "/implementation",
        "/complete",
        "/scenario/{index}",
        "/scenario/{index}/{step}"
    })
    public String app() {
        return "forward:/index.html";
    }
}
