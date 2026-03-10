package com.redis.workshop.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class WorkshopManifestLoader {

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper();
    private static final ObjectMapper YAML_MAPPER = new ObjectMapper(new YAMLFactory());
    private static final List<String> DEFAULT_LOCATIONS = List.of(
        "classpath:workshop-manifest.yaml",
        "classpath:workshop-manifest.yml",
        "classpath:workshop-manifest.json"
    );

    private final Environment environment;
    private final ResourceLoader resourceLoader;

    public WorkshopManifestLoader(Environment environment, ResourceLoader resourceLoader) {
        this.environment = environment;
        this.resourceLoader = resourceLoader;
    }

    public Optional<WorkshopConfig> loadWorkshopConfig() {
        return locateManifest(environment, resourceLoader)
            .map(this::loadWorkshopConfig);
    }

    public static boolean hasManifest(Environment environment, ResourceLoader resourceLoader) {
        return locateManifest(environment, resourceLoader).isPresent();
    }

    static Optional<ManifestSource> locateManifest(Environment environment, ResourceLoader resourceLoader) {
        for (String location : candidateLocations(environment)) {
            Resource resource = resolveResource(resourceLoader, location);
            if (resource.exists()) {
                return Optional.of(new ManifestSource(location, resource));
            }
        }
        return Optional.empty();
    }

    private WorkshopConfig loadWorkshopConfig(ManifestSource manifestSource) {
        WorkshopManifest manifest = readManifest(manifestSource);
        validateManifest(manifest, manifestSource.location());

        LinkedHashMap<String, String> editableFiles = new LinkedHashMap<>();
        Map<String, String> originalContents = new HashMap<>();
        Map<String, String> languages = new HashMap<>();

        for (WorkshopManifestFile editableFile : manifest.editableFiles()) {
            editableFiles.put(editableFile.name(), editableFile.path());
            originalContents.put(editableFile.name(), resolveResetContent(manifestSource, editableFile));
            if (StringUtils.hasText(editableFile.language())) {
                languages.put(editableFile.name(), editableFile.language().trim());
            }
        }

        return new ManifestWorkshopConfig(
            manifest.moduleName(),
            manifest.title(),
            manifest.description(),
            resolveBasePath(manifestSource, manifest.basePath()),
            editableFiles,
            originalContents,
            languages
        );
    }

    private WorkshopManifest readManifest(ManifestSource manifestSource) {
        try (InputStream inputStream = manifestSource.resource().getInputStream()) {
            ObjectMapper mapper = manifestSource.location().endsWith(".json") ? JSON_MAPPER : YAML_MAPPER;
            return mapper.readValue(inputStream, WorkshopManifest.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read workshop manifest from " + manifestSource.location(), e);
        }
    }

    private static void validateManifest(WorkshopManifest manifest, String location) {
        if (manifest == null) {
            throw new IllegalStateException("Workshop manifest at " + location + " is empty");
        }
        if (!StringUtils.hasText(manifest.moduleName())) {
            throw new IllegalStateException("Workshop manifest at " + location + " must define moduleName");
        }
        if (manifest.editableFiles() == null || manifest.editableFiles().isEmpty()) {
            throw new IllegalStateException("Workshop manifest at " + location + " must define editableFiles");
        }

        Set<String> names = new HashSet<>();
        for (WorkshopManifestFile editableFile : manifest.editableFiles()) {
            if (editableFile == null) {
                throw new IllegalStateException("Workshop manifest at " + location + " contains a null editable file entry");
            }
            if (!StringUtils.hasText(editableFile.name())) {
                throw new IllegalStateException("Workshop manifest at " + location + " contains an editable file without a name");
            }
            if (!StringUtils.hasText(editableFile.path())) {
                throw new IllegalStateException("Editable file " + editableFile.name() + " in " + location + " must define path");
            }
            if (!names.add(editableFile.name())) {
                throw new IllegalStateException("Workshop manifest at " + location + " contains duplicate editable file name " + editableFile.name());
            }
            if (editableFile.resetContent() == null && !StringUtils.hasText(editableFile.resetContentLocation())) {
                throw new IllegalStateException(
                    "Editable file " + editableFile.name() + " in " + location + " must define resetContent or resetContentLocation"
                );
            }
        }
    }

    private String resolveResetContent(ManifestSource manifestSource, WorkshopManifestFile editableFile) {
        if (editableFile.resetContent() != null) {
            return editableFile.resetContent();
        }

        Resource resetContentResource = resolveRelativeResource(manifestSource, editableFile.resetContentLocation());
        if (!resetContentResource.exists()) {
            throw new IllegalStateException(
                "Reset content resource " + editableFile.resetContentLocation() + " for " + editableFile.name() + " does not exist"
            );
        }

        try (InputStream inputStream = resetContentResource.getInputStream()) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to read reset content resource " + editableFile.resetContentLocation() + " for " + editableFile.name(),
                e
            );
        }
    }

    private Resource resolveRelativeResource(ManifestSource manifestSource, String location) {
        if (isExplicitLocation(location)) {
            return resolveResource(resourceLoader, location);
        }

        if (manifestSource.location().startsWith("classpath:")) {
            String manifestPath = manifestSource.location().substring("classpath:".length());
            int separatorIndex = manifestPath.lastIndexOf('/');
            String parentPath = separatorIndex >= 0 ? manifestPath.substring(0, separatorIndex + 1) : "";
            return resourceLoader.getResource("classpath:" + parentPath + location);
        }

        if (manifestSource.resource().isFile()) {
            try {
                Path manifestPath = manifestSource.resource().getFile().toPath();
                return new FileSystemResource(manifestPath.getParent().resolve(location).normalize());
            } catch (IOException ignored) {
                // Fall through to a process-relative path.
            }
        }

        return new FileSystemResource(Paths.get(location).toAbsolutePath().normalize());
    }

    private static String resolveBasePath(ManifestSource manifestSource, String basePath) {
        if (!StringUtils.hasText(basePath)) {
            return null;
        }

        Path configuredPath = Paths.get(basePath);
        if (configuredPath.isAbsolute()) {
            return configuredPath.normalize().toString();
        }

        try {
            if (manifestSource.resource().isFile()) {
                Path manifestPath = manifestSource.resource().getFile().toPath();
                return manifestPath.getParent().resolve(configuredPath).normalize().toString();
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                "Relative basePath " + basePath + " in " + manifestSource.location()
                    + " must resolve to a filesystem location. Use an absolute basePath or configure "
                    + "WORKSHOP_SOURCE_PATH/WORKSHOP_BASE_PATH at runtime.",
                e
            );
        }

        throw new IllegalStateException(
            "Relative basePath " + basePath + " in " + manifestSource.location()
                + " must resolve to a filesystem location. Use an absolute basePath or configure "
                + "WORKSHOP_SOURCE_PATH/WORKSHOP_BASE_PATH at runtime."
        );
    }

    private static List<String> candidateLocations(Environment environment) {
        String explicitLocation = firstNonBlank(
            environment.getProperty("workshop.manifest.location"),
            environment.getProperty("WORKSHOP_MANIFEST_LOCATION"),
            System.getenv("WORKSHOP_MANIFEST_LOCATION")
        );
        if (StringUtils.hasText(explicitLocation)) {
            return List.of(explicitLocation.trim());
        }
        return DEFAULT_LOCATIONS;
    }

    private static Resource resolveResource(ResourceLoader resourceLoader, String location) {
        if (location.startsWith("classpath:") || location.startsWith("file:")) {
            return resourceLoader.getResource(location);
        }
        return new FileSystemResource(Paths.get(location).toAbsolutePath().normalize());
    }

    private static boolean isExplicitLocation(String location) {
        if (!StringUtils.hasText(location)) {
            return false;
        }
        return location.startsWith("classpath:")
            || location.startsWith("file:")
            || Paths.get(location).isAbsolute();
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }

    record ManifestSource(String location, Resource resource) {
    }
}
