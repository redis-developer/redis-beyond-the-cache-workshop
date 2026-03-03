package com.redis.workshop.memory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.Map;

/**
 * Request payload for searching long-term memory.
 */
public record SearchRequest(
    String text,
    @JsonProperty("user_id") Map<String, Object> userId,
    String namespace,
    List<String> topics,
    Integer limit,
    Integer offset,
    @JsonProperty("distance_threshold") Double distanceThreshold
) {
    /**
     * Simple constructor for basic search.
     */
    public SearchRequest(String text, String userId, int limit) {
        this(text, userId != null ? Map.of("eq", userId) : null, null, null, limit, null, null);
    }
}

