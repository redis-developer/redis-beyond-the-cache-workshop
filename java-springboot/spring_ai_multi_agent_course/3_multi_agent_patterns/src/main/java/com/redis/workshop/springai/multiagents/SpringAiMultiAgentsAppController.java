package com.redis.workshop.springai.multiagents;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpringAiMultiAgentsAppController {

    @GetMapping("/")
    public String root() {
        return "redirect:/app/";
    }

    @GetMapping({"/app", "/app/"})
    public String app() {
        return "forward:/app/index.html";
    }
}
