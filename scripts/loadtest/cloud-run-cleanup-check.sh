#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

OUTPUT_DIR="${OUTPUT_DIR:-scripts/loadtest/output/cloudrun}"
INPUT="${INPUT:-}"
WAIT_SECONDS="${WAIT_SECONDS:-0}"
LIVE_CHECKS="${LIVE_CHECKS:-off}"

if [[ -z "${INPUT}" ]]; then
  if [[ -d "${OUTPUT_DIR}" ]]; then
    INPUT="$(ls -t "${OUTPUT_DIR}"/*.jsonl 2>/dev/null | head -n 1 || true)"
  fi
fi

if [[ -z "${INPUT}" || ! -f "${INPUT}" ]]; then
  echo "[warn] No Cloud Run launch run log was found under ${OUTPUT_DIR}. Cleanup verification was skipped." >&2
  exit 0
fi

INPUT="${INPUT}" \
OUTPUT_DIR="${OUTPUT_DIR}" \
WAIT_SECONDS="${WAIT_SECONDS}" \
bash "${SCRIPT_DIR}/public-cleanup-check.sh"

INPUT="${INPUT}" \
OUTPUT_DIR="${OUTPUT_DIR}" \
LIVE_CHECKS="${LIVE_CHECKS}" \
bash "${REPO_ROOT}/scripts/ops/cloud-run-cleanup-check.sh"
