package com.redis.workshop.locks.service;

import com.redis.workshop.locks.model.CacheSimulationResult;
import com.redis.workshop.locks.model.EventSimulationResult;
import com.redis.workshop.locks.model.ImportSimulationResult;
import com.redis.workshop.locks.model.JobSimulationResult;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ScenarioService {
    private static final String JOB_LOCK_KEY = "lock:job:nightly";
    private static final String CACHE_LOCK_KEY = "lock:cache:pricing";
    private static final String IMPORT_LOCK_KEY = "lock:import:customers";

    private final LockManager lockManager;
    private final Duration cacheTtl;
    private final Set<String> processedEvents = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private final AtomicReference<CacheEntry> cacheEntry = new AtomicReference<>();

    public ScenarioService(
        LockManager lockManager,
        @Value("${workshop.cache.ttl:5s}") Duration cacheTtl
    ) {
        this.lockManager = lockManager;
        this.cacheTtl = cacheTtl;
    }

    public JobSimulationResult runJobSimulation(int workers) {
        AtomicInteger ran = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();
        boolean shouldLock = lockManager.isEnabled();

        runConcurrent(workers, () -> {
            if (shouldLock) {
                boolean didRun = lockManager.runWithLock(JOB_LOCK_KEY, () -> {
                    simulateWork(250);
                    ran.incrementAndGet();
                });
                if (!didRun) {
                    skipped.incrementAndGet();
                }
            } else {
                simulateWork(250);
                ran.incrementAndGet();
            }
        });

        return new JobSimulationResult(workers, ran.get(), skipped.get(), lockModeLabel(shouldLock));
    }

    public CacheSimulationResult runCacheSimulation(int requests) {
        AtomicInteger rebuilds = new AtomicInteger();
        AtomicInteger hits = new AtomicInteger();
        boolean shouldLock = lockManager.isEnabled();

        runConcurrent(requests, () -> {
            CacheEntry entry = cacheEntry.get();
            if (entry != null && !entry.isExpired()) {
                hits.incrementAndGet();
                return;
            }

            if (shouldLock) {
                lockManager.withLock(
                    CACHE_LOCK_KEY,
                    () -> {
                        CacheEntry current = cacheEntry.get();
                        if (current != null && !current.isExpired()) {
                            hits.incrementAndGet();
                            return true;
                        }
                        simulateWork(300);
                        cacheEntry.set(new CacheEntry("value", Instant.now().plus(cacheTtl)));
                        rebuilds.incrementAndGet();
                        return true;
                    },
                    () -> {
                        simulateWork(80);
                        CacheEntry current = cacheEntry.get();
                        if (current != null && !current.isExpired()) {
                            hits.incrementAndGet();
                        }
                        return false;
                    }
                );
            } else {
                CacheEntry current = cacheEntry.get();
                if (current != null && !current.isExpired()) {
                    hits.incrementAndGet();
                    return;
                }
                simulateWork(300);
                cacheEntry.set(new CacheEntry("value", Instant.now().plus(cacheTtl)));
                rebuilds.incrementAndGet();
            }
        });

        return new CacheSimulationResult(requests, rebuilds.get(), hits.get(), lockModeLabel(shouldLock));
    }

    public void resetCache() {
        cacheEntry.set(null);
    }

    public EventSimulationResult runEventSimulation(String eventId, int attempts) {
        AtomicInteger processed = new AtomicInteger();
        boolean shouldLock = lockManager.isEnabled();

        runConcurrent(attempts, () -> {
            if (shouldLock) {
                lockManager.withLock(
                    "lock:event:" + eventId,
                    () -> {
                        if (!processedEvents.contains(eventId)) {
                            simulateWork(200);
                            processedEvents.add(eventId);
                            processed.incrementAndGet();
                        }
                        return true;
                    },
                    () -> true
                );
            } else {
                if (!processedEvents.contains(eventId)) {
                    simulateWork(200);
                    processedEvents.add(eventId);
                    processed.incrementAndGet();
                }
            }
        });

        int processedCount = processed.get();
        int duplicates = Math.max(0, processedCount - 1);
        return new EventSimulationResult(eventId, attempts, processedCount, duplicates, lockModeLabel(shouldLock));
    }

    public void resetEvents() {
        processedEvents.clear();
    }

    public ImportSimulationResult runImportSimulation(int attempts) {
        AtomicInteger started = new AtomicInteger();
        AtomicInteger skipped = new AtomicInteger();
        boolean shouldLock = lockManager.isEnabled();

        runConcurrent(attempts, () -> {
            if (shouldLock) {
                boolean didRun = lockManager.runWithLock(IMPORT_LOCK_KEY, () -> {
                    simulateWork(400);
                    started.incrementAndGet();
                });
                if (!didRun) {
                    skipped.incrementAndGet();
                }
            } else {
                simulateWork(400);
                started.incrementAndGet();
            }
        });

        return new ImportSimulationResult(attempts, started.get(), skipped.get(), lockModeLabel(shouldLock));
    }

    private String lockModeLabel(boolean shouldLock) {
        return shouldLock ? "redisson" : "none";
    }

    private void runConcurrent(int workers, Runnable task) {
        int poolSize = Math.min(Math.max(workers, 1), 8);
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(workers);
        List<Runnable> tasks = new ArrayList<>();

        for (int i = 0; i < workers; i += 1) {
            tasks.add(() -> {
                try {
                    ready.countDown();
                    start.await();
                    task.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        for (Runnable runnable : tasks) {
            executor.submit(runnable);
        }

        try {
            ready.await(5, TimeUnit.SECONDS);
            start.countDown();
            done.await(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdownNow();
        }
    }

    private void simulateWork(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private record CacheEntry(String value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
