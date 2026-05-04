package com.redis.workshop.springai.fundamentals.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.redis.workshop.springai.fundamentals.frontend",
    "com.redis.workshop.infrastructure"
})
public class SpringAiFundamentalsFrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringAiFundamentalsFrontendApplication.class, args);
    }
}
