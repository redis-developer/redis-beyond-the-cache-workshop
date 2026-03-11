---
title: Workshop-Owned Layout Packet Index
status: active
---

# Purpose

These packet files are the handoff contract for moving each workshop’s frontend and backend modules under one workshop-owned directory while preserving the existing split runtime model.

The target architecture is defined in:

- [layout-contract.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/plans/workshop-owned-layout/layout-contract.md)

# Ground rules

- Only edit files listed in `allowed_files`.
- If a needed change falls outside the packet, stop and record the blocker instead of editing adjacent code.
- No opportunistic cleanup.
- Keep Gradle project names, Docker service names, ports, and workshop URLs stable.
- Treat this as a path/layout migration, not a frontend/backend merge.
- Mixed-layout support must land before any workshop directory move starts.
- Only `WL7` owns `workshops.yaml`.
- Only `WL8` owns `scripts/new-workshop.sh`.
- `WL10` removes temporary compatibility code and runs last.

# Parallel lanes

1. Wave 1: `WL1`
2. Wave 2: `WL2`
3. Wave 3: `WL3`, `WL4`, `WL5`, `WL6`
4. Wave 4: `WL7`
5. Wave 5: `WL8`, `WL9`
6. Wave 6: `WL10`

Peak safe concurrency: 4 agents in Wave 3.

# Packet summary

| ID | Title | Status | Depends on | Primary ownership |
| --- | --- | --- | --- | --- |
| WL1 | Layout Contract | ready | - | `plans/workshop-owned-layout/**` |
| WL2 | Mixed-Layout Platform Support | blocked | `WL1` | `java-springboot/settings.gradle.kts`, hub path tooling, validator mixed-layout support |
| WL3 | Session Workshop Directory Migration | blocked | `WL1`, `WL2` | `java-springboot/1_session_management/**`, `java-springboot/1_session_management_frontend/**` |
| WL4 | Search Workshop Directory Migration | blocked | `WL1`, `WL2` | `java-springboot/2_full_text_search/**`, `java-springboot/2_full_text_search_frontend/**` |
| WL5 | Locks Workshop Directory Migration | blocked | `WL1`, `WL2` | `java-springboot/3_distributed_locks/**`, `java-springboot/3_distributed_locks_frontend/**` |
| WL6 | Agent Memory Workshop Directory Migration | blocked | `WL1`, `WL2` | `java-springboot/4_agent_memory/**`, `java-springboot/4_agent_memory_frontend/**` |
| WL7 | Registry Canonicalization | blocked | `WL3`, `WL4`, `WL5`, `WL6` | `workshops.yaml`, generated hub compose outputs |
| WL8 | Scaffold Update | blocked | `WL7` | `scripts/new-workshop.sh` |
| WL9 | Validation And Docs | blocked | `WL7` | `scripts/validate-workshops.sh`, root contributor docs |
| WL10 | Legacy Cleanup | blocked | `WL8`, `WL9` | mixed-layout fallback removal in shared path tooling |

# Notes

- `WL2` is the enabling packet. It must make the platform tolerate a partially migrated repo before the workshop move packets start.
- `WL3` through `WL6` are the main parallel wave. Each packet owns exactly one workshop.
- `WL7` centralizes the `workshops.yaml` path updates so the workshop packets do not collide on that file.
- `WL8` updates the scaffold only after the canonical path model is stable.
- `WL9` tightens validation and updates contributor-facing docs after the canonical path model is stable.
- `WL10` removes the temporary mixed-layout compatibility layer once the repo is fully migrated.
