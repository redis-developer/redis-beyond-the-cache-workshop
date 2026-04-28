package com.redis.workshop.infrastructure;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class SessionRuntimeDiagnosticsClient {

    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(20);
    private static final String REMOTE_FAILURE_MESSAGE = "Unable to collect compiler diagnostics right now.";
    private static final String EXECUTION_PLANE_KEY_HEADER = "X-Execution-Plane-Key";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String executionPlaneKey;

    SessionRuntimeDiagnosticsClient() {
        this(
            HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build(),
            new ObjectMapper(),
            System.getenv("WORKSHOP_EXECUTION_SHARED_SECRET")
        );
    }

    SessionRuntimeDiagnosticsClient(HttpClient httpClient, ObjectMapper objectMapper, String executionPlaneKey) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
        this.executionPlaneKey = executionPlaneKey == null ? "" : executionPlaneKey.trim();
    }

    EditorDiagnosticsService.DiagnosticsResponse collectDiagnostics(
        URI endpoint,
        WorkshopConfig workshopConfig,
        Map<String, String> overrides
    ) {
        DiagnosticsRequest requestBody = new DiagnosticsRequest(
            workshopConfig.getEditableFiles().entrySet().stream()
                .map(entry -> new EditableFileRequest(entry.getKey(), toRuntimeRelativePath(workshopConfig, entry.getValue())))
                .toList(),
            buildOverrideFiles(workshopConfig, overrides),
            new CommandRequest(
                List.of(
                    "bash",
                    "./gradlew",
                    "-p",
                    ".",
                    "--console=plain",
                    ":" + workshopConfig.getModuleName() + ":compileJava"
                ),
                ".",
                Map.of(),
                REQUEST_TIMEOUT.toMillis()
            )
        );

        try {
            HttpResponse<String> response = sendJson(endpoint, requestBody);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return failedResponse();
            }

            DiagnosticsResponse responseBody = objectMapper.readValue(response.body(), DiagnosticsResponse.class);
            if (responseBody.diagnostics == null) {
                return failedResponse();
            }

            List<EditorDiagnosticsService.EditorDiagnostic> diagnostics = responseBody.diagnostics.stream()
                .map(diagnostic -> new EditorDiagnosticsService.EditorDiagnostic(
                    diagnostic.id,
                    diagnostic.file,
                    diagnostic.line,
                    diagnostic.severity,
                    diagnostic.message
                ))
                .toList();
            return new EditorDiagnosticsService.DiagnosticsResponse(diagnostics, responseBody.error);
        } catch (IOException exception) {
            return failedResponse();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return failedResponse();
        }
    }

    void restoreFiles(
        URI endpoint,
        WorkshopConfig workshopConfig,
        Map<String, String> baselineSnapshot
    ) throws IOException {
        RestoreRequest requestBody = new RestoreRequest(
            workshopConfig.getEditableFiles().entrySet().stream()
                .filter(entry -> baselineSnapshot.containsKey(entry.getKey()))
                .map(entry -> new RestoreFileSnapshot(
                    entry.getKey(),
                    toRuntimeRelativePath(workshopConfig, entry.getValue()),
                    baselineSnapshot.get(entry.getKey())
                ))
                .toList()
        );

        try {
            HttpResponse<String> response = sendJson(endpoint, requestBody);
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IOException("Remote restore failed with status " + response.statusCode());
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while restoring files", exception);
        }
    }

    private EditorDiagnosticsService.DiagnosticsResponse failedResponse() {
        return new EditorDiagnosticsService.DiagnosticsResponse(List.of(), REMOTE_FAILURE_MESSAGE);
    }

    private HttpResponse<String> sendJson(URI endpoint, Object payload) throws IOException, InterruptedException {
        String body = objectMapper.writeValueAsString(payload);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder(endpoint)
            .timeout(REQUEST_TIMEOUT)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8));
        if (StringUtils.hasText(executionPlaneKey)) {
            requestBuilder.header(EXECUTION_PLANE_KEY_HEADER, executionPlaneKey);
        }
        return httpClient.send(
            requestBuilder.build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );
    }

    private List<OverrideFileRequest> buildOverrideFiles(WorkshopConfig workshopConfig, Map<String, String> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return List.of();
        }

        List<OverrideFileRequest> overrideFiles = new ArrayList<>();
        for (Map.Entry<String, String> entry : workshopConfig.getEditableFiles().entrySet()) {
            String overrideContent = overrides.get(entry.getKey());
            if (overrideContent != null) {
                overrideFiles.add(new OverrideFileRequest(entry.getKey(), overrideContent));
            }
        }
        return List.copyOf(overrideFiles);
    }

    private String toRuntimeRelativePath(WorkshopConfig workshopConfig, String relativePath) {
        Path normalizedPath = Path.of(relativePath).normalize();
        if (normalizedPath.isAbsolute()) {
            throw new IllegalArgumentException("Workshop editable file path must be relative: " + relativePath);
        }
        return workshopConfig.getModuleName() + "/" + normalizedPath.toString().replace('\\', '/');
    }

    private record DiagnosticsRequest(
        List<EditableFileRequest> editableFiles,
        List<OverrideFileRequest> overrides,
        CommandRequest command
    ) {
    }

    private record EditableFileRequest(
        String fileName,
        String relativePath
    ) {
    }

    private record OverrideFileRequest(
        String fileName,
        String content
    ) {
    }

    private record CommandRequest(
        List<String> command,
        String workingDirectoryRelativePath,
        Map<String, String> environment,
        long timeoutMillis
    ) {
    }

    private record RestoreRequest(
        List<RestoreFileSnapshot> files
    ) {
    }

    private record RestoreFileSnapshot(
        String fileName,
        String relativePath,
        String content
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class DiagnosticsResponse {
        public List<DiagnosticResponse> diagnostics;
        public String error;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static final class DiagnosticResponse {
        public String id;
        public String file;
        public int line;
        public String severity;
        public String message;
    }
}
