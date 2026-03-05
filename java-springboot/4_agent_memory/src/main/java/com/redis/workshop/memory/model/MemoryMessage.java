package com.redis.workshop.memory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

/**
 * Represents a message in the memory system (part of working memory).
 */
public record MemoryMessage(
    String id,
    String role,
    String content,
    @JsonProperty("created_at") Instant createdAt
) {
    public MemoryMessage(String role, String content) {
        this(null, role, content, Instant.now());
    }
}

