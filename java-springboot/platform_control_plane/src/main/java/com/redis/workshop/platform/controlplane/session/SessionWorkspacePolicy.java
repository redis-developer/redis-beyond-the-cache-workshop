package com.redis.workshop.platform.controlplane.session;

record SessionWorkspacePolicy(
    String mountPath,
    String sourceRef,
    boolean writable,
    boolean deleteOnTerminate
) {

    private static final String DEFAULT_WORKSPACE_PATH = "/workshop-sources";

    SessionWorkspacePolicy {
        mountPath = requireText(mountPath, "mountPath");
        sourceRef = requireText(sourceRef, "sourceRef");
    }

    static SessionWorkspacePolicy defaultFor(String releaseId, SessionMode mode) {
        boolean mutable = mode == SessionMode.LAB;
        return new SessionWorkspacePolicy(
            DEFAULT_WORKSPACE_PATH,
            "release:" + requireText(releaseId, "releaseId"),
            mutable,
            mutable
        );
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required");
        }
        return value.trim();
    }
}
