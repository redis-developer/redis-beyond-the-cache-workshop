package com.redis.workshop.session;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "com.redis.workshop.session",      // This workshop's package
    "com.redis.workshop.infrastructure" // Shared infrastructure (EditorController, SpaController)
})
public class SessionManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(SessionManagementApplication.class, args);
    }
}

