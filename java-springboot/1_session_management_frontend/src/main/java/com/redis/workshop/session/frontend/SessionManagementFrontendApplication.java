package com.redis.workshop.session.frontend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
    "com.redis.workshop.session.frontend",
    "com.redis.workshop.infrastructure"
})
public class SessionManagementFrontendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SessionManagementFrontendApplication.class, args);
    }
}
