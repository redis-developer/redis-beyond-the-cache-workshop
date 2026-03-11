---
id: CV9
title: Validation
status: blocked
depends_on:
  - CV1
  - CV2
  - CV3
  - CV4
  - CV5
  - CV6
  - CV7
  - CV8
owner: unassigned
allowed_files:
  - java-springboot/build.gradle.kts
  - java-springboot/buildSrc/**
  - scripts/validate-workshops.sh
forbidden_files:
  - workshop-frontend-shared/**
  - java-springboot/workshop-infrastructure/**
  - java-springboot/1_session_management_frontend/**
  - java-springboot/2_full_text_search_frontend/**
  - java-springboot/3_distributed_locks_frontend/**
  - java-springboot/4_agent_memory_frontend/**
  - scripts/new-workshop.sh
  - workshops.yaml
---

# Goal

Extend the standardization validator so the content-driven workshop-view contract cannot silently drift.

# Why this exists

- The repo already has a standardization check; the new content-driven layer needs the same protection.
- Without validator coverage, new workshops or migrations can backslide into large hardcoded instruction views.

# Required changes

- Update the validation path to check for the new content-driven structure.
- Validate the presence and minimum shape of workshop content files.
- Validate that scaffold output matches the new content-driven convention.
- Add at least one failure-mode check that proves the validator catches missing content structure or stale scaffold output.

# Acceptance criteria

- The repo still has one clear validation command.
- The command fails when the content-driven structure is missing or stale.
- Validation logic does not need to edit workshop-specific files.

# Verification

- `/bin/zsh -lc 'env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./java-springboot/gradlew -p java-springboot --no-daemon workshopStandardizationCheck'`
- In a temporary repo copy, remove one required workshop content file and run `bash scripts/validate-workshops.sh` to confirm the validator fails.

# Out of scope

- Fixing workshop content drift in this packet.
- Editing scaffold behavior directly.

# Handoff back

- Record the new validation checks added for the content-driven convention.
- Record any deferred follow-up checks that were intentionally not added.
