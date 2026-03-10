package com.redis.workshop.infrastructure;

import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

final class ManifestWorkshopConfig implements WorkshopConfig {

    private final String moduleName;
    private final String workshopTitle;
    private final String workshopDescription;
    private final String basePath;
    private final Map<String, String> editableFiles;
    private final Map<String, String> originalContents;
    private final Map<String, String> languages;

    ManifestWorkshopConfig(
        String moduleName,
        String workshopTitle,
        String workshopDescription,
        String basePath,
        Map<String, String> editableFiles,
        Map<String, String> originalContents,
        Map<String, String> languages
    ) {
        this.moduleName = moduleName;
        this.workshopTitle = workshopTitle;
        this.workshopDescription = workshopDescription;
        this.basePath = basePath;
        this.editableFiles = Collections.unmodifiableMap(new LinkedHashMap<>(editableFiles));
        this.originalContents = Map.copyOf(originalContents);
        this.languages = Map.copyOf(languages);
    }

    @Override
    public Map<String, String> getEditableFiles() {
        return editableFiles;
    }

    @Override
    public String getOriginalContent(String fileName) {
        return originalContents.get(fileName);
    }

    @Override
    public String getBasePath() {
        if (StringUtils.hasText(basePath)) {
            return basePath;
        }
        return WorkshopConfig.super.getBasePath();
    }

    @Override
    public String getModuleName() {
        return moduleName;
    }

    @Override
    public String getLanguage(String fileName) {
        String configuredLanguage = languages.get(fileName);
        if (StringUtils.hasText(configuredLanguage)) {
            return configuredLanguage;
        }

        String relativePath = editableFiles.get(fileName);
        if (StringUtils.hasText(relativePath)) {
            String pathLanguage = WorkshopConfig.super.getLanguage(relativePath);
            if (!"text".equals(pathLanguage)) {
                return pathLanguage;
            }
        }

        return WorkshopConfig.super.getLanguage(fileName);
    }

    @Override
    public String getWorkshopTitle() {
        if (StringUtils.hasText(workshopTitle)) {
            return workshopTitle;
        }
        return WorkshopConfig.super.getWorkshopTitle();
    }

    @Override
    public String getWorkshopDescription() {
        if (StringUtils.hasText(workshopDescription)) {
            return workshopDescription;
        }
        return WorkshopConfig.super.getWorkshopDescription();
    }
}
