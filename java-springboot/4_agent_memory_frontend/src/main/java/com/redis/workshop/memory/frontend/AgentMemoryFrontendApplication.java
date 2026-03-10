package com.redis.workshop.memory.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.redis.workshop.memory.frontend",
    "com.redis.workshop.infrastructure"
})
public class AgentMemoryFrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentMemoryFrontendApplication.class, args);
    }
}
