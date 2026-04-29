---
id: P5
title: Manager code proxy
status: done
depends_on: ["P2"]
owner: Erdos
allowed_files: ["java-springboot/workshop-infrastructure/src/main/java/com/redis/workshop/infrastructure/BackendProxyController.java", "java-springboot/workshop-infrastructure/src/main/java/com/redis/workshop/infrastructure/CodeEditorWebSocketProxyHandler.java", "java-springboot/workshop-infrastructure/src/main/java/com/redis/workshop/infrastructure/CodeEditorWebSocketProxyConfig.java", "java-springboot/workshop-infrastructure/src/test/java/com/redis/workshop/infrastructure/BackendProxyControllerTest.java", "java-springboot/workshop-infrastructure/src/test/java/com/redis/workshop/infrastructure/CodeEditorWebSocketProxyHandlerTest.java"]
forbidden_files: ["java-springboot/session_runtime_tools/runner-image/**", "scripts/run-workshop.sh", "java-springboot/workshop-infrastructure/src/main/java/com/redis/workshop/infrastructure/LocalSessionRunnerManager.java", "java-springboot/workshop-infrastructure/src/main/java/com/redis/workshop/infrastructure/SessionRunnerProperties.java", "workshop-frontend-shared/src/**", "java-springboot/1_session_management_frontend/**", "java-springboot/platform_control_plane/**"]
---

# Goal

Expose the session-local VS Code server through the frontend manager at `/code/`, including WebSocket traffic.

# Why This Exists

Code server UIs require WebSockets. The existing Redis Insight style HTTP proxy is not enough for embedded VS Code.

# Required Changes

1. Add `/code`, `/code/`, and `/code/**` HTTP proxying to `BackendProxyController`.
2. Reuse the shared contract from the packet index for target URI resolution.
3. Add WebSocket proxy support for `/code/**` to the local code editor process.
4. Preserve Redis Insight and learner app proxy behavior.
5. Rewrite redirects, cookies, and response bodies as needed for path-based proxying.
6. Add tests for path rewriting and WebSocket handler behavior.

# Acceptance Criteria

1. `GET /code/` proxies to the configured local editor URI.
2. `X-Forwarded-Prefix: /session/example` produces external code paths under `/session/example/code/`.
3. WebSocket upgrade requests under `/code/` are forwarded to the editor server.
4. Existing Redis Insight proxy tests still pass.

# Verification

1. `./gradlew --no-daemon :workshop-infrastructure:test`
2. Manual check after all packets land: `curl -sS -i http://localhost:38809/code/`

# Out Of Scope

1. Do not start the code editor process.
2. Do not edit the platform control plane route proxy.
3. Do not edit frontend Vue files.
4. Do not edit runner image packaging.

# Handoff Back

Report the HTTP proxy path behavior, WebSocket support shape, and any limitations for code server path rewriting.
