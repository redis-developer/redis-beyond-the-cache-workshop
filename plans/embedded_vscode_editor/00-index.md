---
title: Embedded VS Code Editor Packet Index
status: active
feature: embedded-vscode-editor
---

# Purpose

This packet set replaces the custom browser code editor with a session-scoped embedded VS Code experience while keeping lifecycle controls owned by the stable workshop shell.

# Architecture Decision

Use a browser VS Code server inside the session runner. The editor must be reachable only through the workshop frontend proxy at `/code/`. The learner edits files in the session workspace. The platform shell remains responsible for reset, restart, recompile, Redis Insight navigation, and hub navigation.

# Shared Contract

All implementation packets use this contract unless a repair packet changes it.

1. Public editor proxy path is `/code/`.
2. Local editor process binds only to `127.0.0.1`.
3. Default local editor port is `39000`.
4. Environment variables are `WORKSHOP_LOCAL_CODE_EDITOR_COMMAND`, `WORKSHOP_LOCAL_CODE_EDITOR_PORT`, and `WORKSHOP_LOCAL_CODE_EDITOR_HEALTH_PATH`.
5. The editor workspace root is `WORKSHOP_SESSION_WORKSPACE_PATH`.
6. The initial server must run without learner authentication inside the container because access is already session-scoped by the outer workshop route.
7. The first version must disable or avoid terminal exposure unless a later packet explicitly enables it with controls.

# Ground Rules

1. Edit only files listed in each packet `allowed_files`.
2. Do not edit files listed in `forbidden_files`.
3. Do not do opportunistic cleanup.
4. Do not change packet docs from inside a worker packet.
5. If a required change crosses packet ownership, stop and report the blocker.
6. Keep the existing custom editor available until the embedded VS Code path is verified.

# Waves

Wave 1 can start immediately because the packets have no file overlap.

1. `P1` packages and launches the VS Code server process in the session runner image and local runner script.
2. `P2` adds backend properties and lifecycle management for the editor process.
3. `P3` creates the shared frontend VS Code shell and shell-owned controls.
4. `P6` adds control-plane route WebSocket support for `/session/{sessionId}/code/`.

Wave 2 starts after `P2` and `P3`.

1. `P5` adds manager-side HTTP and WebSocket proxying for `/code/` after `P2` defines the editor properties.
2. `P7` migrates the current workshop editor pages to the shared VS Code shell after `P3`.

Wave 3 starts after `P1`, `P2`, `P3`, `P5`, `P6`, `P7`, and `R1`.

1. `R1` repairs SPA controller route coverage for the migrated `/editor` pages.
2. `P4` updates operations docs, validation notes, and final verification guidance.
3. `P8` updates workshop content copy and scaffold expectations if the implementation changes learner-facing editor language.

# Packet Summary

P1: Runner packaging and process defaults. Status: done. Owner: Plato. Depends on: none. Primary ownership: session runner image and launch scripts.

P2: Backend lifecycle. Status: done. Owner: Leibniz. Depends on: none. Primary ownership: session runner properties, process lifecycle, and tests.

P3: Shared frontend VS Code shell. Status: done. Owner: Carver. Depends on: none. Primary ownership: shared editor shell components and helpers.

P4: Docs and validation cleanup. Status: done. Owner: Anscombe. Depends on: P1, P2, P3, P5, P6, P7, R1. Primary ownership: documentation and validation scripts.

P5: Manager code proxy. Status: done. Owner: Erdos. Depends on: P2. Primary ownership: workshop infrastructure HTTP and WebSocket proxy.

P6: Public session WebSocket route proxy. Status: done. Owner: Kuhn. Depends on: none. Primary ownership: platform control plane route proxy.

P7: Workshop editor integrations. Status: done. Owner: Popper. Depends on: P3. Primary ownership: per-workshop editor views and frontend tests. Follow-up: R1 done.

P8: Content and scaffold copy. Status: done. Owner: Hilbert. Depends on: P7. Primary ownership: workshop content and scaffold validation.

R1: Editor route SPA controllers. Status: done. Owner: Aquinas. Depends on: P7. Primary ownership: non-session frontend SPA controller route mappings.

R2: Scaffold validator numbered views. Status: done. Owner: codex. Depends on: P8. Primary ownership: scaffold smoke validator route and content expectations.
