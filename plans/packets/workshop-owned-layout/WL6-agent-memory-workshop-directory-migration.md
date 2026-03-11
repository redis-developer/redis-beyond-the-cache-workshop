---
id: WL6
title: Agent Memory Workshop Directory Migration
status: blocked
depends_on:
  - WL1
  - WL2
owner: unassigned
allowed_files:
  - java-springboot/4_agent_memory/**
  - java-springboot/4_agent_memory_frontend/**
forbidden_files:
  - java-springboot/settings.gradle.kts
  - workshops.yaml
  - scripts/new-workshop.sh
  - scripts/validate-workshops.sh
  - java-springboot/workshop-hub/**
  - java-springboot/1_session_management/**
  - java-springboot/1_session_management_frontend/**
  - java-springboot/2_full_text_search/**
  - java-springboot/2_full_text_search_frontend/**
  - java-springboot/3_distributed_locks/**
  - java-springboot/3_distributed_locks_frontend/**
  - plans/packets/**
---

# Goal

Move workshop 4 into the canonical workshop-owned directory layout.

# Why this exists

- Workshop 4 has the richest workshop-local surface area: content views, frontend behavior, backend integration, and the AMS sidecar in standalone compose.

# Required changes

- Move the backend module to `java-springboot/4_agent_memory/backend`.
- Move the frontend module to `java-springboot/4_agent_memory/frontend`.
- Keep workshop-local docs and standalone compose at `java-springboot/4_agent_memory/`.
- Update workshop-local Dockerfiles, build files, compose paths, and local docs inside the workshop tree.
- Preserve runtime names, ports, and frontend/backend separation.

# Acceptance criteria

- All workshop 4 backend code lives under `java-springboot/4_agent_memory/backend`.
- All workshop 4 frontend code lives under `java-springboot/4_agent_memory/frontend`.
- Workshop-local compose and docs still work from the workshop root.
- No shared files outside the workshop-owned boundary are edited.

# Verification

- `/bin/zsh -lc 'env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./java-springboot/gradlew -p java-springboot --no-daemon :4_agent_memory_frontend:test --rerun-tasks --tests com.redis.workshop.memory.frontend.AgentMemoryFrontendIntegrationTest'`
- `/bin/zsh -lc 'env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./java-springboot/gradlew -p java-springboot --no-daemon :4_agent_memory:test'`
- `docker compose -f java-springboot/4_agent_memory/docker-compose.yml config --services`

# Out of scope

- Editing `workshops.yaml`
- Editing Gradle root settings
- Updating scaffold or validator behavior

# Handoff back

- Record the final workshop 4 directory tree.
- Record any workshop-local docs/compose changes needed because of the move.
