package com.redis.workshop.infrastructure;

import org.springframework.util.StringUtils;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

final class SessionRuntimeResolver {

    private final FrontendRuntimeProperties runtimeProperties;
    private final SessionRunnerProperties runnerProperties;
    private final Path legacyModuleRoot;

    SessionRuntimeResolver(FrontendRuntimeProperties runtimeProperties) {
        this(runtimeProperties, null, (Path) null);
    }

    SessionRuntimeResolver(FrontendRuntimeProperties runtimeProperties, SessionRunnerProperties runnerProperties) {
        this(runtimeProperties, runnerProperties, (Path) null);
    }

    SessionRuntimeResolver(FrontendRuntimeProperties runtimeProperties, WorkshopConfig workshopConfig) {
        this(
            runtimeProperties,
            null,
            workshopConfig == null || !StringUtils.hasText(workshopConfig.getBasePath())
                ? null
                : Paths.get(workshopConfig.getBasePath()).toAbsolutePath().normalize()
        );
    }

    SessionRuntimeResolver(FrontendRuntimeProperties runtimeProperties, Path legacyModuleRoot) {
        this(runtimeProperties, null, legacyModuleRoot);
    }

    SessionRuntimeResolver(
        FrontendRuntimeProperties runtimeProperties,
        SessionRunnerProperties runnerProperties,
        Path legacyModuleRoot
    ) {
        this.runtimeProperties = runtimeProperties;
        this.runnerProperties = runnerProperties;
        this.legacyModuleRoot = legacyModuleRoot == null ? null : legacyModuleRoot.toAbsolutePath().normalize();
    }

    Optional<String> resolveSessionId() {
        return runtimeProperties.resolveSessionId();
    }

    Optional<URI> resolveBackendUri() {
        if (runnerProperties != null && runnerProperties.isEnabled()) {
            Optional<URI> childBackendUri = runnerProperties.resolveChildBackendUri();
            if (childBackendUri.isPresent()) {
                return childBackendUri;
            }
        }
        return runtimeProperties.resolveBackendUri();
    }

    Optional<URI> resolveSessionRuntimeBaseUri() {
        return runtimeProperties.resolveSessionRuntimeBaseUri();
    }

    Optional<Path> resolveWorkspaceRoot() {
        return runtimeProperties.resolveWorkspacePath()
            .or(() -> Optional.ofNullable(legacyModuleRoot));
    }

    Optional<Path> resolveCodeEditorWorkspaceRoot() {
        return runtimeProperties.resolveCodeEditorWorkspacePath();
    }

    Optional<Path> resolveModuleRoot() {
        return resolveWorkspaceRoot();
    }

    Optional<Path> resolveContentRoot() {
        return resolveModuleRoot().map(moduleRoot -> moduleRoot.resolve("src/main/resources/workshop-content").normalize());
    }

    Optional<Path> resolvePathWithinModule(String relativePath) {
        return resolvePathWithinRoot(resolveModuleRoot(), relativePath);
    }

    Optional<Path> resolveCodeEditorPathWithinModule(String relativePath) {
        return resolvePathWithinRoot(resolveCodeEditorWorkspaceRoot(), relativePath);
    }

    private Optional<Path> resolvePathWithinRoot(Optional<Path> root, String relativePath) {
        if (!StringUtils.hasText(relativePath)) {
            return Optional.empty();
        }

        if (root.isEmpty()) {
            return Optional.empty();
        }

        Path normalizedRelativePath = Path.of(relativePath).normalize();
        if (normalizedRelativePath.isAbsolute()) {
            return Optional.empty();
        }

        Path resolvedPath = root.get().resolve(normalizedRelativePath).normalize();
        if (!resolvedPath.startsWith(root.get())) {
            return Optional.empty();
        }
        return Optional.of(resolvedPath);
    }
}
