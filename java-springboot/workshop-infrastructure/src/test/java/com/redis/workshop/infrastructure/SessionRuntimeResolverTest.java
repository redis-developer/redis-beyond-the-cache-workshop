package com.redis.workshop.infrastructure;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SessionRuntimeResolverTest {

    @TempDir
    Path tempDir;

    @Test
    void prefersSessionRuntimeInputsAndPreservesLegacyFallback() {
        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setSessionId("sess-001");
        runtimeProperties.setWorkspacePath(tempDir.resolve("session-workspace").toString());
        runtimeProperties.setSessionBackendUrl("http://session.internal:19090");
        runtimeProperties.setSourcePath(tempDir.resolve("legacy-workspace").toString());
        runtimeProperties.setBackendUrl("http://legacy.internal:18080");

        WorkshopConfig workshopConfig = new TestWorkshopConfig(tempDir.resolve("module-root"));
        SessionRuntimeResolver resolver = new SessionRuntimeResolver(runtimeProperties, workshopConfig);

        assertThat(resolver.resolveSessionId()).contains("sess-001");
        assertThat(resolver.resolveWorkspaceRoot()).contains(tempDir.resolve("session-workspace").toAbsolutePath().normalize());
        assertThat(resolver.resolveModuleRoot()).contains(tempDir.resolve("session-workspace").toAbsolutePath().normalize());
        assertThat(resolver.resolveBackendUri()).contains(java.net.URI.create("http://session.internal:19090"));
    }

    @Test
    void rejectsPathsThatEscapeResolvedWorkspace() {
        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setWorkspacePath(tempDir.resolve("session-workspace").toString());
        SessionRuntimeResolver resolver = new SessionRuntimeResolver(runtimeProperties, new TestWorkshopConfig(tempDir.resolve("module-root")));

        assertThat(resolver.resolvePathWithinModule("src/main/resources/application.properties"))
            .contains(tempDir.resolve("session-workspace/src/main/resources/application.properties").toAbsolutePath().normalize());
        assertThat(resolver.resolvePathWithinModule("../escape.txt")).isEmpty();
    }

    @Test
    void fallsBackToLocalRunnerBackendWhenNoBackendUrlIsConfigured() {
        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        SessionRunnerProperties runnerProperties = new SessionRunnerProperties();
        runnerProperties.setEnabled(true);
        runnerProperties.setChildPort(19191);

        SessionRuntimeResolver resolver = new SessionRuntimeResolver(runtimeProperties, runnerProperties);

        assertThat(resolver.resolveBackendUri()).contains(java.net.URI.create("http://127.0.0.1:19191"));
    }

    @Test
    void prefersLocalRunnerBackendWhenSessionRunnerIsEnabled() {
        FrontendRuntimeProperties runtimeProperties = new FrontendRuntimeProperties();
        runtimeProperties.setBackendUrl("http://legacy.internal:18080");
        runtimeProperties.setSessionBackendUrl("http://session.internal:18080");
        SessionRunnerProperties runnerProperties = new SessionRunnerProperties();
        runnerProperties.setEnabled(true);
        runnerProperties.setChildPort(19191);

        SessionRuntimeResolver resolver = new SessionRuntimeResolver(runtimeProperties, runnerProperties);

        assertThat(resolver.resolveBackendUri()).contains(java.net.URI.create("http://127.0.0.1:19191"));
    }

    private record TestWorkshopConfig(Path moduleRoot) implements WorkshopConfig {

        @Override
        public Map<String, String> getEditableFiles() {
            return Map.of();
        }

        @Override
        public String getOriginalContent(String fileName) {
            return null;
        }

        @Override
        public String getBasePath() {
            return moduleRoot.toString();
        }

        @Override
        public String getModuleName() {
            return "session-runtime-resolver-test";
        }
    }
}
