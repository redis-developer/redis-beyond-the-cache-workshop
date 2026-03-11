---
id: CV5
title: Search Workshop Migration
status: blocked
depends_on:
  - CV1
  - CV2
  - CV3
owner: unassigned
allowed_files:
  - java-springboot/2_full_text_search_frontend/**
forbidden_files:
  - workshop-frontend-shared/**
  - java-springboot/workshop-infrastructure/**
  - java-springboot/1_session_management_frontend/**
  - java-springboot/3_distributed_locks_frontend/**
  - java-springboot/4_agent_memory_frontend/**
  - scripts/new-workshop.sh
  - scripts/validate-workshops.sh
---

# Goal

Move workshop 2 instructional content out of Vue templates and onto the shared content-driven path.

# Why this exists

- Workshop 2 is the clearest example of a multi-stage narrative-plus-implementation workshop.
- It exercises both stage pages and editor instructions without the complexity of workshop 4.

# Required changes

- Create workshop-owned content files for the instructional copy currently embedded in `SearchHome.vue` and `SearchEditor.vue`.
- Move test-step copy out of `SearchDemo.vue` when it fits the shared contract cleanly.
- Refactor the workshop 2 views to use the shared delivery/rendering path from `CV2` and `CV3`.
- Keep search state, API calls, and demo behavior in Vue.

# Acceptance criteria

- Workshop 2 stage copy and implementation steps live outside the Vue templates.
- The workshop continues to present the same stage flow and test flow.
- Existing workshop 2 frontend integration coverage passes without behavioral regression.

# Verification

- `/bin/zsh -lc 'env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./java-springboot/gradlew -p java-springboot --no-daemon :2_full_text_search_frontend:test --rerun-tasks --tests com.redis.workshop.search.frontend.FullTextSearchFrontendIntegrationTest'`

# Out of scope

- Changing shared renderer contracts.
- Editing any other workshop module.
- Scaffold or validator changes.

# Handoff back

- Record which workshop 2 views now load content externally.
- Record any test/demo instructions that intentionally remained in Vue and why.
