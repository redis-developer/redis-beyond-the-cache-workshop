package com.redis.workshop.locks.model;

public record EventSimulationResult(String eventId, int attempts, int processed, int duplicates, String lockMode) {
}
