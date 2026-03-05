# Redis Beyond the Cache Workshop

A hands-on workshop series demonstrating Redis capabilities beyond simple caching: session management, full-text search, distributed locks, and AI agent memory.

## Quick Start

**Prerequisites:** Docker and Docker Compose

### Option 1: Workshop Hub (Recommended)

Run all workshops from a central hub:

```bash
docker compose up -d
```

Open **http://localhost:9000** to access the Workshop Hub.

The Hub provides:
- Workshop catalog with descriptions
- One-click start/stop for each workshop
- Integrated Redis Insight at **http://localhost:5540**

### Option 2: Run Individual Workshops

Run a specific workshop directly:

```bash
cd java-springboot/workshop-hub

# Start infrastructure (Redis, Redis Insight)
docker compose -f docker-compose.local.yml --profile infrastructure up -d

# Start a specific workshop
docker compose -f docker-compose.local.yml --profile workshop-1_session_management up -d
```

Replace `workshop-1_session_management` with:
- `workshop-2_full_text_search`
- `workshop-3_distributed_locks`
- `workshop-4_agent_memory`

## Workshops

| # | Workshop | Difficulty | Time | Port |
|---|----------|------------|------|------|
| 1 | [Session Management](java-springboot/1_session_management/README.md) | Beginner | 30 min | 8080 |
| 2 | [Full-Text Search](java-springboot/2_full_text_search/README.md) | Beginner | 30 min | 8081 |
| 3 | [Distributed Locks](java-springboot/3_distributed_locks/README.md) | Intermediate | 35 min | 8082 |
| 4 | [Agent Memory Server](java-springboot/4_agent_memory/README.md) | Intermediate | 40 min | 8083 |

## Workshop Format

Each workshop follows a consistent 3-stage format:

1. **See the Problem** - Experience the issue firsthand
2. **Implement the Solution** - Write code with guided instructions
3. **Verify It Works** - Test and observe the fix

## Accessing Services

| Service | URL | Description |
|---------|-----|-------------|
| Workshop Hub | http://localhost:9000 | Central dashboard |
| Redis Insight | http://localhost:5540 | Redis data browser |
| Workshop 1 | http://localhost:8080 | Session Management |
| Workshop 2 | http://localhost:8081 | Full-Text Search |
| Workshop 3 | http://localhost:8082 | Distributed Locks |
| Workshop 4 | http://localhost:8083 | Agent Memory |

## Requirements by Workshop

| Workshop | Redis | Postgres | OpenAI API Key |
|----------|-------|----------|----------------|
| 1. Session Management | Yes | No | No |
| 2. Full-Text Search | Yes | No | No |
| 3. Distributed Locks | Yes | Yes | No |
| 4. Agent Memory | Yes | No | Yes |

## Stopping Services

```bash
# Stop Workshop Hub
docker compose down

# Stop individual workshops
cd java-springboot/workshop-hub
docker compose -f docker-compose.local.yml down
```

## Troubleshooting

**Port conflict:**
```bash
# Check what's using a port
lsof -i :8080

# Stop all workshop containers
docker compose down
```

**Clean restart:**
```bash
docker compose down -v
docker compose up -d
```

## Repository Structure

```
redis-beyond-the-cache-workshop/
├── docker-compose.yml           # Workshop Hub launcher
├── workshops.yaml               # Workshop registry
├── java-springboot/
│   ├── 1_session_management/    # Workshop 1
│   ├── 2_full_text_search/      # Workshop 2
│   ├── 3_distributed_locks/     # Workshop 3
│   ├── 4_agent_memory/          # Workshop 4
│   └── workshop-hub/            # Hub application
└── workshop-frontend-shared/    # Shared UI components
```

## License

MIT

