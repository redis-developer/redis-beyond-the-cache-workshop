package com.redis.workshop.hub.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class WorkshopEnvOverrides {
    private Map<String, String> common;
    private Map<String, String> frontend;
    private Map<String, String> backend;

    public Map<String, String> getCommon() {
        if (common == null || common.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(common));
    }

    public void setCommon(Map<String, String> common) {
        this.common = common;
    }

    public Map<String, String> getFrontend() {
        if (frontend == null || frontend.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(frontend));
    }

    public void setFrontend(Map<String, String> frontend) {
        this.frontend = frontend;
    }

    public Map<String, String> getBackend() {
        if (backend == null || backend.isEmpty()) {
            return Map.of();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(backend));
    }

    public void setBackend(Map<String, String> backend) {
        this.backend = backend;
    }
}
