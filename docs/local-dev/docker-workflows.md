# Local Docker Workflows

Docker is local development infrastructure only.
The production runtime is Cloud Run session runners.

## Recommended Path

Use the authoring helper from the repository root:

```bash
./scripts/run-workshop.sh up 1_session_management
```

The helper starts dependency containers and runs the workshop frontend and backend from the working tree.
Use it for workshop authoring, content edits, learner code changes, and single workshop debugging.

Stop it with:

```bash
./scripts/run-workshop.sh down 1_session_management
```

## Local Platform Path

Use the control plane plus execution plane only for local platform lifecycle tests.
This path exercises launch, route binding, restart, rebuild restart, terminate, and execution plane contract calls.
It does not replace the authoring helper.
Cloud Run remains the production provider.

Build a local runner image before launching a session:

```bash
java-springboot/session_runtime_tools/runner-image/build-local-runner-image.sh \
  --workshop-id 1_session_management \
  --image-tag redis-workshop-session-runner:1_session_management
```

Start the execution plane on port 9002:

```bash
cd java-springboot
EXECUTION_PLANE_SHARED_SECRET=localdevsecret \
EXECUTION_PLANE_RUNTIME_PROVIDER=docker \
SERVER_PORT=9002 \
./gradlew :platform_execution_plane:bootRun \
  --args='--platform.execution-plane.docker.image-overrides.1_session_management=redis-workshop-session-runner:1_session_management'
```

Start the control plane on port 9001 in a second terminal:

```bash
cd java-springboot
EXECUTION_PLANE_SHARED_SECRET=localdevsecret \
EXECUTION_PLANE_BASE_URL=http://localhost:9002 \
SERVER_PORT=9001 \
./gradlew :platform_control_plane:bootRun
```

## Removed Local Surface

The legacy root container image path was removed.
It duplicated the authoring helper and required a privileged local container.

Do not restore it unless a concrete local testing requirement cannot be met by the helper.
