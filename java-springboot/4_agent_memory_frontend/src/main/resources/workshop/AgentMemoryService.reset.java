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

    // TODO: Step 0 - Initialize the client field
    // private final MemoryAPIClient client;
    private final MemoryAPIClient client = null; // Placeholder until initialized

    public AgentMemoryService(
            @Value("${agent-memory.server.url}") String serverUrl,
            @Value("${agent-memory.server.namespace:workshop}") String namespace) {
        // TODO: Step 0 - Initialize the MemoryAPIClient
        // this.client = MemoryAPIClient.builder(serverUrl)
        //         .defaultNamespace(namespace)
        //         .timeout(30.0)
        //         .build();
    }

    // ==================== Working Memory Operations ====================

    public WorkingMemoryResponse getWorkingMemory(String sessionId) {
        // TODO: Step 1 - Implement using client.workingMemory().getWorkingMemory(sessionId)
        // try {
        //     return client.workingMemory().getWorkingMemory(sessionId);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to get working memory", e);
        // }
        return null; // Placeholder
    }

    public WorkingMemoryResponse putWorkingMemory(String sessionId, WorkingMemory memory) {
        // TODO: Step 2 - Implement using client.workingMemory().putWorkingMemory(sessionId, memory)
        // try {
        //     return client.workingMemory().putWorkingMemory(sessionId, memory);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to put working memory", e);
        // }
        return null; // Placeholder
    }

    public AckResponse deleteWorkingMemory(String sessionId) {
        // TODO: Step 3 - Implement using client.workingMemory().deleteWorkingMemory(sessionId)
        // try {
        //     return client.workingMemory().deleteWorkingMemory(sessionId);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to delete working memory", e);
        // }
        return null; // Placeholder
    }

    // ==================== Long-Term Memory Operations ====================

    public AckResponse createLongTermMemory(List<MemoryRecord> memories) {
        // TODO: Step 4 - Implement using client.longTermMemory().createLongTermMemories(memories)
        // try {
        //     return client.longTermMemory().createLongTermMemories(memories);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to create long-term memory", e);
        // }
        return null; // Placeholder
    }

    public MemoryRecordResults searchLongTermMemory(String text, String userId, int limit) {
        // TODO: Step 5 - Implement using client.longTermMemory().searchLongTermMemories(searchRequest)
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
        return null; // Placeholder
    }

    public MemoryRecord getMemory(String memoryId) {
        // TODO: Step 6 - Implement using client.longTermMemory().getLongTermMemory(memoryId)
        // try {
        //     return client.longTermMemory().getLongTermMemory(memoryId);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to get memory", e);
        // }
        return null; // Placeholder
    }

    public AckResponse deleteMemories(List<String> memoryIds) {
        // TODO: Step 7 - Implement using client.longTermMemory().deleteLongTermMemories(memoryIds)
        // try {
        //     return client.longTermMemory().deleteLongTermMemories(memoryIds);
        // } catch (Exception e) {
        //     throw new RuntimeException("Failed to delete memories", e);
        // }
        return null; // Placeholder
    }

    // ==================== Health Check ====================

    public HealthCheckResponse healthCheck() {
        // TODO: Implement using client.health().healthCheck()
        // try {
        //     return client.health().healthCheck();
        // } catch (Exception e) {
        //     throw new RuntimeException("Health check failed", e);
        // }
        return null; // Placeholder
    }

    // ==================== Helper Methods ====================

    public static MemoryMessage createMessage(String role, String content) {
        return MemoryMessage.builder()
                .role(role)
                .content(content)
                .build();
    }

    public static MemoryRecord createMemoryRecord(String text, String sessionId, String userId) {
        return MemoryRecord.builder()
                .text(text)
                .sessionId(sessionId)
                .userId(userId)
                .memoryType(MemoryType.SEMANTIC)
                .build();
    }
}
