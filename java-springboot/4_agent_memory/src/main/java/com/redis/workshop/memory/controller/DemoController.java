package com.redis.workshop.memory.controller;

import com.redis.workshop.memory.model.*;
import com.redis.workshop.memory.service.AgentMemoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Demo controller with simplified endpoints for the workshop.
 */
@RestController
@RequestMapping("/api/demo")
public class DemoController {

    private final AgentMemoryService memoryService;

    public DemoController(AgentMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    /**
     * Demo: Store a user preference (long-term memory).
     */
    @PostMapping("/preference")
    public Map<String, Object> storePreference(
            @RequestParam String userId,
            @RequestParam String preference) {
        
        var memory = new CreateMemoryRequest.ExtractedMemoryRecord(
                preference,
                "semantic",
                List.of("preferences", "user-settings"),
                userId
        );
        
        try {
            memoryService.createLongTermMemory(List.of(memory)).block();
            return Map.of(
                    "success", true,
                    "message", "Preference stored in long-term memory",
                    "userId", userId,
                    "preference", preference
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * Demo: Search for user preferences.
     */
    @GetMapping("/preferences")
    public Map<String, Object> searchPreferences(
            @RequestParam String userId,
            @RequestParam(defaultValue = "preferences") String query) {
        
        try {
            SearchResponse response = memoryService.searchLongTermMemory(query, userId, 5).block();
            return Map.of(
                    "success", true,
                    "query", query,
                    "userId", userId,
                    "memories", response.memories() != null ? response.memories() : List.of(),
                    "total", response.total() != null ? response.total() : 0
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * Demo: Start a conversation (working memory).
     */
    @PostMapping("/conversation")
    public Map<String, Object> startConversation(@RequestParam String userId) {
        String sessionId = "session-" + UUID.randomUUID().toString().substring(0, 8);
        
        var memory = new WorkingMemory(
                sessionId,
                List.of(new MemoryMessage("system", "You are a helpful assistant.")),
                userId
        );
        
        try {
            memoryService.putWorkingMemory(sessionId, memory).block();
            return Map.of(
                    "success", true,
                    "sessionId", sessionId,
                    "userId", userId,
                    "message", "Conversation started"
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * Demo: Add a message to conversation.
     */
    @PostMapping("/conversation/{sessionId}/message")
    public Map<String, Object> addMessage(
            @PathVariable String sessionId,
            @RequestParam String role,
            @RequestParam String content) {
        
        try {
            WorkingMemory current = memoryService.getWorkingMemory(sessionId)
                    .onErrorReturn(new WorkingMemory(sessionId, List.of(), null))
                    .block();
            
            List<MemoryMessage> messages = new java.util.ArrayList<>(
                    current.messages() != null ? current.messages() : List.of()
            );
            messages.add(new MemoryMessage(role, content));
            
            WorkingMemory updated = new WorkingMemory(sessionId, messages, current.userId());
            WorkingMemory result = memoryService.putWorkingMemory(sessionId, updated).block();
            
            return Map.of(
                    "success", true,
                    "sessionId", sessionId,
                    "messageCount", result.messages() != null ? result.messages().size() : 0,
                    "tokens", result.tokens() != null ? result.tokens() : 0
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }

    /**
     * Demo: Get conversation history.
     */
    @GetMapping("/conversation/{sessionId}")
    public Map<String, Object> getConversation(@PathVariable String sessionId) {
        try {
            WorkingMemory memory = memoryService.getWorkingMemory(sessionId).block();
            return Map.of(
                    "success", true,
                    "sessionId", sessionId,
                    "messages", memory.messages() != null ? memory.messages() : List.of(),
                    "context", memory.context() != null ? memory.context() : ""
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }
}

