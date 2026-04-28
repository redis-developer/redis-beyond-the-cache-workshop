---
id: P3
title: Documentation Cleanup
status: done
depends_on:
  - P1 schema shape
owner: unassigned
allowed_files:
  - README.md
  - docs/**
forbidden_files:
  - workshops.yaml
  - scripts/**
  - java-springboot/**
---

# Goal

Update documentation so maintainers understand that `workshops.yaml` owns both hub metadata and deployable release metadata.

# Required Changes

- Replace guidance that says release metadata lives in `release-catalog.yaml`.
- Explain nested `workshops[].releases`.
- Keep local Docker override guidance accurate.

# Acceptance Criteria

- Docs no longer tell maintainers to edit `release-catalog.yaml` for normal releases.
- The runtime model remains clear for local Docker and Cloud Run.

# Verification

- `rg -n "release-catalog.yaml|pilot-launch-rules.yaml|workshops.yaml" README.md docs`

# Out Of Scope

- Java and script changes.

# Handoff Back

- Report changed docs and any remaining intentional legacy references.
