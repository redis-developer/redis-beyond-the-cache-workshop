package com.redis.workshop.memory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Represents working memory for a session.
 * Working memory contains conversation messages and context for the current session.
 */
public record WorkingMemory(
    @JsonProperty("session_id") String sessionId,
    List<MemoryMessage> messages,
    List<MemoryRecord> memories,
    Map<String, Object> data,
    String context,
    @JsonProperty("user_id") String userId,
    String namespace,
    Integer tokens,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("updated_at") Instant updatedAt
) {
    /**
     * Constructor for creating new working memory.
     */
    public WorkingMemory(String sessionId, List<MemoryMessage> messages, String userId) {
        this(sessionId, messages, null, null, null, userId, null, null, null, null);
    }
}

