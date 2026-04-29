# Cloud Run Session Runner Operations

This runbook describes the production Cloud Run runtime target.
Local Docker execution plane runs are for platform lifecycle testing only and are documented in [docker-workflows.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/local-dev/docker-workflows.md).

Use it with:

1. `infra/terraform/cloudrun`
2. `scripts/loadtest/cloud-run-100-user-window.sh`
3. `scripts/ops/cloud-run-readiness-check.sh`
4. `scripts/ops/cloud-run-cleanup-check.sh`

## Deployment Flow

Cloud Run is the production target for event based workshops.

Operator sequence:

1. Publish digest pinned control plane, execution plane, and smoke session runner images.
2. Update nested release image metadata in `workshops.yaml` so `combined` points at the runner image digest.
4. Initialize and validate `infra/terraform/cloudrun`.
5. Apply the Cloud Run Terraform root with the real `project_id`, `environment`, `gateway_host`, control plane image, and execution plane image.
6. Copy the `session_runner_defaults` Terraform output into execution plane configuration.
7. Configure the public routing layer so learner traffic continues to enter through `/session/{sessionId}/`.
8. Enable Cloud Run launch rules only after one smoke launch, restart without rebuild, Redis cleanup, workspace cleanup, and terminate pass.

Validation commands:

```bash
terraform -chdir=infra/terraform/cloudrun init
terraform -chdir=infra/terraform/cloudrun validate
LIVE_CHECKS=on BASE_URL=https://CONTROL_PLANE_HOST bash scripts/loadtest/cloud-run-100-user-window.sh
INPUT=scripts/loadtest/output/cloudrun/LATEST.jsonl bash scripts/ops/cloud-run-readiness-check.sh
```

The Terraform root creates only long lived platform services. Per session Cloud Run services are created and deleted by the execution plane.

For local platform lifecycle tests, the execution plane can use `EXECUTION_PLANE_RUNTIME_PROVIDER=docker` instead of the production Cloud Run provider.
That local mode can use `platform.execution-plane.docker.image-overrides.<workshopId>` to point at a local runner image tag without editing the immutable release record in `workshops.yaml`.

## Runtime Model

Each learner session runs as one Cloud Run service.

Inside that service:

1. The manager process listens on `$PORT`.
2. The child workshop JVM listens on localhost.
3. The manager proxies learner app traffic to the child JVM.
4. The manager serves diagnostics, restore, status, restart, Redis Insight, and code editor proxy endpoints.
5. Redis runs as a local child process in the same session container.
6. Redis Insight runs as a local child process in the same session container when the image includes it.
7. VS Code runs as a local code-server child process in the same session container.
8. Editable files live in local workspace storage and are snapshotted to durable storage.

## Embedded VS Code Editor

The embedded editor uses `code-server` installed in the session runner image. The runner build argument defaults `CODE_SERVER_VERSION` to `4.103.2`. The selected setup starts code-server without learner authentication inside the container because public access is already scoped by the outer workshop session route. The process must bind only to `127.0.0.1` and defaults to port `39000`.

The runner image provides `/opt/runner/bin/start-code-editor.sh`, which starts code-server with:

1. `--auth none`
2. `--bind-addr 127.0.0.1:${WORKSHOP_LOCAL_CODE_EDITOR_PORT}`
3. `--disable-proxy`
4. `--disable-telemetry`
5. `--disable-update-check`
6. `--disable-workspace-trust`
7. user data and extensions under the session workspace state
8. workspace root from `WORKSHOP_SESSION_WORKSPACE_PATH`

The first version disables terminal exposure by setting the integrated terminal profile to `/bin/false` and exporting `SHELL=/bin/false`.

Manager editor settings:

1. `WORKSHOP_LOCAL_CODE_EDITOR_COMMAND`: command used to start the local code editor process. The runner default is `/opt/runner/bin/start-code-editor.sh`.
2. `WORKSHOP_LOCAL_CODE_EDITOR_PORT`: localhost port for code-server. Defaults to `39000`.
3. `WORKSHOP_LOCAL_CODE_EDITOR_HEALTH_PATH`: health path used in runtime status. Defaults to `/healthz`.
4. `WORKSHOP_SESSION_WORKSPACE_PATH`: workspace directory opened by code-server and used by reset, diagnostics, rebuild, and restart workflows.

Routing contract:

1. The manager proxies code-server HTTP traffic through `/code/`.
2. The manager proxies code-server WebSocket traffic under the same `/code/` path.
3. The public route exposes the editor as `/session/{sessionId}/code/`.
4. The control plane strips `/session/{sessionId}` before forwarding to the manager and sends `X-Forwarded-Prefix: /session/{sessionId}` so manager rewrites keep links session scoped.
5. No code-server port should be exposed directly from the Cloud Run service.

Reset and rebuild:

1. Reset uses the manager restore endpoint to restore editable files from the workshop manifest.
2. Reset does not automatically rebuild the child JVM.
3. Learners must use Recompile App after reset so the child JVM is rebuilt from the restored workspace.
4. Restart without rebuild reuses the current runnable child artifact.
5. Restart with rebuild runs the configured child rebuild command against the session workspace before starting the child JVM.

## Required Cloud Run Settings

1. Concurrency: `1`
2. Max instances: `1`
3. Min instances: `0` unless a workshop event needs warm starts
4. CPU allocation: always allocated while active
5. Request timeout: long enough for restart and diagnostics endpoints
6. Ingress: private unless the public routing layer owns exposure
7. Service labels: session id, workshop id, actor id, resource class, and cleanup owner

## Runner Image Build Surface

The runner image build surface lives under `java-springboot/session_runtime_tools/runner-image`.

The local builder creates a temporary Docker context containing:

1. The selected workshop backend module.
2. The selected workshop frontend module, which provides the manager process.
3. `workshop-infrastructure`, which provides the session runner manager implementation.
4. Shared Gradle wrapper and build files.
5. `workshop-frontend-shared`.
6. `workshops.yaml` as read only baseline metadata, including nested release records.

The builder synthesizes a minimal `settings.gradle.kts` inside the temporary context. It does not modify the checked in workshop source files.

Manual local build command. This defaults to the `local-redis-insight` variant, which includes Redis server:

```bash
java-springboot/session_runtime_tools/runner-image/build-local-runner-image.sh \
  --workshop-id 1_session_management \
  --image-tag redis-workshop-session-runner:1_session_management \
  --output build/session-runner-image.json
```

Add Redis Insight to the default local variant when the operator supplies the archive:

```bash
java-springboot/session_runtime_tools/runner-image/build-local-runner-image.sh \
  --workshop-id 1_session_management \
  --variant local-redis-insight \
  --redis-insight-archive /path/to/redisinsight.tar.gz \
  --image-tag redis-workshop-session-runner:1_session_management-local-redis-insight \
  --output build/session-runner-image-local-redis-insight.json
```

The Redis Insight archive is intentionally supplied by the operator or CI job. This keeps licensing, provenance, and version selection outside the repository while still producing an image that contains the binary when local process mode is selected.

## Image Naming And Digest Pinning

For a one workshop smoke test, publish all required images and emit Terraform image variables with:

```bash
terraform -chdir=infra/terraform/cloudrun apply \
  -target=google_artifact_registry_repository.session_runners \
  -var='project_id=personal-rdlts' \
  -var='environment=rdlts' \
  -var='gateway_host=placeholder.invalid' \
  -var='execution_plane_shared_secret=generate-a-long-random-value'

gcloud auth configure-docker europe-west4-docker.pkg.dev

scripts/ci/publish-cloud-run-smoke-images.sh \
  --project-id personal-rdlts \
  --region europe-west4 \
  --repository-id session-runners \
  --workshop-id 1_session_management \
  --push \
  --output build/cloud-run-smoke-images.json \
  --tfvars-output build/cloud-run-smoke-images.auto.tfvars
```

The script builds and pushes the session runner first, then builds the control plane image with the Session Management runner digest patched into the bundled `workshops.yaml` release metadata. Copy the generated `control_plane_image` and `execution_plane_image` values into the Terraform root variables for the smoke apply.

Local tags should use:

```text
redis-workshop-session-runner:<workshop-id>
redis-workshop-session-runner:<workshop-id>-local-redis-insight
```

Published tags should use:

```text
<registry>/<project>/session-runners/<workshop-id>:<source-revision>-<variant>
```

Release and launch metadata must reference the pushed image by digest:

```text
<registry>/<project>/session-runners/<workshop-id>@sha256:<digest>
```

CI scaffold command:

```bash
scripts/ci/publish-session-runner-image.sh \
  --workshop-id 1_session_management \
  --repository us-docker.pkg.dev/example/redis-workshops/session-runners/1_session_management \
  --source-revision "$GIT_SHA" \
  --push \
  --output build/session-runner-publish.json
```

When `--push` is omitted, the CI script writes local tag metadata without claiming a digest pinned reference.

## Required Image Labels

Runner images must include these labels:

1. `org.opencontainers.image.title`
2. `org.opencontainers.image.description`
3. `org.opencontainers.image.source-revision`
4. `com.redis.workshop.runner.module`
5. `com.redis.workshop.runner.workshop-id`
6. `com.redis.workshop.runner.variant`
7. `com.redis.workshop.runner.manager-port`
8. `com.redis.workshop.runner.child-auto-start`

The source revision label should match the commit SHA used by CI. The workshop id and module labels are used for operator inspection and should not replace digest pinned release metadata.

## Runner Environment Variables

Required or defaulted manager settings:

1. `PORT`: Cloud Run manager port. Defaults to `8080`.
2. `WORKSHOP_SESSION_RUNNER_ENABLED`: defaults to `true`.
3. `WORKSHOP_SESSION_RUNNER_AUTO_START`: defaults to `false` in the image. The image starts no child JVM until launch configuration opts in or restart starts it.
4. `WORKSHOP_SESSION_ID`: session id assigned by the control plane.
5. `WORKSHOP_ID`: selected workshop id baked into the image.
6. `WORKSHOP_SESSION_WORKSPACE_PATH`: writable local workspace path. Defaults to `/opt/runner/workspace`.
7. `WORKSHOP_BASELINE_PATH`: immutable baseline source path. Defaults to `/opt/runner/baseline`.
8. `WORKSHOP_CHILD_PORT`: localhost child JVM port. Defaults to `18080`.
9. `WORKSHOP_CHILD_COMMAND`: child JVM command. Leave empty until a session launch or restart should start the child process.
10. `WORKSHOP_CHILD_WORKING_DIRECTORY`: working directory for the child JVM command.
11. `WORKSHOP_CHILD_HEALTH_PATH`: optional child JVM health path.
12. `WORKSHOP_REDIS_MODE`: always `local-process` for Redis backed workshops.
13. `JAVA_OPTS`: optional manager JVM settings.

Local Redis and Redis Insight settings:

1. `WORKSHOP_LOCAL_REDIS_PORT`: defaults to `6379`.
2. `WORKSHOP_LOCAL_REDIS_COMMAND`: defaults to a localhost only `redis-server` command in the `local-redis-insight` variant.
3. `WORKSHOP_LOCAL_REDIS_HEALTH_COMMAND`: defaults to `redis-cli` ping in the `local-redis-insight` variant.
4. `WORKSHOP_REDIS_INSIGHT_PORT` or `WORKSHOP_LOCAL_REDIS_INSIGHT_PORT`: defaults to `5540`.
5. `WORKSHOP_REDIS_INSIGHT_COMMAND` or `WORKSHOP_LOCAL_REDIS_INSIGHT_COMMAND`: must point at the installed Redis Insight binary when Redis Insight is enabled.
6. `WORKSHOP_REDIS_INSIGHT_HEALTH_PATH` or `WORKSHOP_LOCAL_REDIS_INSIGHT_HEALTH_PATH`: optional Redis Insight health path.

## Ports And Health Checks

The image exposes only the manager port:

1. Manager HTTP port: `8080` by default through `$PORT`.
2. Child JVM port: localhost only, `18080` by default.
3. Local Redis port: localhost only, `6379` by default.
4. Local Redis Insight port: localhost only, `5540` by default.

The Docker health check calls:

```text
GET /health
```

The manager health endpoint must stay up when the child JVM is down. Child JVM, Redis, and Redis Insight health are reported through runner status and should not replace the manager health check.

## Local Proof Command Shape

The local proof should run the manager with a child command similar to:

```bash
WORKSHOP_SESSION_ID=local-session-1 \
WORKSHOP_ID=1_session_management \
WORKSHOP_SESSION_WORKSPACE_PATH=/tmp/workshop-session-1 \
WORKSHOP_CHILD_PORT=18080 \
WORKSHOP_CHILD_COMMAND="./gradlew bootRun --args='--server.port=18080'" \
WORKSHOP_CHILD_WORKING_DIRECTORY=/tmp/workshop-session-1/1_session_management \
./gradlew :1_session_management_frontend:bootRun
```

The exact module can change as implementation evolves, but the behavior must stay the same: manager process remains available and child JVM can be restarted.

For direct local workshop testing, use:

```bash
./scripts/run-workshop.sh up 1_session_management
./scripts/run-workshop.sh down 1_session_management
```

When `code-server` is available on the maintainer machine, the helper starts it automatically through `WORKSHOP_LOCAL_CODE_EDITOR_COMMAND` and prints local workshop URLs. When `code-server` is not installed, the helper still starts the manager and child JVM but logs that the embedded VS Code process will not start locally.

Local editor checks:

1. Open the workshop editor route.
2. Confirm the embedded frame loads from `/code/`.
3. In a public session route, confirm the same frame loads from `/session/{sessionId}/code/`.
4. Reset code through the editor shell.
5. Use Recompile App after reset and confirm the learner app reflects restored files.
6. Confirm Redis Insight remains available and restart or rebuild targets only the child JVM.

Validation note:

`bash scripts/validate-workshops.sh` verifies that generated workshop scaffolds use numbered content pages such as `workshop-content/views/0.yaml` and `workshop-content/views/1.yaml`, and that editor routes use the shared shell contract.

## Restart Procedure

1. Learner edits files through the editor.
2. Learner clicks restart.
3. Manager receives restart request.
4. Manager stops the child process.
5. Manager starts the child process from the current workspace.
6. Manager streams or stores child logs for diagnostics.
7. Manager reports current child state.

Restart must not build a new container image.

## Workspace Procedure

At launch:

1. Copy baseline source to local writable workspace.
2. Check the durable workspace prefix for the session.
3. If a durable snapshot exists, replace the local workspace with that full snapshot.
4. Start child JVM from local workspace.

During session:

1. Editor reads and writes local workspace files.
2. Diagnostics run against local workspace files.
3. Snapshot the full local workspace before a child JVM restart starts.
4. Snapshot the full local workspace after a restore request completes.
5. Snapshot the full local workspace on periodic checkpoints during active sessions.

At termination:

1. Write final snapshot if retention requires it.
2. Delete local workspace.
3. Delete or archive durable workspace prefix.

Cloud Run scale down:

1. The local workspace is not durable across instance replacement.
2. The latest completed durable snapshot is the recovery point.
3. Local edits made after the latest completed snapshot are not guaranteed to survive an instance stop.
4. The manager should report the last successful workspace snapshot time in runtime status once that status field exists.

Retention:

1. Local workspace data should be removed whenever a session terminates.
2. Durable workspace prefixes should use `sessions/{sessionId}/workspace`.
3. Normal sessions should retain durable snapshots until the session TTL cleanup completes.
4. Event sessions should keep durable snapshots for 7 days when learner recovery or support review is needed.
5. Debug sessions may keep durable snapshots for up to 30 days when incident review requires it.
6. Delete the durable prefix immediately when the learner or operator explicitly discards the session.
7. Use Cloud Storage lifecycle rules for durable prefix expiry rather than relying only on application cleanup.

Cloud Storage remains the default durable store. Use Filestore only when a workshop requires shared POSIX filesystem behavior, very high frequency small file mutation, or build tooling that does not work correctly from a restored snapshot model.

## Local Redis And Redis Insight Procedure

Use this mode by default. It keeps learner inspectable Redis state inside the same Cloud Run session boundary as the workshop JVM.

Use the `local-redis-insight` image variant for this mode. The variant installs `redis-server` and unpacks a supplied Redis Insight archive into `/opt/runner/redis-insight` during image build. Redis and Redis Insight must bind only to `127.0.0.1`; the manager owns any learner visible proxy path.

At launch:

1. Manager starts Redis as a localhost child process.
2. Manager starts Redis Insight as a localhost child process.
3. Manager starts the workshop child JVM with localhost Redis settings.
4. Manager proxies Redis Insight through the session path.
5. Manager reports local Redis and Redis Insight health probes separately from workshop JVM health.

At restart:

1. Workshop JVM restart does not restart Redis by default.
2. Redis and Redis Insight stay available unless restore explicitly resets session data.

At termination:

1. Manager stops the workshop JVM.
2. Manager stops Redis Insight.
3. Manager stops Redis.
4. Manager deletes or archives local Redis data according to retention policy.

## Rollback

Rollback options:

1. Disable Cloud Run session runner launch rules and return to local maintainer path.
2. Keep existing sessions alive while blocking new launches.
3. Delete failed session services after diagnostics are captured.

Cloud Run rollback procedure:

1. Disable new Cloud Run launches in the control plane launch rules.
2. Keep existing healthy session services alive until learners finish or the event owner approves termination.
3. Capture execution plane logs, runner status, workspace snapshot status, and local Redis cleanup evidence for failed sessions.
4. Revert launch rules to the last known good runtime path.
5. Run one smoke launch and one terminate on the last known good local maintainer path.
6. Run `bash scripts/ops/public-rollback-validation.sh --validate`.
7. Delete failed Cloud Run session services only after diagnostics are captured.
8. Record the decision in `docs/pilot/cloud-run-results-template.md`.

Rollback triggers:

1. Cross session data exposure.
2. Redis Insight session boundary failure.
3. Launch success rate below `95%`.
4. Stable `/session/{sessionId}/` route not preserved.
5. Restart without rebuild unavailable for healthy runner services.
6. Cloud Run service deletion lag leaves stale learner services after cleanup.
7. Local Redis state escapes the session boundary.

## Cost Controls

1. Keep min instances at `0` outside scheduled events.
2. Delete session services on termination.
3. Keep runner images generic and digest pinned.
4. Prefer Cloud Storage snapshots before Filestore.
5. Use local Redis inside the session runner.
6. Record actual event costs during Cloud Run cost validation.

Cost evidence must distinguish:

1. Idle platform with no active session services.
2. Event only operation with min instances `0`.
3. Always warm event operation with configured min instances for accepted cold start targets.

Record active session minutes, service min instance settings, CPU and memory class, Redis mode, Cloud Storage retained bytes, Artifact Registry pulls, and observed cleanup lag. Use [cloud-run-cost-and-scale.md](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/operations/cloud-run-cost-and-scale.md) for the evidence format.

## Build Cache Assumptions

1. The local builder creates an isolated Docker context per build unless `--context-dir` is supplied.
2. Docker layer cache should be reused for Gradle dependency downloads when the same builder host is used.
3. CI should use a persistent Docker or BuildKit cache keyed by Gradle files, the selected workshop module, `workshop-infrastructure`, and `workshop-frontend-shared`.
4. Cache misses are acceptable. Correctness must not depend on pre warmed Gradle, npm, or Docker caches.
5. The generated context intentionally excludes `build`, `.gradle`, and `node_modules` directories.
6. Redis Insight archives are cache inputs for `local-redis-insight` images and should be versioned outside the repository.

## Cleanup Checks

Run cleanup checks after every Cloud Run evidence window:

```bash
INPUT=scripts/loadtest/output/cloudrun/LATEST.jsonl bash scripts/loadtest/cloud-run-cleanup-check.sh
```

Live cleanup checks can also inspect Cloud Run services and workspace prefixes:

```bash
LIVE_CHECKS=on \
GCP_PROJECT=PROJECT_ID \
GCP_REGION=europe-west4 \
WORKSPACE_BUCKET=BUCKET_NAME \
EXPECT_WORKSPACE_PREFIX_DELETED=1 \
INPUT=scripts/loadtest/output/cloudrun/LATEST.jsonl \
bash scripts/ops/cloud-run-cleanup-check.sh
```

Required cleanup evidence:

1. No active or terminating sessions remain from the evidence window.
2. No stale public route metadata remains after terminal cleanup.
3. No Cloud Run session service remains for cleaned sessions.
4. Workspace prefixes are deleted or retained according to the declared retention mode.
5. Local Redis data is deleted or retained according to the declared retention mode.

## Readiness Checklist

1. Local runner proof passes.
2. Cloud Run service creation is idempotent.
3. Manager health survives child JVM failure.
4. Restart works without image rebuild.
5. Local Redis and Redis Insight bind only to localhost.
6. Workspace snapshots survive Cloud Run instance replacement.
7. code-server binds only to localhost and is reachable through `/code/`.
8. Public session routing exposes the editor only through `/session/{sessionId}/code/`.
9. Reset code requires Recompile App before restored files affect the child JVM.
10. Termination deletes Cloud Run service and cleans Redis or workspace state.
