# Session Management Local Development

This document covers local maintainer workflows only.
Production sessions run through the control plane, execution plane, and Cloud Run session runners.

## Recommended Local Workflow

From the repository root:

```bash
./scripts/run-workshop.sh up 1_session_management
```

Open http://localhost:8080.

Stop it with:

```bash
./scripts/run-workshop.sh down 1_session_management
```

This path starts Redis and Redis Insight, then runs the frontend and backend from the working tree.

## Redis Insight

Local Redis Insight is available at http://localhost:5540.

In production, Redis Insight is session scoped and opened through the session route.
