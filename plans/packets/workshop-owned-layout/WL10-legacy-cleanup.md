---
id: WL10
title: Legacy Cleanup
status: blocked
depends_on:
  - WL8
  - WL9
owner: unassigned
allowed_files:
  - java-springboot/settings.gradle.kts
  - java-springboot/workshop-hub/src/main/java/com/redis/workshop/hub/tools/ComposeGenerator.java
  - java-springboot/workshop-hub/src/test/java/com/redis/workshop/hub/tools/ComposeGeneratorTest.java
  - java-springboot/workshop-hub/scripts/prebuild-workshop-frontends.sh
  - scripts/validate-workshops.sh
forbidden_files:
  - workshops.yaml
  - scripts/new-workshop.sh
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

Remove the temporary mixed-layout compatibility layer after the repo is fully migrated.

# Why this exists

- `WL2` intentionally adds temporary fallback logic to unlock a parallel migration.
- Once the migration is complete, the platform should assume only the canonical layout.

# Required changes

- Remove old-layout fallback branches from root settings path mapping, compose generation path discovery, frontend prebuild discovery, and validation.
- Leave only the canonical workshop-owned layout support in shared path tooling.
- Keep tests and validator coverage aligned with the final canonical layout.

# Acceptance criteria

- Shared path tooling assumes only the canonical workshop-owned layout.
- No compatibility code for the old sibling layout remains.
- Validation and tests still pass under the final layout.

# Verification

- `/bin/zsh -lc 'env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./java-springboot/gradlew -p java-springboot --no-daemon :workshop-hub:test --tests com.redis.workshop.hub.tools.ComposeGeneratorTest'`
- `/bin/zsh -lc 'env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./java-springboot/gradlew -p java-springboot --no-daemon workshopStandardizationCheck'`

# Out of scope

- Moving workshop files
- Updating canonical registry paths
- Updating scaffold output

# Handoff back

- Record which temporary compatibility branches were removed.
- Confirm that only the canonical layout is supported now.
