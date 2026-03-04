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
        this.client = MemoryAPIClient.builder(serverUrl)
                .defaultNamespace(namespace)
                .timeout(30.0)
                .build();
    }

    // ==================== Working Memory Operations ====================

    public WorkingMemoryResponse getWorkingMemory(String sessionId) {
        try {
            return client.workingMemory().getWorkingMemory(sessionId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get working memory", e);
        }
    }

    public WorkingMemoryResponse putWorkingMemory(String sessionId, WorkingMemory memory) {
        try {
            return client.workingMemory().putWorkingMemory(sessionId, memory);
        } catch (Exception e) {
            throw new RuntimeException("Failed to put working memory", e);
        }
    }

    public AckResponse deleteWorkingMemory(String sessionId) {
        try {
            return client.workingMemory().deleteWorkingMemory(sessionId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete working memory", e);
        }
    }

    // ==================== Long-Term Memory Operations ====================

    public AckResponse createLongTermMemory(List<MemoryRecord> memories) {
        try {
            return client.longTermMemory().createLongTermMemories(memories);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create long-term memory", e);
        }
    }

    public MemoryRecordResults searchLongTermMemory(String text, String userId, int limit) {
        try {
            SearchRequest request = SearchRequest.builder()
                    .text(text)
                    .userId(userId)
                    .limit(limit)
                    .build();
            return client.longTermMemory().searchLongTermMemories(request);
        } catch (Exception e) {
            throw new RuntimeException("Failed to search long-term memory", e);
        }
    }

    public MemoryRecord getMemory(String memoryId) {
        try {
            return client.longTermMemory().getLongTermMemory(memoryId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get memory", e);
        }
    }

    public AckResponse deleteMemories(List<String> memoryIds) {
        try {
            return client.longTermMemory().deleteLongTermMemories(memoryIds);
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete memories", e);
        }
    }

    // ==================== Health Check ====================

    public HealthCheckResponse healthCheck() {
        try {
            return client.health().healthCheck();
        } catch (Exception e) {
            throw new RuntimeException("Health check failed", e);
        }
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
