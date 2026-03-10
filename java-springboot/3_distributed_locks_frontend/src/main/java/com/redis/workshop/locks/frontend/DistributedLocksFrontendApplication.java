package com.redis.workshop.locks.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.redis.workshop.locks.frontend",
    "com.redis.workshop.infrastructure"
})
public class DistributedLocksFrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistributedLocksFrontendApplication.class, args);
    }
}
