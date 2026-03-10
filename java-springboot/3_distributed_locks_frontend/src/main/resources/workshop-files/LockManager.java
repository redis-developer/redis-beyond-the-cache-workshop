package com.redis.workshop.locks.service;

import java.time.Duration;
import java.util.function.Supplier;

// TODO: Uncomment the import below after adding Redisson dependencies
// import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class LockManager {
    // TODO: Uncomment the field below after adding Redisson dependencies
    // private final RedissonClient redissonClient;
    private final String lockMode;
    private final Duration waitTime;
    private final Duration leaseTime;

    public LockManager(
        // TODO: Uncomment the parameter below after adding Redisson dependencies
        // @Autowired(required = false) RedissonClient redissonClient,
        @Value("${workshop.lock.mode:none}") String lockMode,
        @Value("${workshop.lock.wait:200ms}") Duration waitTime,
        @Value("${workshop.lock.lease:5s}") Duration leaseTime
    ) {
        // TODO: Uncomment the line below after adding Redisson dependencies
        // this.redissonClient = redissonClient;
        this.lockMode = lockMode;
        this.waitTime = waitTime;
        this.leaseTime = leaseTime;
    }

    public boolean isEnabled() {
        // TODO: Update this check after implementing the lock
        // return "redisson".equalsIgnoreCase(lockMode) && redissonClient != null;
        return false;
    }

    public <T> T withLock(String lockKey, Supplier<T> onAcquired, Supplier<T> onBusy) {
        if (!isEnabled()) {
            return onAcquired.get();
        }
        // TODO: Implement Redisson lock here
        return onAcquired.get();
    }

    public boolean runWithLock(String lockKey, Runnable onAcquired) {
        return withLock(lockKey, () -> {
            onAcquired.run();
            return true;
        }, () -> false);
    }

    public Duration getWaitTime() {
        return waitTime;
    }

    public Duration getLeaseTime() {
        return leaseTime;
    }

    public String getLockMode() {
        return lockMode;
    }
}
