# Redis Session Tenancy

Production sessions use one Redis mode: `local-process`.

Each learner session gets its own Cloud Run runner container. Redis runs as a localhost child process in that same container, and Redis Insight runs there too when the runner image includes it.

## Runtime Contract

The control plane passes local Redis settings as runtime config for Redis backed workshops.

Required or defaulted settings:

1. `WORKSHOP_REDIS_MODE`: always `local-process`.
2. `WORKSHOP_REDIS_KEY_PREFIX`: session scoped prefix used by workshop code when needed.
3. `WORKSHOP_REDIS_FLAVOR`: descriptive flavor, normally `standard`.
4. `WORKSHOP_LOCAL_REDIS_COMMAND`: localhost Redis command.
5. `WORKSHOP_LOCAL_REDIS_PORT`: localhost Redis port.
6. `WORKSHOP_LOCAL_REDIS_DATA_PATH`: optional local Redis data path.
7. `WORKSHOP_LOCAL_REDIS_HEALTH_COMMAND`: Redis health probe.
8. `WORKSHOP_REDIS_INSIGHT_COMMAND`: localhost Redis Insight command.
9. `WORKSHOP_REDIS_INSIGHT_PORT`: localhost Redis Insight port.
10. `WORKSHOP_REDIS_INSIGHT_BASE_PATH`: session proxy base path.
11. `WORKSHOP_REDIS_INSIGHT_HEALTH_PATH`: Redis Insight health probe path.

## Isolation

Local process mode isolates data by session container.

1. Redis binds to `127.0.0.1`.
2. Redis Insight binds to `127.0.0.1`.
3. Redis Insight is reachable only through the session manager proxy under `/session/{sessionId}/redis-insight/`.
4. Terminating the session stops Redis, stops Redis Insight, and deletes the session container.

## Cleanup

Termination cleanup checks the session boundary, not external credentials.

1. Stop the workshop JVM.
2. Stop Redis Insight.
3. Stop Redis.
4. Delete or archive the local workspace and Redis data according to retention policy.
5. Delete the Cloud Run session service.

## Remaining Risks

1. Redis Insight must not expose a route outside the session manager proxy.
2. Local Redis plus Redis Insight increases Cloud Run memory per session, so cost evidence must include the selected resource class.
