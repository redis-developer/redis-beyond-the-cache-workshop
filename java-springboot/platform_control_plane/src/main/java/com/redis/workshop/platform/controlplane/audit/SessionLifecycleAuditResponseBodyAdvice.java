package com.redis.workshop.platform.controlplane.audit;

import com.redis.workshop.platform.controlplane.observability.SessionLifecycleMetricsRecorder;
import com.redis.workshop.platform.controlplane.persistence.model.AuditActionType;
import com.redis.workshop.platform.controlplane.persistence.model.AuditResult;
import com.redis.workshop.platform.controlplane.persistence.model.AuditTargetType;
import com.redis.workshop.platform.controlplane.release.ReleaseCatalogService;
import com.redis.workshop.platform.controlplane.security.CurrentActor;
import com.redis.workshop.platform.controlplane.security.CurrentActorProvider;
import com.redis.workshop.platform.controlplane.session.SessionController;
import com.redis.workshop.platform.controlplane.session.SessionResponse;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice(assignableTypes = SessionController.class)
public class SessionLifecycleAuditResponseBodyAdvice implements ResponseBodyAdvice<Object> {

    private final AuditTrailService auditTrailService;
    private final CurrentActorProvider currentActorProvider;
    private final SessionLifecycleMetricsRecorder sessionLifecycleMetricsRecorder;
    private final ReleaseCatalogService releaseCatalogService;
    private final ReleaseCatalogAuditHooks releaseCatalogAuditHooks;

    public SessionLifecycleAuditResponseBodyAdvice(
        AuditTrailService auditTrailService,
        CurrentActorProvider currentActorProvider,
        SessionLifecycleMetricsRecorder sessionLifecycleMetricsRecorder,
        ReleaseCatalogService releaseCatalogService,
        ReleaseCatalogAuditHooks releaseCatalogAuditHooks
    ) {
        this.auditTrailService = auditTrailService;
        this.currentActorProvider = currentActorProvider;
        this.sessionLifecycleMetricsRecorder = sessionLifecycleMetricsRecorder;
        this.releaseCatalogService = releaseCatalogService;
        this.releaseCatalogAuditHooks = releaseCatalogAuditHooks;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
        Object body,
        MethodParameter returnType,
        MediaType selectedContentType,
        Class<? extends HttpMessageConverter<?>> selectedConverterType,
        ServerHttpRequest request,
        ServerHttpResponse response
    ) {
        if (!(body instanceof SessionResponse sessionResponse)) {
            return body;
        }

        CurrentActor actor = currentActorProvider.findCurrentActor().orElse(null);
        if (actor == null) {
            return body;
        }

        String method = request.getMethod() == null ? "" : request.getMethod().name();
        String path = request.getURI().getPath();
        String requestId = request.getHeaders().getFirst("X-Request-Id");

        if ("POST".equals(method) && "/api/sessions".equals(path)) {
            AuditActionType actionType = AuditActionType.SESSION_CREATED;
            auditTrailService.record(new AuditEventPayload(
                actor.actorId(),
                actor.actorType(),
                actionType,
                AuditTargetType.SESSION,
                sessionResponse.sessionId(),
                AuditResult.SUCCESS,
                null,
                requestId,
                sessionResponse.sessionId(),
                sessionResponse.workshopId(),
                sessionResponse.releaseVersion(),
                null,
                null
            ));
            sessionLifecycleMetricsRecorder.recordSessionCreateSuccess(sessionResponse);
            recordReleaseSelection(actor, actionType, sessionResponse, requestId, "create");
            return body;
        }

        if ("POST".equals(method) && path.matches("^/api/sessions/[^/]+/restart$")) {
            AuditActionType actionType = actor.isAdmin() ? AuditActionType.SESSION_RESTARTED_BY_ADMIN : AuditActionType.SESSION_RESTARTED_BY_USER;
            auditTrailService.record(new AuditEventPayload(
                actor.actorId(),
                actor.actorType(),
                actionType,
                AuditTargetType.SESSION,
                sessionResponse.sessionId(),
                AuditResult.SUCCESS,
                null,
                requestId,
                sessionResponse.sessionId(),
                sessionResponse.workshopId(),
                sessionResponse.releaseVersion(),
                null,
                null
            ));
            recordReleaseSelection(actor, actionType, sessionResponse, requestId, "restart");
            return body;
        }

        if ("DELETE".equals(method) && path.startsWith("/api/sessions/") && !path.startsWith("/api/sessions/admin/")) {
            auditTrailService.record(new AuditEventPayload(
                actor.actorId(),
                actor.actorType(),
                actor.isAdmin() ? AuditActionType.SESSION_TERMINATED_BY_ADMIN : AuditActionType.SESSION_TERMINATED_BY_USER,
                AuditTargetType.SESSION,
                sessionResponse.sessionId(),
                AuditResult.SUCCESS,
                sessionResponse.terminationReason(),
                requestId,
                sessionResponse.sessionId(),
                sessionResponse.workshopId(),
                sessionResponse.releaseVersion(),
                null,
                null
            ));
            sessionLifecycleMetricsRecorder.recordSessionTermination(actor, sessionResponse);
        }

        return body;
    }

    private void recordReleaseSelection(
        CurrentActor actor,
        AuditActionType actionType,
        SessionResponse sessionResponse,
        String requestId,
        String launchAction
    ) {
        releaseCatalogService.findRelease(sessionResponse.workshopId(), sessionResponse.releaseVersion())
            .ifPresentOrElse(
                release -> releaseCatalogAuditHooks.recordReleaseSelection(
                    actor,
                    actionType,
                    sessionResponse,
                    requestId,
                    launchAction,
                    release
                ),
                () -> releaseCatalogAuditHooks.recordReleaseSelectionLookupMiss(
                    actor,
                    actionType,
                    sessionResponse,
                    requestId,
                    launchAction
                )
            );
    }
}
