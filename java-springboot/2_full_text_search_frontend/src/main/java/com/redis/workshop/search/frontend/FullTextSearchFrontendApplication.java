package com.redis.workshop.search.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.redis.workshop.search.frontend",
    "com.redis.workshop.infrastructure"
})
public class FullTextSearchFrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(FullTextSearchFrontendApplication.class, args);
    }
}
