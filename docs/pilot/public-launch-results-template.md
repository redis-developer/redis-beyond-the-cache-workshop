# Public Launch Results Template

Use one copy of this template for each public launch evidence window.

## Run Identity

1. Run date:
2. Operator:
3. Environment:
4. Control plane commit:
5. Hub commit:
6. Gateway or ingress commit:
7. Results window:

## Release State

| Workshop | Release id | Release version | Rule state | Resource class | Notes |
| --- | --- | --- | --- | --- | --- |
| `1_session_management` |  |  |  | Standard Redis lab |  |
| `2_full_text_search` |  |  |  | Standard Redis lab |  |
| `3_distributed_locks` |  |  |  | Stateful lab |  |
| `4_agent_memory` |  |  |  | AI lab |  |

## Traffic Shape

1. Manual smoke completed:
2. Steady script:
3. Burst script:
4. Soak script:
5. Cleanup check script:
6. Rollback validation script:
7. Planned requests:
8. Burst size:
9. Soak cycles:
10. Actor setup:

## Cloud Run Evidence

1. Cloud Run project:
2. Cloud Run region:
3. Control plane service:
4. Execution plane service:
5. Artifact Registry repository:
6. Workspace bucket:
7. Session runner image digests:
8. Session service min instances:
9. Session service max instances:
10. Session service concurrency:
11. Max active session services:
12. Cloud Run quota errors:
13. Image pull errors:
14. Deletion lag:
15. Redis mode:
16. Local Redis cleanup evidence:
17. Workspace retained bytes:
18. Operator interventions:
19. Telemetry gaps:

## Quantitative Results

| Metric | `1_session_management` | `2_full_text_search` | `3_distributed_locks` | `4_agent_memory` | Notes |
| --- | --- | --- | --- | --- | --- |
| Launch requests |  |  |  |  |  |
| Launch successes |  |  |  |  |  |
| Launch failures |  |  |  |  |  |
| Success rate |  |  |  |  |  |
| Startup latency p50 |  |  |  |  |  |
| Startup latency p95 |  |  |  |  |  |
| Startup latency max |  |  |  |  |  |
| Cleanup lag p50 |  |  |  |  |  |
| Cleanup lag p95 |  |  |  |  |  |
| Cleanup lag max |  |  |  |  |  |
| Degraded sessions |  |  |  |  |  |
| Capacity denials |  |  |  |  |  |
| Manual interventions |  |  |  |  |  |

## Cost Evidence

| Scenario | Min instances | Active session minutes | Cloud Run cost | Storage cost | Total | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Idle |  |  |  |  |  |  |
| Event only |  |  |  |  |  |  |
| Always warm |  |  |  |  |  |  |

Cost decision:

1. Event only cost compared with accepted warm service estimate:
2. Always warm cost accepted:
3. Cost blockers:
4. Next cost review date:

## Alert Smoke Evidence

| Alert id | Live tested | Simulated | Blocked | Evidence | Notes |
| --- | --- | --- | --- | --- | --- |
| `control_plane_error_rate_high` |  |  |  |  |  |
| `launch_rejection_spike` |  |  |  |  |  |
| `startup_timeout_spike` |  |  |  |  |  |
| `cleanup_backlog_growth` |  |  |  |  |  |
| `degraded_session_spike` |  |  |  |  |  |
| `capacity_denial_spike` |  |  |  |  |  |
| `release_regression_detected` |  |  |  |  |  |

## Baseline Decision

1. Pass thresholds met:
2. Pause thresholds triggered:
3. Rollback thresholds triggered:
4. Missing telemetry blockers:
5. Open risks:

## Final Decision

1. Result: ship, pause, or roll back
2. Highest priority repair:
3. Recommended owner:
4. Follow up date:
