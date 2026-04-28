---
id: R1
title: Stale Reference Repair
status: done
depends_on:
  - P2
owner: codex-main
allowed_files:
  - scripts/ops/common.sh
  - scripts/ops/pilot-launch-rules-check.sh
  - scripts/ops/pilot-preflight.sh
  - .github/workflows/release-catalog-ci.yml
  - .github/workflows/release-catalog-images.yml
  - .github/actions/release-catalog-image-build/action.yml
forbidden_files:
  - workshops.yaml
  - java-springboot/**
---

# Goal

Remove references to deleted standalone release and launch rule files from ops and workflow surfaces.

# Verification

- `rg --hidden -n "release-catalog.yaml|pilot-launch-rules.yaml|platform\\.controlplane\\.release\\.catalog\\.path|platform\\.controlplane\\.release\\.defaults\\.path" . --glob '!build/**' --glob '!**/build/**' --glob '!node_modules/**' --glob '!.git/**'`
- `bash scripts/validate-workshops.sh`
