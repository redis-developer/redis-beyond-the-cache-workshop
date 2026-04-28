---
id: P2
title: Markdown content and dynamic context
status: done
depends_on: []
owner: unassigned
allowed_files: ["workshop-frontend-shared/src/content/**", "workshop-frontend-shared/src/components/WorkshopMarkdownRenderer.vue", "workshop-frontend-shared/src/components/WorkshopContentBlockRenderer.vue", "workshop-frontend-shared/src/components/WorkshopContentRenderer.vue", "workshop-frontend-shared/src/components/WorkshopContentWidget.vue", "workshop-frontend-shared/scripts/**"]
forbidden_files: ["java-springboot/1_session_management_frontend/**", "java-springboot/2_full_text_search_frontend/**", "java-springboot/3_distributed_locks_frontend/**", "java-springboot/4_agent_memory_frontend/**", "java-springboot/platform_control_plane/**"]
---

# P2 Markdown Content And Dynamic Context

## Goal

Define and implement Markdown first workshop content with safe dynamic placeholders.

## Why This Exists

Workshop authors should write most instructions in Markdown while still inserting runtime values such as session id, learner app URL, Redis Insight URL, and runtime status.

## Required Changes

1. Add a Markdown renderer or extend the existing content renderer to support Markdown first content.

2. Add a safe placeholder interpolator for values such as `{{ session.id }}` and `{{ links.learnerApp }}`.

3. Add validation for missing placeholders and unsupported placeholder paths.

4. Support widget declarations inside content using a constrained syntax.

5. Add renderer verification coverage using real or fixture content.

## Acceptance Criteria

1. Markdown content can include inline dynamic values.

2. Placeholder resolution is allowlisted and does not evaluate JavaScript.

3. Interpolated values are escaped by default.

4. Links with dynamic values are validated before rendering.

5. Widget declarations are parsed without hardcoding any one workshop.

## Verification

1. Run `npm run verify:content-renderer` if available.

2. Run the shared frontend build or nearest package build.

3. Add at least one fixture covering inline text, links, missing placeholders, and widget declarations.

## Out Of Scope

1. Runtime API implementation.

2. Workshop migrations.

3. Iframe layout and runtime toolbar.

## Handoff Back

Return the content format, placeholder contract, widget declaration format, verification commands, and any migration notes.
