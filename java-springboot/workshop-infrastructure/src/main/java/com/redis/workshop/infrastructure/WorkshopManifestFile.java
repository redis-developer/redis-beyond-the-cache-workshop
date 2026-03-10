package com.redis.workshop.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record WorkshopManifestFile(
    String name,
    String path,
    String language,
    String resetContent,
    String resetContentLocation
) {
}
