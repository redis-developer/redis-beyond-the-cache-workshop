---
id: P1
title: Shared shell foundation
status: done
depends_on: []
owner: unassigned
allowed_files: ["workshop-frontend-shared/src/components/WorkshopShell.vue", "workshop-frontend-shared/src/components/WorkshopAppFrame.vue", "workshop-frontend-shared/src/components/WorkshopRuntimeToolbar.vue", "workshop-frontend-shared/src/composables/useWorkshopShellState.js", "workshop-frontend-shared/src/index.js"]
forbidden_files: ["java-springboot/1_session_management_frontend/**", "java-springboot/2_full_text_search_frontend/**", "java-springboot/3_distributed_locks_frontend/**", "java-springboot/4_agent_memory_frontend/**", "java-springboot/platform_control_plane/**", "scripts/**"]
---

# P1 Shared Shell Foundation

## Goal

Create the shared frontend shell used by all workshops.

The shell must stay alive while the learner application is restarted, rebuilt, unavailable, or broken.

## Why This Exists

Today each workshop owns too much shell behavior. This makes new workshops slower to create and lets route handling, runtime buttons, and content loading drift.

## Required Changes

1. Add a shared `WorkshopShell.vue` component.

2. Add a shared `WorkshopAppFrame.vue` component for iframe rendering and learner application availability states.

3. Add a shared `WorkshopRuntimeToolbar.vue` component for restart, rebuild, Redis Insight, status, and navigation actions.

4. Add a shell state composable that keeps shell level state independent from learner application state.

5. Export the new shell pieces from the shared frontend package.

## Acceptance Criteria

1. The shell can render static instructional content and an iframe in the same layout.

2. The iframe can show loading, available, unavailable, rebuilding, and error states.

3. Runtime actions are represented as props or events and are not hardcoded to local Docker or Cloud Run.

4. The shell has a widget slot or registry integration point but does not implement workshop specific widgets.

5. Existing shared components are not removed.

## Verification

1. Run the shared frontend build or the nearest package build.

2. Run existing frontend tests if available.

3. Manually inspect the component API for runtime agnosticism.

## Out Of Scope

1. Migrating any workshop to the new shell.

2. Implementing Markdown interpolation.

3. Changing backend runtime APIs.

4. Changing scaffold scripts.

## Handoff Back

Return changed files, component API summary, verification commands, and any required follow up contracts for the migration packets.
