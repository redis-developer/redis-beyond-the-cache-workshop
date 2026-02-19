package com.redis.workshop.infrastructure;

import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
public class EditorController {

    private final WorkshopConfig workshopConfig;

    public EditorController(WorkshopConfig workshopConfig) {
        this.workshopConfig = workshopConfig;
    }

    /**
     * Get list of editable files for this workshop
     */
    @GetMapping("/files")
    public Map<String, Object> getEditableFiles() {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, String>> files = workshopConfig.getEditableFiles().entrySet().stream()
            .map(entry -> Map.of(
                "name", entry.getKey(),
                "path", entry.getValue(),
                "language", workshopConfig.getLanguage(entry.getKey())
            ))
            .collect(Collectors.toList());
        response.put("files", files);
        response.put("workshopTitle", workshopConfig.getWorkshopTitle());
        response.put("workshopDescription", workshopConfig.getWorkshopDescription());
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
            for (String fileName : workshopConfig.getEditableFiles().keySet()) {
                String originalContent = workshopConfig.getOriginalContent(fileName);
                if (originalContent != null) {
                    Path filePath = getFilePath(fileName);
                    if (filePath != null) {
                        Files.createDirectories(filePath.getParent());
                        Files.writeString(filePath, originalContent, StandardCharsets.UTF_8);
                    }
                }
            }

            response.put("success", true);
            response.put("message", "All files restored to original state");
        } catch (Exception e) {
            response.put("success", false);
            response.put("error", "Failed to restore files: " + e.getMessage());
        }

        return response;
    }

    private Path getFilePath(String fileName) {
        String relativePath = workshopConfig.getEditableFiles().get(fileName);
        if (relativePath == null) {
            return null;
        }
        return Paths.get(workshopConfig.getBasePath()).resolve(relativePath);
    }
}

