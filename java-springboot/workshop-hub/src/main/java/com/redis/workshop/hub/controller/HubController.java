package com.redis.workshop.hub.controller;

import com.redis.workshop.hub.model.Workshop;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
public class HubController {

    @GetMapping("/")
    public String index(Model model) {
        List<Workshop> workshops = getAvailableWorkshops();
        model.addAttribute("workshops", workshops);
        return "index";
    }

    private List<Workshop> getAvailableWorkshops() {
        List<Workshop> workshops = new ArrayList<>();
        
        workshops.add(new Workshop(
            "1_session_management",
            "Distributed Session Management",
            "Learn how to implement distributed session management using Redis and Spring Session. " +
            "Experience the problem of in-memory sessions, then solve it with Redis-backed sessions " +
            "that persist across application restarts and enable horizontal scaling.",
            "Beginner",
            30,
            "http://localhost:8080",
            new String[]{"Sessions", "Spring Session", "Distributed Systems", "Scalability"}
        ));
        
        // Future workshops can be added here
        // workshops.add(new Workshop(...));
        
        return workshops;
    }
}

