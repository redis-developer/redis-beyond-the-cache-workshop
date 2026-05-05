package com.redis.workshop.springai.patterns.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.redis.workshop.springai.patterns.frontend",
    "com.redis.workshop.infrastructure"
})
public class MultiAgentPatternsFrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiAgentPatternsFrontendApplication.class, args);
    }
}
