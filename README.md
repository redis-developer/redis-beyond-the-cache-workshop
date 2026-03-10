# Redis Beyond the Cache Workshop

Hands-on workshops demonstrating Redis capabilities beyond caching: session management, full-text search, distributed locks, and AI agent memory.

## Quick Start

**Prerequisites:** Docker

### Option 1: Workshop Hub (Recommended)

```bash
docker compose up -d
```

Open **http://localhost:9000**

### Option 2: Run Individual Workshops

```bash
cd java-springboot/workshop-hub

# Start infrastructure (Redis, Redis Insight)
docker compose -f docker-compose.local.yml --profile infrastructure up -d

# Start a specific workshop
docker compose -f docker-compose.local.yml --profile workshop-1_session_management up -d
```

Available profiles:
- `workshop-1_session_management`
- `workshop-2_full_text_search`
- `workshop-3_distributed_locks`
- `workshop-4_agent_memory`

## Workshops

| # | Workshop | Port |
|---|----------|------|
| 1 | [Session Management](java-springboot/1_session_management/README.md) | 8080 |
| 2 | [Full-Text Search](java-springboot/2_full_text_search/README.md) | 8081 |
| 3 | [Distributed Locks](java-springboot/3_distributed_locks/README.md) | 8082 |
| 4 | [Agent Memory Server](java-springboot/4_agent_memory/README.md) | 8083 |

## Services

| Service | URL |
|---------|-----|
| Workshop Hub | http://localhost:9000 |
| Redis Insight | http://localhost:5540 |

## Requirements

| Workshop | Postgres | OpenAI API Key |
|----------|----------|----------------|
| 1-2 | No | No |
| 3 | Yes | No |
| 4 | No | Yes |

## Stopping

```bash
docker compose down
```

## Contributing

### Adding a New Workshop

1. Run the scaffold script:
```bash
./scripts/new-workshop.sh <id> "<title>" <serviceName> <frontendPort> [backendPort]
```

2. Regenerate compose files:
```bash
./java-springboot/gradlew :workshop-hub:generateCompose
```

3. Fill in the generated TODOs in both modules and update the frontend prebuild list in `java-springboot/workshop-hub/Dockerfile` if the hub DinD image should ship the workshop with prebuilt assets.

### Workshop Structure

Each workshop now consists of two modules:
- `java-springboot/<id>/` - Backend APIs, demo logic, learner-editable source, backend Dockerfile, README
- `java-springboot/<id>_frontend/` - SPA hosting, editor/file operations, backend proxying, frontend Dockerfile, Vue app

The scaffold creates both modules and registers both services in `workshops.yaml`.

### Registry

All workshops are registered in `workshops.yaml`. Required fields:
- `id`, `title`, `description`
- `serviceName`, `port`, `url`
- `dockerfile`, `topics`
- `frontendServiceName`, `frontendPort`, `frontendDockerfile`
- `backendServiceName`, `backendPort`, `backendDockerfile`

For compatibility with existing hub logic, keep `serviceName`, `port`, and `dockerfile` aligned with the frontend service values.

## License

MIT
