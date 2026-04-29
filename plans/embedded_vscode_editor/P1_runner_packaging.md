---
id: P1
title: Runner packaging and process defaults
status: done
depends_on: []
owner: Plato
allowed_files: ["java-springboot/session_runtime_tools/runner-image/Dockerfile", "java-springboot/session_runtime_tools/runner-image/build-local-runner-image.sh", "java-springboot/session_runtime_tools/runner-image/validate-runner-image-template.sh", "scripts/ci/publish-session-runner-image.sh", "scripts/ci/publish-cloud-run-smoke-images.sh", "scripts/run-workshop.sh"]
forbidden_files: ["java-springboot/workshop-infrastructure/src/main/java/com/redis/workshop/infrastructure/BackendProxyController.java", "java-springboot/workshop-infrastructure/src/main/java/com/redis/workshop/infrastructure/LocalSessionRunnerManager.java", "java-springboot/workshop-infrastructure/src/main/java/com/redis/workshop/infrastructure/SessionRunnerProperties.java", "workshop-frontend-shared/src/**", "java-springboot/1_session_management_frontend/frontend/src/**"]
---

# Goal

Package a browser VS Code server into the session runner environment and expose default process configuration through environment variables.

# Why This Exists

The editor must run inside each learner session so code, Redis, Redis Insight, and app rebuilds remain session-scoped.

# Required Changes

1. Add the selected browser VS Code server package to the session runner image.
2. Add default environment variables from the index shared contract.
3. Ensure the server binds to `127.0.0.1` and uses `WORKSHOP_SESSION_WORKSPACE_PATH` as the workspace.
4. Update local runner startup so local testing can start the same editor process when available.
5. Keep the existing Redis Insight runner behavior unchanged.
6. Update runner image template validation for the new editor defaults.

# Acceptance Criteria

1. The runner image can start the manager, learner app, Redis, Redis Insight, and VS Code server process together.
2. The VS Code server process listens only on the configured local port.
3. Local development has an explicit command path for starting the editor process.
4. Existing Redis Insight image checks still pass.

# Verification

1. `bash java-springboot/session_runtime_tools/runner-image/validate-runner-image-template.sh`
2. `bash scripts/validate-workshops.sh`
3. If dependencies are available locally, build a runner image with `bash java-springboot/session_runtime_tools/runner-image/build-local-runner-image.sh --workshop-id 1_session_management`

# Out Of Scope

1. Do not edit Java proxy or lifecycle classes.
2. Do not edit Vue components or routes.
3. Do not alter workshop content.

# Handoff Back

Report the selected VS Code server distribution, install mechanism, command line, port, health path, and any network dependency introduced during Docker build.
