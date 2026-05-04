---
id: P1
title: Redis backed email portal identity
status: done
depends_on: []
owner: unassigned
allowed_files:
  - java-springboot/platform_control_plane/build.gradle.kts
  - java-springboot/platform_control_plane/src/main/java/com/redis/workshop/platform/controlplane/security/**
  - java-springboot/platform_control_plane/src/main/java/com/redis/workshop/platform/controlplane/portal/**
  - java-springboot/platform_control_plane/src/main/resources/application.yaml
  - java-springboot/platform_control_plane/src/main/resources/application-local.yaml
  - java-springboot/platform_control_plane/src/test/java/com/redis/workshop/platform/controlplane/security/**
  - java-springboot/platform_control_plane/src/test/java/com/redis/workshop/platform/controlplane/portal/**
forbidden_files:
  - frontend/**
  - infra/**
  - java-springboot/platform_control_plane/src/main/java/com/redis/workshop/platform/controlplane/session/**
  - plans/email_portal_sessions/**
---

# Goal

Create a passwordless email portal identity backed by Redis and expose it to Spring Security as the current learner actor.

# Why This Exists

The current hub uses browser supplied actor headers. That is fine for local scripts, but not for a user facing portal because a learner could spoof another email. This packet adds an opaque Redis backed portal session so the backend, not the browser, decides the authenticated actor.

# Required Changes

1. Add the minimal Redis dependency needed for a Redis backed portal session store.
2. Add portal configuration for cookie name, TTL, secure cookie behavior, and Redis key prefix.
3. Add an email normalization and validation path. Lowercase and trim emails before they become actor ids.
4. Add `POST /api/portal/login`, `GET /api/portal/me`, and `POST /api/portal/logout`.
5. Store portal sessions in Redis with an opaque random token and TTL. Do not store raw token values as keys without hashing.
6. Set an HttpOnly SameSite Lax cookie on login and clear it on logout.
7. Add a portal session authentication filter before anonymous authentication. It should resolve the cookie, load the Redis session, and create a `CurrentActor` with type `LEARNER` and authority `platform:learner`.
8. Keep `HeaderAuthenticatedActorFilter` available for local or operational clients, but do not make the browser depend on it.
9. Permit unauthenticated access to the portal login and current user endpoints as needed.

# Acceptance Criteria

1. A valid email login creates a Redis backed portal session and returns the normalized email.
2. A request with the portal cookie can call `/api/sessions`.
3. A request without the portal cookie cannot call `/api/sessions`.
4. Logout deletes or invalidates the Redis session and clears the cookie.
5. Invalid email input returns a validation error.

# Verification

1. Run `./gradlew --no-daemon :platform_control_plane:test`.
2. Add focused tests for login, current user, logout, and authentication filter behavior.
3. Record any Redis environment variables introduced.

# Out Of Scope

1. Do not edit frontend files.
2. Do not change the session launch flow or execution plane client.
3. Do not add passwords, magic links, or external identity providers.

# Handoff Back

Report the endpoint contract, cookie name, Redis key prefix, new configuration keys, tests added, and any blocker around Redis configuration.
