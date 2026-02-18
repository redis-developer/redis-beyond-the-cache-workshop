# Workshop 1: Distributed Session Management with Redis

## Learning Objectives

By the end of this workshop, you will:
- Understand the limitations of in-memory session storage
- Learn how to configure Spring Session with Redis
- Experience the benefits of distributed session management
- See how sessions persist across application restarts
- Understand how Redis enables horizontal scaling

## Prerequisites

- Java 21 or higher
- Redis running on localhost:6379
- Redis Insight (optional, for visualizing session data)

## Workshop Flow

This is a **hands-on, interactive workshop** with 3 stages:

### **STAGE 1: See the Problem**
- Run the application with default in-memory sessions
- Experience session loss on application restart
- Understand why this doesn't work in production

### **STAGE 2: Fix the Problem**
- Follow step-by-step instructions to enable Redis session management
- Uncomment configuration in code
- Learn about Spring Session configuration

### **STAGE 3: Verify the Solution**
- Test session persistence across restarts
- View session data in Redis
- Understand the benefits of distributed sessions

## Step-by-Step Instructions

### 1. Start Redis

Make sure Redis is running:
```bash
redis-cli ping
# Should return: PONG
```

### 2. Run the Application

```bash
cd java-springboot
./gradlew :1_session_management:bootRun
```

### 3. Open the Application

Navigate to: **http://localhost:8080**

Login with:
- Username: `user`
- Password: `password`

### 4. Follow the Interactive Instructions

The application will guide you through:
1. **STAGE 1**: Testing in-memory sessions (see the problem)
2. **STAGE 2**: Enabling Redis session management (your task)
3. **STAGE 3**: Verifying distributed sessions work (the solution)

## Key Configuration Files

### `build.gradle.kts`
Contains Redis dependencies (commented out by default):
```kotlin
// implementation("org.springframework.boot:spring-boot-starter-data-redis")
// implementation("org.springframework.session:spring-session-data-redis")
```

**Your Task:**
1. Uncomment both Redis dependency lines

### `application.properties`
Contains Redis session configuration (disabled by default):
```properties
# TODO: Change 'none' to 'redis' to enable Redis session storage
spring.session.store-type=none
#spring.session.redis.namespace=spring:session
#spring.session.redis.flush-mode=immediate
#spring.session.redis.repository-type=default
```

**Your Task:**
1. Change `spring.session.store-type=none` to `spring.session.store-type=redis`
2. Uncomment the 3 Redis configuration lines

### `RedisSessionConfig.java`
Contains the Redis session configuration (commented out by default):
```java
// TODO: Uncomment to enable Redis HTTP Session
//@Configuration
//@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
```

**Your Task:**
1. Uncomment the `@Configuration` annotation
2. Uncomment the `@EnableRedisHttpSession` annotation
3. Uncomment the import statement

## What You'll Learn

### Problem: In-Memory Sessions
- Sessions lost on application restart
- Cannot scale horizontally (load balancing issues)
- No session sharing between instances
- Downtime during deployments

### Solution: Redis Session Management
- Sessions persist across restarts
- Horizontal scaling with load balancers
- Session sharing between multiple instances
- Zero-downtime deployments
- Centralized session storage

## Viewing Sessions in Redis

### Using Redis CLI
```bash
redis-cli
KEYS spring:session:*
```

### Using Redis Insight
1. Open http://localhost:5540
2. Connect to your Redis instance
3. Search for keys: `spring:session:*`
4. Explore session data structure

## Testing Scenarios

### Test 1: Session Persistence
1. Login and note your Session ID
2. Stop the application (Ctrl+C)
3. Restart: `./gradlew :1_session_management:bootRun`
4. Refresh the page
   - **Without Redis**: Logged out
   - **With Redis**: Still logged in

### Test 2: Multiple Sessions
1. Login in Chrome
2. Login in Firefox (or incognito)
3. Each browser gets a different Session ID
4. Both sessions are stored in Redis

### Test 3: Max Sessions Limit
1. Login from 2 different browsers (works)
2. Try logging in from a 3rd browser
3. Login is blocked (max 2 sessions configured)

## Additional Resources

- [Spring Session Documentation](https://spring.io/projects/spring-session)
- [Redis Session Management Guide](https://redis.io/docs/manual/session-management/)
- [Spring Security Session Management](https://docs.spring.io/spring-security/reference/servlet/authentication/session-management.html)

## Congratulations!

You've successfully implemented distributed session management with Redis!

