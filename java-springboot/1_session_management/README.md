# Workshop 1: Distributed Session Management

Learn how to solve the problem of lost sessions using Redis and Spring Session.

## What You'll Learn

- Why in-memory sessions break in production
- How to configure Spring Session with Redis
- How sessions persist across application restarts

## Run with Docker

```bash
cd java-springboot/workshop-hub

# Start the hub, Redis, and workshop 1
docker compose -f docker-compose.local.yml --profile infrastructure up -d
docker compose -f docker-compose.local.yml --profile workshop-1_session_management up -d
```

Open **http://localhost:9000** and launch the **Session Management** workshop from the Hub.

Login: `user` / `password`

## Workshop Flow

| Stage | What You Do |
|-------|-------------|
| 1. See the Problem | Login, restart the workshop backend, observe session loss |
| 2. Fix It | Update the 3 editable files listed below |
| 3. Verify | Rebuild and restart the workshop backend, then confirm the session survives |

## Editable Files And Tasks

### 1. `build.gradle.kts`

Uncomment the Redis dependencies:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-redis")
implementation("org.springframework.session:spring-session-data-redis")
```

### 2. `src/main/resources/application.properties`

Enable Redis-backed session storage:

```properties
spring.session.store-type=redis
spring.session.redis.namespace=spring:session
spring.session.redis.flush-mode=immediate
spring.session.redis.repository-type=default
```

### 3. `src/main/java/com/redis/workshop/session/config/SecurityConfig.java`

Uncomment the `HttpSessionSecurityContextRepository` import, the `SecurityContextRepository` bean, and the `securityContext(...)` filter-chain wiring.

## Verify The Fix

1. Log in and note the current session ID.
2. Restart the workshop backend from the Hub without rebuilding and confirm the session is lost.
3. Make the three code changes above.
4. Rebuild and restart the workshop backend from the Hub.
5. Refresh the workshop and confirm you stay logged in with the same session ID.

## View Sessions In Redis Insight

Use the **Open Redis Insight** action inside the workshop, or open **http://localhost:5540** directly.

Search for:

```text
spring:session:*
```

## Stopping

```bash
docker compose -f docker-compose.local.yml --profile workshop-1_session_management down
```

## Resources

- [Spring Session Docs](https://spring.io/projects/spring-session)
- [Redis Session Management](https://redis.io/docs/manual/session-management/)
