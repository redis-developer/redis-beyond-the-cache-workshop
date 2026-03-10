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

    private String backendUrl;
    private String sourcePath;
    @Value("${workshop.backend.url:}")
    private String legacyBackendUrl;
    @Value("${workshop.source.path:}")
    private String legacySourcePath;

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

    public Optional<URI> resolveBackendUri() {
        String value = firstNonBlank(
            backendUrl,
            legacyBackendUrl,
            System.getenv("WORKSHOP_BACKEND_URL")
        );
        if (!StringUtils.hasText(value)) {
            return Optional.empty();
        }
        return Optional.of(URI.create(value));
    }

    public Optional<Path> resolveSourcePath() {
        String value = firstNonBlank(
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

    private static String firstNonBlank(String... candidates) {
        for (String candidate : candidates) {
            if (StringUtils.hasText(candidate)) {
                return candidate;
            }
        }
        return null;
    }
}
