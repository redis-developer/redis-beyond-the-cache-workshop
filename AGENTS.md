# Workshop Implementation Guide (Agents)

This repository uses a single registry (`workshops.yaml`) as the source of truth for workshops. Follow these steps to add a new workshop cleanly and keep the registry, runtime, and UI in sync.

## Quick Start (Recommended)

Run the scaffold script, fill the generated TODOs, validate the registry, and test the workshop locally:

```bash
./scripts/new-workshop.sh <id> "<title>" <serviceName>
bash scripts/validate-workshops.sh
./scripts/run-workshop.sh up <id>
./scripts/run-workshop.sh down <id>
```

Example:

```bash
./scripts/new-workshop.sh 5_rate_limiting "Rate Limiting" rate-limiting
bash scripts/validate-workshops.sh
./scripts/run-workshop.sh up 5_rate_limiting
./scripts/run-workshop.sh down 5_rate_limiting
```

## Detailed Checklist

1) Create module structure
- Paths:
  - `java-springboot/<id>/` for learner editable backend code and domain APIs only
  - `java-springboot/<id>_frontend/` for SPA hosting, workshop content, and workshop app Vue views
- Backend module should include at least: `README.md`, `Dockerfile`, and workshop code.
- Frontend composition module should include at least: `Dockerfile`, thin Spring Boot runtime wiring, `workshop-content/`, and `frontend/` Vue source.

2) Register the workshop in `workshops.yaml`
- Required fields:
  - `id`
  - `title`
  - `description`
  - `difficulty`
  - `estimatedMinutes`
  - `serviceName`
  - `url`
  - `dockerfile`
  - `frontendServiceName`
  - `frontendDockerfile`
  - `backendServiceName`
  - `backendDockerfile`
  - `topics`
- Keep the legacy `serviceName` / `url` / `dockerfile` fields aligned with the frontend service values.
- Do not add local listener values to `workshops.yaml`; the direct local maintainer helper allocates available values at startup and prints URLs.

3) Include the module in Gradle settings
- Add:
  - `include("<id>")`
  - `include("<id>_frontend")`
- File: `java-springboot/settings.gradle.kts`

4) Fill generated TODOs
- Complete backend code, frontend workshop content, and workshop app views.

5) Validate and test
- Run: `bash scripts/validate-workshops.sh`
- Start locally: `./scripts/run-workshop.sh up <id>`
- Stop locally: `./scripts/run-workshop.sh down <id>`

## Runtime Expectations

- The control plane and execution plane use `workshops.yaml` for workshop metadata.
- Workshop services are addressed using the frontend/backend service names from `workshops.yaml`.
- Split workshops should keep the frontend service running while restart/redeploy actions target only the backend service.
- Local direct workshop runs allocate available listener values at startup and store them under the local state directory for status, restart, and down commands.
- Local Docker is for development and authoring. Production learner sessions run as Cloud Run session runners.
- Cloud Run session runners own the editable workshop JVM, Redis, and Redis Insight inside the session boundary.

## Runtime Shell And App Boundary

1. Server side shell behavior belongs in `java-springboot/workshop-infrastructure`, including editor APIs, diagnostics, backend proxying, Redis Insight proxying, session route handling, and runtime lifecycle wiring.
2. Shared Vue shell behavior belongs in `workshop-frontend-shared`, including base path helpers, editor layout, restart controls, Redis Insight links, shell navigation, and shared content rendering.
3. `java-springboot/<id>_frontend/` composes the shell plus workshop app views. It should not copy generic shell controllers, components, or utilities.
4. Workshop app Vue code owns only workshop specific routes, copy, widgets, action handlers, demo state, and domain API calls through `getApiUrl`.
5. Do not hardcode control plane URLs, Redis Insight ports, session runner endpoints, or direct `/api/editor/restore` calls in workshop app views.

## Local Testing

Use the helper from the repository root:

```bash
./scripts/run-workshop.sh up 1_session_management
./scripts/run-workshop.sh down 1_session_management
```

Use the matching workshop id for other workshops.

## Notes

- For non-Java workshops, still register in `workshops.yaml` and ensure the Dockerfile path is correct.
- Avoid hardcoding workshop metadata in the frontend or runtime services.
- Workshop frontend API calls must be safe for base paths so they work directly, behind `/workshop/<service>/`, and under `/session/{sessionId}/`.
- Do not use emojis in any code, UI text, or documentation.
