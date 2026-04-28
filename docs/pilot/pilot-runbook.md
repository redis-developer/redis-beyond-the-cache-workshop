# Pilot Runbook

This runbook defines how to execute the release backed local validation run for the current workshops:

1. `1_session_management`
2. `2_full_text_search`
3. `3_distributed_locks`
4. `4_agent_memory`

Use this runbook together with:

1. `scripts/ops/*`
2. `scripts/loadtest/*`
3. [pilot-launch-rules.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/pilot-launch-rules.md)
4. [release-catalog.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/release-catalog.md)
5. [pilot-results-template.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/pilot/pilot-results-template.md)
6. [pilot-feedback-template.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/pilot/pilot-feedback-template.md)

For the final public launch wave, use the Cloud Run operator surfaces in:

1. [public-launch-runbook.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/operations/public-launch-runbook.md)
2. [public-launch-baselines.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/operations/public-launch-baselines.md)
3. [public-launch-readiness-checklist.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/operations/public-launch-readiness-checklist.md)
4. [public-launch-results-template.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/pilot/public-launch-results-template.md)

## Scope

The pilot verifies:

1. session launch behavior
2. session termination and cleanup behavior
3. learner visible workshop flow
4. Redis and Redis Insight isolation for pilot workshops
5. operator recovery and rollback readiness

## Preflight

Complete these checks before any pilot run:

1. Start the control plane and execution plane services for the target environment.
2. Confirm the workshop catalog loads through the control plane endpoint.
3. Confirm all four release backed workshops are visible.
4. Confirm the pilot launch rules are enabled for the target environment.
5. Confirm `workshops.yaml` contains the expected nested release `releaseId` and `releaseVersion`.
6. Confirm local actor headers or the local proxy actor default are configured.
7. Confirm Docker has enough free space for pilot session churn.
8. Confirm no stale failed sessions are blocking new launches.

Recommended operator commands:

1. `bash scripts/ops/pilot-preflight.sh`
2. `bash scripts/ops/pilot-launch-rules-check.sh`

## Required observations for every run

Capture these values in the results template for every scenario:

1. environment
2. pilot rule state
3. workshop id
4. release id
5. release version
6. request count
7. completed launch count
8. failed launch count
9. termination count
10. startup latency p50
11. startup latency p95
12. startup latency max
13. launch failure rate
14. cleanup lag p50
15. cleanup lag p95
16. count of stuck sessions in `REQUESTED`, `ADMITTED`, `PROVISIONING`, `INITIALIZING`, or `TERMINATING`
17. learner visible errors
18. Redis Insight or Redis isolation issues
19. operator intervention required or not

## Manual smoke checks

Run these before any load scenario:

1. Launch `1_session_management`.
2. Log in through the workshop.
3. Edit a file.
4. Run diagnostics.
5. Restore files.
6. Restart the runtime.
7. Restart with rebuild.
8. Open Redis Insight.
9. Terminate the session.

Then:

1. Launch `2_full_text_search`.
2. Confirm the frontend and backend boot cleanly.
3. Edit a file.
4. Run diagnostics.
5. Restore files.
6. Open Redis Insight.
7. Terminate the session.

Then:

1. Launch `3_distributed_locks`.
2. Confirm the frontend and backend boot cleanly.
3. Exercise at least one lock flow.
4. Run diagnostics.
5. Restore files.
6. Restart the runtime.
7. Open Redis Insight.
8. Terminate the session.

Then:

1. Launch `4_agent_memory`.
2. Confirm the frontend and backend boot cleanly.
3. Open `/lab` and `/demo`.
4. Run diagnostics.
5. Restore files.
6. Restart the runtime.
7. Open Redis Insight.
8. Terminate the session.

## Load scenarios

Run the pilot scenarios in this order.

### Scenario 1: Steady launch load

Goal:
Validate consistent launch and termination under low sustained pressure.

Command:

1. `bash scripts/loadtest/pilot-steady-launch.sh`

Expected focus:

1. stable startup latency
2. low failure rate
3. clean termination behavior
4. no stuck sessions after the run

### Scenario 2: Burst launch load

Goal:
Validate short spike behavior and identify queueing or admission instability.

Command:

1. `bash scripts/loadtest/pilot-burst-launch.sh`

Expected focus:

1. peak launch latency
2. provisioning bottlenecks
3. launch error rate during spikes
4. cleanup completion after the burst

### Scenario 3: Cleanup verification

Goal:
Confirm sessions terminate and clean up without leaving stale runtime state.

Command:

1. `bash scripts/loadtest/pilot-cleanup-check.sh`

Expected focus:

1. termination to cleanup delay
2. stale runtime references
3. stale workspaces
4. repeated cleanup failures

Note:

1. The steady and burst JSONL logs record `terminatedAt`, `cleanupCompletedAt`, and `cleanupLagSeconds`.
2. Use `scripts/loadtest/pilot-summary.sh` against those logs to extract cleanup lag p50 and p95 for the results template.

## Rollback triggers

Pause the run and use the local maintainer path only for recovery if any of these happen:

1. repeated learner visible launch failures for a pilot workshop
2. session startup latency p95 above the agreed threshold for two consecutive runs
3. cleanup lag remains above the agreed threshold after retry windows
4. Redis or Redis Insight exposes another session's data
5. restart or restore behavior corrupts the active session workspace
6. operators need repeated manual cleanup to restore service
7. pilot launch rules point to the wrong release identity

Rollback action:

1. disable the affected pilot rule
2. relaunch using the local maintainer path
3. record the failure in the results and feedback templates
4. open a narrow repair task before resuming

## Post run review

After each run:

1. export or save the raw scenario output
2. fill in the results template
3. fill in the qualitative feedback template
4. compare the run against the previous pilot run
5. list required repairs
6. decide continue, pause, or roll back

## Broader rollout gate

Do not start broader rollout until the local validation shows all of the following:

1. all release backed workshops complete smoke checks without manual fixes
2. steady and burst runs complete with acceptable launch failure rate
3. cleanup verification shows no recurring stale runtime or workspace leakage
4. no cross session data visibility is observed
5. no unresolved learner blocking issues remain
6. operator burden is low enough to support a broader rollout

## Stop signals

Pause broader rollout planning if any of these remain open:

1. unresolved cross session isolation bug
2. repeated stuck sessions after load runs
3. repeated rollback to local maintainer launch rules
4. unclear release ownership or release rule drift
5. missing evidence for startup latency or cleanup behavior
