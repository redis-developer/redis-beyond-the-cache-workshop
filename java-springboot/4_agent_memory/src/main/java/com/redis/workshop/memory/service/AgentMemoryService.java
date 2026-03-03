package com.redis.workshop.memory.service;

import com.redis.workshop.memory.model.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * Service for interacting with the Agent Memory Server.
 * 
 * This service demonstrates how to use the REST API to:
 * - Store and retrieve working memory (conversation context)
 * - Create and search long-term memories (persistent facts)
 * 
 * TODO: Replace this with the official Java client when fully implemented:
 * implementation("com.github.redis.agent-memory-server:agent-memory-client:0.1.0")
 */
@Service
public class AgentMemoryService {

    private final WebClient webClient;
    private final String namespace;

    public AgentMemoryService(
            @Value("${agent-memory.server.url}") String serverUrl,
            @Value("${agent-memory.server.namespace:workshop}") String namespace) {
        this.webClient = WebClient.builder()
                .baseUrl(serverUrl)
                .build();
        this.namespace = namespace;
    }

    // ==================== Working Memory Operations ====================

    /**
     * Get working memory for a session.
     */
    public Mono<WorkingMemory> getWorkingMemory(String sessionId) {
        return webClient.get()
                .uri("/v1/working-memory/{sessionId}", sessionId)
                .retrieve()
                .bodyToMono(WorkingMemory.class);
    }

    /**
     * Save working memory for a session.
     */
    public Mono<WorkingMemory> putWorkingMemory(String sessionId, WorkingMemory memory) {
        return webClient.put()
                .uri("/v1/working-memory/{sessionId}", sessionId)
                .bodyValue(memory)
                .retrieve()
                .bodyToMono(WorkingMemory.class);
    }

    /**
     * Delete working memory for a session.
     */
    public Mono<Map> deleteWorkingMemory(String sessionId) {
        return webClient.delete()
                .uri("/v1/working-memory/{sessionId}", sessionId)
                .retrieve()
                .bodyToMono(Map.class);
    }

    // ==================== Long-Term Memory Operations ====================

    /**
     * Create long-term memories.
     */
    public Mono<Map> createLongTermMemory(List<CreateMemoryRequest.ExtractedMemoryRecord> memories) {
        return webClient.post()
                .uri("/v1/long-term-memory/")
                .bodyValue(new CreateMemoryRequest(memories))
                .retrieve()
                .bodyToMono(Map.class);
    }

    /**
     * Search long-term memory using semantic search.
     */
    public Mono<SearchResponse> searchLongTermMemory(String text, String userId, int limit) {
        return webClient.post()
                .uri("/v1/long-term-memory/search")
                .bodyValue(new SearchRequest(text, userId, limit))
                .retrieve()
                .bodyToMono(SearchResponse.class);
    }

    /**
     * Get a specific memory by ID.
     */
    public Mono<MemoryRecord> getMemory(String memoryId) {
        return webClient.get()
                .uri("/v1/long-term-memory/{memoryId}", memoryId)
                .retrieve()
                .bodyToMono(MemoryRecord.class);
    }

    /**
     * Delete long-term memories by IDs.
     */
    public Mono<Map> deleteMemories(List<String> memoryIds) {
        return webClient.delete()
                .uri(uriBuilder -> uriBuilder
                        .path("/v1/long-term-memory")
                        .queryParam("memory_ids", memoryIds.toArray())
                        .build())
                .retrieve()
                .bodyToMono(Map.class);
    }

    // ==================== Health Check ====================

    /**
     * Check if the Agent Memory Server is healthy.
     */
    public Mono<Map> healthCheck() {
        return webClient.get()
                .uri("/v1/health")
                .retrieve()
                .bodyToMono(Map.class);
    }
}

