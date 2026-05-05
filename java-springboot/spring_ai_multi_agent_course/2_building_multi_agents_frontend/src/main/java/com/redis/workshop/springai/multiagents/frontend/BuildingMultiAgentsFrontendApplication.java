package com.redis.workshop.springai.multiagents.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.redis.workshop.springai.multiagents.frontend",
    "com.redis.workshop.infrastructure"
})
public class BuildingMultiAgentsFrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BuildingMultiAgentsFrontendApplication.class, args);
    }
}
