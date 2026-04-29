# Workshop Infrastructure Module

This shared Spring Boot module provides the runtime shell for workshop frontend modules. It owns workshop metadata loading, content delivery, backend proxying, session runner lifecycle endpoints, Redis Insight proxying, and the embedded VS Code proxy.

## Runtime Boundary

Workshop frontend modules compose this infrastructure with workshop specific Vue views and content. Keep generic shell behavior here or in `workshop-frontend-shared`; keep workshop specific copy, routes, widgets, and domain API calls in each `java-springboot/<id>_frontend/` module.

This module should own:

1. editor APIs for manifest backed restore and legacy file operations
2. diagnostics, status, restart, rebuild, and restore endpoints
3. backend learner app proxying
4. Redis Insight proxying
5. embedded VS Code HTTP and WebSocket proxying
6. shared workshop content loading
7. session runner lifecycle wiring

Workshop app Vue code should not call direct local ports, control plane URLs, session runner endpoints, Redis Insight ports, or `/api/editor/restore` directly. Use shared shell helpers and same origin routes.

## Embedded VS Code

The session runner starts `code-server` as a local child process when `WORKSHOP_LOCAL_CODE_EDITOR_COMMAND` is configured. The runner image default command is `/opt/runner/bin/start-code-editor.sh`, and the runner build currently defaults `CODE_SERVER_VERSION` to `4.103.2`.

Editor process contract:

1. `WORKSHOP_LOCAL_CODE_EDITOR_COMMAND`: command used by the manager to start code-server.
2. `WORKSHOP_LOCAL_CODE_EDITOR_PORT`: localhost only code-server port. Defaults to `39000`.
3. `WORKSHOP_LOCAL_CODE_EDITOR_HEALTH_PATH`: optional code editor health path. Defaults to `/healthz`.
4. `WORKSHOP_SESSION_WORKSPACE_PATH`: workspace root opened by VS Code.

The selected code-server setup runs with no learner authentication inside the container because access is already scoped by the outer session route. It must bind only to `127.0.0.1`, disable telemetry and update checks, disable workspace trust, and avoid terminal exposure in the first release.

Routing contract:

1. Manager local route: `/code/`
2. Public session route: `/session/{sessionId}/code/`
3. Local code-server upstream: `http://127.0.0.1:${WORKSHOP_LOCAL_CODE_EDITOR_PORT}`
4. WebSocket traffic follows the same `/code/` route

The manager rewrites code-server response headers, cookies, and text assets so the editor remains usable behind a base path. The control plane forwards public session traffic to the manager and sets `X-Forwarded-Prefix` so links remain under `/session/{sessionId}`.

## Restore And Rebuild Lifecycle

Reset and rebuild are shell owned. The embedded editor edits files in `WORKSHOP_SESSION_WORKSPACE_PATH`, but it does not decide when the learner JVM is restarted.

Expected flow:

1. Learner edits files in embedded VS Code.
2. Learner saves files in VS Code.
3. Learner uses Recompile App when Java changes need to affect the running learner JVM.
4. Manager runs the child rebuild command against the session workspace.
5. Manager restarts the child JVM from the rebuilt artifact.

Reset flow:

1. Learner clicks Reset Code in the shared editor shell.
2. Shell calls `/internal/session-runner/restore`.
3. Manager restores editable files from the workshop manifest.
4. Learner clicks Recompile App.
5. Manager rebuilds and restarts the child JVM from restored files.

Reset alone only restores files. Recompile App is required before restored Java files affect the learner app.

## Shared Content Delivery

Workshop frontends can place content resources at:

```text
src/main/resources/workshop-content/
  manifest.yaml
  views/
    <view-id>.yaml
```

Runtime API:

1. `GET /api/content/manifest`
2. `GET /api/content/views/{viewId}`

Failure behavior:

1. Missing content manifest returns `404` with `WORKSHOP_CONTENT_NOT_FOUND`.
2. Missing `viewId` in the manifest returns `404` with `WORKSHOP_CONTENT_VIEW_NOT_FOUND`.
3. Malformed manifest or view files return `500` with `WORKSHOP_CONTENT_INVALID`.

The loader resolves `src/main/resources/workshop-content/manifest.yaml` from the configured workshop source path when available, then falls back to packaged `classpath:workshop-content/manifest.yaml`.

## Local Testing

Use the repository helper from the repository root:

```bash
./scripts/run-workshop.sh up 1_session_management
./scripts/run-workshop.sh down 1_session_management
```

When `code-server` is installed locally, the helper configures `WORKSHOP_LOCAL_CODE_EDITOR_COMMAND` and starts the embedded editor automatically. When it is not installed, the helper logs that the embedded VS Code process will not start locally while the manager and child JVM still run.

Manual checks:

1. Open the workshop editor route and confirm the frame loads from `/code/`.
2. Confirm the same route works under `/session/{sessionId}/code/` in a public session.
3. Save a file in VS Code and use Recompile App.
4. Reset Code, then use Recompile App again before checking learner app behavior.
5. Confirm Redis Insight and backend learner app routes remain same origin and session scoped.

## Validation Notes

Run:

```bash
bash scripts/validate-workshops.sh
./gradlew --no-daemon :1_session_management_frontend:build
```

The embedded VS Code validation boundary accepts `WorkshopCodeEditorShell` as the shared editor shell and continues to reject workshop app views that call direct restore transport. Scaffold smoke validation checks numbered content files such as `views/0.yaml` and `views/1.yaml`.

## Legacy Editor Compatibility

The manifest backed editor APIs remain available while embedded VS Code is rolled out. They are used by the legacy editor fallback and by restore behavior.

Manifest fields:

1. `moduleName`
2. `title`
3. `description`
4. `editableFiles[].name`
5. `editableFiles[].path`
6. `editableFiles[].resetContent` or `editableFiles[].resetContentLocation`

Legacy API surface:

1. `GET /api/editor/files`
2. `GET /api/editor/file/{fileName}`
3. `POST /api/editor/file/{fileName}`
4. `POST /internal/session-runner/restore`

New workshop code should prefer the shared embedded editor shell and should keep reset and recompile controls shell owned.
