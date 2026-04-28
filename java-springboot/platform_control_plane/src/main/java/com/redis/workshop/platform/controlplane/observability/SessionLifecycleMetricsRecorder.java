package com.redis.workshop.platform.controlplane.observability;

import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import com.redis.workshop.platform.controlplane.persistence.repository.PlatformSessionRecordRepository;
import com.redis.workshop.platform.controlplane.security.CurrentActor;
import com.redis.workshop.platform.controlplane.session.SessionResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.MultiGauge;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class SessionLifecycleMetricsRecorder {

    private static final EnumSet<PlatformSessionState> ACTIVE_STATES = EnumSet.of(
        PlatformSessionState.REQUESTED,
        PlatformSessionState.ADMITTED,
        PlatformSessionState.PROVISIONING,
        PlatformSessionState.INITIALIZING,
        PlatformSessionState.READY,
        PlatformSessionState.DEGRADED,
        PlatformSessionState.TERMINATING
    );

    private static final EnumSet<PlatformSessionState> PENDING_STATES = EnumSet.of(
        PlatformSessionState.REQUESTED,
        PlatformSessionState.ADMITTED,
        PlatformSessionState.PROVISIONING,
        PlatformSessionState.INITIALIZING,
        PlatformSessionState.TERMINATING
    );

    private final PlatformSessionRecordRepository platformSessionRecordRepository;
    private final MeterRegistry meterRegistry;
    private final Counter sessionCreateRequests;
    private final Counter sessionCreateSuccess;
    private final AtomicLong pendingSessionMaxAgeSeconds = new AtomicLong(0);
    private final AtomicLong sessionTerminationCount = new AtomicLong(0);
    private final MultiGauge activeSessionsGauge;
    private final MultiGauge sessionStateGauge;

    public SessionLifecycleMetricsRecorder(
        PlatformSessionRecordRepository platformSessionRecordRepository,
        MeterRegistry meterRegistry
    ) {
        this.platformSessionRecordRepository = platformSessionRecordRepository;
        this.meterRegistry = meterRegistry;
        this.sessionCreateRequests = Counter.builder("platform.controlplane.session.create.requests")
            .description("Total control plane session create requests")
            .register(meterRegistry);
        this.sessionCreateSuccess = Counter.builder("platform.controlplane.session.create.success")
            .description("Successful control plane session create responses")
            .register(meterRegistry);
        this.activeSessionsGauge = MultiGauge.builder("platform.controlplane.active_sessions")
            .description("Active sessions by workshop and mode")
            .register(meterRegistry);
        this.sessionStateGauge = MultiGauge.builder("platform.controlplane.session_state_count")
            .description("Session counts by state")
            .register(meterRegistry);

        Gauge.builder("platform.controlplane.session.termination.count", sessionTerminationCount, AtomicLong::get)
            .description("Successful control plane session termination count")
            .register(meterRegistry);
        Gauge.builder("platform.controlplane.pending_session_max_age_seconds", pendingSessionMaxAgeSeconds, AtomicLong::get)
            .description("Maximum age in seconds for pending sessions")
            .register(meterRegistry);

        refreshSessionGauges();
    }

    public void recordSessionCreateRequest() {
        sessionCreateRequests.increment();
    }

    public void recordSessionCreateSuccess(SessionResponse sessionResponse) {
        sessionCreateSuccess.increment();
        refreshSessionGauges();
    }

    public void recordSessionTermination(CurrentActor actor, SessionResponse sessionResponse) {
        sessionTerminationCount.incrementAndGet();
        Counter.builder("platform.controlplane.session.termination.by_actor")
            .tag("actor_type", actor.actorType().name())
            .register(meterRegistry)
            .increment();
        refreshSessionGauges();
    }

    @Scheduled(fixedDelayString = "${platform.controlplane.metrics.refresh-ms:30000}")
    public void refreshSessionGauges() {
        List<PlatformSessionRecord> sessions = platformSessionRecordRepository.findAll();

        Map<String, Long> activeCounts = sessions.stream()
            .filter(session -> ACTIVE_STATES.contains(session.getState()))
            .collect(java.util.stream.Collectors.groupingBy(
                session -> session.getWorkshopId() + "|" + session.getMode().name(),
                java.util.stream.Collectors.counting()
            ));

        activeSessionsGauge.register(activeCounts.entrySet().stream()
            .map(entry -> {
                String[] parts = entry.getKey().split("\\|", 2);
                return MultiGauge.Row.of(
                    io.micrometer.core.instrument.Tags.of("workshop_id", parts[0], "mode", parts[1]),
                    entry.getValue()
                );
            })
            .toList(), true);

        Map<String, Long> stateCounts = sessions.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                session -> session.getState().name(),
                java.util.stream.Collectors.counting()
            ));

        sessionStateGauge.register(stateCounts.entrySet().stream()
            .map(entry -> MultiGauge.Row.of(
                io.micrometer.core.instrument.Tags.of("state", entry.getKey()),
                entry.getValue()
            ))
            .toList(), true);

        Instant now = Instant.now();
        long maxPendingAgeSeconds = sessions.stream()
            .filter(session -> PENDING_STATES.contains(session.getState()))
            .mapToLong(session -> Duration.between(session.getCreatedAt(), now).getSeconds())
            .max()
            .orElse(0);

        pendingSessionMaxAgeSeconds.set(maxPendingAgeSeconds);
    }

    public SessionMetricsSnapshot snapshot() {
        return new SessionMetricsSnapshot(
            sessionCreateRequests.count(),
            sessionCreateSuccess.count(),
            sessionTerminationCount.get(),
            pendingSessionMaxAgeSeconds.get()
        );
    }
}
