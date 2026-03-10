package com.redis.workshop.locks.service;

import com.redis.workshop.locks.model.Inventory;
import com.redis.workshop.locks.model.PurchaseResult;
import com.redis.workshop.locks.repository.InventoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PurchaseService {
    private static final String LOCK_KEY = "lock:inventory:1";

    private final InventoryRepository inventoryRepository;
    private final LockManager lockManager;
    private final int delayMs;

    public PurchaseService(
        InventoryRepository inventoryRepository,
        LockManager lockManager,
        @Value("${workshop.purchase.delay-ms:200}") int delayMs
    ) {
        this.inventoryRepository = inventoryRepository;
        this.lockManager = lockManager;
        this.delayMs = delayMs;
    }

    public PurchaseResult purchase() {
        boolean shouldLock = lockManager.isEnabled();
        String modeLabel = shouldLock ? "redisson" : "none";
        if (!shouldLock) {
            return purchaseWithoutLock(modeLabel);
        }
        return lockManager.withLock(
            LOCK_KEY,
            () -> purchaseWithoutLock(modeLabel),
            () -> {
                Inventory current = inventoryRepository.getDefaultInventory();
                return new PurchaseResult(false, "Lock busy, try again", current.quantity(), modeLabel);
            }
        );
    }

    private PurchaseResult purchaseWithoutLock(String modeLabel) {
        Inventory current = inventoryRepository.getDefaultInventory();
        if (current.quantity() <= 0) {
            return new PurchaseResult(false, "Sold out", current.quantity(), modeLabel);
        }

        simulateSlowWork();

        int updatedQuantity = current.quantity() - 1;
        inventoryRepository.updateQuantity(current.id(), updatedQuantity);
        Inventory updated = inventoryRepository.getDefaultInventory();
        return new PurchaseResult(true, "Purchase accepted", updated.quantity(), modeLabel);
    }

    private void simulateSlowWork() {
        if (delayMs <= 0) {
            return;
        }
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
