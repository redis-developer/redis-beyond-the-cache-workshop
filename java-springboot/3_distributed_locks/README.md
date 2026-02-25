# Workshop 3: Distributed Locks with Redisson + Redis

## Learning Objectives

By the end of this workshop, you will:
- Understand common distributed lock scenarios (jobs, cache stampede, event dedupe, imports, inventory)
- Implement distributed locking with Redisson in Spring Boot
- Coordinate lock mode and lease timings via configuration
- Verify lock behavior using a live demo panel

## Prerequisites

- Java 21 or higher
- Redis running on localhost:6379
- Postgres running on localhost:5432 (database: `workshop`, user: `workshop`)
- Redis Insight (optional)

## Workshop Flow

This is a **hands-on, interactive workshop** with 3 stages:

### **STAGE 1: See the Problems**
- Run the scenarios without locks
- Observe duplicated jobs, cache rebuilds, event processing, imports, and inventory oversell

### **STAGE 2: Implement Redisson Locks**
- Implement `tryLock` + `unlock` in a shared LockManager
- Enable lock mode in configuration

### **STAGE 3: Verify the Fix**
- Re-run each scenario
- Confirm only one worker does the critical work

## Step-by-Step Instructions

### 1. Start Redis + Postgres

```bash
docker-compose -f java-springboot/3_distributed_locks/docker-compose.yml up -d redis postgres
```

### 2. Run the Application

```bash
cd java-springboot
./gradlew :3_distributed_locks:bootRun
```

### 3. Open the Application

Navigate to: **http://localhost:8082**

## Key Configuration Files

### `application.properties`
Controls lock mode and timings:
```properties
workshop.lock.mode=none
workshop.lock.wait=200ms
workshop.lock.lease=5s
workshop.cache.ttl=5s
```

**Your Task (Stage 2):**
1. Change `workshop.lock.mode=none` to `workshop.lock.mode=redisson`

### `LockManager.java`
Contains TODOs for the Redisson lock:
```java
public <T> T withLock(String lockKey, Supplier<T> onAcquired, Supplier<T> onBusy) {
    if (!isEnabled()) {
        return onAcquired.get();
    }
    // TODO: Replace with Redisson lock implementation.
    return onAcquired.get();
}
```

**Your Task (Stage 2):**
1. Use `redissonClient.getLock(lockKey)`
2. Call `tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS)`
3. Run the work on success, otherwise return `onBusy`
4. Always `unlock()` in a `finally` block

## Scenarios You Will Test

1. **Single-Runner Job** — only one worker should run a scheduled job
2. **Cache Stampede** — only one worker should rebuild a hot cache
3. **Event Deduplication** — only one worker should process a webhook/event
4. **Single Import** — only one worker should run a batch import
5. **Inventory Oversell (Postgres)** — only one purchase should update stock at a time

## Additional Resources

- [Redisson Documentation](https://github.com/redisson/redisson)
- [Redisson Distributed Locks](https://github.com/redisson/redisson/wiki/8.-distributed-locks-and-synchronizers)
- [Spring Boot + Redisson Starter](https://github.com/redisson/redisson/tree/master/redisson-spring-boot-starter)

## Congratulations!

You've implemented Redisson-based distributed locks and validated them across real-world scenarios.
