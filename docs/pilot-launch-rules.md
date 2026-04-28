# Release Launch Defaults

This document describes the operator contract for release backed launch defaults. Release metadata now lives inside each workshop entry in `workshops.yaml` under `workshops[].releases[]`.

The control plane reads this property:

1. `platform.controlplane.release.environment`
   Default: `local`

Current workshop to release mapping:

| Workshop | Release ID | Release Version | Environments | Enabled |
| --- | --- | --- | --- | --- |
| `1_session_management` | `session-management-2026.04.1` | `2026.04.1` | `local`, `cloud-run` | `true` |
| `2_full_text_search` | `full-text-search-2026.04.1` | `2026.04.1` | `local`, `cloud-run` | `true` |
| `3_distributed_locks` | `distributed-locks-2026.04.1` | `2026.04.1` | `local`, `cloud-run` | `true` |
| `4_agent_memory` | `agent-memory-2026.04.1` | `2026.04.1` | `local`, `cloud-run` | `true` |

## Rule Shape

Each release backed workshop has one or more nested release records. The default active release for a workshop is the enabled release whose `defaultForWorkshop` flag is true and whose `environments` includes the running control plane environment.

```yaml
workshops:
  - id: 1_session_management
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

## Enable

1. Confirm the parent workshop exists in `workshops.yaml`.
2. Set the matching nested release to `enabled: true`.
3. Set `defaultForWorkshop: true` when this release should be the default launch path for the workshop.
4. Keep the release scoped to the environments where it should run.
5. Set `platform.controlplane.release.environment` to one of the release environments in the target deployment.

## Disable For Local Development

1. Set the matching nested release to `enabled: false`.
2. Leave the release entry in place so it can be re enabled without changing workshop code.
3. The control plane will expose the explicit `current` local development path for that workshop.

## Inspect

1. Read the workshop entry in `workshops.yaml` to confirm the immutable release identity.
2. Confirm `releaseId`, `releaseVersion`, `defaultForWorkshop`, `enabled`, and `environments` on the nested release.
3. Confirm the running control plane environment matches one of the configured rule environments.
4. If the release is missing or incomplete, treat the release path as invalid and repair it before launch.
5. If multiple releases are marked as the workshop default, treat the release path as invalid and repair it before launch.

## Failure Rules

1. A missing release fails closed and should be repaired before launch.
2. An incomplete release fails closed and should be repaired before launch.
3. Duplicate default releases for one workshop fail closed and should be repaired before launch.
4. A disabled release uses the explicit `current` local development path.
5. A release whose environments do not include the current deployment uses the explicit `current` local development path.

## Operator Notes

1. Do not edit workshop implementation modules to pause or resume a release default.
2. Keep enablement narrow and explicit.
3. Use local Docker image overrides for unpublished local images instead of changing the immutable release identity.
