---
title: Content-Driven Workshop Views Packet Index
status: active
---

# Purpose

These packet files are the handoff contract for moving workshop instruction copy out of Vue templates and into content files that non-Vue maintainers can edit safely.

The target architecture is:

- instructional copy lives in workshop-owned content files
- shared frontend code renders that content
- workshop Vue files keep only state, API calls, and interaction logic

# Ground rules

- Only edit files listed in `allowed_files`.
- If a needed change falls outside the packet, stop and record the blocker instead of editing adjacent code.
- No opportunistic cleanup.
- Keep dynamic behavior in Vue; only the instructional layer becomes content-driven.
- Use action IDs for interactive links/buttons instead of hardcoding behavior inside content.
- Generated or scaffolded content files belong to exactly one packet owner.

# Parallel lanes

1. Wave 1: `CV1`
2. Wave 2: `CV2`, `CV3`
3. Wave 3: `CV4`, `CV5`, `CV6`, `CV7`
4. Wave 4: `CV8`
5. Wave 5: `CV9`

# Packet summary

| ID | Title | Status | Depends on | Primary ownership |
| --- | --- | --- | --- | --- |
| CV1 | Content Contract | ready | - | `plans/content-driven-views/**` |
| CV2 | Shared Content Delivery | blocked | `CV1` | `java-springboot/workshop-infrastructure/**` |
| CV3 | Shared Content Renderer | blocked | `CV1` | `workshop-frontend-shared/**` |
| CV4 | Session Workshop Migration | blocked | `CV1`, `CV2`, `CV3` | `java-springboot/1_session_management_frontend/**` |
| CV5 | Search Workshop Migration | blocked | `CV1`, `CV2`, `CV3` | `java-springboot/2_full_text_search_frontend/**` |
| CV6 | Locks Workshop Migration | blocked | `CV1`, `CV2`, `CV3` | `java-springboot/3_distributed_locks_frontend/**` |
| CV7 | Agent Memory Workshop Migration | blocked | `CV1`, `CV2`, `CV3` | `java-springboot/4_agent_memory_frontend/**` |
| CV8 | Scaffold Update | blocked | `CV1`, `CV2`, `CV3`, `CV4`, `CV5`, `CV6`, `CV7` | `scripts/new-workshop.sh` |
| CV9 | Validation | blocked | `CV1`, `CV2`, `CV3`, `CV4`, `CV5`, `CV6`, `CV7`, `CV8` | `java-springboot/build.gradle.kts`, `java-springboot/buildSrc/**`, `scripts/validate-workshops.sh` |

# Notes

- `CV1` must freeze the content schema and the boundary between declarative content and code-backed behavior before implementation starts.
- `CV2` and `CV3` are intentionally separate so backend delivery and frontend rendering can proceed independently once the schema is stable.
- Each workshop migration packet owns one frontend module only. No workshop packet may edit `workshop-frontend-shared` or `workshop-infrastructure`.
- `CV8` and `CV9` run last so the scaffold and validator target the settled platform shape instead of an evolving intermediate state.
