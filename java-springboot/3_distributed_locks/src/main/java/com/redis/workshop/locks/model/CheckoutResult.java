package com.redis.workshop.locks.model;

public record CheckoutResult(
    boolean success,
    String message,
    int validations,
    int reservations,
    boolean wouldDeadlock,
    String lockMode
) {
    public static CheckoutResult success(int validations, int reservations, String lockMode) {
        return new CheckoutResult(true, "Checkout completed", validations, reservations, false, lockMode);
    }

    public static CheckoutResult busy(String lockMode) {
        return new CheckoutResult(false, "Lock busy, try again", 0, 0, false, lockMode);
    }

    public static CheckoutResult deadlocked(String lockMode) {
        return new CheckoutResult(false, "Deadlock detected - non-reentrant lock", 0, 0, true, lockMode);
    }
}

