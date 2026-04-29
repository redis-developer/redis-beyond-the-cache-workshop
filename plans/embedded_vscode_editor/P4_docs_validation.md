---
id: P4
title: Docs and validation cleanup
status: done
depends_on: ["P1", "P2", "P3", "P5", "P6", "P7", "R1"]
owner: Anscombe
allowed_files: ["docs/operations/cloud-run-session-runner.md", "java-springboot/workshop-infrastructure/README.md", "scripts/validate-workshops.sh", "plans/embedded_vscode_editor/00-index.md"]
forbidden_files: ["java-springboot/session_runtime_tools/runner-image/**", "java-springboot/workshop-infrastructure/src/main/java/**", "java-springboot/workshop-infrastructure/src/test/java/**", "workshop-frontend-shared/src/**", "java-springboot/1_session_management_frontend/**"]
---

# Goal

Document the embedded VS Code architecture and update validation expectations after implementation lands.

# Why This Exists

The production model changes from a custom editor UI to a session-local tool process. Operators and future workshop authors need a clear contract for how the editor is packaged, proxied, and tested.

# Required Changes

1. Document the session runner services: manager, learner JVM, Redis, Redis Insight, and VS Code server.
2. Document the `/code/` proxy path and the editor environment variables.
3. Document local testing steps and Cloud Run expectations.
4. Update validation if scaffold or boundary checks need to recognize embedded VS Code.
5. Update `00-index.md` packet statuses after implementation review.

# Acceptance Criteria

1. Operations docs describe how to test the editor locally and in Cloud Run.
2. Infrastructure README lists the code editor proxy and restore lifecycle.
3. Validation does not flag intended embedded VS Code shell usage.
4. The packet index reflects final packet statuses.

# Verification

1. `bash scripts/validate-workshops.sh`
2. `./gradlew --no-daemon :1_session_management_frontend:build`
3. Manual check: docs include `/code/`, `WORKSHOP_LOCAL_CODE_EDITOR_COMMAND`, and the recompile after reset requirement.

# Out Of Scope

1. Do not implement runtime behavior.
2. Do not implement frontend behavior.
3. Do not change workshop content files.

# Handoff Back

Report validation status, documentation links, and any known follow-up packets.
