# Docker Setup

Docker is used for local workshop dependencies.
Production sessions run as Cloud Run session runners.

## Recommended Local Workflow

Prerequisites:

1. Docker Desktop or a compatible Docker engine.
2. Java 21.

Start a workshop from the repository root:

```bash
./scripts/run-workshop.sh up 1_session_management
```

Open http://localhost:8080.

Stop it with:

```bash
./scripts/run-workshop.sh down 1_session_management
```

This helper is the only recommended local development path. It starts local dependencies and runs the workshop frontend and backend from your working tree.

## Local Platform Workflow

Use this workflow only when you need the control plane to call the execution plane and have the execution plane manage session runner containers through Docker.
For normal workshop implementation work, use `scripts/run-workshop.sh`.
Cloud Run remains the production runtime.

Build a local runner image before launching a session:

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

## Other Workshops

1. `./scripts/run-workshop.sh up 2_full_text_search`
2. `./scripts/run-workshop.sh up 3_distributed_locks`
3. `./scripts/run-workshop.sh up 4_agent_memory`

## Cloud Run Alignment

Local Docker should mirror the session runner shape without becoming the product runtime.
The runner manager owns restart, rebuild restart, restore, diagnostics, session route handling, backend proxying, Redis, and Redis Insight lifecycle.

Workshop app views should not hardcode control plane URLs, Redis Insight ports, session runner endpoints, or direct `/api/editor/restore` calls.
Use shared shell controls, shell URL helpers, and `getApiUrl` for domain API calls so local Docker and `/session/{sessionId}/` keep the same app behavior.

## Redis Insight

The local helper exposes Redis Insight at http://localhost:5540.

In production, Redis Insight belongs to each session runner and is reached through the session route.

## Troubleshooting

Check helper status:

```bash
./scripts/run-workshop.sh status 1_session_management
```

Stop and restart the helper if local dependencies need to be recreated:

```bash
./scripts/run-workshop.sh down 1_session_management
./scripts/run-workshop.sh up 1_session_management
```

If a port is already in use, stop the process that owns it before starting the workshop again.
