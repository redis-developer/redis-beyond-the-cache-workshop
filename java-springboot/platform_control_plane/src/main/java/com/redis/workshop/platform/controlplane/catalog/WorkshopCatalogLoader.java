package com.redis.workshop.platform.controlplane.catalog;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Component
class WorkshopCatalogLoader {

    private final ResourceLoader resourceLoader;
    private final String registryPath;
    private final YamlPropertySourceLoader yamlPropertySourceLoader = new YamlPropertySourceLoader();

    WorkshopCatalogLoader(
        ResourceLoader resourceLoader,
        @Value("${workshop.registry.path:workshops.yaml}") String registryPath
    ) {
        this.resourceLoader = resourceLoader;
        this.registryPath = registryPath;
    }

    List<WorkshopCatalogDocument> loadWorkshops() {
        Resource resource = resolveRegistryResource();
        if (resource == null) {
            return List.of();
        }

        try {
            MutablePropertySources propertySources = new MutablePropertySources();
            for (PropertySource<?> propertySource : yamlPropertySourceLoader.load("workshop-registry", resource)) {
                propertySources.addLast(propertySource);
            }
            Binder binder = new Binder(ConfigurationPropertySources.from(propertySources));
            WorkshopRegistryDocument registry = binder.bind("", Bindable.of(WorkshopRegistryDocument.class))
                .orElseGet(WorkshopRegistryDocument::new);
            return registry.getWorkshops() == null ? List.of() : List.copyOf(registry.getWorkshops());
        } catch (IOException ex) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to read workshop registry", ex);
        }
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

    public static class WorkshopRegistryDocument {
        private int version;
        private List<WorkshopCatalogDocument> workshops = Collections.emptyList();

        public int getVersion() {
            return version;
        }

        public void setVersion(int version) {
            this.version = version;
        }

        public List<WorkshopCatalogDocument> getWorkshops() {
            return workshops;
        }

        public void setWorkshops(List<WorkshopCatalogDocument> workshops) {
            this.workshops = workshops;
        }
    }

    public static class WorkshopCatalogDocument {
        private String id;
        private String title;
        private String description;
        private String difficulty;
        private int estimatedMinutes;
        private String url;
        private List<String> topics = Collections.emptyList();

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getDifficulty() {
            return difficulty;
        }

        public void setDifficulty(String difficulty) {
            this.difficulty = difficulty;
        }

        public int getEstimatedMinutes() {
            return estimatedMinutes;
        }

        public void setEstimatedMinutes(int estimatedMinutes) {
            this.estimatedMinutes = estimatedMinutes;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public List<String> getTopics() {
            return topics;
        }

        public void setTopics(List<String> topics) {
            this.topics = topics;
        }
    }
}
