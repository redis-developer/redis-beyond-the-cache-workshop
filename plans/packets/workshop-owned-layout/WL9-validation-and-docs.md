---
id: WL9
title: Validation And Docs
status: blocked
depends_on:
  - WL7
owner: unassigned
allowed_files:
  - scripts/validate-workshops.sh
  - java-springboot/build.gradle.kts
  - README.md
  - AGENTS.md
  - DOCKER-SETUP.md
forbidden_files:
  - scripts/new-workshop.sh
  - workshops.yaml
  - java-springboot/settings.gradle.kts
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

Make the validator and top-level contributor docs enforce and describe the canonical workshop-owned layout.

# Why this exists

- After the move, stale sibling-layout paths should fail validation instead of silently lingering.
- Contributors need one top-level description of the new workshop layout.

# Required changes

- Update validation rules to require the canonical nested backend/frontend layout once the migration is complete.
- Keep `workshopStandardizationCheck` as the single entry point.
- Update top-level contributor docs to describe the new layout and workflow.

# Acceptance criteria

- Validation fails if a workshop reverts to the old sibling layout.
- Top-level docs no longer describe the old path model.
- The validation command stays the same.

# Verification

- `/bin/zsh -lc 'env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./java-springboot/gradlew -p java-springboot --no-daemon workshopStandardizationCheck'`
- In a temporary repo copy, revert one migrated workshop path to the legacy sibling layout and confirm `bash scripts/validate-workshops.sh` fails.

# Out of scope

- Updating scaffold logic
- Removing mixed-layout fallback code from shared path tooling

# Handoff back

- Record the new validator checks added for the canonical layout.
- Record any contributor-facing docs that were updated.
