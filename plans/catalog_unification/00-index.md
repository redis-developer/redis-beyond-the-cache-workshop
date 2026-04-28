---
title: Catalog Unification Packets
status: active
---

# Purpose

Unify workshop discovery metadata and deployable release metadata into `workshops.yaml`.

# Ground Rules

- Only edit files listed in each packet `allowed_files`.
- Do not cross packet boundaries for adjacent cleanup.
- `workshops.yaml` and Java runtime loaders belong to P1 only.
- Validation and docs must follow the schema established by P1.

# Unified Shape

Each workshop entry owns its release list:

```yaml
releases:
  - releaseId: session-management-2026.04.1
    releaseVersion: 2026.04.1
    mode: LAB
    defaultForWorkshop: true
    enabled: true
    environments:
      - local
      - cloud-run
    images:
      combined: registry.example.com/workshops/session-management-runner@sha256:...
    resourceClass: small
    sessionTtlMinutes: 60
    mutableDependencies:
      - redis
```

# Parallel Lanes

1. Wave 1: P1 owns schema/runtime, P2 owns validation/script alignment, P3 owns docs cleanup.
2. Wave 2: Review, targeted repairs, full validation.

# Packet Summary

| ID | Title | Status | Depends on | Primary ownership |
| --- | --- | --- | --- | --- |
| P1 | Unified Runtime Registry | done | none | `workshops.yaml`, control-plane Java loaders/tests |
| P2 | Validator And Scripts | done | P1 schema shape | `scripts/**`, CI workflow references |
| P3 | Documentation Cleanup | done | P1 schema shape | `docs/**`, `README.md` |
| R1 | Stale Reference Repair | done | P2 | deleted catalog/rule path references |
