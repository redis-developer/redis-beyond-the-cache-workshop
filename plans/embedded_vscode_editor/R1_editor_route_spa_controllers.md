---
id: R1
title: Editor route SPA controllers
status: done
depends_on: ["P7"]
owner: Aquinas
allowed_files: ["java-springboot/2_full_text_search_frontend/src/main/java/com/redis/workshop/search/frontend/infrastructure/SearchSpaController.java", "java-springboot/2_full_text_search_frontend/src/test/java/com/redis/workshop/search/frontend/FullTextSearchFrontendIntegrationTest.java", "java-springboot/3_distributed_locks_frontend/src/main/java/com/redis/workshop/locks/frontend/infrastructure/LocksSpaController.java", "java-springboot/3_distributed_locks_frontend/src/test/java/com/redis/workshop/locks/frontend/DistributedLocksFrontendIntegrationTest.java", "java-springboot/4_agent_memory_frontend/src/main/java/com/redis/workshop/memory/frontend/infrastructure/MemorySpaController.java", "java-springboot/4_agent_memory_frontend/src/test/java/com/redis/workshop/memory/frontend/AgentMemoryFrontendIntegrationTest.java"]
forbidden_files: ["workshop-frontend-shared/src/**", "java-springboot/*_frontend/frontend/src/**", "java-springboot/workshop-infrastructure/**", "java-springboot/session_runtime_tools/**", "java-springboot/platform_control_plane/**", "scripts/**"]
---

# Goal

Make the migrated embedded editor route reachable in every current workshop frontend.

# Why This Exists

`P7` migrated workshop editor Vue views, but non-session workshop SPA controllers do not consistently forward `/editor` to `index.html`. That causes frontend module build and route smoke tests to fail.

# Required Changes

1. Add `/editor` to the full-text search, distributed locks, and agent memory SPA controller route mappings when missing.
2. Update or preserve route tests so `/editor` forwards to `index.html` in those modules.
3. Do not change the Vue editor integration from `P7`.

# Acceptance Criteria

1. `/editor` forwards to `index.html` for search, locks, and memory frontends.
2. Existing route mappings continue to work.
3. The four frontend modules from `P7` build successfully.

# Verification

1. `./gradlew --no-daemon :1_session_management_frontend:build :2_full_text_search_frontend:build :3_distributed_locks_frontend:build :4_agent_memory_frontend:build`

# Out Of Scope

1. Do not edit shared frontend components.
2. Do not edit workshop Vue views.
3. Do not edit runtime or proxy code.

# Handoff Back

Report which SPA controllers were updated and whether the four frontend build command passes.
