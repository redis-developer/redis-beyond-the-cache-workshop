package com.redis.workshop.platform.controlplane.session;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
class SessionLifecycleReconciler {

    private static final Logger logger = LoggerFactory.getLogger(SessionLifecycleReconciler.class);

    private final SessionService sessionService;

    SessionLifecycleReconciler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @Scheduled(fixedDelayString = "${platform.controlplane.session-reconciler.refresh-ms:30000}")
    void reconcile() {
        Instant now = Instant.now();
        int expired = sessionService.reconcileExpiredSessions(now);
        int terminationRetries = sessionService.reconcileLogicalEndedRuntimeTerminationRetries();
        int cleanupCompleted = sessionService.reconcileCompletedCleanupMetadata();

        if (expired > 0 || terminationRetries > 0 || cleanupCompleted > 0) {
            logger.info(
                "Session lifecycle reconciliation processed {} expired sessions, {} termination retries, {} cleanup completions",
                expired,
                terminationRetries,
                cleanupCompleted
            );
        }
    }
}
