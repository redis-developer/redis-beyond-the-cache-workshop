package com.redis.workshop.platform.contracts.executionplane;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ExecutionPlaneContractSerializationTest {

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

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void serializesLaunchRequestWithDigestPinnedArtifacts() throws Exception {
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
            Map.of("SPRING_PROFILES_ACTIVE", "session")
        );

        String json = assertRoundTrip(request, ExecutionPlaneLaunchRequest.class);

        assertThat(json).contains("europe-west4-docker.pkg.dev");
    }

    @Test
    void serializesLaunchRequestWithLocalRedisPolicy() throws Exception {
        ExecutionPlaneRedisPolicy redisPolicy = new ExecutionPlaneRedisPolicy(
            ExecutionPlaneRedisMode.LOCAL_PROCESS,
            "session:session-1:",
            "stack",
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
        );

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
            redisPolicy,
            redisPolicy.toRuntimeConfig()
        );

        String json = assertRoundTrip(request, ExecutionPlaneLaunchRequest.class);

        assertThat(json)
            .contains("LOCAL_PROCESS", "redisInsightCommand")
            .doesNotContain("passwordSecretRef");
    }

    @Test
    void serializesLocalRedisPolicy() throws Exception {
        ExecutionPlaneRedisPolicy redisPolicy = new ExecutionPlaneRedisPolicy(
            ExecutionPlaneRedisMode.LOCAL_PROCESS,
            "session:session-1:",
            "stack",
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
        );

        String json = assertRoundTrip(redisPolicy, ExecutionPlaneRedisPolicy.class);

        assertThat(json).contains("LOCAL_PROCESS", "redisInsightCommand");
        assertThat(redisPolicy.toRuntimeConfig())
            .containsEntry("WORKSHOP_REDIS_MODE", "local-process")
            .containsEntry("WORKSHOP_REDIS_INSIGHT_PORT", "5540");
    }

    @Test
    void serializesLaunchAccepted() throws Exception {
        assertRoundTrip(
            new ExecutionPlaneLaunchAccepted("session-1", RUNTIME_REF, ROUTE_BINDING),
            ExecutionPlaneLaunchAccepted.class
        );
    }

    @Test
    void serializesTerminateRequest() throws Exception {
        assertRoundTrip(
            new ExecutionPlaneTerminateRequest(
                "session-1",
                RUNTIME_REF,
                ExecutionPlaneTerminationMode.USER_REQUESTED
            ),
            ExecutionPlaneTerminateRequest.class
        );
    }

    @Test
    void serializesTerminateAccepted() throws Exception {
        assertRoundTrip(
            new ExecutionPlaneTerminateAccepted(
                "session-1",
                RUNTIME_REF,
                ExecutionPlaneTerminationMode.USER_REQUESTED
            ),
            ExecutionPlaneTerminateAccepted.class
        );
    }

    @Test
    void serializesRouteBinding() throws Exception {
        assertRoundTrip(ROUTE_BINDING, ExecutionPlaneRouteBinding.class);
    }

    @Test
    void serializesRuntimeRefAsOpaqueHandle() throws Exception {
        String json = assertRoundTrip(RUNTIME_REF, ExecutionPlaneRuntimeRef.class);

        assertThat(json)
            .contains("provider", "handle")
            .doesNotContain("namespace", "workloadName");
    }

    @Test
    void serializesResourcePolicy() throws Exception {
        assertRoundTrip(RESOURCE_POLICY, ExecutionPlaneResourcePolicy.class);
    }

    @Test
    void serializesWorkspacePolicy() throws Exception {
        assertRoundTrip(WORKSPACE_POLICY, ExecutionPlaneWorkspacePolicy.class);
    }

    @Test
    void serializesStatusUpdate() throws Exception {
        ExecutionPlaneStatusUpdate update = new ExecutionPlaneStatusUpdate(
            "session-1",
            RUNTIME_REF,
            ExecutionPlaneWorkloadStatus.READY,
            null,
            "ready",
            Instant.parse("2026-04-25T10:15:30Z")
        );

        assertRoundTrip(update, ExecutionPlaneStatusUpdate.class);
    }

    private <T> String assertRoundTrip(T value, Class<T> type) throws Exception {
        String json = objectMapper.writeValueAsString(value);
        T restored = objectMapper.readValue(json, type);

        assertThat(restored).isEqualTo(value);
        return json;
    }
}
