#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

OUTPUT_DIR="${OUTPUT_DIR:-scripts/loadtest/output/public}"
INPUT="${INPUT:-}"

if [[ -z "${INPUT}" ]]; then
  if [[ -d "${OUTPUT_DIR}" ]]; then
    INPUT="$(ls -t "${OUTPUT_DIR}"/*.jsonl 2>/dev/null | head -n 1 || true)"
  fi
fi

if [[ -z "${INPUT}" || ! -f "${INPUT}" ]]; then
  echo "[warn] No public launch run log was found under ${OUTPUT_DIR}. Cleanup verification was skipped." >&2
  exit 0
fi

INPUT="${INPUT}" OUTPUT_DIR="${OUTPUT_DIR}" bash "${SCRIPT_DIR}/pilot-cleanup-check.sh"
