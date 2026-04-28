package com.redis.workshop.platform.controlplane.session;

interface SessionRuntimeReadinessProbe {

    void awaitReady(ExecutionPlaneRouteBinding routeBinding);
}
