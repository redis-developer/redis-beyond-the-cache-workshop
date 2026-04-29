# Redis Beyond the Cache Workshop

Hands-on workshops demonstrating Redis capabilities beyond caching: session management, full-text search, distributed locks, and AI agent memory.

## Runtime Model

The product runtime is the control plane plus Cloud Run session runners.
Each learner session owns one runner service with the editable workshop JVM, Redis, and Redis Insight inside the session boundary.

Local Docker is only for development, workshop authoring, and reproducing sessions without deploying to Google Cloud.

Read these first:

1. [Docker setup](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/DOCKER-SETUP.md)
2. [Cloud Run session runner runbook](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/operations/cloud-run-session-runner.md)
3. [Redis session tenancy](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/docs/operations/redis-session-tenancy.md)

## Local Maintainer Quick Start

**Prerequisites:** Docker and Java 21.

```bash
./scripts/run-workshop.sh up 1_session_management
```

Open the frontend URL printed by the helper.

This is the recommended local development path. It starts workshop dependencies, allocates available local listener values, and runs the frontend and backend from your working tree.

Other workshops:

1. `./scripts/run-workshop.sh up 2_full_text_search`
2. `./scripts/run-workshop.sh up 3_distributed_locks`
3. `./scripts/run-workshop.sh up 4_agent_memory`

## Local Platform Lifecycle Testing

Use `scripts/run-workshop.sh` for workshop authoring, content edits, learner code changes, and single workshop debugging.
Use the control plane plus execution plane only when you need to test platform lifecycle behavior such as launch, route binding, restart, rebuild restart, terminate, and execution plane contract calls.

In local platform mode, Docker is only the execution plane runtime provider.
Cloud Run remains the production runtime provider.
Build a local runner image first:

```bash
java-springboot/session_runtime_tools/runner-image/build-local-runner-image.sh \
  --workshop-id 1_session_management \
  --image-tag redis-workshop-session-runner:1_session_management
```

Start the execution plane:

```bash
cd java-springboot
EXECUTION_PLANE_SHARED_SECRET=localdevsecret \
EXECUTION_PLANE_RUNTIME_PROVIDER=docker \
SERVER_PORT=9002 \
./gradlew :platform_execution_plane:bootRun \
  --args='--platform.execution-plane.docker.image-overrides.1_session_management=redis-workshop-session-runner:1_session_management'
```

Start the control plane in a second terminal:

```bash
cd java-springboot
EXECUTION_PLANE_SHARED_SECRET=localdevsecret \
EXECUTION_PLANE_BASE_URL=http://localhost:9002 \
SERVER_PORT=9001 \
./gradlew :platform_control_plane:bootRun
```

## Workshops

| # | Workshop |
|---|----------|
| 1 | [Session Management](java-springboot/1_session_management/README.md) |
| 2 | [Full-Text Search](java-springboot/2_full_text_search/README.md) |
| 3 | [Distributed Locks](java-springboot/3_distributed_locks/README.md) |
| 4 | [Agent Memory Server](java-springboot/4_agent_memory/README.md) |

## Services

| Service | URL |
|---------|-----|
| Workshop local frontend | Printed by `./scripts/run-workshop.sh up <id>` |
| Local Redis Insight | Printed by `./scripts/run-workshop.sh up <id>` |

## Requirements

| Workshop | Postgres | OpenAI API Key |
|----------|----------|----------------|
| 1-2 | No | No |
| 3 | Yes | No |
| 4 | No | Yes |

## Stopping

```bash
./scripts/run-workshop.sh down 1_session_management
```

## Contributing

### Adding a New Workshop

1. Run the scaffold script:
```bash
./scripts/new-workshop.sh <id> "<title>" <serviceName>
```

2. Fill in the generated TODOs in both modules and workshop content files.

3. Validate the registry and workshop wiring:
```bash
bash scripts/validate-workshops.sh
```

4. Test the workshop locally:
```bash
./scripts/run-workshop.sh up <id>
./scripts/run-workshop.sh down <id>
```

### Workshop Structure

Each workshop has a learner backend, a frontend composition module, and shared runtime shell code:

1. `java-springboot/<id>/` owns learner editable backend code and domain APIs only.
2. `java-springboot/<id>_frontend/` hosts the SPA and workshop content, then composes the shared shell with workshop app views.
3. Server side shell behavior belongs in `java-springboot/workshop-infrastructure`: editor APIs, diagnostics, backend proxying, Redis Insight proxying, session route handling, and runtime lifecycle wiring.
4. Shared Vue shell behavior belongs in `workshop-frontend-shared`: base path helpers, editor layout, restart controls, Redis Insight links, shell navigation, and shared content rendering.
5. Workshop app Vue code owns only workshop specific routes, copy, widgets, action handlers, demo state, and learner backend domain API calls through `getApiUrl`.

The scaffold creates both modules and registers both services in `workshops.yaml`.
Do not hardcode control plane URLs, Redis Insight ports, session runner endpoints, or direct `/api/editor/restore` calls in workshop app views.

### Registry

All workshops are registered in `workshops.yaml`. Required fields:

1. `id`, `title`, `description`
2. `serviceName`, `url`
3. `dockerfile`, `topics`
4. `frontendServiceName`, `frontendDockerfile`
5. `backendServiceName`, `backendDockerfile`
6. `releases` for deployable release metadata when the workshop is release backed

Keep `serviceName`, `url`, and `dockerfile` aligned with the frontend service values.
Do not add local listener values to `workshops.yaml`; the direct local maintainer helper allocates available values at startup and prints the resulting URLs.
Each release entry lives under its parent workshop as `workshops[].releases[]` and includes `releaseId`, `releaseVersion`, `mode`, `defaultForWorkshop`, `enabled`, `environments`, `images`, `resourceClass`, `sessionTtlMinutes`, and `mutableDependencies`.
Local Docker development can still use image overrides without changing the immutable release identity in `workshops.yaml`.

## License

MIT
