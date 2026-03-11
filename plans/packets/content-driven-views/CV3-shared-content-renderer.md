---
id: CV3
title: Shared Content Renderer
status: blocked
depends_on:
  - CV1
owner: unassigned
allowed_files:
  - workshop-frontend-shared/**
forbidden_files:
  - java-springboot/workshop-infrastructure/**
  - java-springboot/1_session_management_frontend/**
  - java-springboot/2_full_text_search_frontend/**
  - java-springboot/3_distributed_locks_frontend/**
  - java-springboot/4_agent_memory_frontend/**
  - scripts/new-workshop.sh
  - scripts/validate-workshops.sh
---

# Goal

Add shared frontend renderers that can display workshop content files without requiring each workshop to hand-author large Vue templates.

# Why this exists

- The maintainability problem is primarily in the workshop view layer.
- Shared renderers must exist before workshop-specific migrations can replace embedded HTML.

# Required changes

- Implement shared renderers/components for the content contract from `CV1`.
- Support at least: headings/body copy, markdown blocks, alerts/callouts, code blocks, standard step lists, editor-step lists, and action links/buttons.
- Expose a clear API for workshops to bind action IDs to code-backed handlers.
- Add any package-local smoke harness or tests needed to verify rendering behavior.
- Export the new shared renderer surface through `workshop-frontend-shared/src/index.js`.

# Acceptance criteria

- A workshop can render instructional content from structured files without embedding large raw instruction HTML blocks in the view.
- The shared API is generic and not workshop-specific.
- The renderer supports both narrative pages and editor-style instruction panels.

# Verification

- Add and run a package-local smoke verification inside `workshop-frontend-shared`, and record the exact command used.
- Manual check: confirm the shared renderer can represent one standard stage page and one editor step list from the `CV1` examples.

# Out of scope

- Delivery/runtime infrastructure.
- Editing any workshop frontend module.
- Scaffold or validation updates.

# Handoff back

- Record the shared component/API surface workshops should consume.
- Record any content block types deferred to a follow-up packet.
