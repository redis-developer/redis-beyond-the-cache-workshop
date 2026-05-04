---
id: P3
title: Redis deployment wiring
status: done
depends_on: []
owner: unassigned
allowed_files:
  - infra/terraform/cloudrun/**
  - docs/operations/**
  - scripts/ops/**
forbidden_files:
  - frontend/**
  - java-springboot/platform_control_plane/src/main/java/**
  - java-springboot/platform_control_plane/src/main/resources/**
  - plans/email_portal_sessions/**
---

# Goal

Wire the control plane so production can point portal sessions at Redis without changing code.

# Why This Exists

The backend packet can add Redis support, but deployment needs a clear way to provide Redis connection settings. This packet owns Terraform and operations docs only.

# Required Changes

1. Add Terraform variables for the control plane Redis endpoint and optional secret references needed by the backend configuration keys from P1.
2. Pass Redis connection values to the control plane Cloud Run service as environment variables.
3. Prefer secret references for passwords or tokens.
4. Document how local, staging, and production should provide Redis for portal sessions.
5. Keep this as wiring only. Do not provision a managed Redis instance unless the existing Terraform already has that pattern.

# Acceptance Criteria

1. Terraform can configure the control plane with Redis connection values.
2. Sensitive Redis credentials are not committed in plaintext.
3. The docs explain that portal sessions are stored in Redis while workshop deployment records remain in the control plane database.

# Verification

1. Run `terraform -chdir=infra/terraform/cloudrun validate` if provider configuration is available.
2. If validation is blocked by local provider setup, run a static check and report the blocker.

# Out Of Scope

1. Do not edit Java source or application yaml files.
2. Do not edit frontend files.
3. Do not create a full Redis Cloud or Memorystore provisioning module in this phase.

# Handoff Back

Report variable names, environment names, secret expectations, and validation status.
