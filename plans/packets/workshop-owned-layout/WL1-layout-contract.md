---
id: WL1
title: Layout Contract
status: ready
depends_on: []
owner: unassigned
allowed_files:
  - plans/workshop-owned-layout/**
forbidden_files:
  - plans/packets/**
  - java-springboot/**
  - scripts/**
  - workshops.yaml
---

# Goal

Freeze the exact workshop-owned directory layout and migration rules before code changes begin.

# Why this exists

- The migration touches many shared path assumptions.
- The workshop move packets need one stable target layout so they do not invent incompatible folder shapes.

# Required changes

- Write the target layout contract under `plans/workshop-owned-layout/`.
- Record the canonical backend/frontend Dockerfile paths and Gradle module directory paths.
- Record the mixed-layout compatibility phase and the final cleanup phase.
- Record that project names, service names, ports, and URLs stay stable during the migration.

# Acceptance criteria

- There is one clear target layout for all workshops.
- The contract explicitly distinguishes temporary mixed-layout support from final canonical layout.
- The contract names which later packet owns shared hotspots such as `workshops.yaml`, `new-workshop.sh`, and `settings.gradle.kts`.

# Verification

- Manual review of `plans/workshop-owned-layout/layout-contract.md`.
- Manual check that the contract resolves the earlier ambiguity around whether this is a runtime merge or only a path/layout migration.

# Out of scope

- Editing any production code or build files.

# Handoff back

- Record the exact target directory tree.
- Record the canonical path conventions that later packets must implement.
