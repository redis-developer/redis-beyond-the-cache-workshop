package com.redis.workshop.platform.controlplane.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WorkshopCatalogLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void resolvesRegistryByWalkingUpFromWorkingDirectory() throws Exception {
        Path registry = tempDir.resolve("workshops.yaml");
        Files.writeString(registry, "version: 1\nworkshops: []\n");

        WorkshopCatalogLoader loader = new WorkshopCatalogLoader(new DefaultResourceLoader(), "workshops.yaml");

        Path resolved = loader.resolveRegistryPath(tempDir.resolve("java-springboot/platform_control_plane"));

        assertThat(resolved).isEqualTo(registry);
    }
}
