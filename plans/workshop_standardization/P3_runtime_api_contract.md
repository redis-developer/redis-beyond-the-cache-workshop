---
id: P3
title: Runtime API contract
status: done
depends_on: []
owner: unassigned
allowed_files: ["java-springboot/platform_control_plane/src/main/java/com/redis/workshop/platform/controlplane/session/**", "java-springboot/platform_control_plane/src/test/java/com/redis/workshop/platform/controlplane/session/**", "java-springboot/platform_control_plane/src/main/java/com/redis/workshop/platform/controlplane/api/**", "java-springboot/platform_control_plane/src/test/java/com/redis/workshop/platform/controlplane/api/**", "frontend/src/api/**", "frontend/src/types/**"]
forbidden_files: ["workshop-frontend-shared/**", "java-springboot/1_session_management_frontend/**", "java-springboot/2_full_text_search_frontend/**", "java-springboot/3_distributed_locks_frontend/**", "java-springboot/4_agent_memory_frontend/**", "scripts/**"]
---

# P3 Runtime API Contract

## Goal

Expose a runtime agnostic session shell contract that works for local Docker and Cloud Run.

## Why This Exists

The shared shell should not know whether a workshop is running locally, in Cloud Run, or somewhere else. It needs links, state, and available actions from the platform.

## Required Changes

1. Define the session shell runtime response shape.

2. Include session id, workshop id, learner app URL, Redis Insight URL, hub URL, runtime state, frontend state, backend state, and available actions.

3. Keep restart and rebuild operations behind existing platform action endpoints where possible.

4. Add tests for the response contract.

5. Avoid coupling the contract to Docker specific language.

## Acceptance Criteria

1. The API can describe local Docker sessions and Cloud Run sessions with the same shape.

2. The response includes enough information for the shared shell to render toolbar actions and iframe state.

3. Rebuild availability is explicit.

4. Missing optional capabilities are represented cleanly.

5. Existing launch, restart, rebuild, and terminate behavior is preserved.

## Verification

1. Run the focused platform control plane tests for session APIs.

2. Add or update contract tests for local and Cloud Run style session data.

3. Confirm no shared frontend files are edited in this packet.

## Out Of Scope

1. Building the shared shell UI.

2. Markdown rendering.

3. Migrating existing workshops.

## Handoff Back

Return the final response shape, endpoint names, changed files, test commands, and any frontend integration notes.
