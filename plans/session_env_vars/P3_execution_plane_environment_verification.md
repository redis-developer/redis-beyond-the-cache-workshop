---
id: P3
title: Execution Plane Environment Verification
status: done
depends_on: []
owner: worker
allowed_files:
  - java-springboot/platform_execution_plane/src/test/java/com/redis/workshop/platform/executionplane/docker/DockerSessionRuntimeAdapterTest.java
  - java-springboot/platform_execution_plane/src/test/java/com/redis/workshop/platform/executionplane/cloudrun/CloudRunSessionRuntimeAdapterTest.java
  - java-springboot/platform_contracts/src/test/java/com/redis/workshop/platform/contracts/executionplane/ExecutionPlaneContractSerializationTest.java
  - java-springboot/platform_contracts/src/test/java/com/redis/workshop/platform/contracts/executionplane/ExecutionPlaneContractValidationTest.java
forbidden_files:
  - frontend/**
  - java-springboot/platform_control_plane/**
  - java-springboot/platform_execution_plane/src/main/**
  - java-springboot/platform_contracts/src/main/**
---

# P3: Execution Plane Environment Verification

## Goal

Add tests proving execution plane runtime config entries become environment variables for both Docker and Cloud Run session runners.

## Why This Exists

The adapters appear to already support this behavior. This packet should lock the behavior down without changing production adapter code unless a test exposes a real gap.

## Required Changes

1. Add or extend Docker adapter tests so `OPENAI_API_KEY` in runtime config results in a Docker `--env OPENAI_API_KEY=value` argument.
2. Add or extend Cloud Run adapter tests so `OPENAI_API_KEY` in runtime config appears in service spec environment.
3. Add contract serialization coverage only if current serialization tests do not already cover arbitrary runtime config.
4. Do not change adapter source unless the tests prove the behavior is missing.
5. Do not log or print real secrets in tests. Use placeholder values like `test-openai-key`.

## Acceptance Criteria

1. Docker runtime config env passthrough is covered.
2. Cloud Run runtime config env passthrough is covered.
3. Existing execution plane contract tests still pass.
4. No source files outside allowed tests are modified unless a real defect is found and reported before changing.

## Verification

Run:

```bash
./java-springboot/gradlew -p java-springboot --no-daemon :platform_execution_plane:test :platform_contracts:test
```

## Out Of Scope

Control plane request validation is owned by P1. Hub UI behavior is owned by P2.

## Handoff Back

Report changed files, tests added, and whether adapter source changes were unnecessary.
