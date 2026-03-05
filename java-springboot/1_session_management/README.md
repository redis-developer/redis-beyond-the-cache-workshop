# Workshop 1: Distributed Session Management

Learn how to solve the problem of lost sessions using Redis and Spring Session.

## What You'll Learn

- Why in-memory sessions break in production
- How to configure Spring Session with Redis
- How sessions persist across application restarts

## Run with Docker

```bash
# From repository root
cd java-springboot/workshop-hub

# Start Redis + Workshop
docker compose -f docker-compose.local.yml --profile infrastructure up -d
docker compose -f docker-compose.local.yml --profile workshop-1_session_management up -d
```

Open **http://localhost:8080**

Login: `user` / `password`

## Workshop Flow

| Stage | What You Do |
|-------|-------------|
| 1. See the Problem | Login, restart app, observe session loss |
| 2. Fix It | Enable Redis session in 3 files |
| 3. Verify | Restart app, session persists |

## Your Tasks

### 1. `build.gradle.kts` - Uncomment dependencies
```kotlin
implementation("org.springframework.boot:spring-boot-starter-data-redis")
implementation("org.springframework.session:spring-session-data-redis")
```

### 2. `application.properties` - Enable Redis store
```properties
spring.session.store-type=redis
spring.session.redis.namespace=spring:session
spring.session.redis.flush-mode=immediate
spring.session.redis.repository-type=default
```

### 3. `RedisSessionConfig.java` - Uncomment annotations
```java
@Configuration
@EnableRedisHttpSession(maxInactiveIntervalInSeconds = 1800)
```

## View Sessions in Redis Insight

Open **http://localhost:5540** and search for:
```
spring:session:*
```

## Stopping

```bash
docker compose -f docker-compose.local.yml --profile workshop-1_session_management down
```

## Resources

- [Spring Session Docs](https://spring.io/projects/spring-session)
- [Redis Session Management](https://redis.io/docs/manual/session-management/)

