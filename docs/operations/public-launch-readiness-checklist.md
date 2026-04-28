# Public Launch Readiness Checklist

Use this checklist before a final public launch decision.

## Platform Surface

1. `/api/catalog/**` and `/api/sessions/**` are the public control plane surfaces in use.
2. `/session/{sessionId}/**` is the learner session surface in use.
3. `/manager/api/**` no longer serves public traffic.
4. `/workshop/{workshopId}/**` no longer serves public traffic.

## Runtime Model

1. No public request path depends on local Docker.
2. No public request path builds workshop images at request time.
3. Session workspaces are isolated per session.
4. Stateful dependencies that expose learner data are session scoped.

## Ownership

1. Control plane ownership is documented for auth, policy, release selection, audit, and session state.
2. Execution plane ownership is documented for runtime launch, restart, diagnostics, restore, sidecars, and cleanup.
3. Gateway and public traffic policy ownership is documented separately from runtime ownership.
4. Every critical alert has an owner team and a runbook link.

## Evidence

1. Manual smoke passed for all four release backed workshops.
2. Steady, burst, and soak evidence logs are attached.
3. Cleanup verification passed on the latest evidence window.
4. Rollback validation passed on the current release state.
5. The latest results file uses [public-launch-results-template.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/pilot/public-launch-results-template.md).

## Cloud Run Evidence

1. `scripts/loadtest/cloud-run-100-user-window.sh` produced the Cloud Run evidence manifest.
2. The Cloud Run run used `TARGET_CONCURRENT_USERS=100`.
3. The run included steady, burst, soak, cleanup, readiness, and rollback validation.
4. `scripts/ops/cloud-run-readiness-check.sh` passed or recorded only accepted telemetry gaps.
5. Session service min instances, max instances, concurrency, memory, CPU, quota blockers, and deletion lag are recorded.
6. Local Redis, Redis Insight, route metadata, workspace cleanup, and session service cleanup evidence are recorded.

## Alerts And Readiness

1. Alert inventory exists at [public-launch-alerts.json](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/scripts/ops/public-launch-alerts.json).
2. Each critical failure class has owner, severity, scope, and runbook.
3. Alert smoke evidence is recorded as live tested, simulated, or blocked.
4. Any missing telemetry blocker is written down explicitly.

## Decision

1. All workshops meet the pass thresholds in [public-launch-baselines.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/operations/public-launch-baselines.md).
2. No rollback threshold is triggered.
3. Open risks have named owners and due dates.
4. Final decision is written as ship, pause, or roll back.
