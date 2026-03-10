package com.redis.workshop.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EditorControllerTest {

    @TempDir
    Path configuredSourceDir;

    @TempDir
    Path fallbackSourceDir;

    @Test
    void loadSaveAndRestoreUseConfiguredSourcePath() throws IOException {
        String fileName = "application.properties";
        String relativePath = "src/main/resources/application.properties";
        String originalContent = "spring.session.store-type=none\n";

        Path configuredFile = configuredSourceDir.resolve(relativePath);
        Files.createDirectories(configuredFile.getParent());
        Files.writeString(configuredFile, originalContent);

        Path fallbackFile = fallbackSourceDir.resolve(relativePath);
        Files.createDirectories(fallbackFile.getParent());
        Files.writeString(fallbackFile, "fallback=true\n");

        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setSourcePath(configuredSourceDir.toString());

        WorkshopConfig workshopConfig = new TestWorkshopConfig(
            fallbackSourceDir.toString(),
            Map.of(fileName, relativePath),
            Map.of(fileName, originalContent)
        );
        EditorController controller = new EditorController(workshopConfig, runtimeProperties);

        Map<String, Object> loaded = controller.loadFile(fileName);
        assertThat(loaded.get("content")).isEqualTo(originalContent);

        controller.saveFile(fileName, Map.of("content", "spring.session.store-type=redis\n"));
        assertThat(Files.readString(configuredFile)).isEqualTo("spring.session.store-type=redis\n");
        assertThat(Files.readString(fallbackFile)).isEqualTo("fallback=true\n");

        controller.restoreFiles();
        assertThat(Files.readString(configuredFile)).isEqualTo(originalContent);
    }

    @Test
    void returnsErrorForUnknownFile() {
        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        WorkshopConfig workshopConfig = new TestWorkshopConfig(
            fallbackSourceDir.toString(),
            Map.of("application.properties", "src/main/resources/application.properties"),
            Map.of()
        );
        EditorController controller = new EditorController(workshopConfig, runtimeProperties);

        Map<String, Object> response = controller.loadFile("unknown.txt");
        assertThat(response.get("error")).isEqualTo("Invalid file name: unknown.txt");
    }

    private static class TestWorkshopConfig implements WorkshopConfig {
        private final String basePath;
        private final Map<String, String> editableFiles;
        private final Map<String, String> originalContents;

        private TestWorkshopConfig(
            String basePath,
            Map<String, String> editableFiles,
            Map<String, String> originalContents
        ) {
            this.basePath = basePath;
            this.editableFiles = editableFiles;
            this.originalContents = originalContents;
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
            return basePath;
        }

        @Override
        public String getModuleName() {
            return "workshop-infrastructure-test";
        }
    }
}
