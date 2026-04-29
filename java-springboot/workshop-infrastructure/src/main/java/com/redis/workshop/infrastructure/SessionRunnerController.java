package com.redis.workshop.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@ConditionalOnProperty(prefix = "workshop.session-runner", name = "enabled", havingValue = "true")
public class SessionRunnerController {

    private final LocalSessionRunnerManager runnerManager;
    private final EditorDiagnosticsService diagnosticsService;

    @Autowired
    public SessionRunnerController(
        LocalSessionRunnerManager runnerManager,
        WorkshopConfig workshopConfig,
        FrontendRuntimeProperties runtimeProperties
    ) {
        this.runnerManager = runnerManager;
        this.diagnosticsService = new EditorDiagnosticsService(workshopConfig, runtimeProperties);
    }

    SessionRunnerController(LocalSessionRunnerManager runnerManager, EditorDiagnosticsService diagnosticsService) {
        this.runnerManager = runnerManager;
        this.diagnosticsService = diagnosticsService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        LocalSessionRunnerManager.SessionRunnerStatus status = runnerManager.status();
        return Map.of(
            "status", status.enabled() ? "UP" : "DISABLED",
            "runnerState", status.state(),
            "childAlive", status.childAlive()
        );
    }

    @GetMapping("/internal/session-runner/status")
    public LocalSessionRunnerManager.SessionRunnerStatus status() {
        return runnerManager.status();
    }

    @PostMapping("/internal/session-runner/restart")
    public LocalSessionRunnerManager.SessionRunnerStatus restart(@RequestBody(required = false) Map<String, Object> body) {
        boolean rebuild = Boolean.TRUE.equals(body == null ? null : body.get("rebuild"));
        boolean async = Boolean.TRUE.equals(body == null ? null : body.get("async"));
        return async ? runnerManager.requestRestart(rebuild) : runnerManager.restart(rebuild);
    }

    @PostMapping("/internal/session-runner/diagnostics")
    public Map<String, Object> diagnostics(
        @RequestBody(required = false) Map<String, Object> body,
        HttpServletRequest request
    ) {
        EditorDiagnosticsService.DiagnosticsResponse diagnostics = diagnosticsService.collectDiagnostics(
            extractOverrides(body),
            request
        );
        if (diagnostics.error() == null) {
            return Map.of("diagnostics", diagnostics.diagnostics());
        }
        return Map.of(
            "diagnostics", diagnostics.diagnostics(),
            "error", diagnostics.error()
        );
    }

    @PostMapping("/internal/session-runner/restore")
    public Map<String, Object> restore() throws IOException {
        diagnosticsService.restoreFiles();
        return Map.of("restored", true);
    }

    private Map<String, String> extractOverrides(Map<String, Object> body) {
        if (body == null || !(body.get("overrides") instanceof List<?> rawOverrides)) {
            return Map.of();
        }

        java.util.LinkedHashMap<String, String> overrides = new java.util.LinkedHashMap<>();
        for (Object item : rawOverrides) {
            if (!(item instanceof Map<?, ?> override)) {
                continue;
            }
            Object fileName = override.get("fileName");
            Object content = override.get("content");
            if (fileName instanceof String fileNameValue && content instanceof String contentValue) {
                overrides.put(fileNameValue, contentValue);
            }
        }
        return overrides;
    }
}
