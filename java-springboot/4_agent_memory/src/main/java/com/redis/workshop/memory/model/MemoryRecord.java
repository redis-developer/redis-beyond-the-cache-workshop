package com.redis.workshop.memory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.List;

/**
 * Represents a long-term memory record.
 */
public record MemoryRecord(
    String id,
    String text,
    @JsonProperty("session_id") String sessionId,
    @JsonProperty("user_id") String userId,
    String namespace,
    @JsonProperty("memory_type") String memoryType,
    List<String> topics,
    List<String> entities,
    @JsonProperty("created_at") Instant createdAt,
    @JsonProperty("last_accessed") Instant lastAccessed,
    Double dist
) {
    /**
     * Constructor for creating new memory records.
     */
    public MemoryRecord(String text, String memoryType, List<String> topics, String userId) {
        this(null, text, null, userId, null, memoryType, topics, null, null, null, null);
    }
}

