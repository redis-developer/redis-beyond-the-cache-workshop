package com.redis.workshop.platform.controlplane.session;

import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class SessionLifecycleReconcilerTest {

    @Test
    void reconcileRunsExpiryThenTerminationRetryThenCleanupCompletion() {
        SessionService sessionService = mock(SessionService.class);
        when(sessionService.reconcileExpiredSessions(any())).thenReturn(1);
        when(sessionService.reconcileLogicalEndedRuntimeTerminationRetries()).thenReturn(2);
        when(sessionService.reconcileCompletedCleanupMetadata()).thenReturn(3);

        SessionLifecycleReconciler reconciler = new SessionLifecycleReconciler(sessionService);

        reconciler.reconcile();

        InOrder inOrder = inOrder(sessionService);
        inOrder.verify(sessionService).reconcileExpiredSessions(any());
        inOrder.verify(sessionService).reconcileLogicalEndedRuntimeTerminationRetries();
        inOrder.verify(sessionService).reconcileCompletedCleanupMetadata();
        verifyNoMoreInteractions(sessionService);
    }
}
