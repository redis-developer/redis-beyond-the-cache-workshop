# Index Template

```md
---
title: Packet Index
status: active
---

# Purpose

These packet files are the handoff contract for parallel subagent work. The boundary is file ownership, not theme.

# Ground rules

- Only edit files listed in `allowed_files`.
- If a needed change falls outside the packet, stop and record the blocker instead of editing adjacent code.
- No opportunistic cleanup.
- Every packet must record verification before handoff.
- Generated files belong to exactly one packet owner.

# Parallel lanes

1. Wave 1: list packets that can start immediately.
2. Wave 2: list packets that start after wave 1 dependencies finish.
3. Remaining order: list the final packets and gating rules.

# Packet summary

| ID | Title | Status | Depends on | Primary ownership |
| --- | --- | --- | --- | --- |
| P1 | Example | ready | - | `path/**` |
```

# Packet Template

```md
---
id: P4
title: Scaffold
status: ready
depends_on:
  - P1
  - P2
owner: unassigned
allowed_files:
  - path/to/owned/file
forbidden_files:
  - path/to/forbidden/file
---

# Goal

State the outcome in one sentence.

# Why this exists

- Explain the problem this packet isolates.

# Required changes

- List the concrete changes the packet owner must make.

# Acceptance criteria

- List observable outcomes.

# Verification

- List exact commands or manual checks.

# Out of scope

- Name adjacent areas the packet must not touch.

# Handoff back

- Record what the reviewer should hear back about.
```

# Dispatcher Prompt

```text
You are assigned one isolated work packet in the repository at <REPO_PATH>.

Before making changes, read these two files:
1. <INDEX_FILE>
2. <PACKET_FILE>

Treat the packet file as the contract for this task.

Rules:
- Edit only files listed in `allowed_files`.
- Do not edit files listed in `forbidden_files`.
- Do not edit any packet docs under `plans/packets/`.
- Do not do opportunistic cleanup or adjacent refactors.
- If your packet dependencies are not already complete, do not start implementation; reply with `Status: blocked` and name the unmet dependency.
- If you need a change outside the packet boundary, stop and report it as a blocker.
- Complete the packet end-to-end, including the verification described in the packet.

In your final response, use exactly this structure:

Status: done | blocked
Files changed:
- <path>
Verification run:
- <command or manual check>
Blockers/follow-ups:
- <item or "none">
```

# Repair Prompt

```text
You are assigned one isolated repair packet in the repository at <REPO_PATH>.

Before making changes, read these two files:
1. <INDEX_FILE>
2. <REPAIR_PACKET_FILE>

Treat the packet file as the contract for this task.

Rules:
- Edit only files listed in `allowed_files`.
- Do not edit files listed in `forbidden_files`.
- Do not edit any packet docs under `plans/packets/`.
- Do not do opportunistic cleanup or adjacent refactors.
- Reproduce the issue or validate the current gap before changing code.
- If you need a change outside the packet boundary, stop and report it as a blocker.
- Complete the packet end-to-end, including the verification described in the packet.

In your final response, use exactly this structure:

Status: done | blocked
Files changed:
- <path>
Verification run:
- <command or manual check>
Blockers/follow-ups:
- <item or "none">
```

# Review Checklist

- Confirm the packet dependencies were actually satisfied before work started.
- Confirm all changed files are inside the packet allowlist.
- Confirm forbidden files were not touched.
- Re-run or validate the packet verification steps.
- Check the acceptance criteria, not just test success.
- If incomplete, prefer creating a narrow repair packet over reopening the full packet.
- Update the packet board after review so the next wave uses current statuses.

# Split Heuristics

- Split by owned files first, by implementation idea second.
- Keep shared/generated artifacts under one owner.
- Put validation and scaffold work in separate packets from runtime behavior.
- Use repair packets for narrow follow-ups rather than reopening broad packets.
- If two packets need the same file, the split is not safe enough.
