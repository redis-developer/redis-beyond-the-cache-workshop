---
id: P1
title: Control Plane Session Environment Contract
status: done
depends_on: []
owner: worker
allowed_files:
  - java-springboot/platform_control_plane/src/main/java/com/redis/workshop/platform/controlplane/session/CreateSessionRequest.java
  - java-springboot/platform_control_plane/src/main/java/com/redis/workshop/platform/controlplane/session/SessionService.java
  - java-springboot/platform_control_plane/src/main/java/com/redis/workshop/platform/controlplane/session/SessionLaunchDescriptor.java
  - java-springboot/platform_control_plane/src/test/java/com/redis/workshop/platform/controlplane/session/SessionControllerWebMvcTest.java
  - java-springboot/platform_control_plane/src/test/java/com/redis/workshop/platform/controlplane/session/SessionServiceTest.java
forbidden_files:
  - frontend/**
  - java-springboot/platform_execution_plane/**
  - java-springboot/platform_contracts/**
---

# P1: Control Plane Session Environment Contract

## Goal

Accept optional learner supplied environment variables on `POST /api/sessions`, validate them, and merge accepted values into the launch runtime config sent to the execution plane.

## Why This Exists

The execution plane already turns runtime config entries into session runner environment variables. The missing piece is the control plane create session contract.

## Required Changes

1. Add a `Map<String, String>` field named `sessionEnvironment` to `CreateSessionRequest` for session environment variables.
2. Normalize incoming keys and values by trimming whitespace.
3. Reject invalid keys with a client error. Valid keys should match standard environment variable names: first character letter or underscore, then letters, digits, or underscore.
4. Reject reserved keys that could change the runner or platform wiring, including `PORT`, `SERVER_PORT`, `JAVA_OPTS`, `JAVA_TOOL_OPTIONS`, `GRADLE_USER_HOME`, `PATH`, `HOME`, `SHELL`, and keys starting with `WORKSHOP_`, `RI_`, `REDIS_`, or `PLATFORM_`.
5. Preserve release runtime config and allow accepted learner variables such as `OPENAI_API_KEY`.
6. Do not persist the submitted values on `PlatformSessionRecord` or expose them in any response.
7. Add tests proving valid variables reach `SessionLaunchRequest.runtimeConfig()` and reserved or invalid variables are rejected.

## Acceptance Criteria

1. Creating a session without environment variables behaves exactly as before.
2. Creating a session with `OPENAI_API_KEY` adds it to the launch request runtime config.
3. Creating a session with `PORT` or `WORKSHOP_ID` fails with a `400 BAD_REQUEST`.
4. Creating a session with an invalid key such as `openai-api-key` fails with a `400 BAD_REQUEST`.
5. No session response includes submitted values.

## Verification

Run:

```bash
./java-springboot/gradlew -p java-springboot --no-daemon :platform_control_plane:test
```

## Out Of Scope

Frontend form work is owned by P2. Execution plane adapter behavior is owned by P3.

## Handoff Back

Report changed files, tests run, and any compatibility notes for the frontend packet.
