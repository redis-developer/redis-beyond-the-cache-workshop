package com.redis.workshop.locks.model;

public record ImportSimulationResult(int attempts, int started, int skipped, String lockMode) {
}
