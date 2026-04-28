---
id: P5
title: Scaffold and validation
status: done
depends_on: ["P1", "P2", "P3"]
owner: unassigned
allowed_files: ["scripts/new-workshop.sh", "scripts/validate-workshops.sh", "java-springboot/workshop-hub/src/main/java/com/redis/workshop/hub/compose/**", "java-springboot/workshop-hub/src/test/**", "docs/**"]
forbidden_files: ["workshop-frontend-shared/**", "java-springboot/1_session_management_frontend/**", "java-springboot/2_full_text_search_frontend/**", "java-springboot/3_distributed_locks_frontend/**", "java-springboot/4_agent_memory_frontend/**"]
---

# P5 Scaffold And Validation

## Goal

Update the new workshop scaffold so authors get a runnable standardized workshop quickly.

## Why This Exists

The main business goal is faster workshop creation. A scaffold that produces a working shell, content file, learner app, backend, and validation baseline reduces setup cost.

## Required Changes

1. Generate shared shell wiring for new workshop frontends.

2. Generate Markdown first content with example dynamic placeholders.

3. Generate a basic learner app iframe target.

4. Generate backend and runtime configuration consistent with `workshops.yaml`.

5. Extend validation to detect nonstandard content layout, manifest id drift, missing shell wiring, and unsafe route duplication.

## Acceptance Criteria

1. A newly scaffolded workshop can run with the stable shell.

2. The scaffold includes a visible learner app iframe.

3. The scaffold includes restart and rebuild controls through the shared shell.

4. Validation fails when manifest ids drift from `workshops.yaml`.

5. Validation fails when required shell content files are missing.

## Verification

1. Run the scaffold script against a temporary workshop id.

2. Run compose generation if affected.

3. Run `scripts/validate-workshops.sh`.

4. Remove any temporary generated workshop before handoff unless explicitly kept.

## Out Of Scope

1. Migrating existing workshops.

2. Implementing shared shell internals.

3. Implementing platform runtime APIs.

## Handoff Back

Return scaffold commands, validation commands, changed files, and any manual steps still required for a new workshop.
