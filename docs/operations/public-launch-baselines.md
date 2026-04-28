# Public Launch Baselines

This document defines the first public launch baselines and decision thresholds for the release backed workshop set.

## Resource Classes

Use these classes for results review:

1. Standard Redis lab
   1. `1_session_management`
   2. `2_full_text_search`
2. Stateful lab
   1. `3_distributed_locks`
3. AI lab
   1. `4_agent_memory`

## Evidence Window

A public launch readiness decision needs all of the following:

1. one manual smoke pass for each workshop
2. one steady run
3. one burst run
4. one soak run
5. one cleanup verification pass
6. one rollback validation pass

## Cloud Run Evidence Window

The accepted 100 concurrent user window needs all of the following:

1. `TARGET_CONCURRENT_USERS=100`
2. `STEADY_ITERATIONS=100`
3. `BURST_SIZE=100`
4. `SOAK_CYCLES=3`
5. `ITERATIONS_PER_CYCLE=34`
6. Cleanup verification against every generated JSONL log
7. Rollback validation against the live control plane entrypoint
8. Cloud Run quota, session service deletion, local Redis cleanup, route metadata, and workspace cleanup notes

Use `scripts/loadtest/cloud-run-100-user-window.sh` for the Cloud Run run and `scripts/ops/cloud-run-readiness-check.sh` for the evidence review.

Record these values in the results template before accepting the run:

1. Google Cloud project and region
2. Control plane and execution plane service names
3. Session service min instances, max instances, concurrency, CPU, and memory
4. Artifact Registry image digest path
5. Workspace bucket and retention mode
6. Local Redis mode
7. Any quota blocker or manual operator intervention

## Pass Thresholds

### Standard Redis Lab

1. Provisioning success rate at or above `99%` for steady load
2. Provisioning success rate at or above `95%` for burst and soak load
3. Admitted to ready p95 at or below `75s`
4. Cleanup lag p95 at or below `30s`
5. Degraded sessions at end of run: `0`
6. Capacity driven quota denials during accepted baseline window: `0`
7. Operator intervention frequency: `0` per run

### Stateful Lab

1. Provisioning success rate at or above `99%` for steady load
2. Provisioning success rate at or above `95%` for burst and soak load
3. Admitted to ready p95 at or below `105s`
4. Cleanup lag p95 at or below `45s`
5. Degraded sessions at end of run: `0`
6. Capacity driven quota denials during accepted baseline window: `0`
7. Operator intervention frequency: `0` per run

### AI Lab

1. Provisioning success rate at or above `98%` for steady load
2. Provisioning success rate at or above `95%` for burst and soak load
3. Admitted to ready p95 at or below `150s`
4. Cleanup lag p95 at or below `60s`
5. Degraded sessions at end of run: `0`
6. Capacity driven quota denials during accepted baseline window: `0`
7. Operator intervention frequency: `0` per run

## Pause Thresholds

Pause public launch review if any of these happen:

1. One workshop misses its success rate threshold in a single evidence window
2. One workshop exceeds startup p95 by up to `25%`
3. Cleanup lag p95 exceeds threshold but is below twice threshold
4. One transient degraded session appears and recovers without manual repair
5. One alert smoke path is missing or blocked but the failure class is otherwise observable

## Rollback Thresholds

Roll back or keep public launch closed if any of these happen:

1. Any cross session file or data leak
2. Any Redis Insight session boundary failure
3. Any success rate below `95%`
4. Any startup p95 above `125%` of threshold for two consecutive runs
5. Any cleanup lag p95 above `2x` threshold
6. Any manual intervention needed to clear stale runtime or workspace state during validation
7. Any missing rollback path validation
8. Any critical alert class with no owner, no runbook, or no observable signal
9. Any Cloud Run session service remains after cleanup without an explicit retention decision
10. Any session secret configuration failure reaches a learner visible runtime
11. Any route metadata failure prevents a READY session from serving `/session/{sessionId}/**`

## Known Manual Metrics

The following readiness signals still require manual capture in the results template:

1. operator intervention frequency
2. quota denials that are clearly capacity driven when no dedicated metric is exported yet
3. release regression confirmation when it depends on comparing two separate evidence windows
4. Cloud Run quota denials when the provider signal is not exported into the control plane yet
5. service creation, route metadata, and cleanup backlog counters until the execution plane exports first class metrics

Record these gaps in the results template instead of fabricating precision.
