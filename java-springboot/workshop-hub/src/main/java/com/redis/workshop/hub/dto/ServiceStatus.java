package com.redis.workshop.hub.dto;

public class ServiceStatus {
    private String name;
    private String type; // "infrastructure" or "workshop"
    private String status; // "running", "stopped", "starting", "stopping", "error"
    private String port;
    private String url;
    private String message;
    private String deploymentStage; // "stopping", "building", "starting", "initializing", "ready"

    public ServiceStatus() {
    }

    public ServiceStatus(String name, String type, String status, String port, String url) {
        this.name = name;
        this.type = type;
        this.status = status;
        this.port = port;
        this.url = url;
    }

    // Getters and setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDeploymentStage() {
        return deploymentStage;
    }

    public void setDeploymentStage(String deploymentStage) {
        this.deploymentStage = deploymentStage;
    }
}

