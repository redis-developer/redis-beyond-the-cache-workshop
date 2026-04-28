package com.redis.workshop.platform.controlplane.release;

import com.redis.workshop.platform.controlplane.session.SessionMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Component
class ReleaseCatalogLoader {

    private final ResourceLoader resourceLoader;
    private final String registryPath;
    private final YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();

    ReleaseCatalogLoader(
        ResourceLoader resourceLoader,
        @Value("${workshop.registry.path:workshops.yaml}") String registryPath
    ) {
        this.resourceLoader = resourceLoader;
        this.registryPath = registryPath;
    }

    List<ReleaseCatalogDocument.ReleaseDocument> loadReleases() {
        Resource resource = resolveRegistryResource();
        if (resource == null) {
            return List.of();
        }

        try {
            MutablePropertySources propertySources = new MutablePropertySources();
            for (PropertySource<?> propertySource : yamlPropertySourceLoader.load("workshop-registry-releases", resource)) {
                propertySources.addLast(propertySource);
            }
            Binder binder = new Binder(ConfigurationPropertySources.from(propertySources));
            ReleaseCatalogDocument registry = binder.bind("", Bindable.of(ReleaseCatalogDocument.class))
                .orElseGet(ReleaseCatalogDocument::new);
            return flattenReleases(registry);
        } catch (IOException ex) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to read workshop registry releases", ex);
        }
    }

    private List<ReleaseCatalogDocument.ReleaseDocument> flattenReleases(ReleaseCatalogDocument registry) {
        if (registry.getWorkshops() == null) {
            return List.of();
        }
        List<ReleaseCatalogDocument.ReleaseDocument> releases = new ArrayList<>();
        for (ReleaseCatalogDocument.WorkshopDocument workshop : registry.getWorkshops()) {
            if (workshop.getReleases() == null) {
                continue;
            }
            for (ReleaseCatalogDocument.ReleaseDocument release : workshop.getReleases()) {
                if (!StringUtils.hasText(release.getWorkshopId())) {
                    release.setWorkshopId(workshop.getId());
                }
                releases.add(release);
            }
        }
        return List.copyOf(releases);
    }

    private Resource resolveRegistryResource() {
        Path resolvedPath = resolveRegistryPath(Paths.get("").toAbsolutePath().normalize());
        if (resolvedPath != null && Files.exists(resolvedPath)) {
            return resourceLoader.getResource(resolvedPath.toUri().toString());
        }

        Resource classpathResource = resourceLoader.getResource("classpath:workshops.yaml");
        return classpathResource.exists() ? classpathResource : null;
    }

    Path resolveRegistryPath(Path workingDirectory) {
        if (!StringUtils.hasText(registryPath)) {
            return null;
        }

        Path configuredPath = Path.of(registryPath).normalize();
        if (configuredPath.isAbsolute()) {
            return configuredPath;
        }

        Path current = workingDirectory.normalize();
        while (current != null) {
            Path candidate = current.resolve(configuredPath).normalize();
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }

        return configuredPath;
    }

    static class ReleaseCatalogDocument {
        private int version;
        private List<WorkshopDocument> workshops = Collections.emptyList();

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public List<WorkshopDocument> getWorkshops() {
            return workshops;
        }

        public void setWorkshops(List<WorkshopDocument> workshops) {
            this.workshops = workshops;
        }

        static class WorkshopDocument {
            private String id;
            private List<ReleaseDocument> releases = Collections.emptyList();

            public String getId() {
                return id;
            }

            public void setId(String id) {
                this.id = id;
            }

            public List<ReleaseDocument> getReleases() {
                return releases;
            }

            public void setReleases(List<ReleaseDocument> releases) {
                this.releases = releases;
            }
        }

        static class ReleaseDocument {
            private String releaseId;
            private String workshopId;
            private String releaseVersion;
            private SessionMode mode;
            private boolean defaultForWorkshop;
            private ReleaseImagesDocument images = new ReleaseImagesDocument();
            private String resourceClass;
            private int sessionTtlMinutes;
            private List<String> mutableDependencies = Collections.emptyList();
            private boolean enabled;
            private List<String> environments = Collections.emptyList();

            public String getReleaseId() {
                return releaseId;
            }

            public void setReleaseId(String releaseId) {
                this.releaseId = releaseId;
            }

            public String getWorkshopId() {
                return workshopId;
            }

            public void setWorkshopId(String workshopId) {
                this.workshopId = workshopId;
            }

            public String getReleaseVersion() {
                return releaseVersion;
            }

            public void setReleaseVersion(String releaseVersion) {
                this.releaseVersion = releaseVersion;
            }

            public SessionMode getMode() {
                return mode;
            }

            public void setMode(SessionMode mode) {
                this.mode = mode;
            }

            public boolean isDefaultForWorkshop() {
                return defaultForWorkshop;
            }

            public void setDefaultForWorkshop(boolean defaultForWorkshop) {
                this.defaultForWorkshop = defaultForWorkshop;
            }

            public ReleaseImagesDocument getImages() {
                return images;
            }

            public void setImages(ReleaseImagesDocument images) {
                this.images = images;
            }

            public String getResourceClass() {
                return resourceClass;
            }

            public void setResourceClass(String resourceClass) {
                this.resourceClass = resourceClass;
            }

            public int getSessionTtlMinutes() {
                return sessionTtlMinutes;
            }

            public void setSessionTtlMinutes(int sessionTtlMinutes) {
                this.sessionTtlMinutes = sessionTtlMinutes;
            }

            public List<String> getMutableDependencies() {
                return mutableDependencies;
            }

            public void setMutableDependencies(List<String> mutableDependencies) {
                this.mutableDependencies = mutableDependencies;
            }

            public boolean isEnabled() {
                return enabled;
            }

            public void setEnabled(boolean enabled) {
                this.enabled = enabled;
            }

            public List<String> getEnvironments() {
                return environments;
            }

            public void setEnvironments(List<String> environments) {
                this.environments = environments;
            }
        }

        static class ReleaseImagesDocument {
            private String frontend;
            private String backend;
            private String combined;
            private String init;

            public String getFrontend() {
                return frontend;
            }

            public void setFrontend(String frontend) {
                this.frontend = frontend;
            }

            public String getBackend() {
                return backend;
            }

            public void setBackend(String backend) {
                this.backend = backend;
            }

            public String getCombined() {
                return combined;
            }

            public void setCombined(String combined) {
                this.combined = combined;
            }

            public String getInit() {
                return init;
            }

            public void setInit(String init) {
                this.init = init;
            }
        }
    }
}
