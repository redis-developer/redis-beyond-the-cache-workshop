---
id: P7
title: Workshop editor integrations
status: done
depends_on: ["P3"]
owner: Popper
allowed_files: ["java-springboot/1_session_management_frontend/frontend/src/views/SessionEditor.vue", "java-springboot/1_session_management_frontend/frontend/src/utils/components.js", "java-springboot/1_session_management_frontend/frontend/src/router/index.js", "java-springboot/1_session_management_frontend/src/main/java/com/redis/workshop/session/frontend/infrastructure/SessionSpaController.java", "java-springboot/1_session_management_frontend/src/test/java/com/redis/workshop/session/frontend/SessionManagementFrontendIntegrationTest.java", "java-springboot/2_full_text_search_frontend/frontend/src/views/SearchEditor.vue", "java-springboot/2_full_text_search_frontend/frontend/src/utils/components.js", "java-springboot/2_full_text_search_frontend/frontend/src/router/index.js", "java-springboot/3_distributed_locks_frontend/frontend/src/views/LocksEditor.vue", "java-springboot/3_distributed_locks_frontend/frontend/src/utils/components.js", "java-springboot/3_distributed_locks_frontend/frontend/src/router/index.js", "java-springboot/4_agent_memory_frontend/frontend/src/views/MemoryEditor.vue", "java-springboot/4_agent_memory_frontend/frontend/src/utils/components.js", "java-springboot/4_agent_memory_frontend/frontend/src/router/index.js"]
forbidden_files: ["workshop-frontend-shared/src/**", "java-springboot/workshop-infrastructure/**", "java-springboot/session_runtime_tools/**", "java-springboot/platform_control_plane/**", "scripts/**"]
---

# Goal

Migrate the current workshop editor pages to the shared embedded VS Code shell.

# Why This Exists

The feature should standardize editor UX across workshops rather than only changing the session management workshop.

# Required Changes

1. Replace direct `WorkshopEditorLayout` usage in current workshop editor views with the shared VS Code shell from `P3`.
2. Preserve Back to Workshop, Redis Insight, and Back to Hub behavior for each workshop.
3. Preserve return route behavior where it already exists.
4. Keep current custom editor components available as fallback if the shared shell exposes a fallback slot.
5. Update per-workshop component exports and route tests as needed.

# Acceptance Criteria

1. Each current workshop editor route renders the same shared VS Code shell.
2. Session management `/4` still works under direct local route and session route base paths.
3. Back to Workshop works for each migrated workshop.
4. Redis Insight behavior is preserved for workshops that expose it.

# Verification

1. `./gradlew --no-daemon :1_session_management_frontend:build`
2. `./gradlew --no-daemon :2_full_text_search_frontend:build`
3. `./gradlew --no-daemon :3_distributed_locks_frontend:build`
4. `./gradlew --no-daemon :4_agent_memory_frontend:build`
5. Manual check after all packets land: open each editor route locally.

# Out Of Scope

1. Do not edit shared frontend components.
2. Do not edit runtime or proxy code.
3. Do not edit workshop instruction content.

# Handoff Back

Report which workshop editor routes were migrated, which controls are visible, and any workshop-specific behavior that could not be preserved.
