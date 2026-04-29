package com.redis.workshop.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

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
    void loadSaveAndRestorePreferSessionWorkspacePath() throws IOException {
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
        runtimeProperties.setWorkspacePath(configuredSourceDir.toString());
        runtimeProperties.setSourcePath(fallbackSourceDir.toString());

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
    void rejectsPathsThatEscapeResolvedWorkspace() {
        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setWorkspacePath(configuredSourceDir.toString());

        WorkshopConfig workshopConfig = new TestWorkshopConfig(
            fallbackSourceDir.toString(),
            Map.of("escape.txt", "../escape.txt"),
            Map.of()
        );
        EditorController controller = new EditorController(workshopConfig, runtimeProperties);

        assertThat(controller.loadFile("escape.txt")).containsEntry("error", "Invalid file name: escape.txt");
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

    @Test
    void editableFileMetadataIncludesResolvedWorkspacePaths() {
        String fileName = "application.properties";
        String relativePath = "src/main/resources/application.properties";

        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setWorkspacePath(configuredSourceDir.toString());
        runtimeProperties.setCodeEditorWorkspacePath("/home/coder/project");

        WorkshopConfig workshopConfig = new TestWorkshopConfig(
            fallbackSourceDir.toString(),
            Map.of(fileName, relativePath),
            Map.of()
        );
        EditorController controller = new EditorController(workshopConfig, runtimeProperties);

        Map<String, Object> response = controller.getEditableFiles();

        assertThat(response).containsEntry("workspaceRoot", configuredSourceDir.toString());
        assertThat(response).containsEntry("codeEditorWorkspaceRoot", "/home/coder/project");
        assertThat(response.get("files"))
            .asList()
            .singleElement()
            .satisfies(file -> assertThat(file)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.MAP)
                .containsEntry("name", fileName)
                .containsEntry("path", relativePath)
                .containsEntry("workspacePath", configuredSourceDir.resolve(relativePath).toString())
                .containsEntry("codeEditorWorkspacePath", "/home/coder/project/" + relativePath));
    }

    @Test
    void loadSaveAndRestoreUseManifestBackedWorkshopConfig() throws IOException {
        String fileName = "application.properties";
        String relativePath = "src/main/resources/application.properties";
        String originalContent = "spring.session.store-type=none\n";

        Path configuredFile = configuredSourceDir.resolve(relativePath);
        Files.createDirectories(configuredFile.getParent());
        Files.writeString(configuredFile, originalContent);

        Path manifestDir = Files.createDirectories(fallbackSourceDir.resolve("manifest"));
        Files.createDirectories(manifestDir.resolve("reset"));
        Files.writeString(manifestDir.resolve("reset/application.properties"), originalContent);
        Files.writeString(
            manifestDir.resolve("workshop-manifest.yaml"),
            """
                moduleName: manifest-workshop
                title: Manifest Workshop
                description: Editor metadata loaded from a manifest
                editableFiles:
                  - name: application.properties
                    path: src/main/resources/application.properties
                    resetContentLocation: reset/application.properties
                """
        );

        WorkshopConfig workshopConfig = new WorkshopManifestLoader(
            new MockEnvironment().withProperty(
                "workshop.manifest.location",
                manifestDir.resolve("workshop-manifest.yaml").toString()
            ),
            new DefaultResourceLoader()
        ).loadWorkshopConfig().orElseThrow();

        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setSourcePath(configuredSourceDir.toString());
        EditorController controller = new EditorController(workshopConfig, runtimeProperties);

        assertThat(controller.getEditableFiles()).containsEntry("workshopTitle", "Manifest Workshop");

        controller.saveFile(fileName, Map.of("content", "spring.session.store-type=redis\n"));
        assertThat(Files.readString(configuredFile)).isEqualTo("spring.session.store-type=redis\n");

        controller.restoreFiles();
        assertThat(Files.readString(configuredFile)).isEqualTo(originalContent);
    }

    @Test
    void collectDiagnosticsKeepsStableResponseShapeForRemoteSessionPath() {
        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setSessionId("session-789");

        WorkshopConfig workshopConfig = new TestWorkshopConfig(
            fallbackSourceDir.toString(),
            Map.of("application.properties", "src/main/resources/application.properties"),
            Map.of()
        );

        EditorDiagnosticsService diagnosticsService = new EditorDiagnosticsService(
            workshopConfig,
            new SessionRuntimeResolver(runtimeProperties, workshopConfig),
            (command, workingDirectory, environment, timeout) -> new EditorDiagnosticsService.CommandResult(0, ""),
            new SessionRuntimeDiagnosticsClient() {
                @Override
                EditorDiagnosticsService.DiagnosticsResponse collectDiagnostics(
                    java.net.URI endpoint,
                    WorkshopConfig config,
                    Map<String, String> overrides
                ) {
                    assertThat(endpoint.toString()).isEqualTo("http://localhost:9000/internal/execution-plane/sessions/session-789/runtime/diagnostics");
                    assertThat(overrides).containsEntry("application.properties", "spring.session.store-type=redis\n");
                    return new EditorDiagnosticsService.DiagnosticsResponse(
                        java.util.List.of(
                            new EditorDiagnosticsService.EditorDiagnostic(
                                "diag-1",
                                "application.properties",
                                4,
                                "error",
                                "remote validation error"
                            )
                        ),
                        null
                    );
                }
            },
            new SessionRuntimeDiagnosticsEndpointResolver()
        );

        EditorController controller = new EditorController(
            workshopConfig,
            new SessionRuntimeResolver(runtimeProperties, workshopConfig),
            diagnosticsService
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/editor/diagnostics");
        request.setServerName("localhost");
        request.setServerPort(18081);
        request.addHeader("Origin", "http://localhost:9000");

        Map<String, Object> response = controller.collectDiagnostics(
            Map.of(
                "overrides",
                java.util.List.of(
                    Map.of(
                        "fileName", "application.properties",
                        "content", "spring.session.store-type=redis\n"
                    )
                )
            ),
            request
        );

        assertThat(response).containsOnlyKeys("diagnostics");
        assertThat(response.get("diagnostics")).isInstanceOf(java.util.List.class);
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
