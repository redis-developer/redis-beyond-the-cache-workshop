package com.redis.workshop.locks.infrastructure;

import com.redis.workshop.infrastructure.WorkshopConfig;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Distributed Locks Workshop Configuration
 *
 * Defines which files can be edited and their original content for the workshop.
 */
@Component
public class DistributedLocksWorkshopConfig implements WorkshopConfig {

    private static final Map<String, String> EDITABLE_FILES = Map.ofEntries(
        Map.entry("build.gradle.kts", "build.gradle.kts"),
        Map.entry("application.properties", "src/main/resources/application.properties"),
        Map.entry("LockManager.java", "src/main/java/com/redis/workshop/locks/service/LockManager.java"),
        Map.entry("PurchaseService.java", "src/main/java/com/redis/workshop/locks/service/PurchaseService.java")
    );

    private static final Map<String, String> ORIGINAL_CONTENTS = Map.ofEntries(
        Map.entry("build.gradle.kts", """
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
                implementation(project(":workshop-infrastructure"))

                implementation("org.springframework.boot:spring-boot-starter-web")
                implementation("org.springframework.boot:spring-boot-starter-jdbc")

                // TODO: Uncomment the lines below to enable Redisson distributed locks
                // implementation("org.springframework.boot:spring-boot-starter-data-redis")
                // implementation("org.redisson:redisson-spring-boot-starter:3.27.2")

                runtimeOnly("org.postgresql:postgresql")

                testImplementation("org.springframework.boot:spring-boot-starter-test")
                testRuntimeOnly("org.junit.platform:junit-platform-launcher")
            }

            tasks.withType<Test> {
                useJUnitPlatform()
            }
            """),
        Map.entry("application.properties", """
            # Application name
            spring.application.name=distributed-locks

            # Server configuration
            server.port=8082

            # Postgres configuration
            spring.datasource.url=jdbc:postgresql://localhost:5432/workshop
            spring.datasource.username=workshop
            spring.datasource.password=workshop
            spring.datasource.hikari.maximum-pool-size=5

            # Redis configuration
            spring.data.redis.host=localhost
            spring.data.redis.port=6379
            spring.redis.host=localhost
            spring.redis.port=6379

            # SQL init
            spring.sql.init.mode=always
            spring.sql.init.continue-on-error=true

            # Workshop behavior
            workshop.lock.mode=none
            workshop.lock.wait=200ms
            workshop.lock.lease=5s
            workshop.purchase.delay-ms=200
            workshop.cache.ttl=5s

            # Logging
            logging.level.com.redis.workshop.locks=INFO
            """),
        Map.entry("LockManager.java", """
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
            """),
        Map.entry("PurchaseService.java", """
            package com.redis.workshop.locks.service;

            import com.redis.workshop.locks.model.Inventory;
            import com.redis.workshop.locks.model.PurchaseResult;
            import com.redis.workshop.locks.repository.InventoryRepository;
            import org.springframework.beans.factory.annotation.Value;
            import org.springframework.stereotype.Service;

            @Service
            public class PurchaseService {
                private static final String LOCK_KEY = "lock:inventory:1";

                private final InventoryRepository inventoryRepository;
                private final LockManager lockManager;
                private final int delayMs;

                public PurchaseService(
                    InventoryRepository inventoryRepository,
                    LockManager lockManager,
                    @Value("${workshop.purchase.delay-ms:200}") int delayMs
                ) {
                    this.inventoryRepository = inventoryRepository;
                    this.lockManager = lockManager;
                    this.delayMs = delayMs;
                }

                public PurchaseResult purchase() {
                    boolean shouldLock = lockManager.isEnabled();
                    String modeLabel = shouldLock ? "redisson" : "none";
                    if (!shouldLock) {
                        return purchaseWithoutLock(modeLabel);
                    }
                    return lockManager.withLock(
                        LOCK_KEY,
                        () -> purchaseWithoutLock(modeLabel),
                        () -> {
                            Inventory current = inventoryRepository.getDefaultInventory();
                            return new PurchaseResult(false, "Lock busy, try again", current.quantity(), modeLabel);
                        }
                    );
                }

                private PurchaseResult purchaseWithoutLock(String modeLabel) {
                    Inventory current = inventoryRepository.getDefaultInventory();
                    if (current.quantity() <= 0) {
                        return new PurchaseResult(false, "Sold out", current.quantity(), modeLabel);
                    }

                    simulateSlowWork();

                    int updatedQuantity = current.quantity() - 1;
                    inventoryRepository.updateQuantity(current.id(), updatedQuantity);
                    Inventory updated = inventoryRepository.getDefaultInventory();
                    return new PurchaseResult(true, "Purchase accepted", updated.quantity(), modeLabel);
                }

                private void simulateSlowWork() {
                    if (delayMs <= 0) {
                        return;
                    }
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }
            """)
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
        return "3_distributed_locks";
    }

    @Override
    public String getWorkshopTitle() {
        return "Distributed Locks";
    }

    @Override
    public String getWorkshopDescription() {
        return "Protect shared workflows with Redis-based distributed locks.";
    }
}
