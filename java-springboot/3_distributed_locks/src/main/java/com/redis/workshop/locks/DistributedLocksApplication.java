package com.redis.workshop.locks;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.redis.workshop.locks",
    "com.redis.workshop.infrastructure"
})
public class DistributedLocksApplication {
    public static void main(String[] args) {
        SpringApplication.run(DistributedLocksApplication.class, args);
    }
}
