---
id: P2
title: Email portal frontend and hub guard
status: done
depends_on: []
owner: unassigned
allowed_files:
  - frontend/src/App.vue
  - frontend/src/router/**
  - frontend/src/views/**
  - frontend/src/components/**
  - frontend/src/services/**
  - frontend/src/store/**
  - frontend/src/assets/**
forbidden_files:
  - java-springboot/**
  - infra/**
  - plans/email_portal_sessions/**
---

# Goal

Add a front page where learners enter an email and then see their own workshop portal.

# Why This Exists

The hub should be user scoped before a learner can deploy a workshop. This packet owns only the browser experience and must rely on the shared portal API contract from the index.

# Required Changes

1. Add a portal entry view for unauthenticated users with a single email field.
2. Add a portal API service for `POST /api/portal/login`, `GET /api/portal/me`, and `POST /api/portal/logout`.
3. Stop sending hardcoded actor headers from browser requests by default.
4. Use `credentials: include` for portal and session API calls.
5. Guard the workshop hub so it loads only after the portal user is known.
6. Show the normalized email in the hub header and provide a logout action.
7. After login, load catalog and sessions as today. The visible sessions must come from the backend owner filter.
8. Keep the existing hub card design and button behavior unless the new portal state requires a small header addition.

# Acceptance Criteria

1. Visiting `/` without a valid portal session shows the email entry page.
2. Entering a valid email takes the learner to the workshop hub.
3. Refreshing the hub keeps the learner authenticated if the Redis portal session cookie is still valid.
4. Logout returns the learner to the email entry page.
5. Browser requests to `/api/sessions` no longer include `X-Platform-Actor-Id` by default.

# Verification

1. Run `npm run build` from `frontend`.
2. Manually inspect the main unauthenticated and authenticated states if a local backend is available.

# Out Of Scope

1. Do not edit backend files.
2. Do not add password, invite code, or magic link flows.
3. Do not redesign the workshop cards.

# Handoff Back

Report changed views, the portal service API shape, and whether the frontend needs any backend contract adjustment.
