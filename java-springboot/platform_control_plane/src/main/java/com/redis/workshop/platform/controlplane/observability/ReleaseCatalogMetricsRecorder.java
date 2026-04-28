package com.redis.workshop.platform.controlplane.observability;

import com.redis.workshop.platform.controlplane.release.ReleaseCatalogEntry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class ReleaseCatalogMetricsRecorder {

    private final MeterRegistry meterRegistry;
    private final Counter catalogLoadSuccess;
    private final Counter catalogLoadFailure;
    private final Counter releaseLookupMiss;
    private final Counter releaseLaunchAttempt;
    private final AtomicLong catalogEntryCount = new AtomicLong(0);
    private final AtomicLong catalogStalenessSeconds = new AtomicLong(-1);
    private final AtomicReference<Instant> lastCatalogLoadAttemptAt = new AtomicReference<>();
    private final AtomicReference<Instant> lastSuccessfulCatalogLoadAt = new AtomicReference<>();
    private final AtomicReference<Instant> lastCatalogLoadFailureAt = new AtomicReference<>();
    private final AtomicReference<String> lastCatalogLoadFailureReason = new AtomicReference<>();

    public ReleaseCatalogMetricsRecorder(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.catalogLoadSuccess = Counter.builder("platform.controlplane.release.catalog.load.success")
            .description("Successful release catalog loads")
            .register(meterRegistry);
        this.catalogLoadFailure = Counter.builder("platform.controlplane.release.catalog.load.failure")
            .description("Failed release catalog loads")
            .register(meterRegistry);
        this.releaseLookupMiss = Counter.builder("platform.controlplane.release.lookup.miss")
            .description("Release catalog lookup misses")
            .register(meterRegistry);
        this.releaseLaunchAttempt = Counter.builder("platform.controlplane.release.launch.attempt")
            .description("Launch or restart attempts correlated to a release identifier")
            .register(meterRegistry);

        Gauge.builder("platform.controlplane.release.catalog.entry_count", catalogEntryCount, AtomicLong::get)
            .description("Release catalog entry count from the last successful load")
            .register(meterRegistry);
        Gauge.builder("platform.controlplane.release.catalog.staleness_seconds", catalogStalenessSeconds, AtomicLong::get)
            .description("Seconds since the last successful release catalog load")
            .register(meterRegistry);
    }

    public void recordCatalogLoadSuccess(int entryCount) {
        Instant now = Instant.now();
        lastCatalogLoadAttemptAt.set(now);
        lastSuccessfulCatalogLoadAt.set(now);
        lastCatalogLoadFailureReason.set(null);
        catalogEntryCount.set(entryCount);
        catalogStalenessSeconds.set(0);
        catalogLoadSuccess.increment();
    }

    public void recordCatalogLoadFailure(String reason) {
        Instant now = Instant.now();
        lastCatalogLoadAttemptAt.set(now);
        lastCatalogLoadFailureAt.set(now);
        lastCatalogLoadFailureReason.set(reason);
        refreshCatalogStaleness();
        catalogLoadFailure.increment();
    }

    public void recordLookupMiss(String workshopId, String releaseVersion) {
        releaseLookupMiss.increment();
        Counter.builder("platform.controlplane.release.lookup.miss.by_release")
            .tag("workshop_id", normalizeTagValue(workshopId))
            .tag("release_version", normalizeTagValue(releaseVersion))
            .register(meterRegistry)
            .increment();
    }

    public void recordReleaseLaunchAttempt(ReleaseCatalogEntry release, String action) {
        releaseLaunchAttempt.increment();
        Counter.builder("platform.controlplane.release.launch.attempt.by_release")
            .tag("workshop_id", normalizeTagValue(release.workshopId()))
            .tag("release_id", normalizeTagValue(release.releaseId()))
            .tag("mode", release.mode().name())
            .tag("action", normalizeTagValue(action))
            .register(meterRegistry)
            .increment();
    }

    @Scheduled(fixedDelayString = "${platform.controlplane.release.metrics.refresh-ms:30000}")
    public void refreshCatalogStaleness() {
        Instant lastSuccess = lastSuccessfulCatalogLoadAt.get();
        if (lastSuccess == null) {
            catalogStalenessSeconds.set(-1);
            return;
        }

        catalogStalenessSeconds.set(Math.max(0, Duration.between(lastSuccess, Instant.now()).getSeconds()));
    }

    public ReleaseCatalogMetricsSnapshot snapshot() {
        return new ReleaseCatalogMetricsSnapshot(
            catalogLoadSuccess.count(),
            catalogLoadFailure.count(),
            releaseLookupMiss.count(),
            releaseLaunchAttempt.count(),
            catalogEntryCount.get(),
            catalogStalenessSeconds.get(),
            lastCatalogLoadAttemptAt.get(),
            lastSuccessfulCatalogLoadAt.get(),
            lastCatalogLoadFailureAt.get(),
            lastCatalogLoadFailureReason.get()
        );
    }

    private String normalizeTagValue(String value) {
        return value == null || value.isBlank() ? "unknown" : value;
    }
}
