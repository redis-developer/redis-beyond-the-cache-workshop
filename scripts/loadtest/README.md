# Load Test Toolkit

This directory contains local validation scripts and Cloud Run public launch wrappers for the release backed workshops:

1. `1_session_management`
2. `2_full_text_search`
3. `3_distributed_locks`
4. `4_agent_memory`

The local validation scripts assume the platform is already running locally on `http://localhost:9000`.

The public scripts reuse the local validation helpers and add a public launch operator surface:

1. `public-steady-launch.sh`
2. `public-burst-launch.sh`
3. `public-soak-launch.sh`
4. `public-cleanup-check.sh`
5. `public-summary.sh`
6. `public-rollback-validation.sh`
7. `cloud-run-100-user-window.sh`
8. `cloud-run-cleanup-check.sh`

If the platform is not reachable, the public launch wrappers skip the live run and exit successfully. This keeps them usable in repository validation while still serving as the operational entrypoint for a real launch window.

## Prerequisites

1. `bash`
2. `curl`
3. `jq`

## Output

Each launch script writes a JSONL run log under `scripts/loadtest/output/`.

The launch scripts use unique actor IDs by default so they do not trip the one active session per user per workshop rule during load.

When termination is enabled, the launch scripts wait for cleanup completion when the session still reports cleanup work in progress. Each JSONL record now includes `terminatedAt`, `cleanupCompletedAt`, and `cleanupLagSeconds`.

## Common Environment Variables

1. `BASE_URL`
   Defaults to `http://localhost:9000`
2. `ACTOR_TYPE`
   Defaults to `LEARNER`
3. `ACTOR_ROLES`
   Defaults to `learner`
4. `WORKSHOPS`
   Comma separated workshop ids. Defaults to `1_session_management,2_full_text_search,3_distributed_locks,4_agent_memory`
5. `POLL_INTERVAL_SECONDS`
   Defaults to `2`
6. `READY_TIMEOUT_SECONDS`
   Defaults to `240`
7. `TERMINATION_TIMEOUT_SECONDS`
   Defaults to `180`
8. `CLEANUP_TIMEOUT_SECONDS`
   Defaults to `180`
9. `TERMINATE_AFTER_READY`
   Defaults to `1`
10. `OUTPUT_DIR`
   Defaults to `scripts/loadtest/output`

## Pilot Steady Launch Load

Runs sequential launch cycles and alternates across the pilot workshops by default.

```bash
ITERATIONS=10 ./scripts/loadtest/pilot-steady-launch.sh
```

Useful overrides:

```bash
ITERATIONS=20 \
WORKSHOPS=1_session_management \
ACTOR_ID_PREFIX=steady-learner \
./scripts/loadtest/pilot-steady-launch.sh
```

## Pilot Burst Launch Load

Launches a burst of concurrent sessions with unique actor IDs.

```bash
BURST_SIZE=10 ./scripts/loadtest/pilot-burst-launch.sh
```

Useful overrides:

```bash
BURST_SIZE=20 \
WORKSHOPS=1_session_management,2_full_text_search \
ACTOR_ID_PREFIX=burst-learner \
./scripts/loadtest/pilot-burst-launch.sh
```

## Pilot Cleanup Verification

Checks a prior run log and verifies that sessions are no longer active and do not still show cleanup work in progress.

By default it uses the newest JSONL file in `scripts/loadtest/output`.

```bash
./scripts/loadtest/pilot-cleanup-check.sh
```

Or point it at a specific run log:

```bash
INPUT=scripts/loadtest/output/steady-20260423T101500Z.jsonl \
./scripts/loadtest/pilot-cleanup-check.sh
```

Optional wait before checking:

```bash
WAIT_SECONDS=30 ./scripts/loadtest/pilot-cleanup-check.sh
```

## Pilot Summary Helper

Summarizes a steady or burst run log.

The summary includes overall launch counts plus per workshop startup and cleanup lag percentiles.

```bash
./scripts/loadtest/pilot-summary.sh
```

Or point it at a specific run log:

```bash
INPUT=scripts/loadtest/output/burst-20260423T102000Z.jsonl \
./scripts/loadtest/pilot-summary.sh
```

## Public Launch Wrappers

Use the public wrappers for the final readiness window.

### Public Steady Launch

```bash
./scripts/loadtest/public-steady-launch.sh
```

### Public Burst Launch

```bash
./scripts/loadtest/public-burst-launch.sh
```

### Public Soak Launch

```bash
SOAK_CYCLES=3 ITERATIONS_PER_CYCLE=8 ./scripts/loadtest/public-soak-launch.sh
```

### Public Cleanup Check

```bash
./scripts/loadtest/public-cleanup-check.sh
```

### Public Summary Helper

```bash
./scripts/loadtest/public-summary.sh
```

### Public Rollback Validation

```bash
./scripts/loadtest/public-rollback-validation.sh
```

## Cloud Run 100 User Window

Use this wrapper for the Cloud Run session runner evidence window. This is the default production validation path.

Dry run plan:

```bash
./scripts/loadtest/cloud-run-100-user-window.sh
```

Live Cloud Run run:

```bash
LIVE_CHECKS=on \
BASE_URL=https://CONTROL_PLANE_HOST \
TARGET_CONCURRENT_USERS=100 \
./scripts/loadtest/cloud-run-100-user-window.sh
```

The wrapper runs steady, burst, soak, cleanup, summary, readiness, and rollback validation against the public control plane entrypoint. It writes evidence under `scripts/loadtest/output/cloudrun`.

Review the latest Cloud Run evidence with:

```bash
./scripts/ops/cloud-run-readiness-check.sh
```

Run cleanup checks against the latest Cloud Run log with:

```bash
./scripts/loadtest/cloud-run-cleanup-check.sh
```

## Practical Flow

1. Run a steady launch test.
2. Run a burst launch test.
3. Run a soak launch test.
4. Run the cleanup check against the resulting log.
5. Run the summary helper on the same log if you want a quick readout.
6. For Cloud Run, run the 100 user window and attach the generated manifest plus JSONL logs to the Cloud Run results template.
