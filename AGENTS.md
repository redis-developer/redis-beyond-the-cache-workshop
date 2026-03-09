# Workshop Implementation Guide (Agents)

This repository uses a single registry (`workshops.yaml`) as the source of truth for workshops. Follow these steps to add a new workshop cleanly and keep the hub, compose files, and UI in sync.

## Quick Start (Recommended)

Run the scaffold script and then regenerate compose files:

```bash
./scripts/new-workshop.sh <id> "<title>" <serviceName> <port>
./java-springboot/gradlew :workshop-hub:generateCompose
```

Example:

```bash
./scripts/new-workshop.sh 3_rate_limiting "Rate Limiting" rate-limiting 8082
./java-springboot/gradlew :workshop-hub:generateCompose
```

## Detailed Checklist

1) Create module structure
- Path: `java-springboot/<id>/`
- Include at least: `README.md`, `Dockerfile`, and any workshop code.

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
  - `topics`

3) Include the module in Gradle settings
- Add: `include("<id>")`
- File: `java-springboot/settings.gradle.kts`

4) Regenerate compose files
- Run: `./java-springboot/gradlew :workshop-hub:generateCompose`
- Outputs:
  - `java-springboot/workshop-hub/docker-compose.local.yml`
  - `java-springboot/workshop-hub/docker-compose.internal.yml`

## Runtime Expectations

- The Workshop Hub reads `workshops.yaml` and exposes `/manager/api/workshops`.
- The frontend uses that endpoint for workshop metadata.
- Workshop services are controlled by the hub using the service name from `workshops.yaml`.

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
- Do not use emojis in any code, UI text, or documentation.
