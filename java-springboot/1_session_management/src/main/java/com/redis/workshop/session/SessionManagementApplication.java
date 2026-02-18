package com.redis.workshop.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.redis.workshop.session",      // This module's components
    "com.redis.workshop.infrastructure" // Shared infrastructure components
})
public class SessionManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(SessionManagementApplication.class, args);
    }
}

