---
id: P4
title: Session management migration
status: done
depends_on: ["P1", "P2", "P3"]
owner: unassigned
allowed_files: ["java-springboot/1_session_management_frontend/**", "java-springboot/1_session_management/src/main/resources/**", "java-springboot/1_session_management/src/test/**"]
forbidden_files: ["workshop-frontend-shared/**", "java-springboot/platform_control_plane/**", "scripts/**", "java-springboot/2_full_text_search_frontend/**", "java-springboot/3_distributed_locks_frontend/**", "java-springboot/4_agent_memory_frontend/**"]
---

# P4 Session Management Migration

## Goal

Migrate Session Management to the shared shell model as the first proof of the standardized workshop architecture.

## Why This Exists

Session Management is the clearest validation case because it needs dynamic session data, a learner app iframe, restart controls, and custom learning interaction.

## Required Changes

1. Replace custom shell level Vue views with shared shell usage.

2. Move instructions into Markdown first content.

3. Keep session specific interaction as a widget.

4. Keep learner application frontend and backend rebuildable independently.

5. Ensure shell restart and rebuild controls target the learner runtime.

## Acceptance Criteria

1. The workshop shell remains available during learner runtime restart and rebuild.

2. The learner app is visible inside the iframe when available.

3. Session id or equivalent dynamic information can appear inside Markdown.

4. The session specific learning widget works without taking ownership of shell infrastructure.

5. Existing learner flow is still recognizable.

## Verification

1. Run the Session Management frontend build.

2. Run focused Session Management backend tests if present.

3. Launch a local session and verify shell, iframe, restart, rebuild, and Redis Insight behavior.

## Out Of Scope

1. Migrating other workshops.

2. Changing shared shell internals.

3. Changing platform runtime API contract.

## Handoff Back

Return changed files, migration notes, manual test evidence, and any repair packet needed before broader migration.
