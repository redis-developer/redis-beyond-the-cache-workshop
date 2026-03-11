---
id: WL7
title: Registry Canonicalization
status: blocked
depends_on:
  - WL3
  - WL4
  - WL5
  - WL6
owner: unassigned
allowed_files:
  - workshops.yaml
  - java-springboot/workshop-hub/docker-compose.local.yml
  - java-springboot/workshop-hub/docker-compose.internal.yml
forbidden_files:
  - java-springboot/settings.gradle.kts
  - scripts/new-workshop.sh
  - scripts/validate-workshops.sh
  - java-springboot/workshop-hub/src/main/**
  - java-springboot/1_session_management/**
  - java-springboot/1_session_management_frontend/**
  - java-springboot/2_full_text_search/**
  - java-springboot/2_full_text_search_frontend/**
  - java-springboot/3_distributed_locks/**
  - java-springboot/3_distributed_locks_frontend/**
  - java-springboot/4_agent_memory/**
  - java-springboot/4_agent_memory_frontend/**
  - plans/packets/**
---

# Goal

Update the registry to use the canonical workshop-owned paths after all workshop directories have moved.

# Why this exists

- `workshops.yaml` is a shared hotspot and should be edited once, not by four parallel workshop packets.
- This is the point where the canonical nested backend/frontend paths become the repo’s official source of truth.

# Required changes

- Update `dockerfile`, `frontendDockerfile`, and `backendDockerfile` for all workshops to the canonical nested paths.
- Regenerate the hub compose outputs after the registry path updates.
- Do not change service names, ports, or URLs.

# Acceptance criteria

- `workshops.yaml` points only to `java-springboot/<id>/backend/...` and `java-springboot/<id>/frontend/...` paths.
- Generated hub compose files resolve the canonical nested paths.
- No mixed old/new registry paths remain.

# Verification

- `/bin/zsh -lc 'env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./java-springboot/gradlew -p java-springboot --no-daemon :workshop-hub:generateCompose'`
- `docker compose -f java-springboot/workshop-hub/docker-compose.local.yml --profile workshop-1_session_management config --services`
- `docker compose -f java-springboot/workshop-hub/docker-compose.local.yml --profile workshop-2_full_text_search config --services`
- `docker compose -f java-springboot/workshop-hub/docker-compose.local.yml --profile workshop-3_distributed_locks config --services`
- `docker compose -f java-springboot/workshop-hub/docker-compose.local.yml --profile workshop-4_agent_memory config --services`

# Out of scope

- Changing shared path resolution code
- Updating scaffold or validator behavior

# Handoff back

- Record the final canonical path pattern now used in `workshops.yaml`.
