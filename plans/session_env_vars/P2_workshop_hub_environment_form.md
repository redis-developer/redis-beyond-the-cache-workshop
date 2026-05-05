---
id: P2
title: Workshop Hub Environment Form
status: done
depends_on: []
owner: worker
allowed_files:
  - frontend/src/components/workshop/WorkshopCard.vue
  - frontend/src/components/workshop/WorkshopGrid.vue
  - frontend/src/services/WorkshopService.js
  - frontend/src/store/index.js
forbidden_files:
  - java-springboot/platform_control_plane/**
  - java-springboot/platform_execution_plane/**
  - java-springboot/platform_contracts/**
---

# P2: Workshop Hub Environment Form

## Goal

Let a learner add optional environment variables before deploying a workshop session from the hub.

## Why This Exists

Spring AI Module 1 needs `OPENAI_API_KEY` available inside the session runner. The learner should be able to provide it at launch time without editing platform configuration.

## Required Changes

1. Add a compact environment variables UI on each `WorkshopCard` when there is no active session.
2. Support at least one simple default row for `OPENAI_API_KEY`, plus the ability to add or remove additional rows.
3. Use password input behavior for values so secrets are not visible by default.
4. Build a plain object map from non empty key and value rows.
5. Pass that map through Vuex `launchWorkshop` to `WorkshopService.createSession`.
6. Do not store submitted values in Vuex after launch completes or fails.
7. Keep the current blocked active session behavior unchanged.
8. Keep layout compact and scannable on desktop and mobile.

## Acceptance Criteria

1. Deploying without environment variables sends the same payload as before.
2. Deploying with `OPENAI_API_KEY` sends a create session payload containing `sessionEnvironment`.
3. Empty rows are ignored.
4. Values are cleared after a launch attempt.
5. The UI does not display submitted values in session cards after launch.

## Verification

Run:

```bash
npm --prefix frontend run build
```

## Out Of Scope

Backend validation is owned by P1. Do not add API fields outside the create session request.

## Handoff Back

Report changed files, the `sessionEnvironment` payload shape sent to `WorkshopService.createSession`, and the build result.
