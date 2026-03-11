---
id: CV8
title: Scaffold Update
status: blocked
depends_on:
  - CV1
  - CV2
  - CV3
  - CV4
  - CV5
  - CV6
  - CV7
owner: unassigned
allowed_files:
  - scripts/new-workshop.sh
forbidden_files:
  - workshop-frontend-shared/**
  - java-springboot/workshop-infrastructure/**
  - java-springboot/1_session_management_frontend/**
  - java-springboot/2_full_text_search_frontend/**
  - java-springboot/3_distributed_locks_frontend/**
  - java-springboot/4_agent_memory_frontend/**
  - scripts/validate-workshops.sh
  - workshops.yaml
---

# Goal

Make new workshops start with the content-driven view shape instead of large embedded Vue instruction templates.

# Why this exists

- The platform only stays maintainable if new workshops begin from the standardized content-driven shape.
- The scaffold should encode the final pattern rather than rely on manual follow-up cleanup.

# Required changes

- Update `new-workshop.sh` to generate workshop content files following the contract from `CV1`.
- Generate thin Vue view stubs that consume the shared delivery/rendering path instead of embedding large instruction blocks.
- Keep the generated workshop aligned with the current manifest/editor structure.
- Update scaffold output text so contributors know how content files fit into the new workflow.

# Acceptance criteria

- A new scaffolded workshop contains content files for maintainable instructional authoring.
- Generated workshop views are thin wrappers rather than large hardcoded instruction templates.
- Scaffold output does not tell contributors to hand-author large Vue instruction sections.

# Verification

- In a temporary repo copy, run `bash scripts/new-workshop.sh 99_content_smoke "Content Smoke" content-smoke 8098 18098`.
- In that temporary repo copy, run `/bin/zsh -lc 'env JAVA_HOME=$(/usr/libexec/java_home -v 21) ./java-springboot/gradlew -p java-springboot --no-daemon -PskipFrontendBuild=true :99_content_smoke_frontend:test --tests com.redis.workshop.contentsmoke.frontend.ContentSmokeFrontendIntegrationTest'`.
- Manual check: confirm the generated frontend module includes content files and thin view/controller stubs instead of large embedded instruction blocks.

# Out of scope

- Editing existing workshop frontend modules.
- Validation logic changes.

# Handoff back

- Record the generated content file layout.
- Record any manual follow-up steps that still exist after scaffolding.
