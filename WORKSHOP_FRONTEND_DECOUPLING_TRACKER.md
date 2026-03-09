# Workshop Frontend Decoupling Tracker

## Goal
Split each workshop into two Spring Boot services without changing the visual design of existing frontends:

1. Backend service for workshop demo/business APIs.
2. Frontend service for SPA hosting, editor/file operations, and backend API proxying.

## Rules Of Execution
- This file is the execution contract for the migration.
- A phase is not complete until all checklist items and Definition of Done are satisfied.
- Start of each new phase requires explicit user sign-off in the Sign-off Log.
- Every completed task must have evidence (PR/commit/test command/result/notes).
- Open risks must be tracked and either mitigated or accepted before final completion.

## Status Board

| Phase | Name | Status | Owner | Notes |
|---|---|---|---|---|
| 0 | Tracker + Baseline | DONE | Codex | Tracker created |
| 1 | Schema + Hub Contracts | DONE | Codex | Completed on 2026-03-09 |
| 2 | Compose + Lifecycle Dual Services | TODO | Codex |  |
| 3 | Shared Frontend Runtime Module | TODO | Codex |  |
| 4 | Pilot Migration (2_full_text_search) | TODO | Codex |  |
| 5 | API Path Normalization (All Workshops) | TODO | Codex |  |
| 6 | Remaining Workshop Migrations | TODO | Codex |  |
| 7 | Docs + Scaffold + Final Validation | TODO | Codex |  |

Status values: `TODO`, `IN_PROGRESS`, `BLOCKED`, `DONE`.

---

## Phase 0: Tracker + Baseline

### Checklist
- [x] Create persistent migration tracker in repository.
- [x] Record baseline architecture snapshot links/notes.
- [x] Record initial risk register entries.

### Definition Of Done
- Tracker exists in repo.
- Baseline notes are captured.
- Phase 1 scope is unambiguous.

### Evidence
- 2026-03-09: `WORKSHOP_FRONTEND_DECOUPLING_TRACKER.md` created.
- 2026-03-09: Baseline snapshot and risk register initialized.

### Baseline Snapshot (2026-03-09)
- One workshop service per workshop in registry (`serviceName`, `port`, `dockerfile`).
- Compose generator emits one service per workshop plus infrastructure.
- Workshop manager lifecycle commands target one service name per workshop.
- Workshop proxy maps `/workshop/{id}/**` to one port target.
- Frontend/editor concerns are bundled in workshop backend service.
- Some workshop frontends still use absolute `/api/...` calls.

---

## Phase 1: Schema + Hub Contracts

### Scope
Extend workshop metadata and hub contracts to model separate frontend/backend workshop services.

### Checklist
- [x] Extend `workshops.yaml` schema for frontend/backend service metadata.
- [x] Update `Workshop` model and parsing logic for new fields.
- [x] Keep backward compatibility where possible.
- [x] Define canonical workshop URL behavior (frontend URL only).
- [x] Add/update validation tests for registry parsing.

### Definition Of Done
- Hub can load workshop entries with dual-service metadata.
- Existing workshop list endpoint returns required data for UI and manager.
- No regression in current workshop listing behavior.

### Evidence
- Working tree changes:
  - `workshops.yaml`
  - `java-springboot/workshop-hub/src/main/java/com/redis/workshop/hub/model/Workshop.java`
  - `java-springboot/workshop-hub/src/test/java/com/redis/workshop/hub/model/WorkshopRegistryParsingTest.java`
- Test command:
  - `./gradlew -p java-springboot -PskipFrontendBuild=true :workshop-hub:test --tests com.redis.workshop.hub.model.WorkshopRegistryParsingTest`
- Results:
  - `BUILD SUCCESSFUL`
  - `WorkshopRegistryParsingTest`: 2 tests, 0 failures, 0 errors.

### Risks
- Contract mismatch between existing frontend expectations and new fields.

### Completion Notes
- Legacy `serviceName`/`port`/`dockerfile` fields remain intact.
- Dual-service metadata is now present in the registry and model.
- `url` remains the canonical frontend URL.

---

## Phase 2: Compose + Lifecycle Dual Services

### Scope
Generate and operate two services per workshop in compose and manager lifecycle.

### Checklist
- [ ] Update compose generator to emit frontend + backend workshop services.
- [ ] Ensure profiles include both services under each `workshop-<id>`.
- [ ] Update manager start/stop/restart flows for dual services.
- [ ] Update status aggregation logic (workshop is healthy only when required services are healthy).
- [ ] Validate local and internal compose outputs.

### Definition Of Done
- `generateCompose` produces correct dual-service configs.
- Hub manager can start/stop/restart a workshop as one logical unit.
- Status endpoint correctly reflects aggregated workshop health.

### Evidence
- [ ] Link commit(s):
- [ ] Test command(s):
- [ ] Results:

### Risks
- Partial-up states can mislead UI if aggregation is wrong.

---

## Phase 3: Shared Frontend Runtime Module

### Scope
Create reusable Spring module for workshop frontend runtime concerns.

### Checklist
- [ ] Create shared module for SPA serving + editor endpoints + backend proxy.
- [ ] Move/reuse `EditorController` and SPA routing behavior appropriately.
- [ ] Add configuration for backend target URL and workshop source path.
- [ ] Handle headers/cookies/redirect rewrites in proxy layer.
- [ ] Add focused tests for proxy and editor behavior.

### Definition Of Done
- Frontend runtime module can be reused by multiple workshops.
- Editor operations work against mounted workshop sources.
- Proxying to backend supports demo API flows.

### Evidence
- [ ] Link commit(s):
- [ ] Test command(s):
- [ ] Results:

### Risks
- Auth/cookie/redirect behavior may break if proxy handling is incomplete.

---

## Phase 4: Pilot Migration (`2_full_text_search`)

### Scope
Migrate one non-auth workshop first to de-risk architecture.

### Checklist
- [ ] Split pilot into backend and frontend Spring Boot services.
- [ ] Keep existing frontend look and UX unchanged.
- [ ] Ensure editor features work through frontend service.
- [ ] Ensure demo/search APIs reachable through frontend proxy.
- [ ] Validate hub controls and open-workshop flow.

### Definition Of Done
- Pilot runs as dual service end-to-end from hub.
- No visual regressions in pilot frontend.
- Editor and demo APIs function normally.

### Evidence
- [ ] Link commit(s):
- [ ] Test command(s):
- [ ] Results:

### Risks
- Hidden assumptions in current same-origin calls.

---

## Phase 5: API Path Normalization (All Workshops)

### Scope
Remove absolute API call assumptions that can break under proxy/base-path conditions.

### Checklist
- [ ] Audit all workshop frontend API calls.
- [ ] Replace direct absolute paths with shared base-path helper where needed.
- [ ] Confirm no visual/style changes introduced.
- [ ] Validate workshop routes under hub proxy paths.

### Definition Of Done
- All workshop frontends are base-path-safe.
- Hub proxy paths work consistently.
- No UI design regressions.

### Evidence
- [ ] Link commit(s):
- [ ] Test command(s):
- [ ] Results:

### Risks
- Missed API call sites in workshop-specific utilities/components.

---

## Phase 6: Remaining Workshop Migrations

### Scope
Migrate remaining workshops to the dual-service structure.

### Checklist
- [ ] Migrate `1_session_management` (including login/logout/auth behavior via frontend service).
- [ ] Migrate `3_distributed_locks`.
- [ ] Migrate `4_agent_memory`.
- [ ] Validate workshop-specific dependencies (Redis/Postgres/AMS/OpenAI constraints).

### Definition Of Done
- All workshops run in dual-service mode.
- Hub controls all workshops consistently.
- Workshop-specific behaviors preserved.

### Evidence
- [ ] Link commit(s):
- [ ] Test command(s):
- [ ] Results:

### Risks
- Session workshop auth flow requires careful cookie and redirect handling.

---

## Phase 7: Docs + Scaffold + Final Validation

### Scope
Finalize tooling and documentation for repeatable future workshop creation.

### Checklist
- [ ] Update scaffold script for dual-service workshop generation.
- [ ] Update repository docs and onboarding instructions.
- [ ] Add/adjust validation checks for registry and compose generation.
- [ ] Run final end-to-end validation matrix.

### Definition Of Done
- New workshop workflow reflects dual-service architecture.
- Compose generation/documentation are aligned.
- Final validation results documented.

### Evidence
- [ ] Link commit(s):
- [ ] Test command(s):
- [ ] Results:

---

## Validation Matrix (Final Gate)

| Area | Check | Result | Notes |
|---|---|---|---|
| Registry | Dual-service metadata parses correctly | PENDING |  |
| Compose | Local + internal generated correctly | PENDING |  |
| Hub Manager | Start/stop/restart workshop unit | PENDING |  |
| Hub Proxy | Workshop open path works | PENDING |  |
| Editor | File list/load/save/restore works | PENDING |  |
| Demo APIs | Frontend -> backend proxy works | PENDING |  |
| Auth (Session workshop) | Login/logout/session behavior preserved | PENDING |  |
| UI | No visual regressions | PENDING |  |

Values: `PENDING`, `PASS`, `FAIL`.

---

## Risk Register

| ID | Risk | Impact | Mitigation | Status |
|---|---|---|---|---|
| R1 | Auth redirect/cookie issues after split | High | Add explicit auth proxy tests; migrate auth workshop after pilot | OPEN |
| R2 | Hardcoded `/api` paths in some frontends | High | Phase 5 normalization pass + search-based audit | OPEN |
| R3 | Hub status mismatch for dual services | Medium | Aggregate service health into one workshop status | OPEN |
| R4 | Compose profile complexity growth | Medium | Keep generator as single source of truth + tests | OPEN |

Values: `OPEN`, `MITIGATED`, `ACCEPTED`, `CLOSED`.

---

## Sign-off Log

| Date | Phase Approved | Approved By | Notes |
|---|---|---|---|
| 2026-03-09 | Tracker Setup | User | Initial tracker requested |
| 2026-03-09 | Phase 1 (Schema + Hub Contracts) | User | Approved to proceed |
