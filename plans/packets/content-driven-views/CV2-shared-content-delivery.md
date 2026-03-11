---
id: CV2
title: Shared Content Delivery
status: blocked
depends_on:
  - CV1
owner: unassigned
allowed_files:
  - java-springboot/workshop-infrastructure/**
forbidden_files:
  - workshop-frontend-shared/**
  - java-springboot/1_session_management_frontend/**
  - java-springboot/2_full_text_search_frontend/**
  - java-springboot/3_distributed_locks_frontend/**
  - java-springboot/4_agent_memory_frontend/**
  - scripts/new-workshop.sh
  - scripts/validate-workshops.sh
---

# Goal

Make workshop frontend services load and expose content resources through shared infrastructure instead of per-workshop custom plumbing.

# Why this exists

- Workshop content should be authored per workshop, but delivered through one shared runtime path.
- The delivery contract must be reusable before individual workshop migrations begin.

# Required changes

- Add a shared model/loader for workshop content resources using the contract from `CV1`.
- Expose a generic runtime access path for frontend modules to fetch or load workshop content without workshop-specific controller logic.
- Handle missing or malformed content files with clear error behavior.
- Add focused tests for the shared loader and delivery path.
- Update shared runtime documentation if needed.

# Acceptance criteria

- Any workshop frontend module can place content resources in its own module and consume them through the shared runtime path.
- The implementation is contract-driven and does not hardcode workshop IDs.
- Shared tests cover at least one happy-path content load and one failure mode.

# Verification

- `/bin/zsh -lc 'env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./java-springboot/gradlew -p java-springboot --no-daemon :workshop-infrastructure:test'`

# Out of scope

- Shared Vue rendering.
- Editing any workshop frontend module.
- Scaffold or validation updates.

# Handoff back

- Record the delivery API or loader contract that workshop frontends must use.
- Record the failure behavior for missing or invalid content files.
