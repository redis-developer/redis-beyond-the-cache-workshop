---
id: P6
title: Public session WebSocket route proxy
status: done
depends_on: []
owner: Kuhn
allowed_files: ["java-springboot/platform_control_plane/src/main/java/com/redis/workshop/platform/controlplane/session/SessionRouteProxyController.java", "java-springboot/platform_control_plane/src/main/java/com/redis/workshop/platform/controlplane/session/SessionRouteProxyService.java", "java-springboot/platform_control_plane/src/main/java/com/redis/workshop/platform/controlplane/session/SessionRouteWebSocketProxyHandler.java", "java-springboot/platform_control_plane/src/main/java/com/redis/workshop/platform/controlplane/session/SessionRouteWebSocketProxyConfig.java", "java-springboot/platform_control_plane/src/test/java/com/redis/workshop/platform/controlplane/session/SessionRouteProxyControllerTest.java", "java-springboot/platform_control_plane/src/test/java/com/redis/workshop/platform/controlplane/session/SessionRouteProxyServiceTest.java", "java-springboot/platform_control_plane/src/test/java/com/redis/workshop/platform/controlplane/session/SessionRouteWebSocketProxyHandlerTest.java"]
forbidden_files: ["java-springboot/workshop-infrastructure/**", "java-springboot/session_runtime_tools/**", "workshop-frontend-shared/src/**", "java-springboot/1_session_management_frontend/**", "scripts/**"]
---

# Goal

Allow WebSocket traffic to pass through the public `/session/{sessionId}` route proxy to the session runner.

# Why This Exists

The embedded editor will work locally only if the manager can proxy WebSockets, but it will work through the hub only if the control plane can also proxy WebSockets.

# Required Changes

1. Detect and route WebSocket upgrade requests under `/session/{sessionId}/**`.
2. Forward WebSocket traffic to the session runner `routeUpstreamBaseUrl`.
3. Preserve existing HTTP proxy behavior for non-WebSocket requests.
4. Keep route admission, session lookup, and error behavior unchanged.
5. Add tests for WebSocket routing decisions and path translation.

# Acceptance Criteria

1. HTTP session route proxy behavior is unchanged.
2. WebSocket requests under `/session/{sessionId}/code/` reach the matching session upstream path.
3. Missing or invalid session IDs are rejected consistently with HTTP routing.
4. Tests cover path preservation after `/session/{sessionId}` stripping.

# Verification

1. `./gradlew --no-daemon :platform_control_plane:test`
2. Manual check after all packets land in hub mode: open `/session/{sessionId}/code/`.

# Out Of Scope

1. Do not edit workshop infrastructure manager proxy files.
2. Do not edit session runner image files.
3. Do not edit frontend Vue files.
4. Do not alter session lifecycle state transitions.

# Handoff Back

Report how WebSocket requests are detected, how upstream URLs are computed, and any Cloud Run limitations.
