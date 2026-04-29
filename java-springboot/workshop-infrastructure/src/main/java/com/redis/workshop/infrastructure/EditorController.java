package com.redis.workshop.infrastructure;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Shared Editor Controller for all workshops.
 * 
 * This controller uses the WorkshopConfig interface to determine which files
 * can be edited and how to restore them. Each workshop just needs to provide
 * its own WorkshopConfig implementation.
 */
@RestController
@RequestMapping("/api/editor")
@ConditionalOnBean(WorkshopConfig.class)
@ConditionalOnExpression(
    "T(org.springframework.util.StringUtils).hasText('${workshop.frontend.session-backend-url:}')"
        + " || T(org.springframework.util.StringUtils).hasText('${workshop.frontend.backend-url:}')"
        + " || T(org.springframework.util.StringUtils).hasText('${workshop.backend.url:}')"
        + " || T(org.springframework.util.StringUtils).hasText('${WORKSHOP_SESSION_BACKEND_URL:}')"
        + " || T(org.springframework.util.StringUtils).hasText('${WORKSHOP_BACKEND_URL:}')"
)
public class EditorController {

      private final WorkshopConfig workshopConfig;
      private final SessionRuntimeResolver runtimeResolver;
      private final EditorDiagnosticsService diagnosticsService;

      @Autowired
      public EditorController(WorkshopConfig workshopConfig, FrontendRuntimeProperties runtimeProperties) {
          this(
              workshopConfig,
              new SessionRuntimeResolver(runtimeProperties, workshopConfig),
              new EditorDiagnosticsService(workshopConfig, runtimeProperties)
          );
      }

      EditorController(
          WorkshopConfig workshopConfig,
          SessionRuntimeResolver runtimeResolver,
          EditorDiagnosticsService diagnosticsService
      ) {
          this.workshopConfig = workshopConfig;
          this.runtimeResolver = runtimeResolver;
          this.diagnosticsService = diagnosticsService;
      }

    /**
     * Get list of editable files for this workshop
     */
    @GetMapping("/files")
    public Map<String, Object> getEditableFiles() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> files = workshopConfig.getEditableFiles().entrySet().stream()
            .map(entry -> {
                Map<String, String> fileMetadata = new HashMap<>();
                fileMetadata.put("name", entry.getKey());
                fileMetadata.put("path", entry.getValue());
                fileMetadata.put("language", workshopConfig.getLanguage(entry.getKey()));
                runtimeResolver.resolvePathWithinModule(entry.getValue())
                    .map(Path::toString)
                    .ifPresent(workspacePath -> fileMetadata.put("workspacePath", workspacePath));
                runtimeResolver.resolveCodeEditorPathWithinModule(entry.getValue())
                    .map(Path::toString)
                    .ifPresent(workspacePath -> fileMetadata.put("codeEditorWorkspacePath", workspacePath));
                return fileMetadata;
            })
            .collect(Collectors.toList());
        response.put("files", files);
        response.put("workshopTitle", workshopConfig.getWorkshopTitle());
        response.put("workshopDescription", workshopConfig.getWorkshopDescription());
        runtimeResolver.resolveWorkspaceRoot()
            .map(Path::toString)
            .ifPresent(workspaceRoot -> response.put("workspaceRoot", workspaceRoot));
        runtimeResolver.resolveCodeEditorWorkspaceRoot()
            .map(Path::toString)
            .ifPresent(workspaceRoot -> response.put("codeEditorWorkspaceRoot", workspaceRoot));
        return response;
    }

    /**
     * Load a file for editing
     */
    @GetMapping("/file/{fileName}")
    public Map<String, Object> loadFile(@PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();

        try {
            Path filePath = getFilePath(fileName);

            if (filePath == null) {
                response.put("error", "Invalid file name: " + fileName);
                return response;
            }

            if (!Files.exists(filePath)) {
                response.put("error", "File not found: " + fileName);
                return response;
            }

            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            response.put("content", content);
            response.put("language", workshopConfig.getLanguage(fileName));
            response.put("fileName", fileName);

        } catch (IOException e) {
            response.put("error", "Failed to read file: " + e.getMessage());
        }

        return response;
    }

    /**
     * Save edited file content
     */
    @PostMapping("/file/{fileName}")
    public Map<String, Object> saveFile(@PathVariable String fileName, @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();

        try {
            String content = payload.get("content");
            if (content == null) {
                response.put("error", "No content provided");
                return response;
            }

            Path filePath = getFilePath(fileName);
            if (filePath == null) {
                response.put("error", "Invalid file name: " + fileName);
                return response;
            }

            // Ensure parent directories exist
            Files.createDirectories(filePath.getParent());
            Files.writeString(filePath, content, StandardCharsets.UTF_8);

            response.put("success", true);
            response.put("message", "File saved successfully");

        } catch (IOException e) {
            response.put("error", "Failed to save file: " + e.getMessage());
        }

        return response;
    }

    /**
     * Restore all editable files to their original state
     */
    @PostMapping("/restore")
    public Map<String, Object> restoreFiles() {
        Map<String, Object> response = new HashMap<>();

        try {
            diagnosticsService.restoreFiles();
            response.put("success", true);
            response.put("message", "All files restored to original state");
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to restore files: " + e.getMessage());
        }

        return response;
    }

    /**
     * Run build diagnostics for the current workshop and map them back to editable files.
     */
    @PostMapping("/diagnostics")
    public Map<String, Object> collectDiagnostics(
        @RequestBody(required = false) Map<String, Object> payload,
        HttpServletRequest request
    ) {
        Map<String, String> overrides = new HashMap<>();
        Object rawOverrides = payload == null ? null : payload.get("overrides");

        if (rawOverrides instanceof List<?> overrideList) {
            for (Object entry : overrideList) {
                if (entry instanceof Map<?, ?> override) {
                    Object fileName = override.get("fileName");
                    Object content = override.get("content");
                    if (fileName instanceof String name && content instanceof String value) {
                        overrides.put(name, value);
                    }
                }
            }
        }

        EditorDiagnosticsService.DiagnosticsResponse diagnostics = diagnosticsService.collectDiagnostics(overrides, request);
        Map<String, Object> response = new HashMap<>();
        response.put("diagnostics", diagnostics.diagnostics());
        if (diagnostics.error() != null) {
            response.put("error", diagnostics.error());
        }
        return response;
    }

      private Path getFilePath(String fileName) {
          String relativePath = workshopConfig.getEditableFiles().get(fileName);
          if (relativePath == null) {
              return null;
          }
          return runtimeResolver.resolvePathWithinModule(relativePath).orElse(null);
      }
  }
