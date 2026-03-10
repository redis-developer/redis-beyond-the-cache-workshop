package com.redis.workshop.hub.model;

import java.util.List;
import java.util.Map;

public class Workshop {
    private static final List<String> DEFAULT_INFRASTRUCTURE_DEPENDENCIES = List.of("redis");
    private static final String DEFAULT_REDIS_FLAVOR = "standard";

    private String id;
    private String title;
    private String description;
    private String difficulty;
    private int estimatedMinutes;
    private String url;
    private String serviceName;
    private int port;
    private String dockerfile;
    private String frontendServiceName;
    private Integer frontendPort;
    private String frontendDockerfile;
    private String backendServiceName;
    private Integer backendPort;
    private String backendDockerfile;
    private List<String> infrastructureDependencies;
    private WorkshopEnvOverrides envOverrides;
    private List<WorkshopSidecar> sidecars;
    private String redisFlavor;
    private Boolean frontendPrebuild;
    private String[] topics;

    public Workshop() {
    }

    public Workshop(String id, String title, String description, String difficulty, 
                    int estimatedMinutes, String url, String[] topics) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.difficulty = difficulty;
        this.estimatedMinutes = estimatedMinutes;
        this.url = url;
        this.topics = topics;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(int estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getDockerfile() {
        return dockerfile;
    }

    public void setDockerfile(String dockerfile) {
        this.dockerfile = dockerfile;
    }

    public String getFrontendServiceName() {
        return frontendServiceName;
    }

    public void setFrontendServiceName(String frontendServiceName) {
        this.frontendServiceName = frontendServiceName;
    }

    public Integer getFrontendPort() {
        return frontendPort;
    }

    public void setFrontendPort(Integer frontendPort) {
        this.frontendPort = frontendPort;
    }

    public String getFrontendDockerfile() {
        return frontendDockerfile;
    }

    public void setFrontendDockerfile(String frontendDockerfile) {
        this.frontendDockerfile = frontendDockerfile;
    }

    public String getBackendServiceName() {
        return backendServiceName;
    }

    public void setBackendServiceName(String backendServiceName) {
        this.backendServiceName = backendServiceName;
    }

    public Integer getBackendPort() {
        return backendPort;
    }

    public void setBackendPort(Integer backendPort) {
        this.backendPort = backendPort;
    }

    public String getBackendDockerfile() {
        return backendDockerfile;
    }

    public void setBackendDockerfile(String backendDockerfile) {
        this.backendDockerfile = backendDockerfile;
    }

    public List<String> getInfrastructureDependencies() {
        if (infrastructureDependencies == null || infrastructureDependencies.isEmpty()) {
            return DEFAULT_INFRASTRUCTURE_DEPENDENCIES;
        }
        return List.copyOf(infrastructureDependencies);
    }

    public void setInfrastructureDependencies(List<String> infrastructureDependencies) {
        this.infrastructureDependencies = infrastructureDependencies;
    }

    public WorkshopEnvOverrides getEnvOverrides() {
        return envOverrides;
    }

    public void setEnvOverrides(WorkshopEnvOverrides envOverrides) {
        this.envOverrides = envOverrides;
    }

    public Map<String, String> getCommonEnvOverrides() {
        if (envOverrides == null) {
            return Map.of();
        }
        return envOverrides.getCommon();
    }

    public Map<String, String> getFrontendEnvOverrides() {
        if (envOverrides == null) {
            return Map.of();
        }
        return envOverrides.getFrontend();
    }

    public Map<String, String> getBackendEnvOverrides() {
        if (envOverrides == null) {
            return Map.of();
        }
        return envOverrides.getBackend();
    }

    public List<WorkshopSidecar> getSidecars() {
        if (sidecars == null || sidecars.isEmpty()) {
            return List.of();
        }
        return List.copyOf(sidecars);
    }

    public void setSidecars(List<WorkshopSidecar> sidecars) {
        this.sidecars = sidecars;
    }

    public String getRedisFlavor() {
        return hasText(redisFlavor) ? redisFlavor : DEFAULT_REDIS_FLAVOR;
    }

    public void setRedisFlavor(String redisFlavor) {
        this.redisFlavor = redisFlavor;
    }

    public boolean usesRedisStack() {
        return "stack".equalsIgnoreCase(getRedisFlavor());
    }

    public Boolean getFrontendPrebuild() {
        return frontendPrebuild != null ? frontendPrebuild : Boolean.TRUE;
    }

    public void setFrontendPrebuild(Boolean frontendPrebuild) {
        this.frontendPrebuild = frontendPrebuild;
    }

    public boolean isFrontendPrebuildEnabled() {
        return Boolean.TRUE.equals(getFrontendPrebuild());
    }

    public String[] getTopics() {
        return topics;
    }

    public void setTopics(String[] topics) {
        this.topics = topics;
    }

    /**
     * Transitional resolver for dual-service migration.
     * Falls back to the legacy single-service fields to preserve behavior.
     */
    public String getEffectiveFrontendServiceName() {
        return hasText(frontendServiceName) ? frontendServiceName : serviceName;
    }

    /**
     * Transitional resolver for dual-service migration.
     * Falls back to the legacy single-service fields to preserve behavior.
     */
    public int getEffectiveFrontendPort() {
        if (frontendPort != null && frontendPort > 0) {
            return frontendPort;
        }
        return port;
    }

    /**
     * Transitional resolver for dual-service migration.
     * Falls back to the legacy single-service fields to preserve behavior.
     */
    public String getEffectiveFrontendDockerfile() {
        return hasText(frontendDockerfile) ? frontendDockerfile : dockerfile;
    }

    /**
     * Transitional resolver for dual-service migration.
     * Falls back to the legacy single-service fields to preserve behavior.
     */
    public String getEffectiveBackendServiceName() {
        if (hasText(backendServiceName)) {
            return backendServiceName;
        }
        if (hasText(serviceName)) {
            return serviceName;
        }
        return id;
    }

    /**
     * Transitional resolver for dual-service migration.
     * Falls back to the legacy single-service fields to preserve behavior.
     */
    public int getEffectiveBackendPort() {
        if (backendPort != null && backendPort > 0) {
            return backendPort;
        }
        return port;
    }

    /**
     * Transitional resolver for dual-service migration.
     * Falls back to the legacy single-service fields to preserve behavior.
     */
    public String getEffectiveBackendDockerfile() {
        return hasText(backendDockerfile) ? backendDockerfile : dockerfile;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
