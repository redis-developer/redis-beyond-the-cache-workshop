package com.redis.workshop.platform.executionplane.cloudrun;

import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneArtifactReference;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneLaunchAccepted;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneLaunchRequest;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneResourcePolicy;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneRestartRequest;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneTerminateRequest;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneTerminationMode;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneWorkspacePolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CloudRunSessionRuntimeAdapterTest {

    private final RecordingCloudRunClient cloudRunClient = new RecordingCloudRunClient();
    private final CloudRunRuntimeProperties properties = properties();
    private final CloudRunSessionRuntimeAdapter adapter = new CloudRunSessionRuntimeAdapter(properties, cloudRunClient);

    @Test
    void launchProducesDeterministicCloudRunServiceMetadata() {
        ExecutionPlaneLaunchAccepted accepted = adapter.launch(launchRequest("sess-001"));

        assertThat(accepted.runtimeRef().provider()).isEqualTo("cloud-run");
        assertThat(accepted.runtimeRef().handle())
            .isEqualTo("projects/workshop-prod/locations/europe-west1/services/ws-sess-001");
        assertThat(accepted.routeBinding().publicBasePath()).isEqualTo("/session/sess-001/");
        assertThat(accepted.routeBinding().gatewayHost()).isEqualTo("workshops.example.com");
        assertThat(accepted.routeBinding().upstreamBaseUrl()).isEqualTo("https://ws-sess-001.run.app");

        CloudRunServiceSpec spec = cloudRunClient.createdSpecs().getFirst();
        assertThat(spec.serviceName()).isEqualTo("ws-sess-001");
        assertThat(spec.image()).contains("@sha256:");
        assertThat(spec.concurrency()).isEqualTo(1);
        assertThat(spec.minInstances()).isZero();
        assertThat(spec.maxInstances()).isEqualTo(1);
        assertThat(spec.environment())
            .containsEntry("WORKSHOP_SESSION_ID", "sess-001")
            .containsEntry("WORKSHOP_REDIS_MODE", "local-process")
            .containsEntry("WORKSHOP_LOCAL_REDIS_PORT", "6379");
        assertThat(spec.secretEnvironment()).isEmpty();
        assertThat(spec.labels())
            .containsEntry("environment", "prod-event")
            .containsEntry("event-id", "redis-days-2026")
            .containsEntry("resource-class", "small")
            .containsEntry("redis-mode", "local-process");
    }

    @Test
    void restartWithoutRebuildCallsManagerEndpointInsteadOfRedeploying() {
        ExecutionPlaneLaunchAccepted accepted = adapter.restart(restartRequest("sess-002", false));

        assertThat(accepted.runtimeRef().handle())
            .isEqualTo("projects/workshop-prod/locations/europe-west1/services/ws-sess-002");
        assertThat(cloudRunClient.createdSpecs()).isEmpty();
        assertThat(cloudRunClient.restartCalls())
            .containsExactly("workshop-prod/europe-west1/ws-sess-002 rebuild=false");
    }

    @Test
    void restartWithRebuildCallsManagerEndpointWithRebuildFlag() {
        adapter.restart(restartRequest("sess-003", true));

        assertThat(cloudRunClient.createdSpecs()).isEmpty();
        assertThat(cloudRunClient.restartCalls())
            .containsExactly("workshop-prod/europe-west1/ws-sess-003 rebuild=true");
    }

    @Test
    void terminateDeletesCloudRunServiceByRuntimeRef() {
        adapter.terminate(new ExecutionPlaneTerminateRequest(
            "sess-004",
            adapter.launch(launchRequest("sess-004")).runtimeRef(),
            ExecutionPlaneTerminationMode.USER_REQUESTED
        ));

        assertThat(cloudRunClient.deleteCalls())
            .containsExactly("workshop-prod/europe-west1/ws-sess-004");
    }

    private ExecutionPlaneLaunchRequest launchRequest(String sessionId) {
        return new ExecutionPlaneLaunchRequest(
            sessionId,
            "1_session_management",
            "session-management-2026.04.1",
            "2026.04.1",
            "LAB",
            artifacts(),
            new ExecutionPlaneResourcePolicy("small", 1000, 1024, 1024, 60),
            new ExecutionPlaneWorkspacePolicy("/workspace", "release:session-management-2026.04.1", true, true),
            null,
            runtimeConfig()
        );
    }

    private ExecutionPlaneRestartRequest restartRequest(String sessionId, boolean rebuild) {
        return new ExecutionPlaneRestartRequest(
            sessionId,
            "1_session_management",
            "session-management-2026.04.1",
            "2026.04.1",
            "LAB",
            rebuild,
            artifacts(),
            new ExecutionPlaneResourcePolicy("small", 1000, 1024, 1024, 60),
            new ExecutionPlaneWorkspacePolicy("/workspace", "release:session-management-2026.04.1", true, true),
            null,
            runtimeConfig()
        );
    }

    private Map<String, ExecutionPlaneArtifactReference> artifacts() {
        return Map.of(
            "runner",
            new ExecutionPlaneArtifactReference(
                "registry.example.com/workshops/session-management-runner@sha256:"
                    + "1111111111111111111111111111111111111111111111111111111111111111"
            )
        );
    }

    private Map<String, String> runtimeConfig() {
        return Map.of(
            "WORKSHOP_CHILD_PORT", "18080",
            "WORKSHOP_ENVIRONMENT", "prod-event",
            "WORKSHOP_EVENT_ID", "redis-days-2026",
            "WORKSHOP_REDIS_MODE", "local-process",
            "WORKSHOP_LOCAL_REDIS_PORT", "6379"
        );
    }

    private CloudRunRuntimeProperties properties() {
        CloudRunRuntimeProperties runtimeProperties = new CloudRunRuntimeProperties();
        runtimeProperties.setProjectId("workshop-prod");
        runtimeProperties.setRegion("europe-west1");
        runtimeProperties.setGatewayHost("workshops.example.com");
        runtimeProperties.setManagerPort(8080);
        runtimeProperties.setConcurrency(1);
        runtimeProperties.setMaxInstances(1);
        runtimeProperties.setEnvironment("prod-event");
        runtimeProperties.setEventId("redis-days-2026");
        return runtimeProperties;
    }

    private static class RecordingCloudRunClient implements CloudRunSessionRuntimeClient {

        private final List<CloudRunServiceSpec> createdSpecs = new ArrayList<>();
        private final List<String> restartCalls = new ArrayList<>();
        private final List<String> deleteCalls = new ArrayList<>();

        @Override
        public CloudRunSessionService createOrReplace(CloudRunServiceSpec serviceSpec) {
            createdSpecs.add(serviceSpec);
            return new CloudRunSessionService(
                serviceSpec.projectId(),
                serviceSpec.region(),
                serviceSpec.serviceName(),
                "https://" + serviceSpec.serviceName() + ".run.app"
            );
        }

        @Override
        public CloudRunSessionService restartManager(String projectId, String region, String serviceName, boolean rebuild) {
            restartCalls.add(projectId + "/" + region + "/" + serviceName + " rebuild=" + rebuild);
            return new CloudRunSessionService(
                projectId,
                region,
                serviceName,
                "https://" + serviceName + ".run.app"
            );
        }

        @Override
        public void delete(String projectId, String region, String serviceName) {
            deleteCalls.add(projectId + "/" + region + "/" + serviceName);
        }

        private List<CloudRunServiceSpec> createdSpecs() {
            return createdSpecs;
        }

        private List<String> restartCalls() {
            return restartCalls;
        }

        private List<String> deleteCalls() {
            return deleteCalls;
        }
    }
}
