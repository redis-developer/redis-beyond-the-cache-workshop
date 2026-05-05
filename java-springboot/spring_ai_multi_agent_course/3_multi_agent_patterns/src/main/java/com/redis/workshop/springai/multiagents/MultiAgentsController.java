package com.redis.workshop.springai.multiagents;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MultiAgentsController {

    private final MultiAgentsService service;

    public MultiAgentsController(MultiAgentsService service) {
        this.service = service;
    }

    @GetMapping({"/api/health", "/login"})
    public Map<String, String> health() {
        return Map.of("status", "ready");
    }

    @GetMapping("/api/multi-agents/status")
    public MultiAgentsStatusResponse status() {
        return service.status();
    }

    @GetMapping("/api/multi-agents/workflow")
    public MultiAgentsWorkflowResponse workflow() {
        return service.workflow();
    }

    @PostMapping("/api/multi-agents/run")
    public MultiAgentsRunResponse run(@RequestBody MultiAgentsRunRequest request) {
        try {
            return service.run(request);
        } catch (RuntimeException exception) {
            return new MultiAgentsRunResponse(false, exception.getMessage(), List.of(), List.of(), null, null, null, null, null, exception.getMessage());
        }
    }
}
