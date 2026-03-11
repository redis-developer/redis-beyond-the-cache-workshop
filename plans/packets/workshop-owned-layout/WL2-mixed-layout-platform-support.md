---
id: WL2
title: Mixed-Layout Platform Support
status: blocked
depends_on:
  - WL1
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

Make the platform tolerate a partially migrated repo where some workshops still use the legacy sibling layout and others already use the canonical workshop-owned layout.

# Why this exists

- The workshop move packets need to run in parallel without all editing shared path files.
- The repo must still build, generate compose, and validate while only some workshops have been moved.

# Required changes

- Update `settings.gradle.kts` so Gradle project names stay stable while project directories can resolve to either the legacy or canonical layout.
- Update shared hub path resolution so compose generation and frontend prebuild discovery can find workshop modules in either layout.
- Update validator logic so it can validate both layouts during the migration window without treating partially migrated workshops as broken.
- Keep the current repo fully working before any workshop is moved.

# Acceptance criteria

- A workshop can move to `java-springboot/<id>/backend` + `java-springboot/<id>/frontend` without immediately forcing all other workshops to move too.
- Gradle project names remain unchanged.
- Compose generation still works in a mixed-layout repo.
- Validation still works in a mixed-layout repo.

# Verification

- `/bin/zsh -lc 'env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./java-springboot/gradlew -p java-springboot --no-daemon :workshop-hub:test --tests com.redis.workshop.hub.tools.ComposeGeneratorTest'`
- `/bin/zsh -lc 'env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./java-springboot/gradlew -p java-springboot --no-daemon workshopStandardizationCheck'`
- Manual or temp-copy smoke check: move one workshop to the canonical layout without touching `workshops.yaml` and confirm compose generation / validation still resolve it.

# Out of scope

- Moving any workshop files.
- Updating canonical registry paths.
- Updating the scaffold to emit the new layout.

# Handoff back

- Record how mixed-layout project-dir/path resolution works.
- Record any temporary compatibility behavior that `WL10` must remove later.
