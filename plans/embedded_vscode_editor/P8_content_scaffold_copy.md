---
id: P8
title: Content and scaffold copy
status: done
depends_on: ["P7"]
owner: Hilbert
allowed_files: ["java-springboot/1_session_management_frontend/src/main/resources/workshop-content/**", "java-springboot/2_full_text_search_frontend/src/main/resources/workshop-content/**", "java-springboot/3_distributed_locks_frontend/src/main/resources/workshop-content/**", "java-springboot/4_agent_memory_frontend/src/main/resources/workshop-content/**", "scripts/new-workshop.sh", "scripts/validate-workshops.sh"]
forbidden_files: ["workshop-frontend-shared/src/**", "java-springboot/*_frontend/frontend/src/**", "java-springboot/workshop-infrastructure/**", "java-springboot/session_runtime_tools/**", "java-springboot/platform_control_plane/**"]
---

# Goal

Align learner-facing content and scaffold copy with the embedded VS Code editor.

# Why This Exists

Some workshop pages currently refer to a custom in-browser code editor. The copy should describe the new VS Code based experience without changing the technical implementation.

# Required Changes

1. Update workshop instruction copy that names the old editor experience.
2. Update scaffold output copy if generated workshops should mention embedded VS Code.
3. Update validation expectations only if the scaffold wording changes.
4. Do not change behavior.

# Acceptance Criteria

1. Existing workshop instructions refer to VS Code or Code Editor consistently.
2. Generated workshop copy still explains that the learner app runs in an iframe and code editing is handled by the stable shell.
3. Validation passes except for known pre-existing repository issues explicitly reported by the packet owner.

# Verification

1. `bash scripts/validate-workshops.sh`
2. Manual content review for all edited YAML files.

# Out Of Scope

1. Do not edit frontend components.
2. Do not edit runtime or proxy code.
3. Do not change actual workshop step logic.

# Handoff Back

Report edited copy, validation result, and any content follow-up.
