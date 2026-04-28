# Release Catalog CI Pipeline

CI is driven from nested release records in `workshops.yaml`.

## Entry points

1. Pull request validation
   Runs `release-catalog-ci` in validate mode.
   Resolves every nested release image target and proves the referenced Dockerfiles build without pushing.
2. Trusted publish
   Runs `release-catalog-ci` on `main` pushes or manual dispatch with `publish=true`.
   Pushes each image target to the repository already declared in the nested release image reference.

## Image enumeration

The workflow does not inspect compose files.
It derives build targets from `workshops.yaml` release records and the repository module naming convention:

1. backend image: `java-springboot/<workshopId>/Dockerfile`
2. frontend image: `java-springboot/<workshopId>_frontend/Dockerfile`
3. combined image: `java-springboot/<workshopId>/Dockerfile`
4. init image: `java-springboot/<workshopId>/Dockerfile.init`

If a nested release declares an `init` image and that file does not exist, the matrix generator fails closed.

## Produced artifact

Trusted publish emits a merged artifact named `release-catalog-digests`.

Schema:

```json
{
  "version": 1,
  "source_revision": "<git sha>",
  "generated_at": "<utc timestamp>",
  "entries": [
    {
      "release_id": "session-management-2026.04.1",
      "workshop_id": "1_session_management",
      "release_version": "2026.04.1",
      "image_role": "backend",
      "image_repository": "registry.example.com/workshops/session-management-backend",
      "image_tag": "registry.example.com/workshops/session-management-backend:2026.04.1-<sha>",
      "image_digest": "sha256:<digest>",
      "source_revision": "<git sha>"
    }
  ]
}
```

This artifact is the machine readable handoff for later `workshops.yaml` release publication and approval tasks.

## Required secrets

Trusted publish requires:

1. `RELEASE_REGISTRY_USERNAME`
2. `RELEASE_REGISTRY_PASSWORD`

The workflow derives the registry host from each nested release image repository.
