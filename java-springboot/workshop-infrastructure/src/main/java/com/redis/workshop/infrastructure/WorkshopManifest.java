package com.redis.workshop.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
record WorkshopManifest(
    String moduleName,
    String basePath,
    String title,
    String description,
    List<WorkshopManifestFile> editableFiles
) {
}
