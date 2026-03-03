package com.redis.workshop.memory.service;

import com.redis.agentmemory.MemoryAPIClient;
import com.redis.agentmemory.models.common.AckResponse;
import com.redis.agentmemory.models.health.HealthCheckResponse;
import com.redis.agentmemory.models.longtermemory.MemoryRecord;
import com.redis.agentmemory.models.longtermemory.MemoryRecordResults;
import com.redis.agentmemory.models.longtermemory.MemoryType;
import com.redis.agentmemory.models.longtermemory.SearchRequest;
import com.redis.agentmemory.models.workingmemory.MemoryMessage;
import com.redis.agentmemory.models.workingmemory.WorkingMemory;
import com.redis.agentmemory.models.workingmemory.WorkingMemoryResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Service for interacting with the Agent Memory Server (AMS) using the official Java SDK.
 *
 * The SDK provides type-safe access to:
 * - Working Memory: Session-scoped conversation context (short-term)
 * - Long-Term Memory: Persistent facts with semantic search
 *
 * In this workshop, you will implement the methods to interact with AMS using the SDK.
 */
@Service
public class AgentMemoryService {

    private final MemoryAPIClient client;

    public AgentMemoryService(
            @Value("${agent-memory.server.url}") String serverUrl,
            @Value("${agent-memory.server.namespace:workshop}") String namespace) {
        // TODO: Step 0 - Initialize the MemoryAPIClient using the builder pattern
        // this.client = MemoryAPIClient.builder(serverUrl)
        //         .defaultNamespace(namespace)
        //         .timeout(30.0)
        //         .build();
        this.client = null;
    }

    // ==================== Working Memory Operations ====================

    /**
     * Get working memory for a session.
     *
     * SDK: client.workingMemory().getWorkingMemory(sessionId)
     */
    public WorkingMemoryResponse getWorkingMemory(String sessionId) {
        // TODO: Step 1 - Use the SDK to get working memory
        // try {
        //     return client.workingMemory().getWorkingMemory(sessionId);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to get working memory", e);
        // }
        return null;
    }

    /**
     * Save working memory for a session.
     *
     * SDK: client.workingMemory().putWorkingMemory(sessionId, memory)
     */
    public WorkingMemoryResponse putWorkingMemory(String sessionId, WorkingMemory memory) {
        // TODO: Step 2 - Use the SDK to save working memory
        // try {
        //     return client.workingMemory().putWorkingMemory(sessionId, memory);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to put working memory", e);
        // }
        return null;
    }

    /**
     * Delete working memory for a session.
     *
     * SDK: client.workingMemory().deleteWorkingMemory(sessionId)
     */
    public AckResponse deleteWorkingMemory(String sessionId) {
        // TODO: Step 3 - Use the SDK to delete working memory
        // try {
        //     return client.workingMemory().deleteWorkingMemory(sessionId);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to delete working memory", e);
        // }
        return null;
    }

    // ==================== Long-Term Memory Operations ====================

    /**
     * Create long-term memories (facts about the user).
     *
     * SDK: client.longTermMemory().createLongTermMemories(memories)
     */
    public AckResponse createLongTermMemory(List<MemoryRecord> memories) {
        // TODO: Step 4 - Use the SDK to create long-term memories
        // try {
        //     return client.longTermMemory().createLongTermMemories(memories);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to create long-term memory", e);
        // }
        return null;
    }

    /**
     * Search long-term memory using semantic search.
     *
     * SDK: client.longTermMemory().searchLongTermMemories(searchRequest)
     *
     * This uses vector similarity to find relevant memories!
     */
    public MemoryRecordResults searchLongTermMemory(String text, String userId, int limit) {
        // TODO: Step 5 - Use the SDK to search long-term memories
        // try {
        //     SearchRequest request = SearchRequest.builder()
        //             .text(text)
        //             .userId(userId)
        //             .limit(limit)
        //             .build();
        //     return client.longTermMemory().searchLongTermMemories(request);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to search long-term memory", e);
        // }
        return null;
    }

    /**
     * Get a specific memory by ID.
     *
     * SDK: client.longTermMemory().getLongTermMemory(memoryId)
     */
    public MemoryRecord getMemory(String memoryId) {
        // TODO: Step 6 - Use the SDK to get a specific memory
        // try {
        //     return client.longTermMemory().getLongTermMemory(memoryId);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to get memory", e);
        // }
        return null;
    }

    /**
     * Delete long-term memories by IDs.
     */
    public AckResponse deleteMemories(List<String> memoryIds) {
        try {
            return client.longTermMemory().deleteLongTermMemories(memoryIds);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete memories", e);
        }
    }

    // ==================== Health Check ====================

    /**
     * Check if the Agent Memory Server is healthy.
     */
    public HealthCheckResponse healthCheck() {
        try {
            return client.health().healthCheck();
        } catch (Exception e) {
            throw new RuntimeException("Health check failed", e);
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Convenience method to create a MemoryMessage.
     */
    public static MemoryMessage createMessage(String role, String content) {
        return MemoryMessage.builder()
                .role(role)
                .content(content)
                .build();
    }

    /**
     * Convenience method to create a MemoryRecord for long-term storage.
     */
    public static MemoryRecord createMemoryRecord(String text, String sessionId, String userId) {
        return MemoryRecord.builder()
                .text(text)
                .sessionId(sessionId)
                .userId(userId)
                .memoryType(MemoryType.SEMANTIC)
                .build();
    }
}

