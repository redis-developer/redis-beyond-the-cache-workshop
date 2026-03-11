# Content-Driven Views Contract

## Scope

This document freezes the authoring contract for moving workshop instructional copy out of Vue templates and into workshop-owned content files.

Goals:

- let workshop maintainers edit instructional copy without editing Vue templates
- give `CV2` and `CV3` one stable schema for delivery and rendering
- preserve the existing split between declarative instructional content and code-owned behavior

Non-goals:

- defining transport endpoints, caching, or loader implementation details
- replacing live demos, forms, charts, or status widgets with declarative content
- encoding arbitrary Vue conditions or component logic in content files

## File Layout And Naming

Each workshop frontend module owns its content under:

```text
java-springboot/<workshop>_frontend/src/main/resources/workshop-content/
```

Required layout:

```text
workshop-content/
  manifest.yaml
  views/
    <view-id>.yaml
```

Rules:

- `view-id` uses `kebab-case` and is stable once published.
- `manifest.yaml` is the only registry file. Loaders must not infer views by directory scan alone.
- Each content file owns exactly one view route.
- One view file may describe a single narrative page, a multi-stage page, or an editor instructions page.
- Large code blocks may stay inline in the YAML document. If external snippet files are added later, they must still be referenced from the owning view file and stay inside `workshop-content/`.

## Manifest Schema

`manifest.yaml` is the per-workshop index for content-driven views.

```yaml
schemaVersion: 1
workshopId: 1_session_management
views:
  - viewId: session-home
    route: /welcome
    pageType: stage-flow
    file: views/session-home.yaml
  - viewId: session-editor
    route: /editor
    pageType: editor
    file: views/session-editor.yaml
```

Required fields:

| Field | Type | Notes |
| --- | --- | --- |
| `schemaVersion` | integer | Starts at `1`. |
| `workshopId` | string | Matches the workshop/module identity. |
| `views[]` | array | One entry per content-driven route. |
| `views[].viewId` | string | Stable `kebab-case` identifier. |
| `views[].route` | string | Vue route path, including leading `/`. |
| `views[].pageType` | enum | `narrative`, `stage-flow`, or `editor`. |
| `views[].file` | string | Relative path to the view document. |

Validation rules:

- `viewId` must be unique within the workshop.
- `route` must be unique within the workshop.
- `pageType` in the manifest and view file must match.
- Unknown fields fail validation in strict mode.

## View Document Schema

Every view file starts with the same top-level envelope:

```yaml
schemaVersion: 1
viewId: session-home
route: /welcome
pageType: stage-flow
title: Session Management
slot: instructions
dataRequirements:
  - sessionId
```

Required top-level fields:

| Field | Type | Notes |
| --- | --- | --- |
| `schemaVersion` | integer | Must match the manifest major version. |
| `viewId` | string | Must match the manifest entry. |
| `route` | string | Must match the manifest entry. |
| `pageType` | enum | `narrative`, `stage-flow`, `editor`. |
| `title` | string | Plain text only. |
| `slot` | enum | Mount target inside the Vue view. For current migrations use `instructions`. |

Optional top-level fields:

| Field | Type | Notes |
| --- | --- | --- |
| `summary` | string | Markdown-enabled short description. |
| `dataRequirements[]` | array | Named runtime tokens expected by the content. |
| `header` | object | Shared page chrome metadata such as stage nav labels. |
| `sections[]` | array | Used by `narrative` and `editor` pages. |
| `stages[]` | array | Used by `stage-flow` pages. |

### `header`

`header` is metadata for existing shared chrome. It does not replace the Vue shell.

```yaml
header:
  showHubLink: true
  stageNav:
    clickable: true
    steps:
      - stageId: intro
        label: Intro
      - stageId: learn
        label: Learn
```

Rules:

- `showHubLink` only signals that the existing Workshop Header keeps its hub affordance.
- `stageNav.steps[].stageId` must match a stage id in `stages[]`.
- Content files do not describe right-column widgets, forms, or result panes.

## Page Types

### Narrative Page

Use `pageType: narrative` for a single instructional surface with ordered sections.

Required shape:

```yaml
sections:
  - sectionId: implementation
    title: Implement the Distributed Lock
    blocks: []
```

### Stage-Flow Page

Use `pageType: stage-flow` when one route contains multiple learner stages and Vue owns the active stage state.

Required shape:

```yaml
stages:
  - stageId: intro
    title: Introduction
    sections: []
```

Rules:

- Stage ordering in YAML is the display order.
- Vue code decides when a stage becomes active, unlocked, complete, or hidden.
- Content must not embed boolean expressions such as `currentStage === 2` or `redisEnabled`.

### Editor Page

Use `pageType: editor` for instructions shown inside `WorkshopEditorLayout`.

Required shape:

```yaml
sections:
  - sectionId: editor-workflow
    blocks: []
```

Rules:

- The editor file tab list and ordering remain owned by the existing workshop manifest, currently `workshop-manifest.yaml` under `editableFiles[]`.
- Content files must not redefine editor tabs or file ordering.
- `openFile.args.file` must match an `editableFiles[].name` entry from the workshop manifest.
- Auto-apply behavior is expressed through action ids, not Vue method names.
- File contents, save mechanics, and editor buffer state stay in code.

## Shared Content Structures

### Section

Sections group blocks under one heading.

```yaml
- sectionId: problem
  title: The Problem
  body: >
    Optional intro copy in markdown.
  blocks: []
```

Rules:

- `sectionId` uses `kebab-case`.
- `title` is plain text.
- `body` is markdown-enabled and optional.

### Block Types

Supported block types for `CV2` and `CV3`:

#### `markdown`

```yaml
- type: markdown
  body: >
    Redis Query Engine adds indexing and querying capabilities to Redis.
```

Use for ordinary narrative paragraphs and simple lists.

#### `callout`

```yaml
- type: callout
  tone: info
  title: Option 1
  body: Use the in-browser code editor for guided changes.
  actions:
    - id: openEditor
      label: Open Code Editor
```

Required fields:

- `tone`: `info`, `success`, `warning`, or `error`
- `body`

Optional fields:

- `title`
- `actions[]`

#### `stepList`

Use for manual tasks, tests, or implementation checklists.

```yaml
- type: stepList
  listId: enable-redis
  variant: ordered
  items:
    - itemId: add-dependencies
      title: Step 1: Add Redis Dependencies
      body: Uncomment the Redis dependencies in `build.gradle.kts`.
      actions:
        - id: openEditor
          label: Open Code Editor
```

Required fields:

- `listId`
- `variant`: `ordered`, `checklist`, or `tests`
- `items[]`

Per-item fields:

| Field | Type | Notes |
| --- | --- | --- |
| `itemId` | string | Stable within the list. |
| `title` | string | Plain text only. |
| `body` | string | Markdown-enabled instructions. |
| `hint` | string | Optional secondary help text, typically rendered as a tooltip or inline assistive note. |
| `actions[]` | array | Optional button/link actions. |
| `supportingBlocks[]` | array | Optional nested `codeSnippet`, `callout`, or `widget` blocks rendered under the item. |

Rules:

- Ordering is the learner order.
- Completion state and progressive reveal stay in Vue code.
- The renderer only emits action events and renders static copy.

#### `editorStepList`

Use for code editor workflows with file-open, auto-apply, save, and read-only reference steps.

```yaml
- type: editorStepList
  listId: sdk-client
  title: Step 0: Initialize the SDK Client
  startAt: 1
  items:
    - itemId: open-agent-memory-service
      body: Open `AgentMemoryService.java`.
      action:
        id: openFile
        label: Open File
        args:
          file: AgentMemoryService.java
```

Required fields:

- `listId`
- `title`
- `items[]`

Optional fields:

- `description`
- `startAt`
- `progressGroup`

Per-item fields:

| Field | Type | Notes |
| --- | --- | --- |
| `itemId` | string | Stable within the list. |
| `body` | string | Markdown-enabled copy. |
| `hint` | string | Optional tooltip/help text in markdown. |
| `action` | object | Single primary action for the row. |

Rules:

- `openFile`, `applyEditorStep`, `saveFile`, and `openReference` are the supported primary actions for current workshops.
- Auto-apply steps must use `applyEditorStep` with a stable `stepId`; the content file must not name a Vue method.

#### `codeSnippet`

```yaml
- type: codeSnippet
  language: java
  title: Implement `withLock()`
  code: |
    RLock lock = redissonClient.getLock(lockKey);
    boolean acquired = false;
```

Required fields:

- `language`
- `code`

Optional fields:

- `title`
- `caption`

Rules:

- Code snippets are display-only unless paired with an explicit action.
- Use `codeSnippet` for multi-line code, not markdown fenced code blocks.

#### `widget`

Use `widget` as the explicit boundary for code-owned behavior rendered inside the instructional column.

```yaml
- type: widget
  widgetId: session-home.session-comparison
```

Rules:

- `widgetId` uses `<view-id>.<slug>` naming.
- Widgets are implemented in Vue code and receive their data from Vue code.
- Widgets are allowed for progress trackers, live status panels, computed comparisons, diagrams, and verification cards.
- Widgets are the approved escape hatch for interactive behavior that does not fit the declarative block model cleanly.

## Markdown And Token Rules

Markdown is allowed only in these fields:

- `summary`
- `section.body`
- `markdown.body`
- `callout.body`
- `stepList.items[].body`
- `stepList.items[].hint`
- `editorStepList.description`
- `editorStepList.items[].body`
- `editorStepList.items[].hint`
- `codeSnippet.caption`

Allowed markdown subset:

- paragraphs
- emphasis and strong emphasis
- ordered and unordered lists
- inline code
- blockquotes
- links to external URLs

Token interpolation:

- Tokens use `{{tokenName}}`.
- Token names use `camelCase` or dotted paths such as `{{session.id}}`.
- Tokens may appear only inside markdown-enabled string fields.
- Missing tokens must fail validation in development and surface a renderer error state in production.

Raw HTML is forbidden everywhere except inside `codeSnippet.code`, where it is treated as plain text.

Explicitly forbidden in content files:

- `<a>`, `<router-link>`, `<button>`, `<strong>`, `<br/>`, inline styles, SVG, or any other raw HTML
- Vue directives such as `v-if`, `v-for`, `:class`, `@click`
- JavaScript expressions, conditions, or template interpolation other than the token form above

## Action Contract

Action ids are stable shared identifiers. Workshop-specific details belong in `args`, never in the id itself.

Finalized shared action ids:

| Action id | Required args | Purpose |
| --- | --- | --- |
| `openHub` | none; optional `mode`, `service` | Open Workshop Hub, or deep-link to a restart/rebuild intent when supported. |
| `openEditor` | none | Navigate to the workshop editor route. |
| `openRedisInsight` | none; optional `query` | Open Redis Insight for the current workshop environment. |
| `openRoute` | `route` | Navigate to another route in the same workshop. |
| `openFile` | `file` | Load a file tab in `WorkshopEditorLayout`. |
| `openReference` | `referenceId` | Load read-only reference content owned by Vue code. |
| `saveFile` | none | Save the current editor buffer. |
| `applyEditorStep` | `stepId` | Run a code-backed auto-apply step. |
| `markItemComplete` | `groupId`, `itemId` | Mark a checklist/test item complete. |
| `setStage` | `stageId` | Move to a named stage. |
| `runCheck` | `checkId` | Trigger a code-owned verification check. |
| `restartLab` | none | Restore workshop files/data to the starting state. |
| `resetProgress` | none; optional `scope` | Clear saved workshop progress. |

Naming rules for args:

- `stepId`, `referenceId`, `checkId`, and `widgetId` use `<view-id>.<slug>`.
- `groupId` matches a `listId`.
- `itemId` matches the owning list item id.

Examples:

```yaml
actions:
  - id: openHub
    label: Open Workshop Hub
    args:
      mode: rebuildRestart
      service: backend

  - id: applyEditorStep
    label: Apply Change
    args:
      stepId: memory-editor.uncommentClientInit
```

## Boundary Between Declarative Content And Vue Code

Declarative content owns:

- headings, paragraphs, lists, code snippets, and callout copy
- learner step ordering and static labels
- stage ids and display labels
- action declarations
- placement of code-owned widgets within the instruction column

Vue code still owns:

- route guards, API calls, persistence, and any mutable state
- deciding the active stage, unlocked steps, completion, and visibility
- demo/search/chat/input forms
- right-column data panels and result panes
- editor buffers, file loading, save behavior, and auto-apply implementations
- computed comparisons, live status checks, and restart/reset side effects
- all widget implementations

Design rule:

- If a change requires branching on runtime state, inspecting API data, or mutating the app, it stays in Vue code.
- If a change only rewrites instructional structure or copy, it belongs in content.

## Current Patterns That Stay Code-Owned

These current patterns do not fit the first-pass contract cleanly and should remain widgets or normal Vue code during migration:

- progressive reveal flows such as `ReentrantLearn.vue`
- live demos, chat/search forms, and test harnesses such as `SearchDemo.vue`, `LocksDemo.vue`, `MemoryChat.vue`, and `MemoryLab.vue`
- right-column live panels such as session details, quick references, and search results
- SVG-heavy diagrams and bespoke visual layouts in `MemoryHome.vue`, `MemoryChallenges.vue`, and `MemoryLearn.vue`
- computed progress bars and completion summaries such as `MemoryEditor.vue`
- read-only API-backed source previews such as the `PurchaseService.java` review in `LocksEditor.vue`

These are not blockers for `CV1`; they define the boundary for later packets.

## Worked Examples

The example files in [`plans/content-driven-views/examples/`](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/plans/content-driven-views/examples) apply this contract to current views:

- [`session-home.yaml`](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/plans/content-driven-views/examples/session-home.yaml)
- [`search-home.yaml`](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/plans/content-driven-views/examples/search-home.yaml)
- [`locks-implement.yaml`](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/plans/content-driven-views/examples/locks-implement.yaml)
- [`memory-editor.yaml`](/Users/raphaeldelio/Documents/GitHub/workshops/redis-beyond-the-cache-workshop/plans/content-driven-views/examples/memory-editor.yaml)

Together they cover:

- a home/stage-flow page
- a second stage-flow page with narrative and implementation stages
- a dedicated implementation page
- an editor-driven page with auto-apply steps
