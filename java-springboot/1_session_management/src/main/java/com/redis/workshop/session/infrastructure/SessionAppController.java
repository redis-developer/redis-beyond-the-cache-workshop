package com.redis.workshop.session.infrastructure;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SessionAppController {

    @GetMapping("/")
    public String root() {
        return "redirect:/app/";
    }

    @GetMapping({"/app", "/app/"})
    public String app() {
        return "forward:/app/index.html";
    }
}
