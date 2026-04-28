#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

INPUT="${INPUT:-}"
LIVE_CHECKS="${LIVE_CHECKS:-auto}"
STANDARD_READY_P95_MAX_SECONDS="${STANDARD_READY_P95_MAX_SECONDS:-75}"
STATEFUL_READY_P95_MAX_SECONDS="${STATEFUL_READY_P95_MAX_SECONDS:-105}"
AI_READY_P95_MAX_SECONDS="${AI_READY_P95_MAX_SECONDS:-150}"
STANDARD_CLEANUP_P95_MAX_SECONDS="${STANDARD_CLEANUP_P95_MAX_SECONDS:-30}"
STATEFUL_CLEANUP_P95_MAX_SECONDS="${STATEFUL_CLEANUP_P95_MAX_SECONDS:-45}"
AI_CLEANUP_P95_MAX_SECONDS="${AI_CLEANUP_P95_MAX_SECONDS:-60}"

usage() {
  cat <<EOF
Usage: ./scripts/ops/public-readiness-check.sh

Environment:
  INPUT                   Optional JSONL run log
  CONTROL_PLANE_URL       Default: ${CONTROL_PLANE_URL}
  HUB_URL                 Default: ${HUB_URL}
  LIVE_CHECKS             auto, on, or off. Default: ${LIVE_CHECKS}
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

require_command curl
require_command jq
require_file "${PUBLIC_RESULTS_TEMPLATE}"
require_file "${PUBLIC_RUNBOOK_FILE}"
require_file "${PUBLIC_BASELINES_FILE}"
require_file "${PUBLIC_READINESS_FILE}"
require_file "${PUBLIC_ALERTS_FILE}"

required_alert_ids=(
  "control_plane_error_rate_high"
  "launch_rejection_spike"
  "startup_timeout_spike"
  "cleanup_backlog_growth"
  "degraded_session_spike"
  "capacity_denial_spike"
  "release_regression_detected"
)

for alert_id in "${required_alert_ids[@]}"; do
  jq -e --arg alert_id "${alert_id}" '
    .alerts[]
    | select(.id == $alert_id)
    | (.ownerTeam | length) > 0
      and (.severity | length) > 0
      and (.scope | length) > 0
      and (.runbook | length) > 0
  ' "${PUBLIC_ALERTS_FILE}" >/dev/null || die "Alert ${alert_id} is missing owner, severity, scope, or runbook"
done
pass "alert inventory covers all critical failure classes"

for script_path in \
  "${REPO_ROOT}/scripts/ops/public-launch-preflight.sh" \
  "${REPO_ROOT}/scripts/ops/public-rollback-validation.sh" \
  "${REPO_ROOT}/scripts/loadtest/public-steady-launch.sh" \
  "${REPO_ROOT}/scripts/loadtest/public-burst-launch.sh" \
  "${REPO_ROOT}/scripts/loadtest/public-soak-launch.sh" \
  "${REPO_ROOT}/scripts/loadtest/public-cleanup-check.sh" \
  "${REPO_ROOT}/scripts/loadtest/public-rollback-validation.sh"; do
  require_file "${script_path}"
done
pass "public launch scripts are present"

latest_public_log() {
  local public_output_dir="${REPO_ROOT}/scripts/loadtest/output/public"
  if [[ ! -d "${public_output_dir}" ]]; then
    return 0
  fi
  ls -t "${public_output_dir}"/*.jsonl 2>/dev/null | head -n 1 || true
}

if [[ -z "${INPUT}" ]]; then
  INPUT="$(latest_public_log || true)"
fi

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

if [[ -n "${INPUT}" && -f "${INPUT}" ]]; then
  note "Checking public launch evidence: ${INPUT}"
  for workshop_id in "${PUBLIC_WORKSHOPS[@]}"; do
    class_name="$(workshop_class "${workshop_id}")"
    ready_limit="$(class_ready_limit "${class_name}")"
    cleanup_limit="$(class_cleanup_limit "${class_name}")"
    ready_p95="$(percentile_from_log "${INPUT}" "${workshop_id}" "launchDurationSeconds")"
    cleanup_p95="$(percentile_from_log "${INPUT}" "${workshop_id}" "cleanupLagSeconds")"
    success_rate="$(success_rate_from_log "${INPUT}" "${workshop_id}")"

    [[ -n "${success_rate}" ]] || {
      warn "No evidence rows found for ${workshop_id}. Readiness thresholds were skipped."
      continue
    }

    awk -v value="${success_rate}" 'BEGIN { exit(value >= 0.95 ? 0 : 1) }' \
      || die "Success rate below rollback threshold for ${workshop_id}: ${success_rate}"

    if [[ -n "${ready_p95}" ]]; then
      awk -v value="${ready_p95}" -v limit="${ready_limit}" 'BEGIN { exit(value <= limit ? 0 : 1) }' \
        || die "Startup p95 exceeds class threshold for ${workshop_id}: ${ready_p95}s > ${ready_limit}s"
    fi

    if [[ -n "${cleanup_p95}" ]]; then
      awk -v value="${cleanup_p95}" -v limit="${cleanup_limit}" 'BEGIN { exit(value <= limit ? 0 : 1) }' \
        || die "Cleanup p95 exceeds class threshold for ${workshop_id}: ${cleanup_p95}s > ${cleanup_limit}s"
    fi
  done
  pass "public launch evidence is inside the current automatic thresholds"
else
  warn "No public launch run log was found. Metric threshold checks were skipped."
fi

should_run_live_checks=0
case "${LIVE_CHECKS}" in
  on)
    should_run_live_checks=1
    ;;
  off)
    should_run_live_checks=0
    ;;
  auto)
    if http_probe "${HUB_URL}/api/catalog/workshops" && http_probe "${CONTROL_PLANE_URL}/api/catalog/workshops"; then
      should_run_live_checks=1
    fi
    ;;
  *)
    die "LIVE_CHECKS must be one of: auto, on, off"
    ;;
esac

if [[ "${should_run_live_checks}" == "1" ]]; then
  init_actor_headers
  response_file="$(mktemp)"
  legacy_file="$(mktemp)"
  trap 'rm -f "${response_file}" "${legacy_file}"' EXIT

  status_code="$(api_get "${HUB_URL}/api/catalog/workshops" "${response_file}")"
  [[ "${status_code}" == "200" ]] || die "Hub catalog surface is not healthy"

  legacy_status="$(curl -sS -o "${legacy_file}" -w "%{http_code}" "${HUB_URL}/workshop/1_session_management/")"
  [[ "${legacy_status}" == "404" ]] || die "Expected retired public workshop route to return 404, got ${legacy_status}"

  legacy_status="$(curl -sS -o "${legacy_file}" -w "%{http_code}" "${HUB_URL}/manager/api/workshops")"
  [[ "${legacy_status}" == "404" ]] || die "Expected retired manager route to return 404, got ${legacy_status}"
  pass "live route retirement checks passed"
else
  warn "Live route retirement checks were skipped. Set LIVE_CHECKS=on when the stack is running."
fi

pass "public readiness check completed"
