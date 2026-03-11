---
id: CV1
title: Content Contract
status: ready
depends_on: []
owner: unassigned
allowed_files:
  - plans/content-driven-views/content-contract.md
  - plans/content-driven-views/examples/**
forbidden_files:
  - java-springboot/**
  - workshop-frontend-shared/**
  - scripts/**
  - workshops.yaml
---

# Goal

Define the canonical content schema and authoring rules for content-driven workshop views before runtime work starts.

# Why this exists

- The repo currently embeds most instructional copy directly inside Vue templates.
- Multiple later packets need one stable contract for content files, actions, and view boundaries.
- Without a fixed content contract, shared renderer and workshop migrations will drift.

# Required changes

- Create a durable contract doc at `plans/content-driven-views/content-contract.md`.
- Define the content file layout and naming convention for workshop-owned content resources.
- Define the schema for narrative pages, stage-based pages, step lists, code snippets, alerts/callouts, and editor-step lists.
- Define how markdown is allowed and where raw HTML is forbidden.
- Define the action ID model for code-backed behaviors such as `openHub`, `openEditor`, `openRedisInsight`, `saveFile`, and auto-apply step actions.
- Define the boundary between declarative content and Vue code.
- Include at least three worked examples mapped from current views: one home page, one implementation page, and one editor page.

# Acceptance criteria

- One contract doc exists and is precise enough for independent backend/frontend implementation.
- The contract covers the current instruction patterns used by workshops 1 through 4.
- The contract explicitly identifies which current view behaviors stay in code.
- The contract includes at least one example for a standard narrative page and one example for an editor-driven page.

# Verification

- Manual check: map [SessionHome.vue](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/java-springboot/1_session_management_frontend/frontend/src/views/SessionHome.vue), [SearchHome.vue](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/java-springboot/2_full_text_search_frontend/frontend/src/views/SearchHome.vue), and [MemoryEditor.vue](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/java-springboot/4_agent_memory_frontend/frontend/src/views/MemoryEditor.vue) to the new schema without requiring raw Vue template authoring.

# Out of scope

- Implementing loaders, endpoints, renderers, or workshop migrations.
- Editing any existing workshop frontend module.

# Handoff back

- Record the content file location convention.
- Record the finalized action ID list.
- Record any current interaction patterns that still do not fit the contract cleanly.
