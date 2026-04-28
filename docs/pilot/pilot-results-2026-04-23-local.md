# Pilot Results 2026 04 23 Local

## Run identity

1. Run date: 2026 04 23
2. Operator: Codex
3. Environment: local
4. Control plane commit: `d3de8314903d1129d5eec956199b424f0ae4a903`
5. Hub commit: `d3de8314903d1129d5eec956199b424f0ae4a903`
6. Pilot environment name: `local`

## Pilot release state

| Workshop | Pilot rule enabled | Release id | Release version | Active local path |
| --- | --- | --- | --- | --- |
| `1_session_management` | yes | `session-management-2026.04.1` | `2026.04.1` | `current` |
| `2_full_text_search` | yes | `full-text-search-2026.04.1` | `2026.04.1` | `current` |

## Scenario 1 steady launch load

1. Scenario name: steady launch load
2. Script used: `scripts/loadtest/pilot-steady-launch.sh`
3. Concurrency: sequential
4. Requests planned: 4
5. Burst or steady: steady
6. Session TTL assumption: default platform session TTL
7. Raw log: [steady-20260423T140603Z.jsonl](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/scripts/loadtest/output/steady-20260423T140603Z.jsonl)

| Metric | `1_session_management` | `2_full_text_search` | Notes |
| --- | --- | --- | --- |
| Launch requests | 2 | 2 | Alternating sequence |
| Launch successes | 2 | 2 | All reached `READY` |
| Launch failures | 0 | 0 |  |
| Failure rate | 0 percent | 0 percent |  |
| Startup latency p50 | 43.5 s | 39 s | Based on 2 samples |
| Startup latency p95 | 68 s | 62 s | With 2 samples, p95 collapsed to max |
| Startup latency max | 68 s | 62 s |  |
| Termination successes | 2 | 2 | Auto terminate after ready |
| Cleanup lag p50 | not captured | not captured | Current scripts verify completion, not lag distribution |
| Cleanup lag p95 | not captured | not captured | Current scripts verify completion, not lag distribution |
| Stuck sessions after run | 0 | 0 | Verified by cleanup check |
| Manual cleanup count | 0 | 0 |  |

## Scenario 2 burst launch load

1. Scenario name: burst launch load
2. Script used: `scripts/loadtest/pilot-burst-launch.sh`
3. Concurrency: 10 concurrent launches
4. Requests planned: 10
5. Burst or steady: burst
6. Session TTL assumption: default platform session TTL
7. Raw log: [burst-20260423T142350Z.jsonl](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/scripts/loadtest/output/burst-20260423T142350Z.jsonl)

| Metric | `1_session_management` | `2_full_text_search` | Notes |
| --- | --- | --- | --- |
| Launch requests | 5 | 5 | Even split across pilot workshops |
| Launch successes | 5 | 5 | All reached `READY` |
| Launch failures | 0 | 0 |  |
| Failure rate | 0 percent | 0 percent |  |
| Startup latency p50 | 56 s | 56 s | Based on 5 samples |
| Startup latency p95 | 56 s | 78 s | Small sample approximation |
| Startup latency max | 56 s | 78 s |  |
| Termination successes | 5 | 5 | Auto terminate after ready |
| Cleanup lag p50 | not captured | not captured | Current scripts verify completion, not lag distribution |
| Cleanup lag p95 | not captured | not captured | Current scripts verify completion, not lag distribution |
| Stuck sessions after run | 0 | 0 | Verified by cleanup check |
| Manual cleanup count | 0 | 0 |  |

## Learner visible behavior

1. Any blank pages: none observed during the automated pilot runs
2. Any failed session launches: none after the path resolution fixes landed
3. Any diagnostics failures: not exercised in the automated load scripts
4. Any restore failures: not exercised in the automated load scripts
5. Any restart failures: not exercised in the automated load scripts
6. Any Redis Insight failures: not exercised in the automated load scripts
7. Any auth or redirect issues: none observed during the automated pilot runs

## Isolation checks

1. File isolation between two sessions: not rerun in this load pass
2. Redis data isolation between two sessions: not rerun in this load pass
3. Redis Insight session boundary: not rerun in this load pass
4. Cleanup removed stale runtime state: pass

## Operator observations

1. Preflight issues:
   Local port conflicts on `8080` and `9000` required running the pilot on `8081` and `9001`.
2. During run issues:
   The first steady attempt failed because the hub could not discover `workshops.yaml` from the module working directory.
   The second issue was Dockerfile path resolution in the hub execution launcher.
   Both issues were fixed during this run.
3. Rollback needed or not:
   No rollback needed after the fixes landed.
4. Manual intervention required:
   Yes. Two narrow runtime path fixes were needed before the pilot load scripts were stable.
5. Longest blocking incident:
   Relative path resolution in the hub for the workshop registry and workshop Dockerfiles.

## Outcome

1. Result: continue with narrow follow up repairs, do not start broad rollout yet
2. Highest priority repair:
   Normalize hub workspace root discovery so session workspaces are created under the intended repo rooted execution workspace path without nested module prefixes.
3. Additional follow up actions:
   Add cleanup lag capture to the load scripts.
   Run explicit two session file and Redis isolation checks against the pilot release path.
   Record a short manual smoke result for diagnostics, restore, restart, and Redis Insight on the pilot release ids.
4. Recommended owner:
   Platform execution plane owner
5. Ready for broader rollout: no
