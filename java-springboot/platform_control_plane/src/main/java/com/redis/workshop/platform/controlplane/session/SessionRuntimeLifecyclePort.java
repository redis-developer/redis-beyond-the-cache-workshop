package com.redis.workshop.platform.controlplane.session;

public interface SessionRuntimeLifecyclePort {

    void requestLaunch(SessionLaunchRequest request);

    void requestRestart(SessionRestartRequest request);

    void requestTermination(SessionTerminationRequest request);

    void requestTerminationRetry(SessionTerminationRequest request);
}
