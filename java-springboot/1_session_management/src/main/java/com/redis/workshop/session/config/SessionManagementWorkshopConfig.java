package com.redis.workshop.session.config;

import com.redis.workshop.infrastructure.WorkshopConfig;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Session Management Workshop Configuration
 * 
 * This is the specific configuration for the "Session Management with Redis" workshop.
 * It defines which files can be edited and their original content.
 * 
 * To create a new workshop, simply create a new class implementing WorkshopConfig
 * with different files and content.
 */
@Component
public class SessionManagementWorkshopConfig implements WorkshopConfig {

    private static final Map<String, String> EDITABLE_FILES = Map.of(
        "build.gradle.kts", "build.gradle.kts",
        "application.properties", "src/main/resources/application.properties",
        "RedisSessionConfig.java", "src/main/java/com/redis/workshop/session/config/RedisSessionConfig.java"
    );

    private static final Map<String, String> ORIGINAL_CONTENTS = Map.of(
        "build.gradle.kts", """
            plugins {
                java
                id("org.springframework.boot")
                id("io.spring.dependency-management")
            }
            
            group = "com.redis.workshop"
            version = "0.0.1-SNAPSHOT"
            
            java {
                toolchain {
                    languageVersion = JavaLanguageVersion.of(21)
                }
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
                // Workshop infrastructure - DO NOT REMOVE
                implementation(project(":workshop-infrastructure"))

                implementation("org.springframework.boot:spring-boot-starter-web")
                implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
                implementation("org.springframework.boot:spring-boot-starter-security")

                // TODO: Uncomment the lines below to enable Redis session management
                // implementation("org.springframework.boot:spring-boot-starter-data-redis")
                // implementation("org.springframework.session:spring-session-data-redis")

                testImplementation("org.springframework.boot:spring-boot-starter-test")
                testImplementation("org.springframework.security:spring-security-test")
                testRuntimeOnly("org.junit.platform:junit-platform-launcher")
            }
            
            tasks.withType<Test> {
                useJUnitPlatform()
            }
            """,
        
        "application.properties", """
            # Application name
            spring.application.name=session-management
            
            # Server configuration
            server.port=8080
            
            # Redis configuration
            spring.data.redis.host=localhost
            spring.data.redis.port=6379
            
            # Session configuration
            # TODO: Change 'none' to 'redis' to enable Redis session storage
            spring.session.store-type=none
            #spring.session.redis.namespace=spring:session
            #spring.session.redis.flush-mode=immediate
            #spring.session.redis.repository-type=default
            
            # Session timeout (30 minutes)
            server.servlet.session.timeout=30m
            
            # Logging
            logging.level.org.springframework.session=DEBUG
            """,
        
        "RedisSessionConfig.java", """
            package com.redis.workshop.session.config;
            
            import org.springframework.context.annotation.Configuration;
            //import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
            
            /**
             * ⭐ WORKSHOP TEACHING CONTENT - Redis Session Configuration ⭐
             * 
             * THIS IS THE KEY TEACHING POINT OF THE WORKSHOP!
             * 
             * This class demonstrates how to enable Redis-backed HTTP sessions in Spring Boot.
             * 
             * STAGE 1 (Current State):
             * - Annotations are commented out
             * - Sessions are stored in-memory (default behavior)
             * - Sessions are lost when the application restarts
             * 
             * STAGE 2 (What You'll Do):
             * - Uncomment @Configuration to activate this class
             * - Uncomment @EnableRedisHttpSession to enable Redis session storage
             * - Uncomment the import statement
             * - Update application.properties to set spring.session.store-type=redis
             * 
             * STAGE 3 (Result):
             * - Sessions are stored in Redis (distributed storage)
             * - Sessions persist across application restarts
             * - Multiple application instances can share sessions
             * - Enables horizontal scaling and zero-downtime deployments
             * 
             * Key Learning Points:
             * - How Spring Session replaces default session management
             * - How @EnableRedisHttpSession configures Redis as session store
             * - How maxInactiveIntervalInSeconds sets session timeout (30 minutes = 1800 seconds)
             * - How this enables distributed session management for microservices
             */
            
            // TODO: Uncomment @Configuration when enabling Redis session management
            //@Configuration
            // TODO: Uncomment the line below to enable Redis HTTP Session (30 minutes = 1800 seconds)
            //@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
            public class RedisSessionConfig {
                // This configuration class will be activated when you uncomment the annotations above
            }
            """
    );

    @Override
    public Map<String, String> getEditableFiles() {
        return EDITABLE_FILES;
    }

    @Override
    public String getOriginalContent(String fileName) {
        return ORIGINAL_CONTENTS.get(fileName);
    }

    @Override
    public String getModuleName() {
        return "1_session_management";
    }
}

