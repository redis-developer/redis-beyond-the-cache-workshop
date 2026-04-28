# Pilot Cleanup Lag Validation 2026 04 24 Local

## Scope

This addendum closes the last open cleanup lag evidence gap:

1. capture cleanup lag in the load artifacts
2. prove the summary helper reports cleanup lag p50 and p95
3. confirm cleanup verification still passes against the same run log

## Environment

1. Control plane: `http://localhost:8081`
2. Hub: `http://localhost:9001`
3. Scenario: steady launch load with 2 requests
4. Raw log: [steady-20260424T082414Z.jsonl](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/scripts/loadtest/output/steady-20260424T082414Z.jsonl)

## Evidence

Each JSONL record now includes:

1. `terminatedAt`
2. `cleanupCompletedAt`
3. `cleanupLagSeconds`

Observed records:

1. `1_session_management`
   `terminatedAt=2026-04-24T08:25:35.916081Z`
   `cleanupCompletedAt=2026-04-24T08:25:35.916082Z`
   `cleanupLagSeconds=0`
2. `2_full_text_search`
   `terminatedAt=2026-04-24T08:26:44.553888Z`
   `cleanupCompletedAt=2026-04-24T08:26:44.553888Z`
   `cleanupLagSeconds=0`

## Summary Output

`scripts/loadtest/pilot-summary.sh` now reports:

1. overall `cleanupLagSamples=2`
2. overall `cleanupLagP50Seconds=0`
3. overall `cleanupLagP95Seconds=0`
4. per workshop `cleanupLagP50Seconds=0`
5. per workshop `cleanupLagP95Seconds=0`

## Cleanup Verification

`scripts/loadtest/pilot-cleanup-check.sh` passed against the same run log.

Observed result:

1. `1_session_management` state `TERMINATED`, cleanup `COMPLETED`
2. `2_full_text_search` state `TERMINATED`, cleanup `COMPLETED`

## Outcome

1. Cleanup lag capture is now present in the pilot load artifacts.
2. The summary helper now exposes cleanup lag p50 and p95 overall and per workshop.
3. The explicit cleanup lag evidence gap is closed.
