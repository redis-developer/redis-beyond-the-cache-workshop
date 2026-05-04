package com.redis.workshop.springai.fundamentals;

import java.util.Map;

import org.springframework.http.MediaType;
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

    @GetMapping("/api/health")
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

    @GetMapping(value = "/api/learner-app", produces = MediaType.TEXT_HTML_VALUE)
    public String learnerApp() {
        return """
            <!doctype html>
            <html lang="en">
              <head>
                <meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>Module 1: Spring AI Fundamentals</title>
              </head>
              <body>
                <main>
                  <h1>Module 1: Spring AI Fundamentals</h1>
                  <p>Learner app structure is ready for workshop content.</p>
                </main>
              </body>
            </html>
            """;
    }
}
