package com.redis.workshop.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EditorDiagnosticsServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsJavaCompilerDiagnosticsAndRestoresTemporaryOverride() throws IOException {
        Path moduleRoot = Files.createDirectories(tempDir.resolve("1_session_management"));
        Path sourceFile = moduleRoot.resolve("src/main/java/com/redis/workshop/session/config/SecurityConfig.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "class SecurityConfig {}\n");

        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setSourcePath(moduleRoot.toString());

        WorkshopConfig workshopConfig = new TestWorkshopConfig(
            moduleRoot,
            "1_session_management",
            Map.of("SecurityConfig.java", "src/main/java/com/redis/workshop/session/config/SecurityConfig.java")
        );

        EditorDiagnosticsService service = new EditorDiagnosticsService(
            workshopConfig,
            runtimeProperties,
            (command, workingDirectory, environment, timeout) -> {
                assertThat(command).contains(":" + workshopConfig.getModuleName() + ":compileJava");
                assertThat(workingDirectory).isEqualTo(moduleRoot.getParent());
                assertThat(Files.readString(sourceFile)).isEqualTo("class SecurityConfig {\n");
                return new EditorDiagnosticsService.CommandResult(
                    1,
                    sourceFile + ":7: error: reached end of file while parsing\n"
                );
            }
        );

        EditorDiagnosticsService.DiagnosticsResponse response = service.collectDiagnostics(
            Map.of("SecurityConfig.java", "class SecurityConfig {\n")
        );

        assertThat(response.error()).isNull();
        assertThat(response.diagnostics()).hasSize(1);
        EditorDiagnosticsService.EditorDiagnostic diagnostic = response.diagnostics().getFirst();
        assertThat(diagnostic.file()).isEqualTo("SecurityConfig.java");
        assertThat(diagnostic.line()).isEqualTo(7);
        assertThat(diagnostic.severity()).isEqualTo("error");
        assertThat(diagnostic.message()).isEqualTo("reached end of file while parsing");
        assertThat(Files.readString(sourceFile)).isEqualTo("class SecurityConfig {}\n");
    }

    @Test
    void parsesKotlinScriptDiagnosticsForGradleFiles() throws IOException {
        Path moduleRoot = Files.createDirectories(tempDir.resolve("1_session_management"));
        Path buildFile = moduleRoot.resolve("build.gradle.kts");
        Files.writeString(buildFile, "plugins {\n    java\n}\n");

        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setSourcePath(moduleRoot.toString());

        WorkshopConfig workshopConfig = new TestWorkshopConfig(
            moduleRoot,
            "1_session_management",
            Map.of("build.gradle.kts", "build.gradle.kts")
        );

        EditorDiagnosticsService service = new EditorDiagnosticsService(
            workshopConfig,
            runtimeProperties,
            (command, workingDirectory, environment, timeout) -> new EditorDiagnosticsService.CommandResult(
                1,
                "e: file:///" + buildFile + ":12:9 Unresolved reference: implementation\n"
            )
        );

        EditorDiagnosticsService.DiagnosticsResponse response = service.collectDiagnostics(
            Map.of("build.gradle.kts", "plugins {\n")
        );

        assertThat(response.error()).isNull();
        assertThat(response.diagnostics()).hasSize(1);
        assertThat(response.diagnostics().getFirst().file()).isEqualTo("build.gradle.kts");
        assertThat(response.diagnostics().getFirst().line()).isEqualTo(12);
        assertThat(response.diagnostics().getFirst().severity()).isEqualTo("error");
        assertThat(response.diagnostics().getFirst().message()).isEqualTo("Unresolved reference: implementation");
        assertThat(Files.readString(buildFile)).isEqualTo("plugins {\n    java\n}\n");
    }

    private record TestWorkshopConfig(
        Path basePath,
        String moduleName,
        Map<String, String> editableFiles
    ) implements WorkshopConfig {

        @Override
        public Map<String, String> getEditableFiles() {
            return editableFiles;
        }

        @Override
        public String getOriginalContent(String fileName) {
            return null;
        }

        @Override
        public String getBasePath() {
            return basePath.toString();
        }

        @Override
        public String getModuleName() {
            return moduleName;
        }
    }
}
