package com.redis.workshop.platform.controlplane.release;

import com.redis.workshop.platform.controlplane.audit.ReleaseCatalogAuditHooks;
import com.redis.workshop.platform.controlplane.observability.ReleaseCatalogMetricsRecorder;
import com.redis.workshop.platform.controlplane.session.SessionMode;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class ReleaseCatalogService {

    private static final Pattern RELEASE_ID_PATTERN = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9._:-]{0,127}$");
    private static final Pattern IMMUTABLE_IMAGE_REFERENCE_PATTERN = Pattern.compile("^[^@\\s]+@sha256:[a-fA-F0-9]{64}$");

    private final ReleaseCatalogLoader releaseCatalogLoader;
    private final ReleaseCatalogAuditHooks releaseCatalogAuditHooks;
    private final ReleaseCatalogMetricsRecorder releaseCatalogMetricsRecorder;

    public ReleaseCatalogService(
        ReleaseCatalogLoader releaseCatalogLoader,
        ReleaseCatalogAuditHooks releaseCatalogAuditHooks,
        ReleaseCatalogMetricsRecorder releaseCatalogMetricsRecorder
    ) {
        this.releaseCatalogLoader = releaseCatalogLoader;
        this.releaseCatalogAuditHooks = releaseCatalogAuditHooks;
        this.releaseCatalogMetricsRecorder = releaseCatalogMetricsRecorder;
    }

    public List<ReleaseCatalogEntry> findAllReleases() {
        return loadValidatedReleases();
    }

    public ReleaseCatalogInspection inspectCatalog() {
        try {
            return ReleaseCatalogInspection.available(findAllReleases());
        } catch (ResponseStatusException ex) {
            return ReleaseCatalogInspection.unavailable(ex.getReason());
        }
    }

    public Optional<ReleaseCatalogEntry> findDefaultRelease(String workshopId) {
        List<ReleaseCatalogEntry> workshopReleases = loadValidatedReleases().stream()
            .filter(entry -> entry.workshopId().equals(workshopId))
            .toList();
        if (workshopReleases.isEmpty()) {
            releaseCatalogMetricsRecorder.recordLookupMiss(workshopId, "default");
            return Optional.empty();
        }
        if (workshopReleases.size() == 1) {
            return Optional.of(workshopReleases.get(0));
        }

        List<ReleaseCatalogEntry> defaults = workshopReleases.stream()
            .filter(ReleaseCatalogEntry::defaultForWorkshop)
            .toList();
        if (defaults.size() != 1) {
            throw invalidCatalog("Workshop " + workshopId + " must declare exactly one default release");
        }
        return Optional.of(defaults.getFirst());
    }

    public Optional<ReleaseCatalogEntry> findRelease(String workshopId, String releaseVersion) {
        if (!StringUtils.hasText(workshopId) || !StringUtils.hasText(releaseVersion)) {
            return Optional.empty();
        }
        Optional<ReleaseCatalogEntry> release = loadValidatedReleases().stream()
            .filter(entry -> entry.workshopId().equals(workshopId))
            .filter(entry -> entry.releaseVersion().equals(releaseVersion))
            .findFirst();
        if (release.isEmpty()) {
            releaseCatalogMetricsRecorder.recordLookupMiss(workshopId, releaseVersion);
        }
        return release;
    }

    public List<SessionMode> findSupportedModes(String workshopId) {
        return loadValidatedReleases().stream()
            .filter(entry -> entry.workshopId().equals(workshopId))
            .map(ReleaseCatalogEntry::mode)
            .distinct()
            .toList();
    }

    List<ReleaseCatalogEntry> loadValidatedReleases() {
        try {
            List<ReleaseCatalogEntry> releases = releaseCatalogLoader.loadReleases().stream()
                .map(this::toEntry)
                .toList();

            Set<String> releaseIds = new HashSet<>();
            Set<String> workshopVersionPairs = new HashSet<>();
            for (ReleaseCatalogEntry entry : releases) {
                if (!releaseIds.add(entry.releaseId())) {
                    throw invalidCatalog("Duplicate releaseId " + entry.releaseId());
                }
                String workshopVersionKey = entry.workshopId() + "::" + entry.releaseVersion();
                if (!workshopVersionPairs.add(workshopVersionKey)) {
                    throw invalidCatalog("Duplicate release version " + entry.releaseVersion() + " for workshop " + entry.workshopId());
                }
            }
            releaseCatalogMetricsRecorder.recordCatalogLoadSuccess(releases.size());
            releaseCatalogAuditHooks.recordCatalogActivation(releases);
            return releases;
        } catch (ResponseStatusException ex) {
            releaseCatalogMetricsRecorder.recordCatalogLoadFailure(ex.getReason());
            releaseCatalogAuditHooks.recordCatalogLoadFailure(ex.getReason());
            throw ex;
        } catch (RuntimeException ex) {
            releaseCatalogMetricsRecorder.recordCatalogLoadFailure(ex.getMessage());
            releaseCatalogAuditHooks.recordCatalogLoadFailure(ex.getMessage());
            throw ex;
        }
    }

    private ReleaseCatalogEntry toEntry(ReleaseCatalogLoader.ReleaseCatalogDocument.ReleaseDocument document) {
        requireText(document.getReleaseId(), "releaseId");
        requireText(document.getWorkshopId(), "workshopId");
        requireText(document.getReleaseVersion(), "releaseVersion");
        requireText(document.getResourceClass(), "resourceClass");
        if (!RELEASE_ID_PATTERN.matcher(document.getReleaseId()).matches()) {
            throw invalidCatalog("releaseId must be a stable identifier: " + document.getReleaseId());
        }
        if (document.getMode() == null) {
            throw invalidCatalog("mode is required for release " + document.getReleaseId());
        }
        if (document.getSessionTtlMinutes() <= 0) {
            throw invalidCatalog("sessionTtlMinutes must be positive for release " + document.getReleaseId());
        }

        ReleaseImageReferences images = new ReleaseImageReferences(
            trimToNull(document.getImages() == null ? null : document.getImages().getFrontend()),
            trimToNull(document.getImages() == null ? null : document.getImages().getBackend()),
            trimToNull(document.getImages() == null ? null : document.getImages().getCombined()),
            trimToNull(document.getImages() == null ? null : document.getImages().getInit())
        );
        if (images.declaredReferences().isEmpty()) {
            throw invalidCatalog("At least one immutable image reference is required for release " + document.getReleaseId());
        }
        for (String imageReference : images.declaredReferences()) {
            if (!IMMUTABLE_IMAGE_REFERENCE_PATTERN.matcher(imageReference).matches()) {
                throw invalidCatalog("Image reference must be immutable and digest pinned: " + imageReference);
            }
        }

        List<String> mutableDependencies = document.getMutableDependencies() == null
            ? List.of()
            : document.getMutableDependencies().stream()
                .map(this::normalizeDependency)
                .toList();
        List<String> environments = document.getEnvironments() == null
            ? List.of()
            : document.getEnvironments().stream()
                .map(this::normalizeEnvironment)
                .toList();
        if (environments.isEmpty()) {
            throw invalidCatalog("environments are required for release " + document.getReleaseId());
        }

        return new ReleaseCatalogEntry(
            document.getReleaseId().trim(),
            document.getWorkshopId().trim(),
            document.getReleaseVersion().trim(),
            document.getMode(),
            document.isDefaultForWorkshop(),
            images,
            document.getResourceClass().trim(),
            document.getSessionTtlMinutes(),
            mutableDependencies,
            document.isEnabled(),
            environments
        );
    }

    private String normalizeDependency(String dependency) {
        String normalized = requireText(dependency, "mutableDependencies").trim().toLowerCase(Locale.ROOT);
        return normalized;
    }

    private String normalizeEnvironment(String environment) {
        return requireText(environment, "environments").trim().toLowerCase(Locale.ROOT);
    }

    private String requireText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw invalidCatalog(fieldName + " is required");
        }
        return value;
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private ResponseStatusException invalidCatalog(String message) {
        return new ResponseStatusException(INTERNAL_SERVER_ERROR, "Invalid release catalog: " + message);
    }
}
