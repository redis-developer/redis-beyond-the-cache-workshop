---
id: P1
title: Local Runner Dynamic Ports
status: completed
depends_on: []
owner: codex-main
allowed_files:
  - scripts/run-workshop.sh
  - workshops.yaml
forbidden_files:
  - scripts/new-workshop.sh
  - scripts/validate-workshops.sh
  - README.md
  - AGENTS.md
  - docs/**
---

# Goal

Make direct local workshop runs allocate free frontend and backend ports without reading fixed ports from `workshops.yaml`.

# Acceptance Criteria

1. `workshops.yaml` has no `port`, `frontendPort`, or `backendPort` fields.
2. `scripts/run-workshop.sh up <workshop>` chooses free ports and prints the chosen URLs.
3. Restart, status, and down reuse the stored ports for that local run.

# Verification

1. `bash -n scripts/run-workshop.sh`
2. `bash scripts/validate-workshops.sh`
3. `bash scripts/run-workshop.sh up 1_session_management`
4. `bash scripts/run-workshop.sh status 1_session_management`
5. `bash scripts/run-workshop.sh down 1_session_management`
