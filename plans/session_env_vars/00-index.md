---
title: Session Environment Variables
status: in_progress
updated: 2026-05-04
---

# Session Environment Variables

Goal: allow the Workshop Hub to collect optional environment variables when a learner launches a session, then pass those values into the session runner runtime.

## Packet Board

| Packet | Title | Status | Depends On | Owner | Files |
| --- | --- | --- | --- | --- | --- |
| P1 | Control Plane Session Environment Contract | done | none | worker | `java-springboot/platform_control_plane/src/main/java/com/redis/workshop/platform/controlplane/session/**`, `java-springboot/platform_control_plane/src/test/java/com/redis/workshop/platform/controlplane/session/**` |
| P2 | Workshop Hub Environment Form | done | none | worker | `frontend/src/components/workshop/**`, `frontend/src/services/WorkshopService.js`, `frontend/src/store/index.js` |
| P3 | Execution Plane Environment Verification | done | none | worker | `java-springboot/platform_execution_plane/src/test/java/com/redis/workshop/platform/executionplane/**`, `java-springboot/platform_contracts/src/test/java/com/redis/workshop/platform/contracts/executionplane/**` |

## Integration Notes

P1 owns validation and merge behavior. The accepted API shape is `sessionEnvironment` on session creation, not a persisted secret field.

P2 owns the user experience and must not log or display values after launch. Treat values as sensitive because `OPENAI_API_KEY` is the first test case.

P3 verifies that existing execution plane adapters already pass runtime config into Docker and Cloud Run session environments.

## Whole Feature Acceptance

1. The hub can launch a workshop with `OPENAI_API_KEY` provided by the learner through `sessionEnvironment`.
2. The control plane rejects invalid environment variable names and reserved runtime keys.
3. Accepted session environment values are merged into launch runtime config and passed to the execution plane.
4. Existing session launch behavior still works without extra environment variables.
5. Tests cover backend merge behavior and frontend payload construction.

## Whole Feature Verification

Run:

```bash
./java-springboot/gradlew -p java-springboot --no-daemon :platform_control_plane:test :platform_execution_plane:test :platform_contracts:test
npm --prefix frontend run build
bash scripts/validate-workshops.sh
```
