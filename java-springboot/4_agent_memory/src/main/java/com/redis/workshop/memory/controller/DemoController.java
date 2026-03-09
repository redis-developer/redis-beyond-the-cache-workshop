package com.redis.workshop.memory.controller;

import com.redis.agentmemory.models.longtermemory.MemoryRecord;
import com.redis.agentmemory.models.longtermemory.MemoryRecordResults;
import com.redis.agentmemory.models.longtermemory.MemoryType;
import com.redis.agentmemory.models.workingmemory.MemoryMessage;
import com.redis.agentmemory.models.workingmemory.WorkingMemory;
import com.redis.agentmemory.models.workingmemory.WorkingMemoryResponse;
import com.redis.workshop.memory.service.AgentMemoryService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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

        MemoryRecord memory = MemoryRecord.builder()
                .text(preference)
                .userId(userId)
                .memoryType(MemoryType.SEMANTIC)
                .topics(List.of("preferences", "user-settings"))
                .build();

        try {
            memoryService.createLongTermMemory(List.of(memory));
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
            MemoryRecordResults response = memoryService.searchLongTermMemory(query, userId, 5);
            return Map.of(
                    "success", true,
                    "query", query,
                    "userId", userId,
                    "memories", response.getMemories() != null ? response.getMemories() : List.of(),
                    "total", response.getTotal()
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

        WorkingMemory memory = WorkingMemory.builder()
                .sessionId(sessionId)
                .messages(List.of(MemoryMessage.builder().role("system").content("You are a helpful assistant.").build()))
                .userId(userId)
                .build();

        try {
            memoryService.putWorkingMemory(sessionId, memory);
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
            WorkingMemoryResponse current;
            try {
                current = memoryService.getWorkingMemory(sessionId);
            } catch (Exception e) {
                current = null;
            }

            List<MemoryMessage> messages = new ArrayList<>();
            if (current != null && current.getMessages() != null) {
                messages.addAll(current.getMessages());
            }
            messages.add(MemoryMessage.builder().role(role).content(content).build());

            WorkingMemory updated = WorkingMemory.builder()
                    .sessionId(sessionId)
                    .messages(messages)
                    .userId(current != null ? current.getUserId() : null)
                    .build();
            WorkingMemoryResponse result = memoryService.putWorkingMemory(sessionId, updated);

            return Map.of(
                    "success", true,
                    "sessionId", sessionId,
                    "messageCount", result.getMessages() != null ? result.getMessages().size() : 0,
                    "tokens", result.getTokens()
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
            WorkingMemoryResponse memory = memoryService.getWorkingMemory(sessionId);
            return Map.of(
                    "success", true,
                    "sessionId", sessionId,
                    "messages", memory.getMessages() != null ? memory.getMessages() : List.of(),
                    "context", memory.getContext() != null ? memory.getContext() : ""
            );
        } catch (Exception e) {
            return Map.of(
                    "success", false,
                    "error", e.getMessage()
            );
        }
    }
}

