package com.redis.workshop.locks.model;

public record CacheSimulationResult(int requests, int rebuilds, int cacheHits, String lockMode) {
}
