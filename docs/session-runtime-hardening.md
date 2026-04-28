# Session Runtime Hardening

This document records the shared runtime hardening contract used by the release backed session path.

## Stateful dependencies

1. Session scoped Redis and Postgres containers bind only to `127.0.0.1` on the host.
2. Each session gets its own Docker network for runtime containers and session scoped sidecars.
3. `3_distributed_locks` uses per session Postgres credentials instead of the old fixed `workshop/workshop` pair.

## AI sidecars

1. AI sidecars stay on the private session network and are not published on learner visible host ports.
2. Workshop backends reach sidecars through the internal service alias, not through a public hub route.
3. `4_agent_memory` launch fails closed when both `OPENAI_API_KEY` and `ANTHROPIC_API_KEY` are absent.

## Secret handling

1. Provider secrets are resolved by the hub at launch time from its own environment or property configuration.
2. Session runtime command logging redacts sensitive environment values such as keys, tokens, passwords, and secrets.
3. Session scoped Postgres credentials are stored only in the session workspace secret file used by the hub restart path.

## Cloud Run execution plane contract

1. Production sessions run as one Cloud Run service per learner session.
2. Redis and Redis Insight run as localhost child processes inside the same session service.
3. Runtime configuration may carry non secret metadata and Secret Manager resource names, but it must not contain raw provider keys, platform tokens, passwords, or per session secret values.
4. Missing Secret Manager resources or missing IAM bindings fail closed before learner traffic is routed.
5. Session dependencies are private by default and are reachable only through localhost inside the session service or through the manager owned session proxy.
