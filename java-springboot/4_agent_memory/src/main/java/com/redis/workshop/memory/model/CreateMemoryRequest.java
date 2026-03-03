package com.redis.workshop.memory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Request payload for creating long-term memories.
 */
public record CreateMemoryRequest(
    List<ExtractedMemoryRecord> memories,
    boolean deduplicate
) {
    public CreateMemoryRequest(List<ExtractedMemoryRecord> memories) {
        this(memories, true);
    }

    /**
     * Memory record structure for creation requests.
     */
    public record ExtractedMemoryRecord(
        String id,
        String text,
        @JsonProperty("memory_type") String memoryType,
        List<String> topics,
        List<String> entities,
        @JsonProperty("user_id") String userId,
        @JsonProperty("session_id") String sessionId,
        String namespace
    ) {
        public ExtractedMemoryRecord(String text, String memoryType, List<String> topics, String userId) {
            this(null, text, memoryType, topics, null, userId, null, null);
        }
    }
}

