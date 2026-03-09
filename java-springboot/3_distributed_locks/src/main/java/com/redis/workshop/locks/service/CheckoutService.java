package com.redis.workshop.locks.service;

import com.redis.workshop.locks.model.CheckoutResult;
import com.redis.workshop.locks.model.Inventory;
import com.redis.workshop.locks.repository.InventoryRepository;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Demonstrates REENTRANT lock behavior.
 * 
 * The checkout() method acquires a lock and then calls validateInventory() and reserveItem(),
 * which ALSO try to acquire the same lock. Without reentrancy, this would deadlock.
 * 
 * Why this matters in real systems:
 * - validateInventory() can be called independently (e.g., from a validation API)
 * - reserveItem() can be called independently (e.g., from a cart service)
 * - checkout() orchestrates both, and all need exclusive access to inventory
 */
@Service
public class CheckoutService {
    private static final String PRODUCT_LOCK_KEY = "lock:product:1";

    private final InventoryRepository inventoryRepository;
    private final LockManager lockManager;

    public CheckoutService(InventoryRepository inventoryRepository, LockManager lockManager) {
        this.inventoryRepository = inventoryRepository;
        this.lockManager = lockManager;
    }

    /**
     * Full checkout flow that demonstrates reentrancy.
     * Acquires lock, then calls methods that also try to acquire the same lock.
     */
    public CheckoutResult checkout() {
        String lockMode = lockManager.isEnabled() ? "redisson" : "none";
        
        if (!lockManager.isEnabled()) {
            // Without locks, just run directly (no reentrancy demonstrated)
            return checkoutWithoutLock(lockMode);
        }

        AtomicInteger validations = new AtomicInteger(0);
        AtomicInteger reservations = new AtomicInteger(0);

        return lockManager.withLock(
            PRODUCT_LOCK_KEY,
            () -> {
                // These methods ALSO try to acquire the same lock.
                // With a reentrant lock, this works. Without it, deadlock.
                boolean validated = validateInventory(validations);
                if (!validated) {
                    return new CheckoutResult(false, "Out of stock", validations.get(), 0, false, lockMode);
                }

                boolean reserved = reserveItem(reservations);
                if (!reserved) {
                    return new CheckoutResult(false, "Reservation failed", validations.get(), reservations.get(), false, lockMode);
                }

                return CheckoutResult.success(validations.get(), reservations.get(), lockMode);
            },
            () -> CheckoutResult.busy(lockMode)
        );
    }

    /**
     * Validates inventory - can be called standalone OR from within checkout().
     * Tries to acquire the lock independently.
     */
    public boolean validateInventory(AtomicInteger counter) {
        return lockManager.withLock(
            PRODUCT_LOCK_KEY,
            () -> {
                counter.incrementAndGet();
                simulateWork(50);
                Inventory inv = inventoryRepository.getDefaultInventory();
                return inv.quantity() > 0;
            },
            () -> false // Lock busy
        );
    }

    /**
     * Reserves an item - can be called standalone OR from within checkout().
     * Tries to acquire the lock independently.
     */
    public boolean reserveItem(AtomicInteger counter) {
        return lockManager.withLock(
            PRODUCT_LOCK_KEY,
            () -> {
                counter.incrementAndGet();
                simulateWork(50);
                Inventory current = inventoryRepository.getDefaultInventory();
                if (current.quantity() <= 0) {
                    return false;
                }
                inventoryRepository.updateQuantity(current.id(), current.quantity() - 1);
                return true;
            },
            () -> false
        );
    }

    private CheckoutResult checkoutWithoutLock(String lockMode) {
        Inventory inv = inventoryRepository.getDefaultInventory();
        if (inv.quantity() <= 0) {
            return new CheckoutResult(false, "Out of stock", 1, 0, false, lockMode);
        }
        simulateWork(100);
        inventoryRepository.updateQuantity(inv.id(), inv.quantity() - 1);
        return CheckoutResult.success(1, 1, lockMode);
    }

    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

