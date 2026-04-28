package com.redis.workshop.platform.controlplane.persistence.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "platform_sessions")
public class PlatformSessionRecord {

    @Id
    @Column(name = "session_id", nullable = false, length = 64)
    private String sessionId;

    @Column(name = "owner_user_id", nullable = false, length = 128)
    private String ownerUserId;

    @Column(name = "team_id", length = 128)
    private String teamId;

    @Column(name = "workshop_id", nullable = false, length = 128)
    private String workshopId;

    @Column(name = "release_version", nullable = false, length = 64)
    private String releaseVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_mode", nullable = false, length = 32)
    private PlatformSessionMode mode;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_state", nullable = false, length = 32)
    private PlatformSessionState state;

    @Column(name = "quota_class", nullable = false, length = 64)
    private String quotaClass;

    @Column(name = "resource_class", length = 64)
    private String resourceClass;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "expired_at")
    private Instant expiredAt;

    @Column(name = "termination_requested_at")
    private Instant terminationRequestedAt;

    @Column(name = "terminated_at")
    private Instant terminatedAt;

    @Column(name = "cleanup_completed_at")
    private Instant cleanupCompletedAt;

    @Column(name = "workspace_ref", length = 255)
    private String workspaceRef;

    @Enumerated(EnumType.STRING)
    @Column(name = "workspace_cleanup_state", nullable = false, length = 32)
    private WorkspaceCleanupState workspaceCleanupState;

    @Enumerated(EnumType.STRING)
    @Column(name = "route_type", nullable = false, length = 32)
    private RouteType routeType;

    @Column(name = "public_entry_url", length = 1024)
    private String publicEntryUrl;

    @Column(name = "internal_runtime_ref", length = 255)
    private String internalRuntimeRef;

    @Column(name = "route_gateway_host", length = 255)
    private String routeGatewayHost;

    @Column(name = "route_service_name", length = 255)
    private String routeServiceName;

    @Column(name = "route_service_namespace", length = 255)
    private String routeServiceNamespace;

    @Column(name = "route_upstream_base_url", length = 1024)
    private String routeUpstreamBaseUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "workspace_policy", nullable = false, length = 32)
    private WorkspacePolicy workspacePolicy;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "failure_message", columnDefinition = "TEXT")
    private String failureMessage;

    @Column(name = "termination_reason", length = 255)
    private String terminationReason;

    @Column(name = "runtime_status", length = 64)
    private String runtimeStatus;

    public PlatformSessionRecord() {
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (lastActivityAt == null) {
            lastActivityAt = createdAt;
        }
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }

    public String getWorkshopId() {
        return workshopId;
    }

    public void setWorkshopId(String workshopId) {
        this.workshopId = workshopId;
    }

    public String getReleaseVersion() {
        return releaseVersion;
    }

    public void setReleaseVersion(String releaseVersion) {
        this.releaseVersion = releaseVersion;
    }

    public PlatformSessionMode getMode() {
        return mode;
    }

    public void setMode(PlatformSessionMode mode) {
        this.mode = mode;
    }

    public PlatformSessionState getState() {
        return state;
    }

    public void setState(PlatformSessionState state) {
        this.state = state;
    }

    public String getQuotaClass() {
        return quotaClass;
    }

    public void setQuotaClass(String quotaClass) {
        this.quotaClass = quotaClass;
    }

    public String getResourceClass() {
        return resourceClass;
    }

    public void setResourceClass(String resourceClass) {
        this.resourceClass = resourceClass;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getLastActivityAt() {
        return lastActivityAt;
    }

    public void setLastActivityAt(Instant lastActivityAt) {
        this.lastActivityAt = lastActivityAt;
    }

    public Instant getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(Instant expiredAt) {
        this.expiredAt = expiredAt;
    }

    public Instant getTerminationRequestedAt() {
        return terminationRequestedAt;
    }

    public void setTerminationRequestedAt(Instant terminationRequestedAt) {
        this.terminationRequestedAt = terminationRequestedAt;
    }

    public Instant getTerminatedAt() {
        return terminatedAt;
    }

    public void setTerminatedAt(Instant terminatedAt) {
        this.terminatedAt = terminatedAt;
    }

    public Instant getCleanupCompletedAt() {
        return cleanupCompletedAt;
    }

    public void setCleanupCompletedAt(Instant cleanupCompletedAt) {
        this.cleanupCompletedAt = cleanupCompletedAt;
    }

    public String getWorkspaceRef() {
        return workspaceRef;
    }

    public void setWorkspaceRef(String workspaceRef) {
        this.workspaceRef = workspaceRef;
    }

    public WorkspaceCleanupState getWorkspaceCleanupState() {
        return workspaceCleanupState;
    }

    public void setWorkspaceCleanupState(WorkspaceCleanupState workspaceCleanupState) {
        this.workspaceCleanupState = workspaceCleanupState;
    }

    public RouteType getRouteType() {
        return routeType;
    }

    public void setRouteType(RouteType routeType) {
        this.routeType = routeType;
    }

    public String getPublicEntryUrl() {
        return publicEntryUrl;
    }

    public void setPublicEntryUrl(String publicEntryUrl) {
        this.publicEntryUrl = publicEntryUrl;
    }

    public String getInternalRuntimeRef() {
        return internalRuntimeRef;
    }

    public void setInternalRuntimeRef(String internalRuntimeRef) {
        this.internalRuntimeRef = internalRuntimeRef;
    }

    public String getRouteGatewayHost() {
        return routeGatewayHost;
    }

    public void setRouteGatewayHost(String routeGatewayHost) {
        this.routeGatewayHost = routeGatewayHost;
    }

    public String getRouteServiceName() {
        return routeServiceName;
    }

    public void setRouteServiceName(String routeServiceName) {
        this.routeServiceName = routeServiceName;
    }

    public String getRouteServiceNamespace() {
        return routeServiceNamespace;
    }

    public void setRouteServiceNamespace(String routeServiceNamespace) {
        this.routeServiceNamespace = routeServiceNamespace;
    }

    public String getRouteUpstreamBaseUrl() {
        return routeUpstreamBaseUrl;
    }

    public void setRouteUpstreamBaseUrl(String routeUpstreamBaseUrl) {
        this.routeUpstreamBaseUrl = routeUpstreamBaseUrl;
    }

    public WorkspacePolicy getWorkspacePolicy() {
        return workspacePolicy;
    }

    public void setWorkspacePolicy(WorkspacePolicy workspacePolicy) {
        this.workspacePolicy = workspacePolicy;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public String getTerminationReason() {
        return terminationReason;
    }

    public void setTerminationReason(String terminationReason) {
        this.terminationReason = terminationReason;
    }

    public String getRuntimeStatus() {
        return runtimeStatus;
    }

    public void setRuntimeStatus(String runtimeStatus) {
        this.runtimeStatus = runtimeStatus;
    }
}
