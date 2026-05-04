---
id: P4
title: Ownership validation and smoke coverage
status: done
depends_on:
  - P1
  - P2
  - P3
owner: unassigned
allowed_files:
  - java-springboot/platform_control_plane/src/test/**
  - frontend/src/**/*.spec.js
  - docs/operations/**
  - scripts/ops/**
  - scripts/loadtest/**
forbidden_files:
  - java-springboot/platform_control_plane/src/main/**
  - frontend/src/views/**
  - frontend/src/services/**
  - frontend/src/store/**
  - infra/terraform/cloudrun/**
  - plans/email_portal_sessions/**
---

# Goal

Add focused validation that email scoped portal users can only see and operate on their own workshop sessions.

# Why This Exists

The main behavior depends on backend identity, frontend flow, and deployment wiring. This packet validates the cross feature behavior after those packets land.

# Required Changes

1. Add or update tests showing two different emails can each create one session for the same workshop.
2. Add or update tests showing one email cannot read, restart, or terminate another email's session.
3. Update operational smoke instructions to use portal login instead of hardcoded actor headers where practical.
4. Preserve existing operations scripts that still need header auth unless P1 explicitly removed that support.

# Acceptance Criteria

1. Two learners with different emails can independently own sessions for the same workshop.
2. A learner cannot access another learner's session by guessing the session id.
3. Smoke instructions explain how to verify portal login, launch, refresh, and logout.

# Verification

1. Run `./gradlew --no-daemon :platform_control_plane:test`.
2. Run `npm run build` from `frontend` if frontend tests or route assumptions are touched.
3. Run any updated smoke script in dry run mode if available.

# Out Of Scope

1. Do not implement backend or frontend feature logic here.
2. Do not change Terraform wiring.
3. Do not add load testing beyond a small ownership smoke path.

# Handoff Back

Report the exact ownership scenarios covered and any remaining manual test steps.
