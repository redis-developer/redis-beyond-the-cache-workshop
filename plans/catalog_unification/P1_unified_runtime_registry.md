---
id: P1
title: Unified Runtime Registry
status: done
depends_on: []
owner: codex-main
allowed_files:
  - workshops.yaml
  - java-springboot/platform_control_plane/src/main/resources/application.yaml
  - java-springboot/platform_control_plane/src/main/resources/release-catalog.yaml
  - java-springboot/platform_control_plane/src/main/resources/pilot-launch-rules.yaml
  - java-springboot/platform_control_plane/Dockerfile
  - java-springboot/platform_control_plane/build.gradle.kts
  - java-springboot/platform_control_plane/src/main/java/com/redis/workshop/platform/controlplane/catalog/**
  - java-springboot/platform_control_plane/src/main/java/com/redis/workshop/platform/controlplane/release/**
  - java-springboot/platform_control_plane/src/test/java/com/redis/workshop/platform/controlplane/catalog/**
  - java-springboot/platform_control_plane/src/test/java/com/redis/workshop/platform/controlplane/release/**
forbidden_files:
  - scripts/**
  - docs/**
---

# Goal

Make `workshops.yaml` the single runtime source for catalog metadata, release artifacts, and launch environment defaults.

# Required Changes

- Move release records from `release-catalog.yaml` into nested `workshops[].releases`.
- Move launch enablement and environments from `pilot-launch-rules.yaml` into each release.
- Update Java loaders/services to flatten nested releases into existing release model objects.
- Preserve public control-plane APIs and session launch behavior.

# Acceptance Criteria

- Hub catalog still returns default release version and supported modes.
- Session launch still gets release artifacts from the unified registry.
- Release default resolution respects `enabled` and `environments`.

# Verification

- `./gradlew --no-daemon :platform_control_plane:test --tests com.redis.workshop.platform.controlplane.catalog.* --tests com.redis.workshop.platform.controlplane.release.*`

# Out Of Scope

- Script validation.
- User-facing docs.

# Handoff Back

- Report changed Java/YAML files and any compatibility caveats.
