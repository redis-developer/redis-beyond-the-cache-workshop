---
name: packet-orchestration
description: Plan and operate project-agnostic parallel feature work using atomic file-owned packets, dependency waves, handoff prompts, and repair packets. Use when Codex needs to split any feature, migration, refactor, or platform change into independent subagent tasks, define packet contracts, decide which packets can run in parallel, generate handoff prompts, review completed packets, or create narrow follow-up repair packets.
---

# Packet Orchestration

Use this skill to run multi-agent work without merge thrash. Treat packets as file-owned contracts, not theme buckets.

Use `packet`, `work packet`, or `workstream` as the unit name. Keep the workflow the same even if the repo uses different language.

## Core Rules

- Make file ownership the hard boundary.
- Put each hotspot file under exactly one packet owner.
- Give generated artifacts a single packet owner even if multiple packets depend on them.
- Stop and create a dependency or repair packet instead of crossing packet boundaries.
- Judge packet success against its contract, not against a vague feature summary.

## Workflow

### 1. Build the packet graph

- Inspect the repo hotspots and dependency order before naming packets.
- Split work so two agents can code in parallel on the same branch without editing the same files.
- Prefer one packet per ownership boundary: API surface, generator, migration, validator, frontend slice, backend slice, deployment artifact, or repair.
- If a packet would require multiple agents to touch the same file, split it again.

Useful split axes:

- By layer: frontend, backend, infra, docs, validation
- By bounded area: service A, service B, package X, package Y
- By artifact ownership: generator, generated output, scaffold, validator
- By rollout dependency: schema first, runtime second, cleanup last
- By repair scope: one file or one tiny hotspot cluster

### 2. Write packet contracts

- Pick one control-plane directory for the repo. Default to `plans/packets/`, but any stable location works.
- Keep the index/status board in one file, usually `00-index.md`.
- Keep one markdown contract per packet in the same directory.
- Use frontmatter with `id`, `title`, `status`, `depends_on`, `owner`, `allowed_files`, and `forbidden_files`.
- Use body sections for `Goal`, `Why this exists`, `Required changes`, `Acceptance criteria`, `Verification`, `Out of scope`, and `Handoff back`.
- Use the templates in [references/templates.md](references/templates.md).

### 3. Dispatch in waves

- Start only packets whose dependencies are actually complete.
- Group packets by zero file overlap, not by conceptual similarity.
- Hand each subagent exactly two docs: the index file and the packet file.
- Use the dispatcher prompt from [references/templates.md](references/templates.md).

### 4. Review against the contract

- Check packet-boundary compliance first.
- Then check acceptance criteria and verification evidence.
- Reproduce the claimed verification when practical.
- Report findings first, ordered by severity, with concrete file references.
- If there are no findings, say so and note residual risks or testing gaps.

### 5. Repair narrowly

- When a packet lands with a narrow gap, create a repair packet `R#-...` instead of reopening the full packet.
- Make repair packets smaller than the packet they fix.
- Keep repair ownership as tight as possible: one file or one tiny cluster of files when feasible.
- Update the packet board after each review so the next wave is based on current reality.

## Packet Design Rules

- Atomic means independent on the same branch, not merely conceptually separate.
- Shared/generated files belong to one packet owner only.
- Prefer one product slice, service slice, or directory pair per packet when possible.
- Acceptance criteria must be observable.
- Verification must name exact commands or manual checks.
- If a packet depends on forbidden files changing first, mark it blocked instead of guessing.

Signs a packet is not atomic enough:

- Two agents would need to edit the same file.
- One packet mixes platform work and product work.
- A packet touches both a generator and its generated outputs when another packet also needs either.
- “While I’m here” cleanup is needed to finish it.
- The reviewer cannot tell what success means without reading unrelated code.

## Status Conventions

- Use `ready`, `blocked`, `in_progress`, and `done` for the normal lifecycle.
- Use `implemented_with_findings` or `done_with_followup` only after review when code landed but repair packets remain.
- Repair packets use `R#` ids and should be independently dispatchable.

## Adapting to a New Repo

- If the repo has no packet framework yet, create:
  - a control-plane directory such as `plans/packets/`
  - `00-index.md`
  - `_TEMPLATE.md`
  - one file per packet
- If packets already exist, refresh statuses and dependencies before proposing the next wave.

When adapting:

- Pick one directory convention and keep it stable.
- Pick one unit name and keep it stable.
- Decide who owns generated files before dispatching.
- Decide which validations are per packet versus whole-repo.
- Make packet contracts strict enough that a reviewer can reject drift without debate.

## Resources

- [references/templates.md](references/templates.md): index template, packet contract template, dispatcher prompt, repair prompt, and review checklist.
