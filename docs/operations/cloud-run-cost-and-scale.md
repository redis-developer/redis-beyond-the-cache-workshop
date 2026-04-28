# Cloud Run Cost And Scale Evidence

This document is the operator surface for proving that Cloud Run event operation stays inside the accepted cost target while preserving session isolation and editable labs.

Use it with:

1. `infra/terraform/cloudrun`
2. `scripts/loadtest/cloud-run-100-user-window.sh`
3. `scripts/loadtest/cloud-run-cleanup-check.sh`
4. `scripts/ops/cloud-run-readiness-check.sh`
5. `scripts/ops/cloud-run-cleanup-check.sh`
6. [cloud-run-results-template.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/pilot/cloud-run-results-template.md)

## Cost Scenarios

Record all three scenarios for every launch decision.

| Scenario | What To Measure | Expected Posture |
| --- | --- | --- |
| Idle | Long lived control plane and execution plane cost with no session services | Session services deleted, min instances `0` |
| Event only | Actual workshop window with Cloud Run session services created on demand | Session services min instances `0`, max instances `1`, concurrency `1` |
| Always warm | Event window with selected min instances raised for startup targets | Only long lived services or approved session classes stay warm |

## Required Cost Inputs

1. Google Cloud project and region.
2. Control plane and execution plane min instance settings.
3. Session runner min instances, max instances, concurrency, CPU, memory, and active session minutes.
4. Number of created Cloud Run session services.
5. Number of deleted Cloud Run session services.
6. Artifact Registry repository and image pull count when available.
7. Cloud Storage workspace retained bytes and lifecycle retention days.
8. Redis mode, expected to be local process.
9. Operator interventions and cleanup retries.

## Billing Export Cost Report

Use the standard BigQuery Cloud Billing export as the source for Cloud Run cost evidence. The script is dry run safe and prints the rendered query unless live checks are enabled.

Required variables:

1. `BILLING_EXPORT_TABLE`: fully qualified export table, for example `billing-project.billing_export.gcp_billing_export_resource_v1_XXXXXX`.
2. `COST_START_DATE`: inclusive evidence window start date, `YYYY-MM-DD`.
3. `COST_END_DATE`: exclusive evidence window end date, `YYYY-MM-DD`.

Dry run:

```bash
BILLING_EXPORT_TABLE=billing-project.billing_export.gcp_billing_export_resource_v1_XXXXXX \
COST_START_DATE=2026-04-01 \
COST_END_DATE=2026-04-02 \
bash scripts/ops/cloud-run-cost-report.sh
```

Live run:

```bash
LIVE_CHECKS=on \
BILLING_EXPORT_TABLE=billing-project.billing_export.gcp_billing_export_resource_v1_XXXXXX \
COST_START_DATE=2026-04-01 \
COST_END_DATE=2026-04-02 \
bash scripts/ops/cloud-run-cost-report.sh
```

The reusable SQL query is [cloud-run-cost-by-label.sql](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/infra/terraform/cloudrun/queries/cloud-run-cost-by-label.sql). It groups cost by date, service, SKU, environment, event id, runtime, component, workshop id, session id, release id, resource class, Redis mode, and cost owner labels. Attach the JSON output or exported CSV to the run template as cost evidence.

## Budget Alert Inputs

The Terraform root can create a project scoped budget alert when the billing account is available to the operator. Keep it disabled for local validation by leaving `budget_amount_units=0`.

Terraform inputs:

1. `billing_account_id`: billing account id that owns the project.
2. `budget_amount_units`: whole currency units for the event spend threshold.
3. `budget_currency`: billing currency, for example `USD` or `EUR`.
4. `budget_alert_thresholds`: alert fractions such as `0.5`, `0.8`, and `1.0`.
5. `budget_notification_channels`: Monitoring notification channel ids.
6. `budget_disable_default_iam_recipients`: set to `true` only if notification channels fully cover the operator path.

Record the resulting budget name from the Terraform `cost_tracking` output in the pilot template.

## Session Cost Labels

Every Cloud Run session service must carry these Billing export labels:

1. `environment`: Terraform environment.
2. `event-id`: event or pilot identifier.
3. `workshop-id`: workshop registry id.
4. `workshop-session-id`: session id.
5. `release-id`: nested release id from `workshops.yaml`.
6. `resource-class`: session resource class.
7. `redis-mode`: local process.

## 100 User Evidence Window

The accepted Cloud Run evidence window is:

1. `100` steady launch attempts.
2. `100` concurrent burst launch attempts.
3. Three soak cycles with enough launches to cover at least `100` total session starts.
4. Cleanup verification for every JSONL log.
5. Rollback validation after the launch window.
6. Readiness threshold check against the latest JSONL log.

Dry run:

```bash
bash scripts/loadtest/cloud-run-100-user-window.sh
```

Live run:

```bash
LIVE_CHECKS=on \
BASE_URL=https://CONTROL_PLANE_HOST \
TARGET_CONCURRENT_USERS=100 \
bash scripts/loadtest/cloud-run-100-user-window.sh
```

Readiness review:

```bash
INPUT=scripts/loadtest/output/cloudrun/LATEST.jsonl \
bash scripts/ops/cloud-run-readiness-check.sh
```

## Cleanup Evidence

Cleanup must prove both application and cloud resource cleanup.

Required checks:

1. Every launched session reaches a terminal state or is explicitly recorded as retained.
2. Terminal sessions have no stale public route metadata.
3. Cloud Run session services with evidence session ids are deleted.
4. Workspace prefixes are deleted or retained according to the declared retention policy.
5. Local Redis data is deleted or retained according to the session retention policy.

Application cleanup:

```bash
INPUT=scripts/loadtest/output/cloudrun/LATEST.jsonl \
bash scripts/loadtest/cloud-run-cleanup-check.sh
```

Cloud resource cleanup:

```bash
LIVE_CHECKS=on \
GCP_PROJECT=PROJECT_ID \
GCP_REGION=europe-west4 \
WORKSPACE_BUCKET=BUCKET_NAME \
EXPECT_WORKSPACE_PREFIX_DELETED=1 \
INPUT=scripts/loadtest/output/cloudrun/LATEST.jsonl \
bash scripts/ops/cloud-run-cleanup-check.sh
```

## Thresholds

Default rollback thresholds:

| Workshop class | Startup p95 | Cleanup p95 | Success rate |
| --- | --- | --- | --- |
| Standard Redis labs | 90 seconds | 45 seconds | At least 95 percent |
| Stateful labs | 120 seconds | 60 seconds | At least 95 percent |
| AI labs | 180 seconds | 90 seconds | At least 95 percent |

Override thresholds with environment variables only when the event owner accepts the new target and records the reason in the results template.

## Cost Decision

Cloud Run is accepted for the event when:

1. Startup and cleanup thresholds pass.
2. No stale session service remains after cleanup.
3. Local Redis cleanup evidence passes.
4. Event only cost is materially lower than the accepted always warm service estimate.
5. Always warm cost is documented separately and used only when startup targets require it.
6. Any Cloud Run quota increase is recorded with owner and date.

If cost evidence is missing, keep Cloud Run as technically validated but not cost validated.
