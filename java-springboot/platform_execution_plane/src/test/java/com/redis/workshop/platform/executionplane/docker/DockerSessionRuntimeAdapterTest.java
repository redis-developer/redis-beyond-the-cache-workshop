package com.redis.workshop.platform.executionplane.docker;

import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneArtifactReference;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneLaunchAccepted;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneLaunchRequest;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneResourcePolicy;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneRestartRequest;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneRuntimeRef;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneTerminateRequest;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneTerminationMode;
import com.redis.workshop.platform.contracts.executionplane.ExecutionPlaneWorkspacePolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DockerSessionRuntimeAdapterTest {

    private final RecordingDockerCommand dockerCommand = new RecordingDockerCommand();
    private final RecordingManagerClient managerClient = new RecordingManagerClient();
    private final DockerSessionRuntimeAdapter adapter = new DockerSessionRuntimeAdapter(
        properties(),
        dockerCommand,
        managerClient
    );

    @Test
    void launchCreatesDeterministicContainerAndLoopbackRouteBinding() {
        dockerCommand.inspectPort("49153");

        ExecutionPlaneLaunchAccepted accepted = adapter.launch(launchRequest("sess-001"));

        assertThat(accepted.runtimeRef().provider()).isEqualTo("docker");
        assertThat(accepted.runtimeRef().handle()).isEqualTo("ws-sess-001");
        assertThat(accepted.routeBinding().publicBasePath()).isEqualTo("/session/sess-001/");
        assertThat(accepted.routeBinding().gatewayHost()).isEqualTo("localhost");
        assertThat(accepted.routeBinding().serviceName()).isEqualTo("ws-sess-001");
        assertThat(accepted.routeBinding().serviceNamespace()).isEqualTo("local-docker");
        assertThat(accepted.routeBinding().upstreamBaseUrl()).isEqualTo("http://localhost:49153");

        List<String> runCommand = dockerCommand.commands().getFirst();
        assertThat(runCommand).containsSequence("run", "--detach", "--name", "ws-sess-001");
        assertThat(runCommand)
            .containsSequence("--label", "workshop-session-id=sess-001")
            .containsSequence("--label", "workshop-id=1_session_management")
            .containsSequence("--label", "release-id=session-management-2026.04.1")
            .containsSequence("--label", "managed-by=workshop-execution-plane");
        assertThat(runCommand)
            .containsSequence("--env", "WORKSHOP_SESSION_ID=sess-001")
            .containsSequence("--env", "WORKSHOP_PUBLIC_BASE_PATH=/session/sess-001/")
            .containsSequence("--env", "PORT=8080")
            .containsSequence("--env", "SERVER_PORT=8080")
            .containsSequence("--env", "OPENAI_API_KEY=test-openai-key")
            .containsSequence("--env", "WORKSHOP_CHILD_PORT=18080")
            .containsSequence("--env", "WORKSHOP_REDIS_MODE=local-process");
        assertThat(runCommand).containsSequence("--publish", "127.0.0.1::8080/tcp");
        assertThat(runCommand).containsSequence("--cpus", "1");
        assertThat(runCommand).containsSequence("--memory", "1024m");
        assertThat(runCommand.getLast()).isEqualTo(runnerImage());

        assertThat(dockerCommand.commands().get(1)).containsExactly(
            "inspect",
            "--format",
            "{{(index (index .NetworkSettings.Ports \"8080/tcp\") 0).HostPort}}",
            "ws-sess-001"
        );
    }

    @Test
    void restartWithoutRebuildCallsManagerEndpointOnMappedPort() {
        dockerCommand.inspectPort("49154");

        ExecutionPlaneLaunchAccepted accepted = adapter.restart(restartRequest("sess-002", false));

        assertThat(accepted.runtimeRef().handle()).isEqualTo("ws-sess-002");
        assertThat(accepted.routeBinding().upstreamBaseUrl()).isEqualTo("http://localhost:49154");
        assertThat(dockerCommand.commands()).containsExactly(List.of(
            "inspect",
            "--format",
            "{{(index (index .NetworkSettings.Ports \"8080/tcp\") 0).HostPort}}",
            "ws-sess-002"
        ));
        assertThat(managerClient.restartCalls())
            .containsExactly(new RestartCall("http://localhost:49154", false));
    }

    @Test
    void restartWithRebuildPassesRebuildFlag() {
        dockerCommand.inspectPort("49155");

        adapter.restart(restartRequest("sess-003", true));

        assertThat(managerClient.restartCalls())
            .containsExactly(new RestartCall("http://localhost:49155", true));
    }

    @Test
    void terminateRemovesContainerFromRuntimeRef() {
        adapter.terminate(new ExecutionPlaneTerminateRequest(
            "sess-004",
            new ExecutionPlaneRuntimeRef("docker", "ws-sess-004"),
            ExecutionPlaneTerminationMode.USER_REQUESTED
        ));

        assertThat(dockerCommand.commands()).containsExactly(List.of(
            "rm",
            "--force",
            "--volumes",
            "ws-sess-004"
        ));
    }

    @Test
    void terminateTreatsAlreadyRemovedContainerAsAccepted() {
        dockerCommand.failWith("Error response from daemon: No such container: ws-sess-004");

        adapter.terminate(new ExecutionPlaneTerminateRequest(
            "sess-004",
            new ExecutionPlaneRuntimeRef("docker", "ws-sess-004"),
            ExecutionPlaneTerminationMode.CLEANUP_RETRY
        ));

        assertThat(dockerCommand.commands()).containsExactly(List.of(
            "rm",
            "--force",
            "--volumes",
            "ws-sess-004"
        ));
    }

    @Test
    void launchUsesCombinedArtifactWhenRunnerArtifactIsAbsent() {
        dockerCommand.inspectPort("49156");

        ExecutionPlaneLaunchAccepted accepted = adapter.launch(new ExecutionPlaneLaunchRequest(
            "sess-005",
            "1_session_management",
            "session-management-2026.04.1",
            "2026.04.1",
            "LAB",
            Map.of("combined", new ExecutionPlaneArtifactReference(runnerImage())),
            new ExecutionPlaneResourcePolicy("small", 1000, 1024, 1024, 60),
            new ExecutionPlaneWorkspacePolicy("/workspace", "release:session-management-2026.04.1", true, true),
            null,
            runtimeConfig()
        ));

        assertThat(accepted.runtimeRef().handle()).isEqualTo("ws-sess-005");
        assertThat(dockerCommand.commands().getFirst().getLast()).isEqualTo(runnerImage());
    }

    @Test
    void launchUsesLocalImageOverrideForWorkshop() {
        DockerRuntimeProperties properties = properties();
        properties.setImageOverrides(Map.of("1_session_management", "redis-workshop-session-runner:1_session_management"));
        DockerSessionRuntimeAdapter overrideAdapter = new DockerSessionRuntimeAdapter(
            properties,
            dockerCommand,
            managerClient
        );
        dockerCommand.inspectPort("49157");

        overrideAdapter.launch(launchRequest("sess-006"));

        assertThat(dockerCommand.commands().getFirst().getLast())
            .isEqualTo("redis-workshop-session-runner:1_session_management");
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
            new ExecutionPlaneArtifactReference(runnerImage())
        );
    }

    private Map<String, String> runtimeConfig() {
        return Map.of(
            "OPENAI_API_KEY", "test-openai-key",
            "WORKSHOP_CHILD_PORT", "18080",
            "WORKSHOP_ENVIRONMENT", "local-platform",
            "WORKSHOP_EVENT_ID", "redis-days-2026",
            "WORKSHOP_REDIS_MODE", "local-process"
        );
    }

    private String runnerImage() {
        return "registry.example.com/workshops/session-management-runner@sha256:"
            + "1111111111111111111111111111111111111111111111111111111111111111";
    }

    private DockerRuntimeProperties properties() {
        DockerRuntimeProperties runtimeProperties = new DockerRuntimeProperties();
        runtimeProperties.setManagerPort(8080);
        runtimeProperties.setGatewayHost("localhost");
        runtimeProperties.setServiceNamespace("local-docker");
        return runtimeProperties;
    }

    private static class RecordingDockerCommand implements DockerCommand {

        private final List<List<String>> commands = new ArrayList<>();
        private String inspectPort = "49153";
        private String failureOutput;

        @Override
        public DockerCommandResult run(List<String> arguments) {
            commands.add(List.copyOf(arguments));
            if (failureOutput != null) {
                return new DockerCommandResult(1, failureOutput, "");
            }
            if (!arguments.isEmpty() && arguments.getFirst().equals("inspect")) {
                return DockerCommandResult.success(inspectPort + "\n");
            }
            return DockerCommandResult.success("ok\n");
        }

        private void inspectPort(String inspectPort) {
            this.inspectPort = inspectPort;
        }

        private void failWith(String failureOutput) {
            this.failureOutput = failureOutput;
        }

        private List<List<String>> commands() {
            return commands;
        }
    }

    private static class RecordingManagerClient implements DockerSessionRunnerManagerClient {

        private final List<RestartCall> restartCalls = new ArrayList<>();

        @Override
        public void restart(String upstreamBaseUrl, boolean rebuild) {
            restartCalls.add(new RestartCall(upstreamBaseUrl, rebuild));
        }

        private List<RestartCall> restartCalls() {
            return restartCalls;
        }
    }

    private record RestartCall(
        String upstreamBaseUrl,
        boolean rebuild
    ) {
    }
}
