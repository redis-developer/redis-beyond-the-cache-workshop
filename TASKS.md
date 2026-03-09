1. Create a single workshop registry (YAML/JSON) with id, title, description, serviceName, port, route, dockerfile, tags, estimatedMinutes.
2. Add a backend endpoint to serve the registry (e.g., /workshops) and update the hub to load workshops from it.
3. Replace hardcoded workshop lists in the frontend store with data from the backend endpoint.
4. Replace hardcoded workshop service/port mappings in WorkshopManagerService with registry lookups.
5. Generate docker-compose files from the registry (or use profiles) to avoid manual edits for each workshop.
6. Add a scripts/new-workshop scaffold command to create a workshop module, metadata, and registration.
7. Standardize workshop folder layout under a single /workshops directory for all languages.
8. Move per-workshop editable file definitions from Java classes to data files (e.g., workshop-config.json) loaded at runtime.
9. Expand workshop-frontend-shared and reuse base path/theme logic in all workshop frontends.
10. Stop committing build artifacts (dist/, build/, src/main/resources/static/) and build them in Docker/CI instead.
11. Define CI checks to validate registry entries, ports, and compose generation.
12. Document the “add workshop” workflow and required metadata in a single onboarding guide.
