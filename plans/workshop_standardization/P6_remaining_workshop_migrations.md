---
id: P6
title: Remaining workshop migrations
status: ready
depends_on: ["P4"]
owner: unassigned
allowed_files: ["java-springboot/2_full_text_search_frontend/**", "java-springboot/3_distributed_locks_frontend/**", "java-springboot/4_agent_memory_frontend/**", "java-springboot/2_full_text_search/src/main/resources/**", "java-springboot/3_distributed_locks/src/main/resources/**", "java-springboot/4_agent_memory/src/main/resources/**"]
forbidden_files: ["workshop-frontend-shared/**", "java-springboot/platform_control_plane/**", "scripts/**", "java-springboot/1_session_management_frontend/**"]
---

# P6 Remaining Workshop Migrations

## Goal

Move Full Text Search, Distributed Locks, and Agent Memory onto the shared shell after Session Management proves the model.

## Why This Exists

The standard only creates long term value if all workshops use the same shell and infrastructure patterns while keeping their learning experiences distinct.

## Required Changes

1. Migrate Full Text Search to shared shell and Markdown first content.

2. Migrate Distributed Locks to shared shell and Markdown first content.

3. Migrate Agent Memory to shared shell and Markdown first content.

4. Keep custom learning interactions as widgets.

5. Remove obsolete custom shell duplication from each migrated workshop.

## Acceptance Criteria

1. Each workshop uses the shared shell for instructions, runtime controls, Redis Insight, and learner app iframe.

2. Each workshop keeps its domain specific learner experience.

3. Existing app code remains rebuildable independently from the shell.

4. No migrated workshop deep imports shared frontend internals by relative path.

5. Route and content manifests follow the standard layout.

## Verification

1. Run frontend builds for each migrated workshop.

2. Run available backend tests for each migrated workshop.

3. Launch each workshop locally and verify shell, iframe, restart, rebuild, and Redis Insight behavior.

4. Run `scripts/validate-workshops.sh`.

## Out Of Scope

1. Changing shared shell internals.

2. Changing platform runtime API contract.

3. Changing scaffold behavior.

## Handoff Back

Return changed files per workshop, verification evidence, and any repair packets for workshop specific issues.
