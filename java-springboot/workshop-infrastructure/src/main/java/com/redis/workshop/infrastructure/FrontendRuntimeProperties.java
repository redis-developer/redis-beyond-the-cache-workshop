package com.redis.workshop.infrastructure;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Component
@ConfigurationProperties(prefix = "workshop.frontend")
public class FrontendRuntimeProperties {

    // Session runtime aliases:
    // workshop.frontend.session-id
    // workshop.frontend.workspace-path
    // workshop.frontend.session-backend-url
    // workshop.frontend.session-runtime-base-url
    private String sessionId;
    private String sessionBackendUrl;
    private String sessionRuntimeBaseUrl;
    private String workspacePath;
    private String codeEditorWorkspacePath;
    private String backendUrl;
    private String sourcePath;
    @Value("${workshop.backend.url:}")
    private String legacyBackendUrl;
    @Value("${workshop.source.path:}")
    private String legacySourcePath;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionBackendUrl() {
        return sessionBackendUrl;
    }

    public void setSessionBackendUrl(String sessionBackendUrl) {
        this.sessionBackendUrl = sessionBackendUrl;
    }

    public String getWorkspacePath() {
        return workspacePath;
    }

    public void setWorkspacePath(String workspacePath) {
        this.workspacePath = workspacePath;
    }

    public String getCodeEditorWorkspacePath() {
        return codeEditorWorkspacePath;
    }

    public void setCodeEditorWorkspacePath(String codeEditorWorkspacePath) {
        this.codeEditorWorkspacePath = codeEditorWorkspacePath;
    }

    public String getSessionRuntimeBaseUrl() {
        return sessionRuntimeBaseUrl;
    }

    public void setSessionRuntimeBaseUrl(String sessionRuntimeBaseUrl) {
        this.sessionRuntimeBaseUrl = sessionRuntimeBaseUrl;
    }

    public String getBackendUrl() {
        return backendUrl;
    }

    public void setBackendUrl(String backendUrl) {
        this.backendUrl = backendUrl;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public void setSourcePath(String sourcePath) {
        this.sourcePath = sourcePath;
    }

    public Optional<String> resolveSessionId() {
        String value = firstNonBlank(
            sessionId,
            System.getenv("WORKSHOP_SESSION_ID")
        );
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        return Optional.of(value.trim());
    }

    public Optional<URI> resolveBackendUri() {
        String value = firstNonBlank(
            sessionBackendUrl,
            System.getenv("WORKSHOP_SESSION_BACKEND_URL"),
            backendUrl,
            legacyBackendUrl,
            System.getenv("WORKSHOP_BACKEND_URL")
        );
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        return Optional.of(URI.create(value));
    }

    public Optional<URI> resolveSessionRuntimeBaseUri() {
        String value = firstNonBlank(
            sessionRuntimeBaseUrl,
            System.getenv("WORKSHOP_SESSION_RUNTIME_BASE_URL")
        );
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        return Optional.of(URI.create(value));
    }

    public Optional<Path> resolveWorkspacePath() {
        String value = firstNonBlank(
            workspacePath,
            System.getenv("WORKSHOP_SESSION_WORKSPACE_PATH"),
            sourcePath,
            legacySourcePath,
            System.getenv("WORKSHOP_SOURCE_PATH"),
            System.getenv("WORKSHOP_BASE_PATH")
        );
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        return Optional.of(Paths.get(value).toAbsolutePath().normalize());
    }

    public Optional<Path> resolveSourcePath() {
        return resolveWorkspacePath();
    }

    public Optional<Path> resolveCodeEditorWorkspacePath() {
        String value = firstNonBlank(
            codeEditorWorkspacePath,
            System.getenv("WORKSHOP_LOCAL_CODE_EDITOR_WORKSPACE_PATH"),
            System.getenv("WORKSHOP_CODE_EDITOR_WORKSPACE_PATH")
        );
        if (!StringUtils.hasText(value)) {
            return resolveWorkspacePath();
        }
        return Optional.of(Paths.get(value).toAbsolutePath().normalize());
    }

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
