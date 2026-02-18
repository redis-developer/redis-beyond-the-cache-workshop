package com.redis.workshop.session.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

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
@Configuration
// TODO: Uncomment the line below to enable Redis HTTP Session (30 minutes = 1800 seconds)
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
public class RedisSessionConfig {
    // This configuration class will be activated when you uncomment the annotations above
}
