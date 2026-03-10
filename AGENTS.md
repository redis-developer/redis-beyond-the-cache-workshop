# Workshop Implementation Guide (Agents)

This repository uses a single registry (`workshops.yaml`) as the source of truth for workshops. Follow these steps to add a new workshop cleanly and keep the hub, compose files, and UI in sync.

## Quick Start (Recommended)

Run the scaffold script and then regenerate compose files:

```bash
./scripts/new-workshop.sh <id> "<title>" <serviceName> <frontendPort> [backendPort]
./java-springboot/gradlew :workshop-hub:generateCompose
```

Example:

```bash
./scripts/new-workshop.sh 5_rate_limiting "Rate Limiting" rate-limiting 8084 18084
./java-springboot/gradlew :workshop-hub:generateCompose
```

## Detailed Checklist

1) Create module structure
- Paths:
  - `java-springboot/<id>/` for backend APIs and learner-editable source
  - `java-springboot/<id>_frontend/` for SPA hosting, editor endpoints, and backend proxying
- Backend module should include at least: `README.md`, `Dockerfile`, and workshop code.
- Frontend module should include at least: `Dockerfile`, Spring Boot frontend runtime wiring, and `frontend/` Vue source.

2) Register the workshop in `workshops.yaml`
- Required fields:
  - `id`
  - `title`
  - `description`
  - `difficulty`
  - `estimatedMinutes`
  - `serviceName`
  - `port`
  - `url`
  - `dockerfile`
  - `frontendServiceName`
  - `frontendPort`
  - `frontendDockerfile`
  - `backendServiceName`
  - `backendPort`
  - `backendDockerfile`
  - `topics`
- Keep the legacy `serviceName` / `port` / `dockerfile` fields aligned with the frontend service values.

3) Include the module in Gradle settings
- Add:
  - `include("<id>")`
  - `include("<id>_frontend")`
- File: `java-springboot/settings.gradle.kts`

4) Regenerate compose files
- Run: `./java-springboot/gradlew :workshop-hub:generateCompose`
- Outputs:
  - `java-springboot/workshop-hub/docker-compose.local.yml`
  - `java-springboot/workshop-hub/docker-compose.internal.yml`

## Runtime Expectations

- The Workshop Hub reads `workshops.yaml` and exposes `/manager/api/workshops`.
- The frontend uses that endpoint for workshop metadata.
- Workshop services are controlled by the hub using the frontend/backend service names from `workshops.yaml`.
- Split workshops should keep the frontend service running while restart/redeploy actions target only the backend service.

## Profiles

Generated compose files include profiles for selective runs:
- `--profile infrastructure`
- `--profile workshops`
- `--profile workshop-<id>`

Example:

```bash
docker-compose -f java-springboot/workshop-hub/docker-compose.local.yml --profile workshop-1_session_management up -d
```

## Notes

- For non-Java workshops, still register in `workshops.yaml` and ensure the Dockerfile path is correct.
- Avoid hardcoding workshop metadata in the frontend or hub services.
- Workshop frontend API calls must be base-path-safe so they work both directly and behind `/workshop/<service>/`.
- If a new workshop frontend should be prebuilt into the hub DinD image, update `java-springboot/workshop-hub/Dockerfile`.
- Do not use emojis in any code, UI text, or documentation.
