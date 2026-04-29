---
id: P3
title: Documentation Cleanup
status: completed
depends_on:
  - P1 schema shape
owner: Gauss
allowed_files:
  - README.md
  - AGENTS.md
  - docs/**
forbidden_files:
  - scripts/**
  - workshops.yaml
  - java-springboot/**
---

# Goal

Update documentation so maintainers know local direct runs use allocated ports and registry entries do not define ports.

# Verification

1. `rg -n "frontendPort|backendPort|\\bport\\b|8080|18080" README.md AGENTS.md docs`
