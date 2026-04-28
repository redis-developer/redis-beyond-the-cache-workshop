---
id: P2
title: Validator And Scripts
status: done
depends_on:
  - P1 schema shape
owner: unassigned
allowed_files:
  - scripts/validate-workshops.sh
  - scripts/new-workshop.sh
  - scripts/ci/**
  - scripts/ops/**
  - scripts/loadtest/**
  - .github/workflows/release-catalog-ci.yml
  - .github/workflows/release-catalog-images.yml
  - .github/actions/release-catalog-image-build/action.yml
forbidden_files:
  - workshops.yaml
  - java-springboot/platform_control_plane/src/main/java/**
  - java-springboot/platform_control_plane/src/test/java/**
  - docs/**
---

# Goal

Align repository validation and automation scripts with nested `workshops[].releases`.

# Required Changes

- Validate release entries from `workshops.yaml`.
- Stop requiring standalone `release-catalog.yaml` as the source of deployable images.
- Update scaffold generation to include a default nested release block when needed.
- Preserve existing workshop structure checks.

# Acceptance Criteria

- `bash scripts/validate-workshops.sh` passes.
- Scripts no longer document or depend on standalone release catalog for release image discovery unless explicitly maintained as a legacy compatibility path.

# Verification

- `bash scripts/validate-workshops.sh`

# Out Of Scope

- Java runtime behavior.
- Docs prose outside scripts.

# Handoff Back

- Report changed script files and validation command output.
