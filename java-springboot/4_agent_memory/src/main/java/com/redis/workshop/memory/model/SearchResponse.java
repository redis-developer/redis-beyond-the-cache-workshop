package com.redis.workshop.memory.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Response from long-term memory search.
 */
public record SearchResponse(
    List<MemoryRecord> memories,
    Integer total,
    @JsonProperty("next_offset") Integer nextOffset
) {}

