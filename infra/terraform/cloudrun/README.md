# Cloud Run Platform Foundation

This Terraform root owns the Phase 9 Cloud Run platform foundation.

It creates long lived platform resources only:

1. Required Google Cloud APIs.
2. Artifact Registry repository for digest pinned session runner images.
3. Cloud Run services for the control plane and execution plane.
4. Service accounts and IAM bindings for platform services and dynamic session runners.
5. Cloud Storage bucket for durable session workspace snapshots.
6. BigQuery billing export landing dataset and optional project scoped budget alert.
7. Default settings the execution plane uses when it creates one Cloud Run session service per learner session.

The execution plane creates and deletes per session Cloud Run services at runtime. Do not model learner session services directly in Terraform.

## Required Inputs

1. `project_id`
2. `environment`
3. `gateway_host`
4. `execution_plane_shared_secret`

Optional image inputs default to placeholder digest references so `terraform validate` can run before real images exist. Replace them with digest pinned images before `terraform apply`.

Because Cloud Run needs image digests before the full Terraform apply, create the Artifact Registry repository first. If Terraform owns the repository, run a targeted bootstrap apply with the same required variables you will use for the full apply:

```bash
terraform -chdir=infra/terraform/cloudrun init
terraform -chdir=infra/terraform/cloudrun apply \
  -target=google_artifact_registry_repository.session_runners \
  -var='project_id=personal-rdlts' \
  -var='environment=rdlts' \
  -var='gateway_host=placeholder.invalid' \
  -var='execution_plane_shared_secret=generate-a-long-random-value'
```

Then authenticate Docker to Artifact Registry:

```bash
gcloud auth configure-docker europe-west4-docker.pkg.dev
```

Publish smoke images first:

```bash
scripts/ci/publish-cloud-run-smoke-images.sh \
  --project-id personal-rdlts \
  --region europe-west4 \
  --repository-id session-runners \
  --workshop-id 1_session_management \
  --push \
  --output build/cloud-run-smoke-images.json \
  --tfvars-output build/cloud-run-smoke-images.auto.tfvars
```

For a personal smoke deployment in project `personal-rdlts`, override these values before apply:

```hcl
project_id                    = "personal-rdlts"
environment                   = "rdlts"
gateway_host                  = "YOUR_GATEWAY_HOST"
execution_plane_shared_secret = "generate-a-long-random-value"
control_plane_image           = "europe-west4-docker.pkg.dev/personal-rdlts/YOUR_REPOSITORY/control-plane@sha256:YOUR_DIGEST"
execution_plane_image         = "europe-west4-docker.pkg.dev/personal-rdlts/YOUR_REPOSITORY/execution-plane@sha256:YOUR_DIGEST"
event_id                      = "personal-rdlts-smoke"
```

Set `workspace_bucket_name` only when the generated bucket name is unavailable. Set `billing_account_id`, `budget_amount_units`, and `budget_notification_channels` only when budget alerts should be created.

To store browser portal sessions in Redis, provision Redis outside this Terraform root and pass the endpoint to the control plane:

```hcl
control_plane_redis_host                       = "redis.example.internal"
control_plane_redis_port                       = 6379
control_plane_redis_database                   = 0
control_plane_redis_username                   = "default"
control_plane_redis_ssl_enabled                = true
control_plane_redis_password_secret_id         = "workshop-hub-redis-password"
control_plane_redis_password_secret_version    = "latest"
```

The password secret must already exist in Secret Manager in `project_id`. Terraform grants the control plane service account `roles/secretmanager.secretAccessor` for that secret and injects it as `WORKSHOP_HUB_REDIS_PASSWORD`.

If you do not have a custom domain yet, set `gateway_host` to a temporary value for the first apply, read `cloud_run_services.control_plane.uri` from the outputs, strip the `https://` prefix, then apply again with `gateway_host` set to that host before launching sessions.

The current implementation proxies through Cloud Run service URLs, so `execution_plane_ingress` and `session_runner_ingress` default to `INGRESS_TRAFFIC_ALL`. Move them to private ingress only after the control plane uses a private service path that is verified by a launch smoke test.

Use the generated image variable file in the full plan and apply:

```bash
terraform -chdir=infra/terraform/cloudrun plan \
  -var-file=../../../build/cloud-run-smoke-images.auto.tfvars
```

## Validation

```bash
terraform -chdir=infra/terraform/cloudrun init
terraform -chdir=infra/terraform/cloudrun validate
```

## Session Runner Defaults

The deployed execution plane uses the real Google Cloud Run runtime client by default through `PLATFORM_EXECUTION_PLANE_CLOUD_RUN_CLIENT=google`.

The deployed control plane receives `EXECUTION_PLANE_BASE_URL` from the execution plane service URI and sends `EXECUTION_PLANE_SHARED_SECRET` on internal execution plane calls. Both long lived Cloud Run services disable Cloud Run invoker IAM in this Terraform root; keep the execution plane shared secret in place because the current verified path uses Cloud Run URL ingress.

When `control_plane_redis_host` is set, the control plane also receives:

```text
WORKSHOP_HUB_REDIS_HOST
WORKSHOP_HUB_REDIS_PORT
WORKSHOP_HUB_REDIS_DATABASE
WORKSHOP_HUB_REDIS_SSL_ENABLED
WORKSHOP_HUB_REDIS_USERNAME
WORKSHOP_HUB_REDIS_PASSWORD
```

`WORKSHOP_HUB_REDIS_USERNAME` is omitted when `control_plane_redis_username` is null. `WORKSHOP_HUB_REDIS_PASSWORD` is omitted when `control_plane_redis_password_secret_id` is null.

The `session_runner_defaults` output records the environment defaults used by the execution plane:

1. Concurrency `1`.
2. Max instances `1`.
3. Min instances `0`.
4. Manager port `8080`.
5. Manager restart timeout `290s`, covering rebuild plus child application startup.
6. Cloud Run URL ingress for the current verified control plane proxy path.
7. Stable public route ownership through `/session/{sessionId}/`.
8. Cost labels for `environment`, `event-id`, `workshop-id`, `workshop-session-id`, `resource-class`, and `redis-mode`.

The output keys match the execution plane environment variables, including:

```text
PLATFORM_EXECUTION_PLANE_CLOUD_RUN_CLIENT=google
PLATFORM_EXECUTION_PLANE_CLOUD_RUN_MANAGER_PORT=8080
PLATFORM_EXECUTION_PLANE_CLOUD_RUN_MANAGER_RESTART_TIMEOUT=290s
PLATFORM_EXECUTION_PLANE_CLOUD_RUN_CONCURRENCY=1000
PLATFORM_EXECUTION_PLANE_CLOUD_RUN_MIN_INSTANCES=0
PLATFORM_EXECUTION_PLANE_CLOUD_RUN_MAX_INSTANCES=1
PLATFORM_EXECUTION_PLANE_CLOUD_RUN_INGRESS=INGRESS_TRAFFIC_ALL
```

## Cost Posture

The default posture is event based:

1. Control plane and execution plane can scale to zero outside events when acceptable for the environment.
2. Session services use min instances `0`.
3. Session services are deleted on termination.
4. Workspace snapshots use Cloud Storage lifecycle cleanup.
5. Redis normally runs as a per session child process inside the session runner container. Redis Cloud is an explicit managed override.

Always warm events should override only the long lived service min instance settings needed to meet the accepted startup target.

## Cost Attribution

Managed resources receive the shared labels from `var.labels` plus `environment`, `managed_by=terraform`, and `runtime=cloud-run`. Control plane and execution plane services also set `component` so Billing export reports can separate long lived service cost.

Dynamic session services are created by the execution plane, not Terraform. The execution plane receives `environment` and `event_id` from this root and combines them with per request workshop id, session id, release id, resource class, and Redis mode labels.

The root creates a BigQuery dataset named by `billing_export_dataset_id`. The Cloud Billing export still has to be connected to that dataset from the billing account. Set `billing_account_id` and a positive `budget_amount_units` to create a project scoped budget alert.

Use the billing query in `queries/cloud-run-cost-by-label.sql` through:

```bash
BILLING_EXPORT_TABLE=billing-project.billing_export.gcp_billing_export_resource_v1_XXXXXX \
COST_START_DATE=2026-04-01 \
COST_END_DATE=2026-04-02 \
bash scripts/ops/cloud-run-cost-report.sh
```

Set `LIVE_CHECKS=on` only when the operator has BigQuery access and is ready to capture cost evidence for the pilot template.
