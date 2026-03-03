package com.redis.workshop.memory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.redis.workshop.memory",
    "com.redis.workshop.infrastructure"
})
public class AgentMemoryApplication {
    public static void main(String[] args) {
        SpringApplication.run(AgentMemoryApplication.class, args);
    }
}

