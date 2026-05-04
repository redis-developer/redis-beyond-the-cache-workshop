# Portal Session Redis Operations

Browser portal sessions are separate from workshop runtime Redis.

The portal login flow stores an opaque browser session token in Redis through the control plane. Workshop deployment records, launch state, ownership, audit rows, and cleanup state remain in the control plane database.

## Runtime Boundary

Use Redis only for short lived browser portal sessions.

1. Portal session keys map opaque token hashes to JSON session documents with the normalized learner email, expiry, and active workshop session ids.
2. The browser receives only an HttpOnly cookie.
3. `/api/sessions` uses the authenticated portal actor when a valid cookie is present.
4. Workshop deployment records remain keyed by `PlatformSessionRecord.ownerUserId` in the control plane database. Redis mirrors active session ids for the current portal session, but the database remains the source of truth after logout or Redis expiry.
5. Per workshop Redis still runs inside each session runner container in `local-process` mode.

Do not use the session runner local Redis process for portal sessions. It is scoped to one learner workshop runtime and is deleted with that runtime.

## Local

Local development can use a disposable Redis instance.

1. Run Redis on localhost or through Docker.
2. Configure the control plane with Spring Data Redis settings such as `SPRING_DATA_REDIS_HOST=localhost` and `SPRING_DATA_REDIS_PORT=6379`.
3. Keep cookie secure mode disabled unless testing through HTTPS.
4. Use a local key prefix so local browser sessions cannot collide with shared environments.

## Staging

Staging should use a dedicated Redis database or isolated key prefix.

1. Create the Redis service outside `infra/terraform/cloudrun`.
2. Store the Redis password in Secret Manager, for example `control-plane-portal-redis-password`.
3. Set the Terraform variables `control_plane_redis_host`, `control_plane_redis_port`, `control_plane_redis_database`, `control_plane_redis_username`, `control_plane_redis_ssl_enabled`, and `control_plane_redis_password_secret_id`.
4. Apply `infra/terraform/cloudrun` so the control plane receives the Redis endpoint and password secret reference.
5. Smoke test login, refresh, logout, and session launch ownership with one staging learner email.

## Portal Ownership Smoke

Use the browser flow for launch smoke checks where practical. Header based actor auth remains available for legacy operations scripts, but public learner validation should prove the Redis backed portal cookie path.

1. Open the hub and log in as `learner-a@example.com`.
2. Launch `1_session_management` and wait until the session reaches `READY` or `DEGRADED`.
3. Refresh the hub and confirm the launched session is still visible for `learner-a@example.com`.
4. Log out, then log in as `learner-b@example.com`.
5. Confirm the `learner-a@example.com` session is not listed for `learner-b@example.com`.
6. Launch `1_session_management` as `learner-b@example.com` and confirm it creates a separate session.
7. Log out and confirm `/api/portal/me` reports an anonymous portal user.

For an API level smoke against a running control plane, use separate cookie jars so ownership is scoped by portal session token:

```bash
BASE_URL=http://localhost:9000
COOKIE_A="$(mktemp)"
COOKIE_B="$(mktemp)"

curl -sS -c "${COOKIE_A}" -H "Content-Type: application/json" \
  -d '{"email":"learner-a@example.com"}' \
  "${BASE_URL}/api/portal/login"
curl -sS -b "${COOKIE_A}" -H "Content-Type: application/json" \
  -d '{"workshopId":"1_session_management"}' \
  "${BASE_URL}/api/sessions"
curl -sS -b "${COOKIE_A}" "${BASE_URL}/api/sessions"

curl -sS -c "${COOKIE_B}" -H "Content-Type: application/json" \
  -d '{"email":"learner-b@example.com"}' \
  "${BASE_URL}/api/portal/login"
curl -sS -b "${COOKIE_B}" "${BASE_URL}/api/sessions"
curl -sS -b "${COOKIE_B}" -H "Content-Type: application/json" \
  -d '{"workshopId":"1_session_management"}' \
  "${BASE_URL}/api/sessions"
curl -sS -b "${COOKIE_B}" -X POST "${BASE_URL}/api/portal/logout"
```

Expected result:

1. Each email can create one active session for `1_session_management`.
2. The `learner-b@example.com` session list does not include the `learner-a@example.com` session id.
3. Browser refresh keeps the portal user authenticated until logout.
4. Logging out and logging back in with the same email restores visible workshop sessions from the control plane database and writes those session ids into the new Redis portal session document.

## Production

Production should use a managed Redis endpoint with access restricted to the control plane path.

1. Provision Redis outside this Terraform root.
2. Require TLS when the provider supports it and set `control_plane_redis_ssl_enabled=true`.
3. Store passwords or tokens only in Secret Manager.
4. Rotate credentials by updating the Secret Manager secret version and reapplying Terraform if a pinned version is used.
5. Keep portal session TTL short enough for browser session risk and long enough for the workshop event.
6. Confirm database backups and retention policy cover the control plane database, not Redis portal session keys.

The Redis data is intentionally disposable. If Redis is flushed or unavailable, learners may need to log in again, but existing workshop deployment records remain in the control plane database and are reattached to the next portal session for the same email.
