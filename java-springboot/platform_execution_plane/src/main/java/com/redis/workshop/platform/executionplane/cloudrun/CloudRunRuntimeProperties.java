package com.redis.workshop.platform.executionplane.cloudrun;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "platform.execution-plane.cloud-run")
public class CloudRunRuntimeProperties {

    private String projectId = "local-project";
    private String region = "us-central1";
    private String apiBaseUrl = "https://run.googleapis.com/v2";
    private String gatewayHost = "localhost";
    private String serviceAccount;
    private String ingress = "INGRESS_TRAFFIC_INTERNAL_LOAD_BALANCER";
    private String vpcConnector;
    private String environment = "unknown";
    private String eventId = "unspecified";
    private int managerPort = 8080;
    private int concurrency = 1;
    private int minInstances = 0;
    private int maxInstances = 1;
    private Duration requestTimeout = Duration.ofSeconds(30);
    private Duration operationPollInterval = Duration.ofSeconds(2);
    private Duration operationPollTimeout = Duration.ofMinutes(10);
    private Duration managerRestartTimeout = Duration.ofSeconds(290);

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getGatewayHost() {
        return gatewayHost;
    }

    public void setGatewayHost(String gatewayHost) {
        this.gatewayHost = gatewayHost;
    }

    public String getServiceAccount() {
        return serviceAccount;
    }

    public void setServiceAccount(String serviceAccount) {
        this.serviceAccount = serviceAccount;
    }

    public String getIngress() {
        return ingress;
    }

    public void setIngress(String ingress) {
        this.ingress = ingress;
    }

    public String getVpcConnector() {
        return vpcConnector;
    }

    public void setVpcConnector(String vpcConnector) {
        this.vpcConnector = vpcConnector;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public int getManagerPort() {
        return managerPort;
    }

    public void setManagerPort(int managerPort) {
        this.managerPort = managerPort;
    }

    public int getConcurrency() {
        return concurrency;
    }

    public void setConcurrency(int concurrency) {
        this.concurrency = concurrency;
    }

    public int getMinInstances() {
        return minInstances;
    }

    public void setMinInstances(int minInstances) {
        this.minInstances = minInstances;
    }

    public int getMaxInstances() {
        return maxInstances;
    }

    public void setMaxInstances(int maxInstances) {
        this.maxInstances = maxInstances;
    }

    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    public Duration getOperationPollInterval() {
        return operationPollInterval;
    }

    public void setOperationPollInterval(Duration operationPollInterval) {
        this.operationPollInterval = operationPollInterval;
    }

    public Duration getOperationPollTimeout() {
        return operationPollTimeout;
    }

    public void setOperationPollTimeout(Duration operationPollTimeout) {
        this.operationPollTimeout = operationPollTimeout;
    }

    public Duration getManagerRestartTimeout() {
        return managerRestartTimeout;
    }

    public void setManagerRestartTimeout(Duration managerRestartTimeout) {
        this.managerRestartTimeout = managerRestartTimeout;
    }
}
