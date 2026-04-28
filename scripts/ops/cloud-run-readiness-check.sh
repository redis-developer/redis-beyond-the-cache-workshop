#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

INPUT="${INPUT:-}"
OUTPUT_DIR="${OUTPUT_DIR:-scripts/loadtest/output/cloudrun}"
LIVE_CHECKS="${LIVE_CHECKS:-off}"
BASE_URL="${BASE_URL:-}"
STANDARD_READY_P95_MAX_SECONDS="${STANDARD_READY_P95_MAX_SECONDS:-90}"
STATEFUL_READY_P95_MAX_SECONDS="${STATEFUL_READY_P95_MAX_SECONDS:-120}"
AI_READY_P95_MAX_SECONDS="${AI_READY_P95_MAX_SECONDS:-180}"
STANDARD_CLEANUP_P95_MAX_SECONDS="${STANDARD_CLEANUP_P95_MAX_SECONDS:-45}"
STATEFUL_CLEANUP_P95_MAX_SECONDS="${STATEFUL_CLEANUP_P95_MAX_SECONDS:-60}"
AI_CLEANUP_P95_MAX_SECONDS="${AI_CLEANUP_P95_MAX_SECONDS:-90}"

usage() {
  cat <<EOF
Usage: ./scripts/ops/cloud-run-readiness-check.sh

Environment:
  INPUT         Optional JSONL evidence log
  OUTPUT_DIR    Default: ${OUTPUT_DIR}
  LIVE_CHECKS   off or on. Default: ${LIVE_CHECKS}
  BASE_URL      Required when LIVE_CHECKS=on
EOF
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

require_file() {
  local file_path="$1"
  [[ -f "${file_path}" ]] || {
    echo "Missing required file: ${file_path}" >&2
    exit 1
  }
}

latest_log() {
  if [[ ! -d "${OUTPUT_DIR}" ]]; then
    return 0
  fi
  ls -t "${OUTPUT_DIR}"/*.jsonl 2>/dev/null | head -n 1 || true
}

workshop_class() {
  case "$1" in
    1_session_management|2_full_text_search)
      echo "standard"
      ;;
    3_distributed_locks)
      echo "stateful"
      ;;
    4_agent_memory)
      echo "ai"
      ;;
    *)
      echo "unknown"
      ;;
  esac
}

class_ready_limit() {
  case "$1" in
    standard) echo "${STANDARD_READY_P95_MAX_SECONDS}" ;;
    stateful) echo "${STATEFUL_READY_P95_MAX_SECONDS}" ;;
    ai) echo "${AI_READY_P95_MAX_SECONDS}" ;;
    *) echo "0" ;;
  esac
}

class_cleanup_limit() {
  case "$1" in
    standard) echo "${STANDARD_CLEANUP_P95_MAX_SECONDS}" ;;
    stateful) echo "${STATEFUL_CLEANUP_P95_MAX_SECONDS}" ;;
    ai) echo "${AI_CLEANUP_P95_MAX_SECONDS}" ;;
    *) echo "0" ;;
  esac
}

percentile_from_log() {
  local file_path="$1"
  local workshop_id="$2"
  local field_name="$3"
  jq -rs --arg workshop_id "${workshop_id}" --arg field_name "${field_name}" '
    [ .[] | select(.workshopId == $workshop_id and .[$field_name] != null) | .[$field_name] ] as $values
    | if ($values | length) == 0 then "" else
        ($values | sort) as $sorted
        | ($sorted | length) as $count
        | (((($count - 1) * 95) / 100) | ceil) as $index
        | ($sorted[$index] | tostring)
      end
  ' "${file_path}"
}

success_rate_from_log() {
  local file_path="$1"
  local workshop_id="$2"
  jq -rs --arg workshop_id "${workshop_id}" '
    [ .[] | select(.workshopId == $workshop_id) ] as $rows
    | if ($rows | length) == 0 then "" else
        (
          ([ $rows[] | select(.launchOutcome == "ready" or .launchOutcome == "degraded") ] | length)
          / ($rows | length)
        )
      end
  ' "${file_path}"
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

require_command jq
require_file "${REPO_ROOT}/infra/terraform/cloudrun/main.tf"
require_file "${REPO_ROOT}/infra/terraform/cloudrun/outputs.tf"
require_file "${REPO_ROOT}/scripts/loadtest/cloud-run-100-user-window.sh"
require_file "${REPO_ROOT}/scripts/loadtest/cloud-run-cleanup-check.sh"
require_file "${REPO_ROOT}/scripts/ops/cloud-run-cleanup-check.sh"
require_file "${REPO_ROOT}/docs/operations/cloud-run-session-runner.md"
require_file "${REPO_ROOT}/docs/operations/cloud-run-cost-and-scale.md"
require_file "${REPO_ROOT}/docs/pilot/cloud-run-results-template.md"

if [[ -z "${INPUT}" ]]; then
  INPUT="$(latest_log || true)"
fi

if [[ -n "${INPUT}" && -f "${INPUT}" ]]; then
  echo "[info] Checking Cloud Run evidence: ${INPUT}" >&2
  total_records="$(jq -rs 'length' "${INPUT}")"
  if [[ "${total_records}" -lt 100 ]]; then
    echo "[warn] Evidence log has ${total_records} rows. The accepted Cloud Run window target is 100." >&2
  fi

  for workshop_id in 1_session_management 2_full_text_search 3_distributed_locks 4_agent_memory; do
    class_name="$(workshop_class "${workshop_id}")"
    ready_limit="$(class_ready_limit "${class_name}")"
    cleanup_limit="$(class_cleanup_limit "${class_name}")"
    ready_p95="$(percentile_from_log "${INPUT}" "${workshop_id}" "launchDurationSeconds")"
    cleanup_p95="$(percentile_from_log "${INPUT}" "${workshop_id}" "cleanupLagSeconds")"
    success_rate="$(success_rate_from_log "${INPUT}" "${workshop_id}")"

    [[ -n "${success_rate}" ]] || {
      echo "[warn] No evidence rows found for ${workshop_id}. Threshold checks were skipped for that workshop." >&2
      continue
    }

    awk -v value="${success_rate}" 'BEGIN { exit(value >= 0.95 ? 0 : 1) }' \
      || {
        echo "Success rate below rollback threshold for ${workshop_id}: ${success_rate}" >&2
        exit 1
      }

    if [[ -n "${ready_p95}" ]]; then
      awk -v value="${ready_p95}" -v limit="${ready_limit}" 'BEGIN { exit(value <= limit ? 0 : 1) }' \
        || {
          echo "Startup p95 exceeds Cloud Run threshold for ${workshop_id}: ${ready_p95}s > ${ready_limit}s" >&2
          exit 1
        }
    fi

    if [[ -n "${cleanup_p95}" ]]; then
      awk -v value="${cleanup_p95}" -v limit="${cleanup_limit}" 'BEGIN { exit(value <= limit ? 0 : 1) }' \
        || {
          echo "Cleanup p95 exceeds Cloud Run threshold for ${workshop_id}: ${cleanup_p95}s > ${cleanup_limit}s" >&2
          exit 1
        }
    fi
  done
  echo "[ok] Cloud Run evidence is inside automatic rollback thresholds" >&2
else
  echo "[warn] No Cloud Run JSONL evidence log was found. Metric threshold checks were skipped." >&2
fi

case "${LIVE_CHECKS}" in
  off)
    echo "[warn] Live Cloud Run checks were skipped. Set LIVE_CHECKS=on and BASE_URL to the public control plane entrypoint." >&2
    ;;
  on)
    require_command curl
    if [[ -z "${BASE_URL}" ]]; then
      echo "BASE_URL is required when LIVE_CHECKS=on" >&2
      exit 1
    fi
    curl -sS --connect-timeout 5 --max-time 10 -o /dev/null "${BASE_URL}/api/catalog/workshops"
    echo "[ok] live Cloud Run control plane catalog check passed" >&2
    ;;
  *)
    echo "LIVE_CHECKS must be off or on" >&2
    exit 1
    ;;
esac

echo "[ok] Cloud Run readiness check completed" >&2
