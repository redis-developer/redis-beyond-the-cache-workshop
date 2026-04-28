package com.redis.workshop.platform.controlplane.session;

import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionRecord;
import com.redis.workshop.platform.controlplane.persistence.model.PlatformSessionState;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record SessionShellResponse(
    String sessionId,
    String workshopId,
    String learnerAppUrl,
    String redisInsightUrl,
    String hubUrl,
    ComponentStatus runtime,
    ComponentStatus frontend,
    ComponentStatus backend,
    List<Action> actions
) {

    private static final Set<PlatformSessionState> RESTARTABLE_STATES = Set.of(
        PlatformSessionState.READY,
        PlatformSessionState.DEGRADED,
        PlatformSessionState.FAILED
    );
    private static final Set<PlatformSessionState> TERMINABLE_STATES = Set.of(
        PlatformSessionState.REQUESTED,
        PlatformSessionState.ADMITTED,
        PlatformSessionState.PROVISIONING,
        PlatformSessionState.INITIALIZING,
        PlatformSessionState.READY,
        PlatformSessionState.DEGRADED
    );

    static SessionShellResponse fromRecord(PlatformSessionRecord record) {
        String learnerAppUrl = learnerAppUrl(record);
        String redisInsightUrl = redisInsightUrl(learnerAppUrl);
        String hubUrl = hubUrl(learnerAppUrl);
        boolean canRestart = canRestart(record);
        boolean canTerminate = TERMINABLE_STATES.contains(record.getState());

        return new SessionShellResponse(
            record.getSessionId(),
            record.getWorkshopId(),
            learnerAppUrl,
            redisInsightUrl,
            hubUrl,
            runtimeStatus(record),
            frontendStatus(record, learnerAppUrl),
            backendStatus(record),
            List.of(
                linkAction("openLearnerApp", learnerAppUrl, StringUtils.hasText(learnerAppUrl), "learner_app_unavailable"),
                linkAction("openRedisInsight", redisInsightUrl, StringUtils.hasText(redisInsightUrl), "redis_insight_unavailable"),
                platformAction("restartRuntime", canRestart, "POST", restartUrl(record), Map.of("rebuild", false), "restart_unavailable"),
                platformAction("rebuildRuntime", canRestart, "POST", restartUrl(record), Map.of("rebuild", true), "rebuild_unavailable"),
                platformAction("terminateSession", canTerminate, "DELETE", sessionUrl(record), null, "terminate_unavailable")
            )
        );
    }

    private static ComponentStatus runtimeStatus(PlatformSessionRecord record) {
        return new ComponentStatus(
            componentState(record.getState()),
            record.getRuntimeStatus(),
            record.getFailureMessage()
        );
    }

    private static ComponentStatus frontendStatus(PlatformSessionRecord record, String learnerAppUrl) {
        if (StringUtils.hasText(learnerAppUrl)
            && record.getState() != PlatformSessionState.TERMINATING
            && record.getState() != PlatformSessionState.TERMINATED
            && record.getState() != PlatformSessionState.CLEANUP_PENDING
            && record.getState() != PlatformSessionState.EXPIRED) {
            return new ComponentStatus("READY", "route_available", null);
        }
        return new ComponentStatus(
            componentState(record.getState()),
            record.getRuntimeStatus(),
            record.getFailureMessage()
        );
    }

    private static ComponentStatus backendStatus(PlatformSessionRecord record) {
        return new ComponentStatus(
            componentState(record.getState()),
            record.getRuntimeStatus(),
            record.getFailureMessage()
        );
    }

    private static String componentState(PlatformSessionState state) {
        return switch (state) {
            case REQUESTED, ADMITTED -> "PENDING";
            case PROVISIONING, INITIALIZING -> "STARTING";
            case READY -> "READY";
            case DEGRADED -> "DEGRADED";
            case TERMINATING, CLEANUP_PENDING -> "STOPPING";
            case TERMINATED, EXPIRED -> "STOPPED";
            case FAILED -> "FAILED";
        };
    }

    private static Action linkAction(String name, String url, boolean available, String unavailableReason) {
        return new Action(
            name,
            available,
            "GET",
            available ? url : null,
            null,
            available ? null : unavailableReason
        );
    }

    private static Action platformAction(
        String name,
        boolean available,
        String method,
        String url,
        Map<String, Object> body,
        String unavailableReason
    ) {
        return new Action(
            name,
            available,
            method,
            url,
            body,
            available ? null : unavailableReason
        );
    }

    private static boolean canRestart(PlatformSessionRecord record) {
        if (!RESTARTABLE_STATES.contains(record.getState())) {
            return false;
        }
        return record.getState() != PlatformSessionState.FAILED
            || !StringUtils.hasText(record.getTerminationReason());
    }

    private static String learnerAppUrl(PlatformSessionRecord record) {
        return StringUtils.hasText(record.getPublicEntryUrl()) ? record.getPublicEntryUrl().trim() : null;
    }

    private static String redisInsightUrl(String learnerAppUrl) {
        if (!StringUtils.hasText(learnerAppUrl)) {
            return null;
        }
        return appendPath(learnerAppUrl, "redis-insight/");
    }

    private static String hubUrl(String learnerAppUrl) {
        if (!StringUtils.hasText(learnerAppUrl)) {
            return "/";
        }
        String route = learnerAppUrl.trim();
        try {
            URI uri = URI.create(route);
            if (uri.isAbsolute()) {
                String prefix = hubPathPrefix(uri.getPath());
                return uri.getScheme() + "://" + uri.getAuthority() + prefix;
            }
        } catch (IllegalArgumentException ignored) {
        }
        return hubPathPrefix(route);
    }

    private static String hubPathPrefix(String path) {
        String normalized = StringUtils.hasText(path) ? path : "/";
        int sessionIndex = normalized.indexOf("/session/");
        int workshopIndex = normalized.indexOf("/workshop/");
        int markerIndex = firstPresentIndex(sessionIndex, workshopIndex);
        if (markerIndex < 0) {
            return "/";
        }
        String prefix = normalized.substring(0, markerIndex);
        if (!StringUtils.hasText(prefix)) {
            return "/";
        }
        return trimTrailingSlash(prefix) + "/";
    }

    private static int firstPresentIndex(int left, int right) {
        if (left < 0) {
            return right;
        }
        if (right < 0) {
            return left;
        }
        return Math.min(left, right);
    }

    private static String appendPath(String baseUrl, String path) {
        String normalizedBase = baseUrl.trim();
        String separator = normalizedBase.endsWith("/") ? "" : "/";
        return normalizedBase + separator + path;
    }

    private static String restartUrl(PlatformSessionRecord record) {
        return sessionUrl(record) + "/restart";
    }

    private static String sessionUrl(PlatformSessionRecord record) {
        return "/api/sessions/" + record.getSessionId();
    }

    private static String trimTrailingSlash(String value) {
        String normalized = value;
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public record ComponentStatus(
        String state,
        String status,
        String message
    ) {
    }

    public record Action(
        String name,
        boolean available,
        String method,
        String url,
        Map<String, Object> body,
        String unavailableReason
    ) {
    }
}
