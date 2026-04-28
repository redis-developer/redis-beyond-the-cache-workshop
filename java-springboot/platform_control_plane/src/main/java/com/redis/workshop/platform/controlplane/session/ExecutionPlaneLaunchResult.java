package com.redis.workshop.platform.controlplane.session;

record ExecutionPlaneLaunchResult(
    String runtimeRef,
    ExecutionPlaneRouteBinding routeBinding
) {

    ExecutionPlaneLaunchResult(String runtimeRef, String publicEntryUrl) {
        this(runtimeRef, ExecutionPlaneRouteBinding.local(publicEntryUrl));
    }

    ExecutionPlaneLaunchResult {
        runtimeRef = normalizeRuntimeRef(runtimeRef);
        if (routeBinding == null) {
            throw new IllegalArgumentException("routeBinding is required");
        }
    }

    String publicEntryUrl() {
        return routeBinding.publicBasePath();
    }

    private static String normalizeRuntimeRef(String value) {
        String normalized = requireText(value, "runtimeRef");
        if (normalized.chars().anyMatch(Character::isWhitespace)) {
            throw new IllegalArgumentException("runtimeRef must not contain whitespace");
        }
        return normalized;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}

record ExecutionPlaneRouteBinding(
    String publicBasePath,
    String gatewayHost,
    String serviceName,
    String serviceNamespace,
    String upstreamBaseUrl
) {

    ExecutionPlaneRouteBinding {
        publicBasePath = normalizePublicBasePath(publicBasePath);
        gatewayHost = requireText(gatewayHost, "gatewayHost");
        serviceName = requireText(serviceName, "serviceName");
        serviceNamespace = requireText(serviceNamespace, "serviceNamespace");
        upstreamBaseUrl = requireText(upstreamBaseUrl, "upstreamBaseUrl");
    }

    static ExecutionPlaneRouteBinding local(String publicEntryUrl) {
        return new ExecutionPlaneRouteBinding(publicEntryUrl, "local", "local-session", "local", "http://localhost");
    }

    private static String normalizePublicBasePath(String value) {
        String normalized = requireText(value, "publicBasePath");
        if (normalized.startsWith("/") && !normalized.startsWith("//")) {
            return normalized;
        }
        try {
            java.net.URI uri = java.net.URI.create(normalized);
            if (uri.isAbsolute()
                && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                return normalized;
            }
        } catch (IllegalArgumentException ignored) {
        }
        throw new IllegalArgumentException("publicBasePath must be an absolute http URL or a root relative path");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
