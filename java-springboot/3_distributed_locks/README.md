# Workshop 3: Distributed Locks

Learn how to implement Redis-backed distributed locks to prevent race conditions in distributed systems.

## What You'll Build

1. A Redis-backed reentrant lock for nested checkout flows
2. A Postgres-backed inventory workflow that avoids oversell
3. A session ready workshop runtime that works both locally and through the control plane

## Release Runtime

1. Frontend route: `/session/<sessionId>/workshop/3_distributed_locks/`
2. Frontend port: `8082`
3. Backend port: `18082`
4. Redis port: `6379`
5. Postgres port: `5432`

The session launcher injects the frontend backend URL, workspace path, Redis host, and Postgres URL at runtime.
The backend keeps localhost defaults so the workshop can run through the local helper.

## Local Development

These commands are for local maintainer use only.
The public platform path launches this workshop through the control plane and execution plane.

```bash
./scripts/run-workshop.sh up 3_distributed_locks
```

Open [http://localhost:8082](http://localhost:8082).

## Workshop Flow

| Stage | Route | What You Do |
|-------|-------|-------------|
| 1 | `/` | Reproduce the race condition problem from the workshop home page |
| 2 | `/reentrant` | Learn why a reentrant lock is required for nested checkout calls |
| 3 | `/reentrant/implement` | Review the implementation guide before editing code |
| 4 | `/reentrant/editor` | Update `build.gradle.kts`, `src/main/resources/application.properties`, and `src/main/java/com/redis/workshop/locks/service/LockManager.java` |
| 5 | `/reentrant/demo` | Rebuild from the workshop controls and verify the job, inventory, and checkout scenarios |

## Scenarios

1. **Single-Runner Job** - Only one worker runs a scheduled job
2. **Cache Stampede** - Only one worker rebuilds cache
3. **Event Deduplication** - Only one worker processes an event
4. **Single Import** - Only one worker runs batch import
5. **Inventory Oversell** - Prevent concurrent stock updates

## Your Tasks

The editable implementation path is:

1. `build.gradle.kts`
2. `src/main/resources/application.properties`
3. `src/main/java/com/redis/workshop/locks/service/LockManager.java`

`src/main/java/com/redis/workshop/locks/service/PurchaseService.java` is included as a read-only review step so you can see how `withLock()` protects the inventory workflow.

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

## Verify in the UI

1. Open `/reentrant/demo`
2. Refresh the lock status until it reports `redisson`
3. Run the job, inventory, and checkout scenarios

## Inspect Runtime State

When the workshop is launched through the session model, use the session scoped Redis Insight route.
For the local helper flow, Redis Insight is available at [http://localhost:5540](http://localhost:5540).

## Stopping

```bash
./scripts/run-workshop.sh down 3_distributed_locks
```

## Resources

- [Redisson Docs](https://github.com/redisson/redisson)
- [Distributed Locks Wiki](https://github.com/redisson/redisson/wiki/8.-distributed-locks-and-synchronizers)
