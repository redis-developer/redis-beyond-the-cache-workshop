package com.redis.workshop.platform.controlplane.session;

interface ExecutionPlaneClient {

    ExecutionPlaneLaunchResult launchSession(SessionLaunchRequest request);

    ExecutionPlaneLaunchResult restartSession(SessionRestartRequest request);

    TerminationResult terminateSession(SessionTerminationRequest request);

    enum TerminationResult {
        TERMINATED,
        ALREADY_TERMINATED
    }

    enum FailureKind {
        UNAUTHORIZED,
        INVALID_RESPONSE,
        REMOTE_FAILURE,
        COMMUNICATION_FAILURE
    }

    final class ExecutionPlaneClientException extends IllegalStateException {

        private final FailureKind failureKind;

        ExecutionPlaneClientException(FailureKind failureKind, String message) {
            super(message);
            this.failureKind = failureKind;
        }

        ExecutionPlaneClientException(FailureKind failureKind, String message, Throwable cause) {
            super(message, cause);
            this.failureKind = failureKind;
        }

        FailureKind failureKind() {
            return failureKind;
        }
    }
}
