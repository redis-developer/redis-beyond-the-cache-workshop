---
id: CV6
title: Locks Workshop Migration
status: blocked
depends_on:
  - CV1
  - CV2
  - CV3
owner: unassigned
allowed_files:
  - java-springboot/3_distributed_locks_frontend/**
forbidden_files:
  - workshop-frontend-shared/**
  - java-springboot/workshop-infrastructure/**
  - java-springboot/1_session_management_frontend/**
  - java-springboot/2_full_text_search_frontend/**
  - java-springboot/4_agent_memory_frontend/**
  - scripts/new-workshop.sh
  - scripts/validate-workshops.sh
---

# Goal

Move workshop 3 instructional content out of Vue templates and onto the shared content-driven path.

# Why this exists

- Workshop 3 includes narrative, implementation steps, and review-only guidance.
- It validates that content-driven views can handle multi-route instructional flows and mixed editable/read-only guidance.

# Required changes

- Create workshop-owned content files for the instructional copy currently embedded in `LocksHome.vue`, `LocksImplement.vue`, and `LocksEditor.vue`.
- Refactor those views to use the shared delivery/rendering path from `CV2` and `CV3`.
- Keep lock status checks, route flow, and demo behavior in Vue.
- Preserve the current read-only review treatment for `PurchaseService.java`.

# Acceptance criteria

- Workshop 3 narrative and implementation instructions live outside the Vue templates.
- Workshop 3 still exposes the same route flow and verification behavior.
- Existing workshop 3 frontend integration coverage passes.

# Verification

- `/bin/zsh -lc 'env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./java-springboot/gradlew -p java-springboot --no-daemon :3_distributed_locks_frontend:test --rerun-tasks --tests com.redis.workshop.locks.frontend.DistributedLocksFrontendIntegrationTest'`

# Out of scope

- Changing shared renderer contracts.
- Editing any other workshop module.
- Scaffold or validator changes.

# Handoff back

- Record which workshop 3 views now load content externally.
- Record any remaining review-only rendering behavior that stayed code-backed.
