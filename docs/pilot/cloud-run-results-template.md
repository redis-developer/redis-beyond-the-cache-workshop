# Cloud Run Results Template

Use one copy of this template for each Cloud Run evidence window.

## Run Identity

1. Run date:
2. Operator:
3. Environment:
4. Google Cloud project:
5. Region:
6. Control plane commit:
7. Execution plane commit:
8. Session runner image digest set:
9. Results window:

## Runtime State

| Component | Value | Notes |
| --- | --- | --- |
| Control plane Cloud Run service |  |  |
| Execution plane Cloud Run service |  |  |
| Artifact Registry repository |  |  |
| Workspace bucket |  |  |
| Gateway host |  |  |
| Redis mode |  | Local process |

## Session Runner Defaults

| Setting | Value | Notes |
| --- | --- | --- |
| Manager port | 8080 |  |
| Concurrency | 1 |  |
| Min instances | 0 |  |
| Max instances | 1 |  |
| Ingress | internal and load balancer |  |
| CPU allocation |  |  |
| Memory |  |  |

## Traffic Shape

1. Script:
2. Manifest:
3. Steady iterations:
4. Burst size:
5. Soak cycles:
6. Actor setup:
7. Terminate after ready:
8. Cleanup wait seconds:

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
| Restart without rebuild smoke |  |  |  |  |  |
| Redis Insight smoke |  |  |  |  |  |

## Cleanup Evidence

| Check | Result | Evidence | Notes |
| --- | --- | --- | --- |
| Session terminal state |  |  |  |
| Stale route metadata |  |  |  |
| Cloud Run session service deletion |  |  |  |
| Workspace prefix cleanup or retention |  |  |  |
| Local Redis data cleanup or retention |  |  |  |

## Cost Evidence

| Scenario | Min instances | Active session minutes | Cloud Run service cost | Storage cost | Total | Notes |
| --- | --- | --- | --- | --- | --- | --- |
| Idle |  |  |  |  |  |  |
| Event only |  |  |  |  |  |  |
| Always warm |  |  |  |  |  |  |

Billing export evidence:

1. `BILLING_EXPORT_TABLE`:
2. `COST_START_DATE`:
3. `COST_END_DATE`:
4. Cost report command:
5. Cost report artifact:
6. Budget display name:
7. Budget amount and currency:
8. Budget alert recipient or notification channel:
9. Terraform `cost_tracking` output:
10. Cost labels confirmed:

## Quota And Limits

1. Cloud Run service count limit:
2. Max services observed:
3. Max active session services:
4. Quota errors:
5. Image pull errors:
6. Cold start issues:
7. Deletion lag:
8. Workspace retained bytes:

## Final Decision

1. Result: ship, pause, or roll back
2. Cost target met:
3. Cleanup target met:
4. Scale target met:
5. Highest priority repair:
6. Recommended owner:
7. Follow up date:
