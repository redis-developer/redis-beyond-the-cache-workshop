package com.redis.workshop.infrastructure;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic Workshop Infrastructure Controller
 *
 * This controller provides the in-browser code editor functionality for ANY workshop.
 * It is completely reusable and configuration-driven through the WorkshopConfig interface.
 *
 * Key Features:
 * - Configuration-driven (no hardcoded file paths or content)
 * - Dependency injection for workshop-specific configuration
 * - Reusable across multiple Redis workshops
 * - Secure file access (only configured files can be edited)
 *
 * To create a new workshop:
 * 1. Implement WorkshopConfig interface with your files and content
 * 2. Annotate with @Component
 * 3. This controller automatically picks it up via dependency injection
 *
 * Students learning about Redis should focus on the workshop teaching content,
 * not this infrastructure code.
 */
@Controller
@RequestMapping("/editor")
public class EditorController {

    private final WorkshopConfig workshopConfig;

    /**
     * Constructor injection of workshop configuration.
     * Spring automatically injects the appropriate WorkshopConfig implementation.
     */
    public EditorController(WorkshopConfig workshopConfig) {
        this.workshopConfig = workshopConfig;
    }

    @GetMapping
    public String editor(Model model) {
        model.addAttribute("files", workshopConfig.getEditableFiles().keySet());
        model.addAttribute("workshopTitle", workshopConfig.getWorkshopTitle());
        model.addAttribute("workshopDescription", workshopConfig.getWorkshopDescription());
        return "editor";
    }

    @GetMapping("/file/{fileName}")
    @ResponseBody
    public Map<String, Object> getFile(@PathVariable String fileName) {
        Map<String, Object> response = new HashMap<>();

        if (!workshopConfig.getEditableFiles().containsKey(fileName)) {
            response.put("error", "File not allowed");
            return response;
        }

        try {
            String basePath = workshopConfig.getBasePath();
            Path filePath = Paths.get(basePath, workshopConfig.getEditableFiles().get(fileName));

            // Debug logging
            System.out.println("Base path: " + basePath);
            System.out.println("File path: " + filePath);
            System.out.println("File exists: " + Files.exists(filePath));

            String content = Files.readString(filePath);

            response.put("success", true);
            response.put("fileName", fileName);
            response.put("content", content);
            response.put("language", workshopConfig.getLanguage(fileName));
        } catch (IOException e) {
            response.put("error", "Failed to read file: " + e.getMessage());
        }

        return response;
    }

    @PostMapping("/file/{fileName}")
    @ResponseBody
    public Map<String, Object> saveFile(@PathVariable String fileName, @RequestBody Map<String, String> payload) {
        Map<String, Object> response = new HashMap<>();

        if (!workshopConfig.getEditableFiles().containsKey(fileName)) {
            response.put("error", "File not allowed");
            return response;
        }

        try {
            String basePath = workshopConfig.getBasePath();
            Path filePath = Paths.get(basePath, workshopConfig.getEditableFiles().get(fileName));
            String content = payload.get("content");

            if (content == null) {
                response.put("error", "No content provided");
                return response;
            }

            Files.writeString(filePath, content);

            response.put("success", true);
            response.put("message", "File saved successfully");
        } catch (IOException e) {
            response.put("error", "Failed to save file: " + e.getMessage());
        }

        return response;
    }

    @PostMapping("/restore")
    @ResponseBody
    public Map<String, Object> restoreFiles() {
        Map<String, Object> response = new HashMap<>();

        try {
            String basePath = workshopConfig.getBasePath();
            int filesRestored = 0;

            // Restore each file to its original content
            for (Map.Entry<String, String> entry : workshopConfig.getEditableFiles().entrySet()) {
                String fileName = entry.getKey();
                String filePath = entry.getValue();
                String originalContent = workshopConfig.getOriginalContent(fileName);

                if (originalContent != null) {
                    Path fullPath = Paths.get(basePath, filePath);
                    Files.writeString(fullPath, originalContent);
                    filesRestored++;
                }
            }

            response.put("success", true);
            response.put("message", "Lab reset successfully! " + filesRestored + " files restored to original state.");
            response.put("filesRestored", filesRestored);
        } catch (IOException e) {
            response.put("error", "Failed to restore files: " + e.getMessage());
        }

        return response;
    }
}

