# Docker Setup for Redis Workshop

This repository includes a Docker Compose configuration to run Redis and Redis Insight for the workshop.

## Prerequisites

- Docker installed on your machine
- Docker Compose installed (usually comes with Docker Desktop)

## Quick Start

### Start Redis and Redis Insight

From the root of this repository, run:

```bash
docker-compose up -d
```

This will start:
- **Redis** on port `6379`
- **Redis Insight** on port `5540`

## Workshop Hub Compose Files

The Workshop Hub compose files are generated from `workshops.yaml`:

```bash
./java-springboot/gradlew :workshop-hub:generateCompose
```

Generated files:
- `java-springboot/workshop-hub/docker-compose.local.yml`
- `java-springboot/workshop-hub/docker-compose.internal.yml`

Profiles are enabled for selective runs:
- Infrastructure only: `docker-compose -f java-springboot/workshop-hub/docker-compose.local.yml --profile infrastructure up -d`
- All workshops: `docker-compose -f java-springboot/workshop-hub/docker-compose.local.yml --profile workshops up -d`
- Single workshop: `docker-compose -f java-springboot/workshop-hub/docker-compose.local.yml --profile workshop-1_session_management up -d`

### Verify Services are Running

```bash
docker-compose ps
```

You should see both services running:
- `redis-workshop` - Redis server
- `redisinsight-workshop` - Redis Insight GUI

### Access Redis Insight

Open your browser and navigate to:
```
http://localhost:5540
```

### Connect Redis Insight to Redis

When you first open Redis Insight, you'll need to add a database connection:

1. Click "Add Redis Database"
2. Select "Add Database Manually"
3. Enter the following details:
   - **Host**: `redis-workshop` (or `localhost` if connecting from host machine)
   - **Port**: `6379`
   - **Database Alias**: `Workshop Redis`
4. Click "Add Redis Database"

## Useful Commands

### View Logs

```bash
# All services
docker-compose logs -f

# Redis only
docker-compose logs -f redis

# Redis Insight only
docker-compose logs -f redis-insight
```

### Stop Services

```bash
docker-compose stop
```

### Start Services (after stopping)

```bash
docker-compose start
```

### Stop and Remove Containers

```bash
docker-compose down
```

### Stop and Remove Containers + Volumes (clean slate)

```bash
docker-compose down -v
```

## Network Configuration

Both services are connected to a custom Docker network called `redis-workshop-network`. This allows:
- Redis Insight to connect to Redis using the service name `redis-workshop`
- Your applications to connect to Redis using `localhost:6379` from the host machine

## Data Persistence

Data is persisted in Docker volumes:
- `redis-workshop-data` - Redis data (AOF persistence enabled)
- `redisinsight-workshop-data` - Redis Insight configuration and preferences

## Troubleshooting

### Port Already in Use

If you get an error about ports already in use:

**For Redis (port 6379):**
```bash
# Find process using port 6379
lsof -ti:6379

# Kill the process
lsof -ti:6379 | xargs kill -9
```

**For Redis Insight (port 5540):**
```bash
# Find process using port 5540
lsof -ti:5540

# Kill the process
lsof -ti:5540 | xargs kill -9
```

### Redis Connection Issues

If Redis Insight can't connect to Redis:
1. Make sure both containers are running: `docker-compose ps`
2. Check Redis logs: `docker-compose logs redis`
3. Try using `localhost` instead of `redis-workshop` as the host

### Reset Everything

To completely reset and start fresh:
```bash
docker-compose down -v
docker-compose up -d
```

## Workshop Integration

The Java Spring Boot workshop (`java-springboot/1_session_management`) is configured to connect to Redis at `localhost:6379`. Make sure the Docker services are running before starting the workshop application.
