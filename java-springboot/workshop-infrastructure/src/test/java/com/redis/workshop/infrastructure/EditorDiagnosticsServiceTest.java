package com.redis.workshop.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EditorDiagnosticsServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsJavaCompilerDiagnosticsAndRestoresTemporaryOverrideWithinSessionWorkspace() throws IOException {
        Path workspaceModuleRoot = Files.createDirectories(tempDir.resolve("workspace/1_session_management"));
        Path fallbackModuleRoot = Files.createDirectories(tempDir.resolve("fallback/1_session_management"));
        Path sourceFile = workspaceModuleRoot.resolve("src/main/java/com/redis/workshop/session/config/SecurityConfig.java");
        Files.createDirectories(sourceFile.getParent());
        Files.writeString(sourceFile, "class SecurityConfig {}\n");
        Path fallbackFile = fallbackModuleRoot.resolve("src/main/java/com/redis/workshop/session/config/SecurityConfig.java");
        Files.createDirectories(fallbackFile.getParent());
        Files.writeString(fallbackFile, "class FallbackSecurityConfig {}\n");

        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setWorkspacePath(workspaceModuleRoot.toString());
        runtimeProperties.setSourcePath(fallbackModuleRoot.toString());

        WorkshopConfig workshopConfig = new TestWorkshopConfig(
            fallbackModuleRoot,
            "1_session_management",
            Map.of("SecurityConfig.java", "src/main/java/com/redis/workshop/session/config/SecurityConfig.java")
        );

        EditorDiagnosticsService service = new EditorDiagnosticsService(
            workshopConfig,
            runtimeProperties,
            (command, workingDirectory, environment, timeout) -> {
                assertThat(command).contains(":" + workshopConfig.getModuleName() + ":compileJava");
                assertThat(workingDirectory).isEqualTo(workspaceModuleRoot.getParent());
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
        assertThat(Files.readString(fallbackFile)).isEqualTo("class FallbackSecurityConfig {}\n");
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

    @Test
    void restoreFilesRebuildsOnlyTheResolvedWorkspaceFromBaselineContent() throws IOException {
        Path workspaceModuleRoot = Files.createDirectories(tempDir.resolve("workspace/1_session_management"));
        Path fallbackModuleRoot = Files.createDirectories(tempDir.resolve("fallback/1_session_management"));
        Path workspaceFile = workspaceModuleRoot.resolve("src/main/java/com/redis/workshop/session/config/SecurityConfig.java");
        Files.createDirectories(workspaceFile.getParent());
        Files.writeString(workspaceFile, "class BrokenSecurityConfig {}\n");
        Path fallbackFile = fallbackModuleRoot.resolve("src/main/java/com/redis/workshop/session/config/SecurityConfig.java");
        Files.createDirectories(fallbackFile.getParent());
        Files.writeString(fallbackFile, "class FallbackSecurityConfig {}\n");

        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setWorkspacePath(workspaceModuleRoot.toString());
        runtimeProperties.setSourcePath(fallbackModuleRoot.toString());

        WorkshopConfig workshopConfig = new RestorableTestWorkshopConfig(
            fallbackModuleRoot,
            "1_session_management",
            Map.of("SecurityConfig.java", "src/main/java/com/redis/workshop/session/config/SecurityConfig.java"),
            Map.of("SecurityConfig.java", "class SecurityConfig {}\n")
        );

        EditorDiagnosticsService service = new EditorDiagnosticsService(
            workshopConfig,
            runtimeProperties,
            (command, workingDirectory, environment, timeout) -> new EditorDiagnosticsService.CommandResult(0, "")
        );

        service.restoreFiles();

        assertThat(Files.readString(workspaceFile)).isEqualTo("class SecurityConfig {}\n");
        assertThat(Files.readString(fallbackFile)).isEqualTo("class FallbackSecurityConfig {}\n");
    }

    @Test
    void usesRemoteDiagnosticsWhenSessionRequestResolvesHubOrigin() throws Exception {
        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setSessionId("session-123");
        runtimeProperties.setSessionRuntimeBaseUrl("http://localhost:9000");

        WorkshopConfig workshopConfig = new TestWorkshopConfig(
            tempDir,
            "1_session_management",
            Map.of("SecurityConfig.java", "src/main/java/com/redis/workshop/session/config/SecurityConfig.java")
        );

        AtomicBoolean localExecutorCalled = new AtomicBoolean(false);
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> remoteResponse = mock(HttpResponse.class);
        when(remoteResponse.statusCode()).thenReturn(200);
        when(remoteResponse.body()).thenReturn("""
            {
              "diagnostics": [
                {
                  "id": "remote-1",
                  "file": "SecurityConfig.java",
                  "line": 9,
                  "severity": "error",
                  "message": "remote compiler failure"
                }
              ]
            }
            """);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(remoteResponse);

        EditorDiagnosticsService service = new EditorDiagnosticsService(
            workshopConfig,
            new SessionRuntimeResolver(runtimeProperties, workshopConfig),
            (command, workingDirectory, environment, timeout) -> {
                localExecutorCalled.set(true);
                return new EditorDiagnosticsService.CommandResult(0, "");
            },
            new SessionRuntimeDiagnosticsClient(httpClient, new ObjectMapper(), "shared-secret"),
            new SessionRuntimeDiagnosticsEndpointResolver()
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/editor/diagnostics");
        request.setServerName("localhost");
        request.setServerPort(18081);
        request.addHeader("Origin", "http://localhost:9000");

        EditorDiagnosticsService.DiagnosticsResponse response = service.collectDiagnostics(
            Map.of("SecurityConfig.java", "class SecurityConfig {\n"),
            request
        );

        assertThat(localExecutorCalled).isFalse();
        assertThat(response.error()).isNull();
        assertThat(response.diagnostics()).hasSize(1);
        assertThat(response.diagnostics().getFirst().file()).isEqualTo("SecurityConfig.java");
        assertThat(response.diagnostics().getFirst().line()).isEqualTo(9);
        assertThat(response.diagnostics().getFirst().message()).isEqualTo("remote compiler failure");
        verify(httpClient).send(
            argThat(httpRequest ->
                httpRequest.uri().equals(URI.create("http://localhost:9000/internal/execution-plane/sessions/session-123/runtime/diagnostics"))
                    && "shared-secret".equals(httpRequest.headers().firstValue("X-Execution-Plane-Key").orElse(null))
            ),
            any(HttpResponse.BodyHandler.class)
        );
    }

    @Test
    void fallsBackToLocalDiagnosticsWhenSessionIdIsNotResolved() throws IOException {
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

        HttpClient httpClient = mock(HttpClient.class);
        EditorDiagnosticsService service = new EditorDiagnosticsService(
            workshopConfig,
            new SessionRuntimeResolver(runtimeProperties, workshopConfig),
            (command, workingDirectory, environment, timeout) -> new EditorDiagnosticsService.CommandResult(
                1,
                sourceFile + ":6: error: local fallback still active\n"
            ),
            new SessionRuntimeDiagnosticsClient(httpClient, new ObjectMapper(), ""),
            new SessionRuntimeDiagnosticsEndpointResolver()
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/editor/diagnostics");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.addHeader("Origin", "http://localhost:9000");

        EditorDiagnosticsService.DiagnosticsResponse response = service.collectDiagnostics(
            Map.of("SecurityConfig.java", "class SecurityConfig {\n"),
            request
        );

        verifyNoInteractions(httpClient);
        assertThat(response.error()).isNull();
        assertThat(response.diagnostics()).hasSize(1);
        assertThat(response.diagnostics().getFirst().file()).isEqualTo("SecurityConfig.java");
        assertThat(response.diagnostics().getFirst().message()).isEqualTo("local fallback still active");
    }

    @Test
    void returnsSafeErrorWhenRemoteDiagnosticsEndpointFails() throws Exception {
        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setSessionId("session-456");
        runtimeProperties.setSessionRuntimeBaseUrl("http://localhost:9000");

        WorkshopConfig workshopConfig = new TestWorkshopConfig(
            tempDir,
            "1_session_management",
            Map.of("SecurityConfig.java", "src/main/java/com/redis/workshop/session/config/SecurityConfig.java")
        );

        AtomicBoolean localExecutorCalled = new AtomicBoolean(false);
        HttpClient httpClient = mock(HttpClient.class);
        HttpResponse<String> remoteResponse = mock(HttpResponse.class);
        when(remoteResponse.statusCode()).thenReturn(502);
        when(remoteResponse.body()).thenReturn("{\"error\":\"upstream failed\"}");
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(remoteResponse);

        EditorDiagnosticsService service = new EditorDiagnosticsService(
            workshopConfig,
            new SessionRuntimeResolver(runtimeProperties, workshopConfig),
            (command, workingDirectory, environment, timeout) -> {
                localExecutorCalled.set(true);
                return new EditorDiagnosticsService.CommandResult(0, "");
            },
            new SessionRuntimeDiagnosticsClient(httpClient, new ObjectMapper(), ""),
            new SessionRuntimeDiagnosticsEndpointResolver()
        );

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/editor/diagnostics");
        request.setServerName("localhost");
        request.setServerPort(18081);
        request.addHeader("Origin", "http://localhost:9000");

        EditorDiagnosticsService.DiagnosticsResponse response = service.collectDiagnostics(Map.of(), request);

        assertThat(localExecutorCalled).isFalse();
        assertThat(response.diagnostics()).isEmpty();
        assertThat(response.error()).isEqualTo("Unable to collect compiler diagnostics right now.");
    }

    @Test
    void restoreFilesUsesRemoteRuntimeEndpointWhenConfigured() throws Exception {
        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setSessionId("session-restore");
        runtimeProperties.setSessionRuntimeBaseUrl("http://localhost:9000");

        WorkshopConfig workshopConfig = new RestorableTestWorkshopConfig(
            tempDir,
            "1_session_management",
            Map.of("SecurityConfig.java", "src/main/java/com/redis/workshop/session/config/SecurityConfig.java"),
            Map.of("SecurityConfig.java", "class SecurityConfig {}\n")
        );

        AtomicBoolean remoteRestoreCalled = new AtomicBoolean(false);
        EditorDiagnosticsService service = new EditorDiagnosticsService(
            workshopConfig,
            new SessionRuntimeResolver(runtimeProperties, workshopConfig),
            (command, workingDirectory, environment, timeout) -> new EditorDiagnosticsService.CommandResult(0, ""),
            new SessionRuntimeDiagnosticsClient() {
                @Override
                void restoreFiles(URI endpoint, WorkshopConfig config, Map<String, String> baselineSnapshot) {
                    remoteRestoreCalled.set(true);
                    assertThat(endpoint).isEqualTo(URI.create("http://localhost:9000/internal/execution-plane/sessions/session-restore/runtime/restore"));
                    assertThat(baselineSnapshot).containsEntry("SecurityConfig.java", "class SecurityConfig {}\n");
                }
            },
            new SessionRuntimeDiagnosticsEndpointResolver()
        );

        service.restoreFiles();

        assertThat(remoteRestoreCalled).isTrue();
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

    private record RestorableTestWorkshopConfig(
        Path basePath,
        String moduleName,
        Map<String, String> editableFiles,
        Map<String, String> originalContents
    ) implements WorkshopConfig {

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
            return basePath.toString();
        }

        @Override
        public String getModuleName() {
            return moduleName;
        }
    }
}
