package com.redis.workshop.locks.controller;

import com.redis.workshop.locks.model.CheckoutResult;
import com.redis.workshop.locks.service.CheckoutService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/checkout")
public class CheckoutController {
    private final CheckoutService checkoutService;

    public CheckoutController(CheckoutService checkoutService) {
        this.checkoutService = checkoutService;
    }

    /**
     * Demonstrates reentrant lock behavior.
     * The checkout() method acquires a lock, then calls validateInventory() and reserveItem()
     * which also try to acquire the same lock.
     */
    @PostMapping
    public CheckoutResult checkout() {
        return checkoutService.checkout();
    }
}

