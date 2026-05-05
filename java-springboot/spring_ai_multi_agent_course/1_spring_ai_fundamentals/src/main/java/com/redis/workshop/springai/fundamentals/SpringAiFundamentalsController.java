package com.redis.workshop.springai.fundamentals;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SpringAiFundamentalsController {

    private final SpringAiDemoService service;

    public SpringAiFundamentalsController(SpringAiDemoService service) {
        this.service = service;
    }

    @GetMapping({"/api/health", "/login"})
    public Map<String, String> health() {
        return Map.of("status", "ready");
    }

    @GetMapping("/api/spring-ai/status")
    public StatusResponse status() {
        return service.status();
    }

    @PostMapping("/api/spring-ai/prompt")
    public PromptResponse prompt(@RequestBody PromptRequest request) {
        return service.prompt(request);
    }

    @PostMapping("/api/spring-ai/structured")
    public StructuredResponse structured(@RequestBody PromptRequest request) {
        return service.structured(request);
    }

    @PostMapping("/api/spring-ai/tools")
    public ToolsResponse tools(@RequestBody ToolsRequest request) {
        return service.tools(request);
    }

    @PostMapping("/api/spring-ai/memory")
    public MemoryResponse memory(@RequestBody MemoryRequest request) {
        return service.memory(request);
    }

    @PostMapping("/api/spring-ai/helper")
    public HelperResponse helper(@RequestBody HelperRequest request) {
        return service.helper(request);
    }
}
