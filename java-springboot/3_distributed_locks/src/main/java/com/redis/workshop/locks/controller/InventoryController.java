package com.redis.workshop.locks.controller;

import com.redis.workshop.locks.model.Inventory;
import com.redis.workshop.locks.model.PurchaseResult;
import com.redis.workshop.locks.repository.InventoryRepository;
import com.redis.workshop.locks.service.PurchaseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class InventoryController {
    private final InventoryRepository inventoryRepository;
    private final PurchaseService purchaseService;

    public InventoryController(InventoryRepository inventoryRepository, PurchaseService purchaseService) {
        this.inventoryRepository = inventoryRepository;
        this.purchaseService = purchaseService;
    }

    @GetMapping("/inventory")
    public Inventory inventory() {
        return inventoryRepository.getDefaultInventory();
    }

    @PostMapping("/purchase")
    public PurchaseResult purchase() {
        return purchaseService.purchase();
    }

    @PostMapping("/reset")
    public Inventory reset(@RequestParam(name = "quantity", defaultValue = "5") int quantity) {
        inventoryRepository.resetInventory(quantity);
        return inventoryRepository.getDefaultInventory();
    }
}
