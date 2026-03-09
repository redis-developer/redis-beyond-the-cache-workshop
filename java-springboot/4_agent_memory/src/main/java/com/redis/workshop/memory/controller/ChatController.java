package com.redis.workshop.memory.controller;

import com.redis.agentmemory.models.longtermemory.MemoryRecord;
import com.redis.workshop.memory.service.ChatService;
import com.redis.workshop.memory.service.ChatService.ChatRequest;
import com.redis.workshop.memory.service.ChatService.ChatResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@RequestBody ChatRequest request) {
        return chatService.chat(request);
    }

    @PostMapping("/memory")
    public Map<String, Object> storeMemory(@RequestBody Map<String, String> request) {
        chatService.storeMemory(request.get("userId"), request.get("text"));
        return Map.of("success", true);
    }

    @DeleteMapping("/session/{sessionId}")
    public Map<String, Object> clearSession(
            @PathVariable String sessionId,
            @RequestParam(required = false, defaultValue = "anonymous") String userId) {
        chatService.clearSession(userId, sessionId);
        return Map.of("success", true);
    }

    @PostMapping("/validate-key")
    public Map<String, Object> validateApiKey(@RequestBody Map<String, String> request) {
        String apiKey = request.get("apiKey");
        // Quick format validation - actual validation happens on first chat
        if (apiKey == null || apiKey.isBlank()) {
            return Map.of("valid", false, "error", "API key is required");
        }
        if (!apiKey.startsWith("sk-")) {
            return Map.of("valid", false, "error", "Invalid API key format");
        }
        return Map.of("valid", true);
    }

    @GetMapping("/memories/{userId}")
    public List<MemoryRecord> getLongTermMemories(@PathVariable String userId) {
        return chatService.getLongTermMemories(userId);
    }
}

