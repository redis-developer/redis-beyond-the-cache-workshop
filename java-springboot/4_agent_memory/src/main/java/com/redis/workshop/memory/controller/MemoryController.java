package com.redis.workshop.memory.controller;

import com.redis.agentmemory.models.common.AckResponse;
import com.redis.agentmemory.models.health.HealthCheckResponse;
import com.redis.agentmemory.models.longtermemory.MemoryRecord;
import com.redis.agentmemory.models.longtermemory.MemoryRecordResults;
import com.redis.agentmemory.models.workingmemory.MemoryMessage;
import com.redis.agentmemory.models.workingmemory.WorkingMemory;
import com.redis.agentmemory.models.workingmemory.WorkingMemoryResponse;
import com.redis.workshop.memory.service.AgentMemoryService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
    public WorkingMemoryResponse getWorkingMemory(@PathVariable String sessionId) {
        return memoryService.getWorkingMemory(sessionId);
    }

    @PutMapping("/working/{sessionId}")
    public WorkingMemoryResponse putWorkingMemory(
            @PathVariable String sessionId,
            @RequestBody WorkingMemory memory) {
        return memoryService.putWorkingMemory(sessionId, memory);
    }

    @DeleteMapping("/working/{sessionId}")
    public AckResponse deleteWorkingMemory(@PathVariable String sessionId) {
        return memoryService.deleteWorkingMemory(sessionId);
    }

    @PostMapping("/working/{sessionId}/message")
    public WorkingMemoryResponse addMessage(
            @PathVariable String sessionId,
            @RequestBody MessageRequest request) {
        // Get current memory, add message, and save
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
        messages.add(MemoryMessage.builder().role(request.role()).content(request.content()).build());

        WorkingMemory updated = WorkingMemory.builder()
                .sessionId(sessionId)
                .messages(messages)
                .userId(current != null ? current.getUserId() : null)
                .build();
        return memoryService.putWorkingMemory(sessionId, updated);
    }

    public record MessageRequest(String role, String content) {}

    // ==================== Long-Term Memory Endpoints ====================

    @PostMapping("/long-term")
    public AckResponse createMemory(@RequestBody List<MemoryRecord> memories) {
        return memoryService.createLongTermMemory(memories);
    }

    @PostMapping("/long-term/search")
    public MemoryRecordResults searchMemory(@RequestBody SearchRequestDto request) {
        return memoryService.searchLongTermMemory(
                request.text(),
                request.userId(),
                request.limit() != null ? request.limit() : 10
        );
    }

    public record SearchRequestDto(String text, String userId, Integer limit) {}

    @GetMapping("/long-term/{memoryId}")
    public MemoryRecord getMemory(@PathVariable String memoryId) {
        return memoryService.getMemory(memoryId);
    }

    @DeleteMapping("/long-term")
    public AckResponse deleteMemories(@RequestParam List<String> memoryIds) {
        return memoryService.deleteMemories(memoryIds);
    }

    // ==================== Health Check ====================

    @GetMapping("/health")
    public Map<String, Object> healthCheck() {
        try {
            HealthCheckResponse result = memoryService.healthCheck();
            return Map.of("status", "connected", "server", result);
        } catch (Exception e) {
            return Map.of("status", "disconnected", "error", e.getMessage());
        }
    }
}

