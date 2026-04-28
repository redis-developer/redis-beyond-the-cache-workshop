package com.redis.workshop.platform.executionplane.session;

import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneLaunchAccepted;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneLaunchRequest;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneRestartRequest;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneTerminateAccepted;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneTerminateRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExecutionPlaneSessionController {

    private final ExecutionPlaneRuntimeAdapter runtimeAdapter;

    public ExecutionPlaneSessionController(ExecutionPlaneRuntimeAdapter runtimeAdapter) {
        this.runtimeAdapter = runtimeAdapter;
    }

    @PostMapping("/internal/execution-plane/sessions")
    public ExecutionPlaneLaunchAccepted launch(@Valid @RequestBody ExecutionPlaneLaunchRequest request) {
        return runtimeAdapter.launch(request);
    }

    @PostMapping("/internal/execution-plane/sessions/{sessionId}/restart")
    public ExecutionPlaneLaunchAccepted restart(
        @PathVariable String sessionId,
        @Valid @RequestBody ExecutionPlaneRestartRequest request
    ) {
        requireMatchingSession(sessionId, request.sessionId());
        return runtimeAdapter.restart(request);
    }

    @DeleteMapping("/internal/execution-plane/sessions/{sessionId}")
    @ResponseStatus(HttpStatus.OK)
    public ExecutionPlaneTerminateAccepted terminate(
        @PathVariable String sessionId,
        @Valid @RequestBody ExecutionPlaneTerminateRequest request
    ) {
        requireMatchingSession(sessionId, request.sessionId());
        return runtimeAdapter.terminate(request);
    }

    private void requireMatchingSession(String pathSessionId, String bodySessionId) {
        if (!pathSessionId.equals(bodySessionId)) {
            throw new IllegalArgumentException("path sessionId must match request sessionId");
        }
    }
}
