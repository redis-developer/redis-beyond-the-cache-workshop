package com.redis.workshop.platform.controlplane.release;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Optional;

@Component
public class PilotLaunchRuleService {

    private static final String CURRENT_RELEASE_VERSION = "current";

    private final ReleaseCatalogService releaseCatalogService;
    private final String currentEnvironment;

    public PilotLaunchRuleService(
        ReleaseCatalogService releaseCatalogService,
        @Value("${platform.controlplane.release.environment:local}") String currentEnvironment
    ) {
        this.releaseCatalogService = releaseCatalogService;
        this.currentEnvironment = normalizeEnvironment(currentEnvironment);
    }

    public WorkshopLaunchDefaults resolveLaunchDefaults(String workshopId) {
        String normalizedWorkshopId = requireText(workshopId, "workshopId");
        Optional<ReleaseCatalogEntry> defaultRelease = releaseCatalogService.findDefaultRelease(normalizedWorkshopId);
        if (defaultRelease.isEmpty()) {
            return WorkshopLaunchDefaults.current();
        }

        ReleaseCatalogEntry release = defaultRelease.get();
        if (!isEnabledForCurrentEnvironment(release)) {
            return WorkshopLaunchDefaults.current();
        }
        return WorkshopLaunchDefaults.releaseBacked(release);
    }

    public String resolveRequestedReleaseVersion(
        String workshopId,
        String requestedReleaseVersion,
        String defaultReleaseVersion
    ) {
        String normalizedWorkshopId = requireText(workshopId, "workshopId");
        String normalizedRequestedReleaseVersion = trimToNull(requestedReleaseVersion);
        String normalizedDefaultReleaseVersion = requireText(defaultReleaseVersion, "defaultReleaseVersion");
        WorkshopLaunchDefaults defaults = resolveLaunchDefaults(normalizedWorkshopId);

        if (!StringUtils.hasText(normalizedRequestedReleaseVersion)) {
            return normalizedDefaultReleaseVersion;
        }

        if (defaults.releaseBacked()) {
            if (!normalizedRequestedReleaseVersion.equals(defaults.defaultReleaseVersion())) {
                throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Release defaults for workshop " + normalizedWorkshopId
                        + " only allows releaseVersion " + defaults.defaultReleaseVersion()
                );
            }
            return normalizedRequestedReleaseVersion;
        }

        if (CURRENT_RELEASE_VERSION.equals(normalizedRequestedReleaseVersion)) {
            return normalizedRequestedReleaseVersion;
        }

        if (releaseCatalogService.findRelease(normalizedWorkshopId, normalizedRequestedReleaseVersion).isPresent()) {
            throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Release defaults for workshop " + normalizedWorkshopId
                    + " are disabled or inactive. Release version " + normalizedRequestedReleaseVersion
                    + " is not launchable in the current environment."
            );
        }

        throw new ResponseStatusException(
            HttpStatus.CONFLICT,
            "Release version " + normalizedRequestedReleaseVersion
                + " is not configured for workshop " + normalizedWorkshopId
        );
    }

    private boolean isEnabledForCurrentEnvironment(ReleaseCatalogEntry release) {
        if (!release.enabled()) {
            return false;
        }
        return release.environments().stream()
            .filter(StringUtils::hasText)
            .map(this::normalizeEnvironment)
            .anyMatch(currentEnvironment::equals);
    }

    private String normalizeEnvironment(String environment) {
        return requireText(environment, "environment").trim().toLowerCase(Locale.ROOT);
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Invalid release default configuration: " + fieldName + " is required"
            );
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
