package com.redis.workshop.platform.contracts.executionplane;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.Map;
import org.hibernate.validator.messageinterpolation.ParameterMessageInterpolator;
import org.junit.jupiter.api.Test;

class ExecutionPlaneContractValidationTest {

    private static final ExecutionPlaneRuntimeRef RUNTIME_REF = new ExecutionPlaneRuntimeRef(
        "cloud-run",
        "projects/workshop/locations/europe-west4/services/ws-session-1"
    );
    private static final ExecutionPlaneRouteBinding ROUTE_BINDING = new ExecutionPlaneRouteBinding(
        "/workshop/session-1/",
        "workshops.example.com",
        "session-1-frontend",
        "execution-plane",
        "https://session-1-frontend.run.app"
    );
    private static final ExecutionPlaneResourcePolicy RESOURCE_POLICY = new ExecutionPlaneResourcePolicy(
        "small",
        500,
        512,
        1024,
        60
    );
    private static final ExecutionPlaneWorkspacePolicy WORKSPACE_POLICY = new ExecutionPlaneWorkspacePolicy(
        "/workspace",
        "release:session-management-2026.04.1",
        true,
        true
    );

    private final Validator validator = Validation.byDefaultProvider()
        .configure()
        .messageInterpolator(new ParameterMessageInterpolator())
        .buildValidatorFactory()
        .getValidator();

    @Test
    void rejectsNonDigestPinnedArtifactReferences() {
        ExecutionPlaneLaunchRequest request = new ExecutionPlaneLaunchRequest(
            "session-1",
            "1_session_management",
            "session-management-2026.04.1",
            "2026.04.1",
            "LAB",
            Map.of("frontend", new ExecutionPlaneArtifactReference("registry.example.com/workshop:latest")),
            RESOURCE_POLICY,
            WORKSPACE_POLICY,
            Map.of()
        );

        assertThat(validator.validate(request))
            .anySatisfy(violation -> assertThat(violation.getPropertyPath().toString()).contains("artifacts"));
    }

    @Test
    void acceptsValidLaunchRequest() {
        ExecutionPlaneLaunchRequest request = new ExecutionPlaneLaunchRequest(
            "session-1",
            "1_session_management",
            "session-management-2026.04.1",
            "2026.04.1",
            "LAB",
            Map.of(
                "frontend",
                new ExecutionPlaneArtifactReference(
                    "europe-west4-docker.pkg.dev/workshop-platform/workshops/session-management-frontend@sha256:"
                        + "1111111111111111111111111111111111111111111111111111111111111111"
                )
            ),
            RESOURCE_POLICY,
            WORKSPACE_POLICY,
            Map.of()
        );

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void acceptsValidSharedContractSurface() {
        assertThat(validator.validate(
            new ExecutionPlaneLaunchAccepted("session-1", RUNTIME_REF, ROUTE_BINDING)
        )).isEmpty();
        assertThat(validator.validate(
            new ExecutionPlaneTerminateRequest("session-1", RUNTIME_REF, ExecutionPlaneTerminationMode.USER_REQUESTED)
        )).isEmpty();
        assertThat(validator.validate(
            new ExecutionPlaneTerminateAccepted("session-1", RUNTIME_REF, ExecutionPlaneTerminationMode.USER_REQUESTED)
        )).isEmpty();
        assertThat(validator.validate(ROUTE_BINDING)).isEmpty();
        assertThat(validator.validate(RUNTIME_REF)).isEmpty();
        assertThat(validator.validate(RESOURCE_POLICY)).isEmpty();
        assertThat(validator.validate(WORKSPACE_POLICY)).isEmpty();
        assertThat(validator.validate(new ExecutionPlaneRedisPolicy(
            ExecutionPlaneRedisMode.LOCAL_PROCESS,
            "session:session-1:",
            "standard",
            new ExecutionPlaneLocalRedisBinding(
                "redis-server --port ${WORKSHOP_LOCAL_REDIS_PORT}",
                6379,
                "/workspace/.redis",
                "redis-cli -p ${WORKSHOP_LOCAL_REDIS_PORT} ping",
                "redisinsight",
                5540,
                "/session/session-1/redis-insight",
                "/api/health"
            )
        ))).isEmpty();
        assertThat(validator.validate(new ExecutionPlaneStatusUpdate(
            "session-1",
            RUNTIME_REF,
            ExecutionPlaneWorkloadStatus.READY,
            null,
            "ready",
            Instant.parse("2026-04-25T10:15:30Z")
        ))).isEmpty();
    }

    @Test
    void rejectsRedisPolicyWithoutRequiredBinding() {
        assertThat(org.assertj.core.api.Assertions.catchThrowable(() -> new ExecutionPlaneRedisPolicy(
            ExecutionPlaneRedisMode.LOCAL_PROCESS,
            "session:session-1:",
            "standard",
            null
        ))).isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("local binding is required");
    }
}
