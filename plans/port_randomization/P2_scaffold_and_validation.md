---
id: P2
title: Scaffold And Validation
status: completed
depends_on:
  - P1 schema shape
owner: Lorentz
allowed_files:
  - scripts/new-workshop.sh
  - scripts/validate-workshops.sh
forbidden_files:
  - scripts/run-workshop.sh
  - workshops.yaml
  - README.md
  - AGENTS.md
  - docs/**
---

# Goal

Remove fixed workshop ports from scaffold generation and registry validation.

# Required Changes

1. `scripts/new-workshop.sh` should accept `<id> "<title>" <serviceName>` only.
2. Generated backend and frontend app defaults can keep internal fallback ports in `application.properties`, but those values must not be written to `workshops.yaml`.
3. `scripts/validate-workshops.sh` should not require or validate registry `port`, `frontendPort`, or `backendPort`.

# Verification

1. `bash scripts/validate-workshops.sh`
2. `bash -n scripts/new-workshop.sh scripts/validate-workshops.sh`
