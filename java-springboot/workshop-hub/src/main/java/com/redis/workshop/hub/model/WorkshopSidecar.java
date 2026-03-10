package com.redis.workshop.hub.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class WorkshopSidecar {
    private String serviceName;
    private String image;
    private Integer port;
    private Map<String, String> env;
    private List<String> dependsOn;
    private List<String> command;
    private WorkshopHealthcheck healthcheck;

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public Map<String, String> getEnv() {
        if (env == null || env.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(env));
    }

    public void setEnv(Map<String, String> env) {
        this.env = env;
    }

    public List<String> getDependsOn() {
        if (dependsOn == null || dependsOn.isEmpty()) {
            return List.of();
        }
        return List.copyOf(dependsOn);
    }

    public void setDependsOn(List<String> dependsOn) {
        this.dependsOn = dependsOn;
    }

    public List<String> getCommand() {
        if (command == null || command.isEmpty()) {
            return List.of();
        }
        return List.copyOf(command);
    }

    public void setCommand(List<String> command) {
        this.command = command;
    }

    public WorkshopHealthcheck getHealthcheck() {
        return healthcheck;
    }

    public void setHealthcheck(WorkshopHealthcheck healthcheck) {
        this.healthcheck = healthcheck;
    }
}
