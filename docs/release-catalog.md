# Unified Workshop Release Catalog Contract

`workshops.yaml` is the single source of truth for both workshop discovery metadata and deployable release metadata. Each workshop entry owns its release records under `workshops[].releases[]`.

## Scope

The unified catalog currently contains release metadata for the release backed workshops:

1. `1_session_management`
2. `2_full_text_search`
3. `3_distributed_locks`
4. `4_agent_memory`

## Purpose

1. `workshops.yaml` defines hub metadata, service metadata, and deployable release metadata in one registry.
2. Each `workshops[].releases[]` entry defines an immutable release record for the control plane.
3. The release record also defines default selection, enablement, and environment scope.
4. Local Docker development can still override image references at launch time without changing release identity.
5. Local listener assignments are not stored in the registry. The direct maintainer helper allocates them at startup.

## Release Shape

```yaml
workshops:
  - id: 1_session_management
    title: Session Management
    serviceName: session-management-frontend
    url: /workshop/session-management-frontend/
    dockerfile: java-springboot/1_session_management_frontend/Dockerfile
    frontendServiceName: session-management-frontend
    frontendDockerfile: java-springboot/1_session_management_frontend/Dockerfile
    backendServiceName: session-management
    backendDockerfile: java-springboot/1_session_management/Dockerfile
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
          combined: registry.example.com/workshops/session-management-runner@sha256:<64-hex-digest>
        resourceClass: small
        sessionTtlMinutes: 60
        mutableDependencies:
          - redis
```

Current release identities:

| Workshop | Release ID | Release Version | Resource Class | Session TTL | Mutable Dependencies |
| --- | --- | --- | --- | --- | --- |
| `1_session_management` | `session-management-2026.04.1` | `2026.04.1` | `small` | `60` | `redis` |
| `2_full_text_search` | `full-text-search-2026.04.1` | `2026.04.1` | `small` | `60` | `redis` |
| `3_distributed_locks` | `distributed-locks-2026.04.1` | `2026.04.1` | `medium` | `60` | `redis`, `postgres` |
| `4_agent_memory` | `agent-memory-2026.04.1` | `2026.04.1` | `medium` | `60` | `redis` |

## Required Fields

1. `releaseId`
2. `releaseVersion`
3. `mode`
4. `defaultForWorkshop`
5. `enabled`
6. `environments`
7. At least one digest pinned image reference under `images`
8. `resourceClass`
9. `sessionTtlMinutes`
10. `mutableDependencies`

## Validation Rules

1. `releaseId` must be stable and non blank.
2. `releaseVersion` must be non blank.
3. Image references must be digest pinned with `@sha256:<64 hex>`.
4. `sessionTtlMinutes` must be positive.
5. Each workshop may have only one default release when multiple releases are declared.
6. Duplicate `releaseId` values are invalid.
7. Duplicate workshop id plus `releaseVersion` pairs are invalid.
8. `environments` must include only supported runtime environments such as `local` and `cloud-run`.

## Release Selection

Release defaults are expressed directly on each nested release by workshop scope, release identity, release version, enablement, and environment scope.

1. The parent workshop entry supplies the workshop id and local Docker metadata.
2. The nested release supplies `releaseId`, immutable `releaseVersion`, mode, enablement, environment scope, image references, resource class, session TTL, and mutable dependency policy.
3. The `defaultForWorkshop` flag selects the default release for the workshop.
4. The `enabled` flag turns the release backed launch path on or off without editing workshop code.
5. The `environments` list controls where the release is active.
6. The control plane reads `platform.controlplane.release.environment` to decide whether a release is active in the current environment.
7. Missing or incomplete release data fails closed instead of guessing a launch path.
8. A disabled or inactive release exposes the explicit `current` local development path.

## Operator Workflow

1. To enable a release backed workshop, set the nested release in `workshops.yaml` to `enabled: true`, set `defaultForWorkshop: true` when it should be the default, and scope `environments` to the intended runtimes.
2. To pause a release backed workshop for local development, set the nested release to `enabled: false` and leave the release identity unchanged.
3. To inspect release backed state, read the parent workshop entry and its nested `releases[]`. Confirm that `releaseId`, `releaseVersion`, `defaultForWorkshop`, `enabled`, and environment scope match the intended launch path.
4. To recover from a bad entry, repair the incomplete release record and re inspect before relaunch.
5. To test local Docker with unpublished or local images, use the supported image override path. Do not replace the immutable release identity just to point at a local image.

## Current Control Plane Usage

1. The control plane loader resolves the default release version from each workshop entry in `workshops.yaml`.
2. Nested release records are additive and reversible by design.
3. Missing or incomplete release configuration must fail closed rather than guessing a launch path.
