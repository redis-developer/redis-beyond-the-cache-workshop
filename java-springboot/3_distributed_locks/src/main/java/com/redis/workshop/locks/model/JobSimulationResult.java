package com.redis.workshop.locks.model;

public record JobSimulationResult(int workers, int ran, int skipped, String lockMode) {
}
