package com.redis.workshop.platform.controlplane.observability;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class SessionLifecycleMetricsInterceptor implements HandlerInterceptor {

    private final SessionLifecycleMetricsRecorder sessionLifecycleMetricsRecorder;

    public SessionLifecycleMetricsInterceptor(SessionLifecycleMetricsRecorder sessionLifecycleMetricsRecorder) {
        this.sessionLifecycleMetricsRecorder = sessionLifecycleMetricsRecorder;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("POST".equalsIgnoreCase(request.getMethod()) && "/api/sessions".equals(request.getRequestURI())) {
            sessionLifecycleMetricsRecorder.recordSessionCreateRequest();
        }
        return true;
    }
}
