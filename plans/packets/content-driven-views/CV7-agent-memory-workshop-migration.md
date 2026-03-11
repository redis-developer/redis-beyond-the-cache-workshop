---
id: CV7
title: Agent Memory Workshop Migration
status: blocked
depends_on:
  - CV1
  - CV2
  - CV3
owner: unassigned
allowed_files:
  - java-springboot/4_agent_memory_frontend/**
forbidden_files:
  - workshop-frontend-shared/**
  - java-springboot/workshop-infrastructure/**
  - java-springboot/1_session_management_frontend/**
  - java-springboot/2_full_text_search_frontend/**
  - java-springboot/3_distributed_locks_frontend/**
  - scripts/new-workshop.sh
  - scripts/validate-workshops.sh
---

# Goal

Move as much of workshop 4 instructional content as possible out of Vue templates while keeping the complex demo/editor behavior code-backed.

# Why this exists

- Workshop 4 is the hardest case and determines whether the content-driven model works broadly instead of only for simpler workshops.
- It mixes narrative pages, editor steps, test instructions, and custom chat/demo behavior.

# Required changes

- Create workshop-owned content files for the instructional copy currently embedded in `MemoryHome.vue`, `MemoryLearn.vue`, `MemoryChallenges.vue`, `MemoryLab.vue`, `MemoryEditor.vue`, and any test-step copy that fits the contract in `MemoryChat.vue` or `MemoryDemo.vue`.
- Refactor the views to use the shared delivery/rendering path from `CV2` and `CV3`.
- Keep apply-step mutations, chat/test state, progress tracking, and API calls in Vue.
- Preserve the current workshop route flow and visual structure.

# Acceptance criteria

- Workshop 4 narrative copy and editor/test instructions that fit the contract live outside the Vue templates.
- The remaining Vue code is clearly behavior-oriented rather than copy-oriented.
- Existing workshop 4 frontend integration coverage passes.

# Verification

- `/bin/zsh -lc 'env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./java-springboot/gradlew -p java-springboot --no-daemon :4_agent_memory_frontend:test --rerun-tasks --tests com.redis.workshop.memory.frontend.AgentMemoryFrontendIntegrationTest'`

# Out of scope

- Changing shared renderer contracts.
- Editing any other workshop module.
- Scaffold or validator changes.

# Handoff back

- Record which workshop 4 views now load content externally.
- Record which workshop 4 behaviors intentionally remained code-backed and why.
