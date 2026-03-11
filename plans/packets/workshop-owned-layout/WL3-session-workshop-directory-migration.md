---
id: WL3
title: Session Workshop Directory Migration
status: blocked
depends_on:
  - WL1
  - WL2
owner: unassigned
allowed_files:
  - java-springboot/1_session_management/**
  - java-springboot/1_session_management_frontend/**
forbidden_files:
  - java-springboot/settings.gradle.kts
  - workshops.yaml
  - scripts/new-workshop.sh
  - scripts/validate-workshops.sh
  - java-springboot/workshop-hub/**
  - java-springboot/2_full_text_search/**
  - java-springboot/2_full_text_search_frontend/**
  - java-springboot/3_distributed_locks/**
  - java-springboot/3_distributed_locks_frontend/**
  - java-springboot/4_agent_memory/**
  - java-springboot/4_agent_memory_frontend/**
  - plans/packets/**
---

# Goal

Move workshop 1 into the canonical workshop-owned directory layout.

# Why this exists

- Workshop-local file moves are the highest-volume part of the migration.
- This packet isolates one workshop so it can be moved independently of the others.

# Required changes

- Move the backend module to `java-springboot/1_session_management/backend`.
- Move the frontend module to `java-springboot/1_session_management/frontend`.
- Keep workshop-local docs and standalone compose at `java-springboot/1_session_management/`.
- Update workshop-local Dockerfiles, build files, compose paths, and local docs inside the workshop tree.
- Preserve runtime names, ports, and frontend/backend separation.

# Acceptance criteria

- All workshop 1 backend code lives under `java-springboot/1_session_management/backend`.
- All workshop 1 frontend code lives under `java-springboot/1_session_management/frontend`.
- Workshop-local compose and docs still work from the workshop root.
- No shared files outside the workshop-owned boundary are edited.

# Verification

- `/bin/zsh -lc 'env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./java-springboot/gradlew -p java-springboot --no-daemon :1_session_management_frontend:test'`
- `docker compose -f java-springboot/1_session_management/docker-compose.yml config --services`
- Manual check that workshop-local docs and compose paths reference the new folder structure where needed.

# Out of scope

- Editing `workshops.yaml`
- Editing Gradle root settings
- Updating scaffold or validator behavior

# Handoff back

- Record the final workshop 1 directory tree.
- Record any workshop-local docs/compose changes needed because of the move.
