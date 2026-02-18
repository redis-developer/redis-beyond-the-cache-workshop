package com.redis.workshop.infrastructure;

import java.util.Map;

/**
 * Workshop Configuration Interface
 * 
 * Implement this interface to define a new workshop's editable files
 * and their original content for restoration.
 * 
 * This allows the workshop infrastructure to be completely reusable
 * across different labs without modification.
 */
public interface WorkshopConfig {
    
    /**
     * Get the map of editable files for this workshop.
     * 
     * @return Map where:
     *         - Key: Display name (e.g., "build.gradle.kts")
     *         - Value: Relative path from module root (e.g., "build.gradle.kts")
     */
    Map<String, String> getEditableFiles();
    
    /**
     * Get the original content for a specific file.
     * Used for the "Restart Lab" functionality.
     * 
     * @param fileName The display name of the file
     * @return The original content of the file, or null if not found
     */
    String getOriginalContent(String fileName);
    
    /**
     * Get the base path for this workshop module.
     * Override this if your module has a different structure.
     * 
     * @return The base path where files are located
     */
    default String getBasePath() {
        String userDir = System.getProperty("user.dir");
        
        // If running from the root project, append the module path
        if (userDir.endsWith("redis-beyond-the-cache-workshop")) {
            return userDir + "/java-springboot/" + getModuleName();
        }
        
        // If running from java-springboot directory
        if (userDir.endsWith("java-springboot")) {
            return userDir + "/" + getModuleName();
        }
        
        // Otherwise assume we're already in the module directory
        return userDir;
    }
    
    /**
     * Get the module name (e.g., "1_session_management").
     * 
     * @return The module directory name
     */
    String getModuleName();
    
    /**
     * Get the language/syntax highlighting mode for a file.
     * Override this to support additional file types.
     * 
     * @param fileName The display name of the file
     * @return Monaco Editor language identifier (e.g., "java", "kotlin", "properties")
     */
    default String getLanguage(String fileName) {
        if (fileName.endsWith(".java")) {
            return "java";
        } else if (fileName.endsWith(".properties")) {
            return "properties";
        } else if (fileName.endsWith(".kts")) {
            return "kotlin";
        } else if (fileName.endsWith(".yaml") || fileName.endsWith(".yml")) {
            return "yaml";
        } else if (fileName.endsWith(".json")) {
            return "json";
        } else if (fileName.endsWith(".xml")) {
            return "xml";
        } else if (fileName.endsWith(".js")) {
            return "javascript";
        } else if (fileName.endsWith(".ts")) {
            return "typescript";
        } else if (fileName.endsWith(".py")) {
            return "python";
        } else if (fileName.endsWith(".go")) {
            return "go";
        }
        return "text";
    }
    
    /**
     * Get the workshop title for display in the editor.
     * 
     * @return The workshop title
     */
    default String getWorkshopTitle() {
        return "Redis Workshop";
    }
    
    /**
     * Get the workshop description for display in the editor.
     * 
     * @return The workshop description
     */
    default String getWorkshopDescription() {
        return "Interactive Redis workshop with in-browser code editing";
    }
}

