# Pilot Follow Up Validation 2026 04 23 Local

## Scope

This follow up validation covers the three items left open after the first local pilot result:

1. hub workspace root normalization
2. explicit two session file and Redis isolation checks
3. a short smoke pass for diagnostics, restore, restart, and Redis Insight on the pilot release path

## Hub workspace root normalization

Result: pass

The hub now provisions session workspaces under the intended repo rooted path:

1. `/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/java-springboot/workshop-hub/build/execution-workspaces/f6747a99-af38-46c4-a9ea-df50f48b5473/workspace`
2. `/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/java-springboot/workshop-hub/build/execution-workspaces/5a3601f9-fb65-439c-864d-74ec6918f398/workspace`

The old nested module path shape is no longer present in the live container mounts.

## Session Management isolation validation

Workshop: `1_session_management`  
Release path: pilot default `2026.04.1`

Sessions used:

1. `f6747a99-af38-46c4-a9ea-df50f48b5473`
2. `5a3601f9-fb65-439c-864d-74ec6918f398`

Checks:

1. File isolation
   `application.properties` in session A was edited with marker `# codex-session-a-marker`
   Session A returned the marker
   Session B did not return the marker
2. Restore
   `POST /session/{sessionId}/api/editor/restore` returned `success=true`
   Session A no longer contained the marker after restore
3. Diagnostics
   `POST /session/{sessionId}/api/editor/diagnostics` returned a payload with `diagnostics`
4. Redis isolation
   `SET codex:isolation sessionA` in session A Redis returned `OK`
   `GET codex:isolation` in session A returned `sessionA`
   `GET codex:isolation` in session B returned empty
5. Redis Insight
   `GET /session/{sessionId}/redis-insight/` returned `200` for both sessions
6. Restart
   `POST /api/sessions/{sessionId}/restart` with `rebuild=false` returned the session to `READY`

Result: pass

## Full Text Search smoke validation

Workshop: `2_full_text_search`  
Release path: pilot default `2026.04.1`

Session used:

1. `500d6953-3e17-467d-8fa9-ef2408d9052e`

Checks:

1. File save
   `application.properties` was edited with marker `# codex-search-smoke`
   The edited content was returned by the live editor file endpoint
2. Diagnostics
   `POST /session/{sessionId}/api/editor/diagnostics` returned a payload with `diagnostics`
3. Restore
   `POST /session/{sessionId}/api/editor/restore` returned `success=true`
   The marker was removed after restore
4. Redis Insight
   `GET /session/{sessionId}/redis-insight/` returned `200`
5. Restart
   `POST /api/sessions/{sessionId}/restart` with `rebuild=false` returned the session to `READY`

Result: pass

## Outcome

1. The hub workspace root issue is fixed and verified live.
2. File isolation and Redis isolation are both verified live for `1_session_management`.
3. Diagnostics, restore, restart, and Redis Insight are verified live for both pilot workshops.
4. The remaining notable gap is cleanup lag measurement.
   The current scripts prove cleanup completion, but they do not yet record p50 or p95 cleanup lag.
