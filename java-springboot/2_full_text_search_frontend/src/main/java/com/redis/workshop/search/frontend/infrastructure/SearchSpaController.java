package com.redis.workshop.search.frontend.infrastructure;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class SearchSpaController {

    @GetMapping({"/", "/search"})
    public String app() {
        return "forward:/index.html";
    }
}
