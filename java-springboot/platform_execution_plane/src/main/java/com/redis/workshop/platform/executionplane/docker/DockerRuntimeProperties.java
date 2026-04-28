package com.redis.workshop.platform.executionplane.docker;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "platform.execution-plane.docker")
public class DockerRuntimeProperties {

    private String dockerBinary = "docker";
    private String gatewayHost = "localhost";
    private String serviceNamespace = "local-docker";
    private int managerPort = 8080;
    private Duration managerRestartTimeout = Duration.ofSeconds(290);
    private Map<String, String> imageOverrides = new LinkedHashMap<>();

    public String getDockerBinary() {
        return dockerBinary;
    }

    public void setDockerBinary(String dockerBinary) {
        this.dockerBinary = dockerBinary;
    }

    public String getGatewayHost() {
        return gatewayHost;
    }

    public void setGatewayHost(String gatewayHost) {
        this.gatewayHost = gatewayHost;
    }

    public String getServiceNamespace() {
        return serviceNamespace;
    }

    public void setServiceNamespace(String serviceNamespace) {
        this.serviceNamespace = serviceNamespace;
    }

    public int getManagerPort() {
        return managerPort;
    }

    public void setManagerPort(int managerPort) {
        this.managerPort = managerPort;
    }

    public Duration getManagerRestartTimeout() {
        return managerRestartTimeout;
    }

    public void setManagerRestartTimeout(Duration managerRestartTimeout) {
        this.managerRestartTimeout = managerRestartTimeout;
    }

    public Map<String, String> getImageOverrides() {
        return imageOverrides;
    }

    public void setImageOverrides(Map<String, String> imageOverrides) {
        this.imageOverrides = imageOverrides == null ? new LinkedHashMap<>() : new LinkedHashMap<>(imageOverrides);
    }
}
