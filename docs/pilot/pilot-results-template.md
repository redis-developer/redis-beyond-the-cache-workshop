# Pilot Results Template

Use one copy of this template per pilot run.

## Run identity

1. Run date:
2. Operator:
3. Environment:
4. Control plane commit:
5. Hub commit:
6. Pilot environment name:

## Pilot release state

| Workshop | Pilot rule enabled | Release id | Release version | Active local path |
| --- | --- | --- | --- | --- |
| `1_session_management` |  |  |  |  |
| `2_full_text_search` |  |  |  |  |
| `3_distributed_locks` |  |  |  |  |
| `4_agent_memory` |  |  |  |  |

## Scenario definition

1. Scenario name:
2. Script used:
3. Concurrency:
4. Requests planned:
5. Burst or steady:
6. Session TTL assumption:

## Quantitative results

| Metric | `1_session_management` | `2_full_text_search` | `3_distributed_locks` | `4_agent_memory` | Notes |
| --- | --- | --- | --- | --- | --- |
| Launch requests |  |  |  |  |  |
| Launch successes |  |  |  |  |  |
| Launch failures |  |  |  |  |  |
| Failure rate |  |  |  |  |  |
| Startup latency p50 |  |  |  |  |  |
| Startup latency p95 |  |  |  |  |  |
| Startup latency max |  |  |  |  |  |
| Termination successes |  |  |  |  |  |
| Cleanup lag p50 |  |  |  |  |  |
| Cleanup lag p95 |  |  |  |  |  |
| Stuck sessions after run |  |  |  |  |  |
| Manual cleanup count |  |  |  |  |  |

## Learner visible behavior

1. Any blank pages:
2. Any failed session launches:
3. Any diagnostics failures:
4. Any restore failures:
5. Any restart failures:
6. Any Redis Insight failures:
7. Any auth or redirect issues:

## Isolation checks

1. File isolation between two sessions: pass or fail
2. Redis data isolation between two sessions: pass or fail
3. Redis Insight session boundary: pass or fail
4. Cleanup removed stale runtime state: pass or fail

## Operator observations

1. Preflight issues:
2. During run issues:
3. Rollback needed or not:
4. Manual intervention required:
5. Longest blocking incident:

## Outcome

1. Result: continue, pause, or roll back
2. Highest priority repair:
3. Additional follow up actions:
4. Recommended owner:
5. Ready for broader rollout: yes or no
