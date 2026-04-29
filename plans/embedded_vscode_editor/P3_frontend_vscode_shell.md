---
id: P3
title: Shared frontend VS Code shell
status: done
depends_on: []
owner: Carver
allowed_files: ["workshop-frontend-shared/src/components/WorkshopCodeEditorFrame.vue", "workshop-frontend-shared/src/components/WorkshopCodeEditorShell.vue", "workshop-frontend-shared/src/components/WorkshopToolFrame.vue", "workshop-frontend-shared/src/components/WorkshopSessionRestartControls.vue", "workshop-frontend-shared/src/index.js", "workshop-frontend-shared/src/utils/basePath.js", "workshop-frontend-shared/src/composables/useWorkshopShellState.js"]
forbidden_files: ["java-springboot/session_runtime_tools/runner-image/**", "scripts/run-workshop.sh", "java-springboot/workshop-infrastructure/src/main/java/com/redis/workshop/infrastructure/**", "java-springboot/workshop-infrastructure/src/test/java/com/redis/workshop/infrastructure/**", "java-springboot/*_frontend/frontend/src/views/**", "java-springboot/*_frontend/frontend/src/router/**"]
---

# Goal

Create reusable shared frontend components for an embedded VS Code iframe while keeping platform controls outside the iframe.

# Why This Exists

Learners should edit code in a familiar VS Code interface, but runtime lifecycle controls must remain controlled by the shared workshop shell and survive editor reloads.

# Required Changes

1. Create a shared component that renders a same-origin `/code/` iframe.
2. Add shell-owned actions for Reset Code and Recompile App outside the iframe.
3. Add a shared helper for computing the editor proxy URL under direct local routes and `/session/{sessionId}` base paths.
4. Preserve a fallback slot or state for current custom editor rollout.
5. Export the new shared component and helper.

# Acceptance Criteria

1. The shared component renders an embedded frame pointed at the proxied code editor URL.
2. Reset Code asks for confirmation and calls the shell-owned restore endpoint.
3. Recompile App calls the existing session runner restart endpoint with rebuild enabled.
4. The component can be used by all workshop frontend modules without per-workshop duplicated control logic.

# Verification

1. `./gradlew --no-daemon :1_session_management_frontend:build`
2. Manual check after all packets land: open `http://localhost:38809/4`
3. Manual check after all packets land: click Reset Code and cancel, then confirm only in a disposable session.

# Out Of Scope

1. Do not edit per-workshop editor views.
2. Do not edit Java proxy or runner lifecycle classes.
3. Do not edit Dockerfiles or scripts.
4. Do not remove the current `CodeEditor.vue` until a later cleanup packet.

# Handoff Back

Report the frame URL generation strategy, shared controls, fallback behavior, and any visual regressions.
