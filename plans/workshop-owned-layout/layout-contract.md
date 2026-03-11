# Workshop-Owned Layout Contract

## Purpose

Move each workshop to a single workshop-owned directory without collapsing the split frontend/backend runtime model.

## Target Layout

Each workshop becomes:

```text
java-springboot/
  <workshop-id>/
    README.md
    docker-compose.yml
    backend/
      build.gradle.kts
      settings.gradle.kts
      Dockerfile
      src/...
    frontend/
      build.gradle.kts
      settings.gradle.kts
      Dockerfile
      frontend/...
      src/...
```

Example:

```text
java-springboot/
  1_session_management/
    README.md
    DEPLOYMENT.md
    docker-compose.yml
    backend/
      Dockerfile
      build.gradle.kts
      settings.gradle.kts
      src/...
    frontend/
      Dockerfile
      build.gradle.kts
      settings.gradle.kts
      frontend/...
      src/...
```

## Stable Runtime Names

The migration must preserve:

- Gradle project names
  - backend: `1_session_management`
  - frontend: `1_session_management_frontend`
- Docker service names from `workshops.yaml`
- Workshop URLs
- Frontend ports `8080`-`8083`
- Backend ports `18080`-`18083`

This is a filesystem/path migration, not a runtime-architecture rewrite.

## Canonical Path Rules

- Backend Dockerfile path becomes `java-springboot/<id>/backend/Dockerfile`
- Frontend Dockerfile path becomes `java-springboot/<id>/frontend/Dockerfile`
- Backend Gradle module directory becomes `java-springboot/<id>/backend`
- Frontend Gradle module directory becomes `java-springboot/<id>/frontend`
- Workshop-local docs and standalone compose stay at the workshop root: `java-springboot/<id>/`

## Migration Strategy

Use a mixed-layout compatibility phase first.

During the migration window, the platform must tolerate both:

- legacy sibling layout
  - `java-springboot/<id>`
  - `java-springboot/<id>_frontend`
- canonical workshop-owned layout
  - `java-springboot/<id>/backend`
  - `java-springboot/<id>/frontend`

Only after all four workshops have been moved should the compatibility fallback be removed.

## Packet Ownership Rules

- Only one packet owns `workshops.yaml`
- Only one packet owns `scripts/new-workshop.sh`
- Only one packet owns `java-springboot/settings.gradle.kts`
- Each workshop migration packet owns exactly one workshop’s backend and frontend directories
- No workshop packet may edit another workshop’s files

## Success Criteria

- Contributors can find all workshop-specific files under one workshop directory
- The hub/generated compose path still works
- Standalone workshop compose files still work
- The scaffold creates the new layout by default
- Validation fails on stale sibling-layout paths after the migration is complete
