---
id: WL8
title: Scaffold Update
status: blocked
depends_on:
  - WL7
owner: unassigned
allowed_files:
  - scripts/new-workshop.sh
forbidden_files:
  - workshops.yaml
  - scripts/validate-workshops.sh
  - java-springboot/**
  - plans/packets/**
---

# Goal

Make new workshops start in the canonical workshop-owned layout instead of the legacy sibling layout.

# Why this exists

- The migration only sticks if the scaffold emits the final layout by default.
- New workshops should not require immediate follow-up moves or path cleanup.

# Required changes

- Update `new-workshop.sh` to generate `java-springboot/<id>/backend` and `java-springboot/<id>/frontend`.
- Preserve the current project naming convention:
  - backend project name remains `<id>`
  - frontend project name remains `<id>_frontend`
- Update generated registry snippets, Dockerfile paths, source mounts, and output text to the canonical layout.

# Acceptance criteria

- A newly scaffolded workshop uses the workshop-owned directory layout immediately.
- Generated instructions do not refer to the old sibling layout.
- Generated registry fields and Dockerfile paths match the canonical contract.

# Verification

- In a temporary repo copy, run `bash scripts/new-workshop.sh 99_layout_smoke "Layout Smoke" layout-smoke 8098 18098`
- Manual check: confirm the scaffold creates `java-springboot/99_layout_smoke/backend` and `java-springboot/99_layout_smoke/frontend`
- Manual check: confirm the scaffold output references the canonical layout and compose generation flow

# Out of scope

- Tightening validation logic
- Moving existing workshops

# Handoff back

- Record the generated directory tree for the sample workshop.
- Record any manual follow-up steps that remain after scaffolding.
