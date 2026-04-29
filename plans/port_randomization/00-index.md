---
title: Port Randomization Packets
status: completed
---

# Purpose

Stop treating workshop registry ports as public runtime configuration. Local maintainer runs should allocate free ports at startup, while platform sessions continue to route through the session manager and proxy.

# Packet Summary

| ID | Title | Status | Depends on | Primary ownership |
| --- | --- | --- | --- | --- |
| P1 | Local Runner Dynamic Ports | completed | none | `scripts/run-workshop.sh`, `workshops.yaml` |
| P2 | Scaffold And Validation | completed | P1 schema shape | `scripts/new-workshop.sh`, `scripts/validate-workshops.sh` |
| P3 | Documentation Cleanup | completed | P1 schema shape | `README.md`, `AGENTS.md`, `docs/**` |

# Schema Decision

`workshops.yaml` no longer requires `port`, `frontendPort`, or `backendPort`. Local direct runs allocate ports and store them under the local state directory for restart, status, and down commands.

# Verification

1. `bash scripts/validate-workshops.sh`
2. `git diff --check`
3. `bash scripts/run-workshop.sh up 1_session_management`
4. `curl -sS -i http://localhost:<allocated-frontend-port>/welcome`
5. `bash scripts/run-workshop.sh down 1_session_management`
