package com.redis.workshop.platform.executionplane.session;

import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneLaunchAccepted;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneLaunchRequest;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneRestartRequest;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneTerminateAccepted;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneTerminateRequest;

public interface ExecutionPlaneRuntimeAdapter {

    ExecutionPlaneLaunchAccepted launch(ExecutionPlaneLaunchRequest request);

    ExecutionPlaneLaunchAccepted restart(ExecutionPlaneRestartRequest request);

    ExecutionPlaneTerminateAccepted terminate(ExecutionPlaneTerminateRequest request);
}
