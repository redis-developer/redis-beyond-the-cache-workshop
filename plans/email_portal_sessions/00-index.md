---
title: Email Portal Sessions Packet Index
status: active
---

# Purpose

Add a first phase email portal where each learner enters an email, gets a Redis backed portal session, and can deploy workshop sessions owned by that email.

# Ground Rules

1. Only edit files listed in each packet `allowed_files`.
2. If a needed change falls outside the packet, stop and report the blocker.
3. No opportunistic cleanup.
4. Do not remove the existing workshop deployment flow.
5. Keep workshop runtime sessions owned by `PlatformSessionRecord.ownerUserId`.
6. Use normalized email as the learner actor id.
7. Store the browser portal session in Redis using an opaque token, not a raw email header from the browser.

# Shared Contract

The intended first phase contract is:

1. `POST /api/portal/login` accepts `{ "email": "learner@example.com" }`.
2. The login endpoint normalizes the email, stores an opaque portal session in Redis, sets an HttpOnly cookie, and returns the authenticated portal user.
3. `GET /api/portal/me` returns the current portal user when the cookie is valid, and an anonymous response or 401 when not authenticated.
4. `POST /api/portal/logout` removes the Redis session and clears the cookie.
5. Existing `/api/sessions` endpoints use the portal session actor. New workshop sessions are created with `ownerUserId` equal to the normalized email.
6. Existing header based actor authentication may remain for local scripts and operations, but the browser hub must not depend on spoofable actor headers.

# Parallel Lanes

1. Wave 1: P1, P2, and P3 are complete.
2. Wave 2: P4 is complete.
3. Final review verifies that a learner can log in with email, see only their sessions, launch one workshop, refresh, and keep ownership.

# Packet Summary

| ID | Title | Status | Depends on | Primary ownership |
| --- | --- | --- | --- | --- |
| P1 | Redis backed email portal identity | done | none | `java-springboot/platform_control_plane/**` identity and security files |
| P2 | Email portal frontend and hub guard | done | none | `frontend/src/**` |
| P3 | Redis deployment wiring | done | none | `infra/terraform/cloudrun/**` and operations docs |
| P4 | Ownership validation and smoke coverage | done | P1, P2, P3 | Cross feature tests and validation docs |
