---
id: workshop_standardization
title: Workshop standardization tracking
status: ready
depends_on: []
owner: codex
allowed_files: ["plans/workshop_standardization/**"]
forbidden_files: []
---

# Workshop Standardization Tracking

## Goal

Standardize workshops around one stable workshop shell that renders Markdown first instructions, runtime controls, Redis Insight access, and an embedded learner application.

The learner application remains separate and can be restarted, rebuilt, or broken without taking down the shell.

## Product Direction

1. Every workshop has a stable shared shell.

2. Every workshop has a learner application surface inside an iframe.

3. The shell owns platform actions such as restart runtime, rebuild runtime, Redis Insight, status, logs, and navigation.

4. Workshop instructions are authored as Markdown first content with dynamic placeholders.

5. Workshop specific interactions are plugged in as widgets instead of whole custom shell frontends.

6. New workshops should start from an opinionated runnable scaffold.

## Packet Board

P1 Shared shell foundation

Status: done

Owner: unassigned

Depends on: none

P2 Markdown content and dynamic context

Status: done

Owner: unassigned

Depends on: none

P3 Runtime API contract

Status: done

Owner: unassigned

Depends on: none

P4 Session management migration

Status: done

Owner: unassigned

Depends on: P1, P2, P3

P5 Scaffold and validation

Status: done

Owner: unassigned

Depends on: P1, P2, P3

P6 Remaining workshop migrations

Status: ready

Owner: unassigned

Depends on: P4

## Execution Waves

Wave 1

1. P1 Shared shell foundation.

2. P2 Markdown content and dynamic context.

3. P3 Runtime API contract.

Status: done

Wave 2

1. P4 Session management migration.

2. P5 Scaffold and validation.

Status: done

Wave 3

1. P6 Remaining workshop migrations.

## Review Rules

1. Check file ownership before reviewing implementation quality.

2. Do not let migration packets edit shared shell files.

3. Do not let shared packets edit individual workshop files.

4. Keep widget customization separate from shell infrastructure.

5. Add repair packets instead of widening packet scope.
