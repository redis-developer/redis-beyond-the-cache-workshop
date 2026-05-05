package com.redis.workshop.springai.fundamentals.frontend.infrastructure;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SpringAiFundamentalsSpaController {

    @GetMapping({"/", "/0", "/1", "/2", "/3", "/4", "/5", "/6", "/editor"})
    public String app() {
        return "forward:/index.html";
    }
}
