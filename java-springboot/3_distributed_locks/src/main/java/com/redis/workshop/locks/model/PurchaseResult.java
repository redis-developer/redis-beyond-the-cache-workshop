package com.redis.workshop.locks.model;

public record PurchaseResult(boolean success, String message, int remaining, String lockMode) {
}
