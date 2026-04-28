# Public Launch Operations Runbook

This runbook is the operator surface for the public control plane and execution plane launch path.

Use it with:

1. [public-launch-readiness-checklist.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/operations/public-launch-readiness-checklist.md)
2. [public-launch-baselines.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/operations/public-launch-baselines.md)
3. [public-launch-results-template.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/pilot/public-launch-results-template.md)
4. [public-launch-alerts.json](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/scripts/ops/public-launch-alerts.json)
5. `scripts/ops/public-launch-preflight.sh`
6. `scripts/ops/public-readiness-check.sh`
7. `scripts/ops/public-rollback-validation.sh`
8. `scripts/loadtest/public-*.sh`
9. `scripts/loadtest/cloud-run-100-user-window.sh`
10. `scripts/ops/cloud-run-readiness-check.sh`
11. `scripts/ops/cloud-run-cleanup-check.sh`

## Scope

The public launch surface covers the four release backed workshops:

1. `1_session_management`
2. `2_full_text_search`
3. `3_distributed_locks`
4. `4_agent_memory`

Ownership split:

1. Control plane owns catalog, launch rules, auth, policy, audit, and release selection.
2. Execution plane owns workspace provisioning, runtime launch, restart, sidecars, and cleanup.
3. Gateway and traffic policy ownership stays outside this runbook. This runbook only consumes those signals.

## Public Launch Flow

1. Run `bash scripts/ops/public-launch-preflight.sh`
2. Run manual smoke checks for all four workshops
3. Run `bash scripts/loadtest/public-steady-launch.sh`
4. Run `bash scripts/loadtest/public-burst-launch.sh`
5. Run `bash scripts/loadtest/public-soak-launch.sh`
6. Run `bash scripts/loadtest/public-cleanup-check.sh`
7. Run `bash scripts/ops/public-rollback-validation.sh`
8. Run `bash scripts/ops/public-readiness-check.sh`
9. Fill [public-launch-results-template.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/pilot/public-launch-results-template.md)

## Cloud Run 100 User Flow

Use this flow as the default production validation path for Cloud Run.

1. Confirm `infra/terraform/cloudrun` is applied with digest pinned control plane and execution plane images.
2. Confirm nested release entries in `workshops.yaml` use digest pinned `combined` runner images.
3. Run `terraform -chdir=infra/terraform/cloudrun validate`.
4. Run `bash scripts/ops/cloud-run-readiness-check.sh`.
5. Run `LIVE_CHECKS=on BASE_URL=https://CONTROL_PLANE_HOST bash scripts/loadtest/cloud-run-100-user-window.sh`.
6. Run `INPUT=scripts/loadtest/output/cloudrun/LATEST.jsonl bash scripts/ops/cloud-run-readiness-check.sh`.
7. Run `INPUT=scripts/loadtest/output/cloudrun/LATEST.jsonl bash scripts/loadtest/cloud-run-cleanup-check.sh`.
8. Record the event evidence in [cloud-run-results-template.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/pilot/cloud-run-results-template.md).

Cloud Run ownership split:

1. Control plane owns `workshops.yaml` release selection, auth, policy, audit, and stable public session route metadata.
2. Execution plane owns creating, restarting, and deleting one Cloud Run session service per learner session.
3. Session runner owns editor APIs, diagnostics, restore, workspace snapshots, child JVM restart, local Redis, and optional Redis Insight processes.
4. Operators own Cloud Run quotas, service account bindings, Artifact Registry, Cloud Storage retention, and cost review.

Rollback triggers for Cloud Run:

1. Cross session data exposure.
2. Redis Insight session boundary failure.
3. Launch success rate below `95%`.
4. Stable `/session/{sessionId}/` public route not preserved.
5. Restart without rebuild unavailable for healthy runner services.
6. Cloud Run session services remain after cleanup.
7. Local Redis state escapes the session boundary.
8. Event only cost is not materially lower than the accepted warm service estimate.

## Control Plane API Errors

Alert:

1. `control_plane_error_rate_high`

Primary signals:

1. control plane API error rate
2. admin release overview
3. session create failures in run logs

Operator steps:

1. Confirm `/api/catalog/workshops` and `/api/sessions` still return expected status codes.
2. Check whether failures are limited to one workshop, one release, or all launches.
3. Compare the failing requests with the latest `workshops.yaml` release state.
4. If failures are broad, pause public launch traffic and open rollback validation.
5. Record the incident in the public launch results template.

## Launch Rejection Spike

Alert:

1. `launch_rejection_spike`

Primary signals:

1. session create denial count by reason
2. operator summary of rejection responses

Operator steps:

1. Determine whether denials are user policy, quota, or capacity driven.
2. If denials are user policy, keep launch open and document the affected cohort.
3. If denials are capacity driven, move to [Capacity Denial Spike](#capacity-denial-spike).
4. If denials follow a new release, move to [Release Regression](#release-regression).

## Startup Timeout Spike

Alert:

1. `startup_timeout_spike`

Primary signals:

1. admitted to ready latency
2. startup timeout count
3. launch outcome distribution in the public load logs

Operator steps:

1. Compare the affected workshop against [public-launch-baselines.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/operations/public-launch-baselines.md).
2. Identify whether the slowdown is isolated to one resource class.
3. Check cleanup backlog at the same time. Startup timeouts and cleanup stalls often arrive together under pressure.
4. If p95 startup time exceeds pause threshold for two consecutive runs, pause public rollout.
5. If p95 startup time exceeds rollback threshold, execute release rollback validation immediately.

## Cleanup Backlog Growth

Alert:

1. `cleanup_backlog_growth`

Primary signals:

1. cleanup pending count
2. cleanup lag p50 and p95
3. stale sessions found by `scripts/loadtest/public-cleanup-check.sh`

Operator steps:

1. Run `bash scripts/loadtest/public-cleanup-check.sh`.
2. Confirm whether cleanup backlog is local to one workshop or global.
3. If backlog is bounded and falling, keep rollout paused and observe.
4. If backlog is growing or stale runtimes remain after retry windows, halt new launch traffic.
5. If the backlog came after a release change, run rollback validation.

## Degraded Session Spike

Alert:

1. `degraded_session_spike`

Primary signals:

1. degraded session count
2. run log launch outcomes marked `degraded`
3. learner visible smoke failures

Operator steps:

1. Identify which workshop class is degraded.
2. Confirm whether degraded sessions recover after restart.
3. If degradation is learner visible, pause public launch for that workshop.
4. If degradation is broad across workshops, treat it as a platform incident and prepare rollback.

## Capacity Denial Spike

Alert:

1. `capacity_denial_spike`

Primary signals:

1. quota denial reason classified as capacity
2. active stateful dependency counts
3. workspace storage pressure

Operator steps:

1. Check whether denials are tied to Redis, Postgres, AI sidecars, or workspace storage.
2. Review the last burst and soak runs against the resource class thresholds.
3. Reduce public launch rate or tighten per workshop quotas before broad rollback.
4. Record the new operating limit in the results template and in the capacity review notes.

## Release Regression

Alert:

1. `release_regression_detected`

Primary signals:

1. ready time regression by workshop release
2. provisioning success regression by workshop release
3. learner visible smoke failures after release change

Operator steps:

1. Confirm the impacted workshop and release pair.
2. Run `bash scripts/ops/public-rollback-validation.sh`.
3. If rollback clears the issue, keep the release paused.
4. Record the failing release id, the rollback decision, and the repair owner.

## Release Rollback

Use this when a release must be taken out of service.

1. Edit the release rule or disable the public release path for the impacted workshop.
2. Restart the control plane.
3. Run `bash scripts/ops/public-rollback-validation.sh --validate`.
4. Launch one smoke session for the impacted workshop.
5. Confirm readiness and cleanup are back inside thresholds.
6. Update the results template with rollback reason and outcome.

## Quota Tuning

Use this when public traffic is healthy but capacity denials or long queues appear.

1. Identify the affected workshop class.
2. Compare current load against the class thresholds in [public-launch-baselines.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/operations/public-launch-baselines.md).
3. Prefer lowering admission or tightening concurrency before widening infrastructure.
4. Record every quota change in the public launch results template.
5. Re run burst and soak validation after the change.

## Cost And Capacity Review

Run this before launch and then on a recurring schedule.

1. Review launch counts by workshop and mode.
2. Review active Redis, Postgres, and AI sidecar counts.
3. Review cleanup lag and stale workspace count.
4. Review AI provider usage notes for `4_agent_memory`.
5. Confirm operator intervention frequency stayed inside the target range.
6. Record decisions and new limits in the results template.

## Alert Smoke Test

For every public launch evidence window:

1. Smoke test `control_plane_error_rate_high` with a temporary failed request or equivalent safe simulation.
2. Smoke test `launch_rejection_spike` with a controlled denial or replayed evidence.
3. Smoke test `startup_timeout_spike` and `cleanup_backlog_growth` with replayed or staged run evidence if live simulation is too risky.
4. Smoke test `degraded_session_spike`, `capacity_denial_spike`, and `release_regression_detected` through staged validation evidence if a live fault is not safe.
5. Record whether each alert was live tested, simulated, or blocked by missing telemetry.

## Session Service Creation Failure

Alert:

1. `session_service_creation_failure`

Primary signals:

1. execution plane service creation errors
2. launch records that fail before Cloud Run session service creation
3. execution plane logs filtered by session id and workshop id

Operator steps:

1. Confirm whether the failure is tied to one release artifact or all templates.
2. If the failure is tied to one release, run rollback validation for that workshop release.
3. If all templates fail, pause admission and keep existing sessions running.
4. Record the missing or live telemetry source in the Cloud Run evidence section.

## Cloud Run Quota Backlog

Alert:

1. `cloud_run_quota_backlog`

Primary signals:

1. quota denial responses from Cloud Run
2. session service creation retries
3. delayed admitted to ready transitions

Operator steps:

1. Check whether delayed sessions are concentrated in one resource class.
2. If retries are draining, keep the run paused until p95 startup is inside threshold.
3. If quota denials continue, stop new launch traffic and record the quota blocker.
4. Do not widen limits without recording the new baseline.

## Session Secret Configuration Failure

Alert:

1. `secret_configuration_failure`

Primary signals:

1. execution plane launch errors from missing Secret Manager values
2. runner startup errors caused by missing provider settings
3. learner visible startup failures in stateful or AI workshops

Operator steps:

1. Confirm the failure is not exposing secret values to learners or logs.
2. Stop new launches for affected workshops.
3. Verify service account and Secret Manager IAM bindings.
4. Roll back the affected release or secret config if learners can hit the failure.

## Cloud Run Session Startup Timeout

Alert:

1. `session_startup_timeout`

Primary signals:

1. `launch_timeout` rows in Cloud Run JSONL evidence
2. admitted to ready p95 by resource class
3. session service readiness and image pull events

Operator steps:

1. Compare startup p95 against the class thresholds.
2. Separate image pull, service creation, and application readiness causes.
3. If the timeout follows a release change, run rollback validation.
4. If the timeout is capacity driven, pause and tune admission before scaling further.

## Cloud Run Route Metadata Failure

Alert:

1. `route_metadata_failure`

Primary signals:

1. READY sessions with missing public entry url
2. failed `/session/{sessionId}/**` smoke checks
3. public route events for session paths

Operator steps:

1. Check whether the control plane recorded route metadata for the session.
2. Check whether the execution plane recorded the Cloud Run service URL and public route metadata.
3. If route metadata is broken for READY sessions, roll back or pause the rollout.
4. Keep the session isolated while preserving diagnostics.

## Cloud Run Cleanup Backlog Growth

Alert:

1. `cleanup_backlog_growth`

Primary signals:

1. cleanup lag p95 in JSONL evidence
2. stale Cloud Run session services
3. execution plane cleanup logs

Operator steps:

1. Run cleanup verification against every JSONL log from the 100 user window.
2. Check stale session services and workspace prefixes older than the session ttl.
3. If backlog is bounded and falling, pause new launches and observe.
4. If backlog grows or requires manual deletion, rollback or keep launch closed.

## Cloud Run Resource Exhaustion

Alert:

1. `cloud_run_resource_exhaustion`

Primary signals:

1. capacity denials
2. Cloud Run quota errors
3. session service CPU and memory headroom
4. Artifact Registry image pull errors

Operator steps:

1. Identify whether pressure comes from standard, stateful, or AI workshops.
2. Prefer admission tuning before increasing public concurrency.
3. If Cloud Run quota is the blocker, record the exact GCP quota and pause the launch decision.
4. Re run burst and soak after any quota or admission change.
