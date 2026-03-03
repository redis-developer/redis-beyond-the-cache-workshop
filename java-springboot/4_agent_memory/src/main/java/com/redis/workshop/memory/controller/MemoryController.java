package com.redis.workshop.memory.controller;

import com.redis.workshop.memory.model.*;
import com.redis.workshop.memory.service.AgentMemoryService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for Agent Memory operations.
 * Provides endpoints for working memory and long-term memory management.
 */
@RestController
@RequestMapping("/api/memory")
public class MemoryController {

    private final AgentMemoryService memoryService;

    public MemoryController(AgentMemoryService memoryService) {
        this.memoryService = memoryService;
    }

    // ==================== Working Memory Endpoints ====================

    @GetMapping("/working/{sessionId}")
    public WorkingMemory getWorkingMemory(@PathVariable String sessionId) {
        return memoryService.getWorkingMemory(sessionId).block();
    }

    @PutMapping("/working/{sessionId}")
    public WorkingMemory putWorkingMemory(
            @PathVariable String sessionId,
            @RequestBody WorkingMemory memory) {
        return memoryService.putWorkingMemory(sessionId, memory).block();
    }

    @DeleteMapping("/working/{sessionId}")
    public Map<String, Object> deleteWorkingMemory(@PathVariable String sessionId) {
        return memoryService.deleteWorkingMemory(sessionId).block();
    }

    @PostMapping("/working/{sessionId}/message")
    public WorkingMemory addMessage(
            @PathVariable String sessionId,
            @RequestBody MemoryMessage message) {
        // Get current memory, add message, and save
        WorkingMemory current = memoryService.getWorkingMemory(sessionId)
                .onErrorReturn(new WorkingMemory(sessionId, List.of(), null))
                .block();
        
        List<MemoryMessage> messages = new java.util.ArrayList<>(
                current.messages() != null ? current.messages() : List.of()
        );
        messages.add(message);
        
        WorkingMemory updated = new WorkingMemory(
                sessionId, messages, current.userId()
        );
        return memoryService.putWorkingMemory(sessionId, updated).block();
    }

    // ==================== Long-Term Memory Endpoints ====================

    @PostMapping("/long-term")
    public Map<String, Object> createMemory(@RequestBody CreateMemoryRequest request) {
        return memoryService.createLongTermMemory(request.memories()).block();
    }

    @PostMapping("/long-term/search")
    public SearchResponse searchMemory(@RequestBody SearchRequest request) {
        return memoryService.searchLongTermMemory(
                request.text(),
                request.userId() != null ? (String) request.userId().get("eq") : null,
                request.limit() != null ? request.limit() : 10
        ).block();
    }

    @GetMapping("/long-term/{memoryId}")
    public MemoryRecord getMemory(@PathVariable String memoryId) {
        return memoryService.getMemory(memoryId).block();
    }

    @DeleteMapping("/long-term")
    public Map<String, Object> deleteMemories(@RequestParam List<String> memoryIds) {
        return memoryService.deleteMemories(memoryIds).block();
    }

    // ==================== Health Check ====================

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        try {
            Map<String, Object> result = memoryService.healthCheck().block();
            return Map.of("status", "connected", "server", result);
        } catch (Exception e) {
            return Map.of("status", "disconnected", "error", e.getMessage());
        }
    }
}

