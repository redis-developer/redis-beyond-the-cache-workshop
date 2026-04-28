# Release Catalog Image CI

`release-catalog-images.yml` builds deployable workshop images from nested release records in `workshops.yaml`.

## Entrypoints

1. Pull requests run validate only mode. Images must build successfully, but nothing is pushed.
2. Pushes to `main` run publish mode. Images are pushed and the pushed digest becomes the source of truth.
3. Manual `workflow_dispatch` runs validate only mode by default and can publish when `publish=true`.

## Required publish secrets

1. `WORKSHOP_IMAGE_REGISTRY_USERNAME`
2. `WORKSHOP_IMAGE_REGISTRY_PASSWORD`

Publish mode fails closed if either secret is missing.

## Build target conventions

1. `frontend` maps to `java-springboot/<workshopId>_frontend/Dockerfile`
2. `backend` maps to `java-springboot/<workshopId>/Dockerfile`
3. `combined` maps to `java-springboot/<workshopId>/Dockerfile`
4. `init` maps to `java-springboot/<workshopId>/Dockerfile.init`

If the mapped module path or Dockerfile does not exist, the workflow fails during matrix generation.

## Output artifact

The workflow uploads `release-catalog-image-digests` containing `release-catalog-digests/release-catalog-image-digests.json`.

Schema:

```json
{
  "schemaVersion": 1,
  "catalogVersion": 1,
  "catalogPath": "workshops.yaml",
  "publish": true,
  "sourceRevision": "<git sha>",
  "generatedAt": "<utc iso8601>",
  "releases": {
    "<releaseId>": {
      "releaseId": "<releaseId>",
      "workshopId": "<workshopId>",
      "releaseVersion": "<releaseVersion>",
      "images": {
        "<imageRole>": {
          "role": "<imageRole>",
          "registry": "<registry host>",
          "repository": "<registry repository>",
          "sourceReference": "<digest pinned reference from catalog>",
          "publishedReference": "<repository@sha256:digest or null>",
          "digest": "<sha256:digest or null>",
          "sourceRevision": "<git sha>",
          "published": true,
          "tags": [
            "<repository>:<releaseVersion>-<shortsha>"
          ]
        }
      }
    }
  }
}
```
