---
id: CV4
title: Session Workshop Migration
status: blocked
depends_on:
  - CV1
  - CV2
  - CV3
owner: unassigned
allowed_files:
  - java-springboot/1_session_management_frontend/**
forbidden_files:
  - workshop-frontend-shared/**
  - java-springboot/workshop-infrastructure/**
  - java-springboot/2_full_text_search_frontend/**
  - java-springboot/3_distributed_locks_frontend/**
  - java-springboot/4_agent_memory_frontend/**
  - scripts/new-workshop.sh
  - scripts/validate-workshops.sh
---

# Goal

Move workshop 1 instructional content out of Vue templates and onto the shared content-driven path.

# Why this exists

- Workshop 1 is a representative stage-based workshop with stateful testing flow.
- It proves that standard narrative pages and test instructions can be maintained outside Vue while keeping session logic in code.

# Required changes

- Create workshop-owned content files for the instructional copy currently embedded in `SessionHome.vue` and `SessionEditor.vue`.
- Refactor the views to use the shared delivery/rendering path from `CV2` and `CV3`.
- Keep session state, progress tracking, API calls, and comparison logic in Vue.
- Preserve the current visual structure and workshop flow.

# Acceptance criteria

- Workshop 1 textual copy and step ordering live outside the Vue templates.
- `SessionHome.vue` and `SessionEditor.vue` become thin view controllers rather than large content templates.
- No workshop 1 behavior regresses in the existing frontend integration test.

# Verification

- `/bin/zsh -lc 'env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./java-springboot/gradlew -p java-springboot --no-daemon :1_session_management_frontend:test --rerun-tasks --tests com.redis.workshop.session.frontend.SessionManagementFrontendIntegrationTest'`

# Out of scope

- Changing shared renderer contracts.
- Editing any other workshop module.
- Scaffold or validator changes.

# Handoff back

- Record which workshop 1 views now load content externally.
- Record any workshop 1 behaviors that intentionally stayed code-backed.
