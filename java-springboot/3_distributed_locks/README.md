# Workshop 3: Distributed Locks

Learn how to implement Redis-backed distributed locks to prevent race conditions in distributed systems.

## What You'll Learn

- Common distributed lock scenarios
- Implementing locks with Redisson
- Preventing inventory oversell and duplicate processing

## Run with Docker

```bash
# From repository root
cd java-springboot/workshop-hub

# Start Redis + Postgres + Workshop
docker compose -f docker-compose.local.yml --profile infrastructure up -d
docker compose -f docker-compose.local.yml --profile workshop-3_distributed_locks up -d
```

Open **http://localhost:8082**

## Workshop Flow

| Stage | What You Do |
|-------|-------------|
| 1. See the Problem | Run scenarios, observe duplicates and overselling |
| 2. Fix It | Implement Redisson lock in LockManager |
| 3. Verify | Re-run scenarios, only one worker succeeds |

## Scenarios

1. **Single-Runner Job** - Only one worker runs a scheduled job
2. **Cache Stampede** - Only one worker rebuilds cache
3. **Event Deduplication** - Only one worker processes an event
4. **Single Import** - Only one worker runs batch import
5. **Inventory Oversell** - Prevent concurrent stock updates

## Your Tasks

### 1. `application.properties` - Enable lock mode
```properties
workshop.lock.mode=redisson
```

### 2. `LockManager.java` - Implement Redisson lock
```java
public <T> T withLock(String lockKey, Supplier<T> onAcquired, Supplier<T> onBusy) {
    if (!isEnabled()) {
        return onAcquired.get();
    }

    RLock lock = redissonClient.getLock(lockKey);
    try {
        boolean acquired = lock.tryLock(waitTime, leaseTime, TimeUnit.MILLISECONDS);
        if (acquired) {
            return onAcquired.get();
        } else {
            return onBusy.get();
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        return onBusy.get();
    } finally {
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

## View Locks in Redis Insight

Open **http://localhost:5540** and search for lock keys.

## Stopping

```bash
docker compose -f docker-compose.local.yml --profile workshop-3_distributed_locks down
```

## Resources

- [Redisson Docs](https://github.com/redisson/redisson)
- [Distributed Locks Wiki](https://github.com/redisson/redisson/wiki/8.-distributed-locks-and-synchronizers)
