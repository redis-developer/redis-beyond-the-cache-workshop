package com.redis.workshop.infrastructure.workspace;

import java.nio.file.Path;
import java.util.Objects;

public record WorkspaceMetadata(
    String sessionId,
    Path baselinePath,
    Path localWorkspacePath,
    String durablePrefix
) {

    public WorkspaceMetadata {
        sessionId = requirePathSegment(sessionId, "sessionId");
        baselinePath = normalizePath(baselinePath, "baselinePath");
        localWorkspacePath = normalizePath(localWorkspacePath, "localWorkspacePath");
        durablePrefix = normalizeDurablePrefix(durablePrefix, sessionId);
    }

    public static String defaultDurablePrefix(String sessionId) {
        return "sessions/" + requirePathSegment(sessionId, "sessionId") + "/workspace";
    }

    private static Path normalizePath(Path path, String name) {
        return Objects.requireNonNull(path, name).toAbsolutePath().normalize();
    }

    private static String normalizeDurablePrefix(String durablePrefix, String sessionId) {
        String value = durablePrefix;
        if (value == null || value.isBlank()) {
            value = defaultDurablePrefix(sessionId);
        }
        value = value.trim().replace('\\', '/');
        while (value.startsWith("/")) {
            value = value.substring(1);
        }
        while (value.endsWith("/")) {
            value = value.substring(0, value.length() - 1);
        }
        if (value.isBlank()) {
            throw new IllegalArgumentException("durablePrefix must not be blank");
        }

        String[] segments = value.split("/");
        for (int index = 0; index < segments.length; index++) {
            segments[index] = requirePathSegment(segments[index], "durablePrefix segment");
        }
        return String.join("/", segments);
    }

    private static String requirePathSegment(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        String trimmed = value.trim();
        if (".".equals(trimmed) || "..".equals(trimmed) || trimmed.contains("/") || trimmed.contains("\\")) {
            throw new IllegalArgumentException(name + " must be a single safe path segment");
        }
        return trimmed;
    }
}
