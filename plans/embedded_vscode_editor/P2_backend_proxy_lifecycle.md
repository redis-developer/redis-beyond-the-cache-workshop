---
id: P2
title: Backend editor process lifecycle
status: done
depends_on: []
owner: Leibniz
allowed_files: ["java-springboot/workshop-infrastructure/src/main/java/com/redis/workshop/infrastructure/SessionRunnerProperties.java", "java-springboot/workshop-infrastructure/src/main/java/com/redis/workshop/infrastructure/LocalSessionRunnerManager.java", "java-springboot/workshop-infrastructure/src/test/java/com/redis/workshop/infrastructure/LocalSessionRunnerManagerTest.java"]
forbidden_files: ["java-springboot/session_runtime_tools/runner-image/**", "scripts/run-workshop.sh", "workshop-frontend-shared/src/**", "java-springboot/1_session_management_frontend/frontend/src/**", "java-springboot/1_session_management_frontend/src/main/java/**"]
---

# Goal

Teach the stable frontend runtime to start and stop the session-local VS Code server as a managed dependency process.

# Why This Exists

The browser VS Code server must live inside each session and must be lifecycle-managed with Redis, Redis Insight, and the learner JVM.

# Required Changes

1. Add code editor properties that resolve `WORKSHOP_LOCAL_CODE_EDITOR_COMMAND`, `WORKSHOP_LOCAL_CODE_EDITOR_PORT`, and `WORKSHOP_LOCAL_CODE_EDITOR_HEALTH_PATH`.
2. Start and stop the code editor dependency process in `LocalSessionRunnerManager`.
3. Add the code editor dependency to the runner status dependencies payload.
4. Ensure the child process receives the editor port in its environment if needed.
5. Add or update tests covering process startup and status reporting.

# Acceptance Criteria

1. The code editor process starts when an editor command is configured.
2. The code editor process stops during runner shutdown.
3. Status dependencies include the code editor process with URI and health path.
4. Redis and Redis Insight process behavior is unchanged.

# Verification

1. `./gradlew --no-daemon :workshop-infrastructure:test`
2. Manual check after all packets land: `curl -sS http://localhost:38809/internal/session-runner/status | jq '.dependencies'`

# Out Of Scope

1. Do not edit proxy controllers.
2. Do not edit the runner image Dockerfile or launch scripts.
3. Do not edit frontend Vue files.
4. Do not add workshop content changes.

# Handoff Back

Report the resolved command, port, health path, status dependency shape, and any editor process startup caveats.
