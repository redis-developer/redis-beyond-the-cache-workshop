#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

LIVE_CHECKS="${LIVE_CHECKS:-off}"
BASE_URL="${BASE_URL:-}"
WORKSHOPS="${WORKSHOPS:-1_session_management,2_full_text_search,3_distributed_locks,4_agent_memory}"
TARGET_CONCURRENT_USERS="${TARGET_CONCURRENT_USERS:-100}"
STEADY_ITERATIONS="${STEADY_ITERATIONS:-100}"
BURST_SIZE="${BURST_SIZE:-100}"
SOAK_CYCLES="${SOAK_CYCLES:-3}"
ITERATIONS_PER_CYCLE="${ITERATIONS_PER_CYCLE:-34}"
OUTPUT_DIR="${OUTPUT_DIR:-scripts/loadtest/output/cloudrun}"
ACTOR_ID_PREFIX="${ACTOR_ID_PREFIX:-cloud-run-learner}"
TERMINATE_AFTER_READY="${TERMINATE_AFTER_READY:-1}"
CLEANUP_WAIT_SECONDS="${CLEANUP_WAIT_SECONDS:-30}"
RUN_ID="cloud-run-100-user-$(date -u +%Y%m%dT%H%M%SZ)"

usage() {
  cat <<EOF
Usage: LIVE_CHECKS=on BASE_URL=https://CONTROL_PLANE_HOST ./scripts/loadtest/cloud-run-100-user-window.sh

Environment:
  LIVE_CHECKS               off or on. Default: ${LIVE_CHECKS}
  BASE_URL                  Required when LIVE_CHECKS=on
  TARGET_CONCURRENT_USERS   Default: ${TARGET_CONCURRENT_USERS}
  STEADY_ITERATIONS         Default: ${STEADY_ITERATIONS}
  BURST_SIZE                Default: ${BURST_SIZE}
  SOAK_CYCLES               Default: ${SOAK_CYCLES}
  ITERATIONS_PER_CYCLE      Default: ${ITERATIONS_PER_CYCLE}
  OUTPUT_DIR                Default: ${OUTPUT_DIR}
  CLEANUP_WAIT_SECONDS      Default: ${CLEANUP_WAIT_SECONDS}
EOF
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

latest_log() {
  if [[ ! -d "${OUTPUT_DIR}" ]]; then
    return 0
  fi
  ls -t "${OUTPUT_DIR}"/*.jsonl 2>/dev/null | head -n 1 || true
}

write_plan() {
  mkdir -p "${OUTPUT_DIR}"
  local plan_file="${OUTPUT_DIR}/${RUN_ID}-plan.json"
  jq -n \
    --arg runId "${RUN_ID}" \
    --arg baseUrl "${BASE_URL}" \
    --arg workshops "${WORKSHOPS}" \
    --arg outputDir "${OUTPUT_DIR}" \
    --argjson targetConcurrentUsers "${TARGET_CONCURRENT_USERS}" \
    --argjson steadyIterations "${STEADY_ITERATIONS}" \
    --argjson burstSize "${BURST_SIZE}" \
    --argjson soakCycles "${SOAK_CYCLES}" \
    --argjson iterationsPerCycle "${ITERATIONS_PER_CYCLE}" \
    --argjson cleanupWaitSeconds "${CLEANUP_WAIT_SECONDS}" \
    '{
      runId: $runId,
      mode: "dry-plan",
      runtime: "cloud-run",
      baseUrl: $baseUrl,
      workshops: ($workshops | split(",")),
      outputDir: $outputDir,
      targetConcurrentUsers: $targetConcurrentUsers,
      scenarios: {
        steady: {iterations: $steadyIterations},
        burst: {concurrentLaunches: $burstSize},
        soak: {cycles: $soakCycles, iterationsPerCycle: $iterationsPerCycle},
        cleanup: {waitSeconds: $cleanupWaitSeconds, input: "each scenario log"},
        readiness: {entrypoint: "scripts/ops/cloud-run-readiness-check.sh"},
        rollback: {entrypoint: "scripts/ops/public-rollback-validation.sh --validate"}
      }
    }' > "${plan_file}"
  echo "[info] Wrote dry run plan to ${plan_file}" >&2
}

run_cleanup_and_summary() {
  local input_log="$1"
  if [[ -z "${input_log}" || ! -f "${input_log}" ]]; then
    return 0
  fi

  INPUT="${input_log}" \
  OUTPUT_DIR="${OUTPUT_DIR}" \
  WAIT_SECONDS="${CLEANUP_WAIT_SECONDS}" \
  bash "${SCRIPT_DIR}/cloud-run-cleanup-check.sh"

  INPUT="${input_log}" OUTPUT_DIR="${OUTPUT_DIR}" bash "${SCRIPT_DIR}/public-summary.sh"
  INPUT="${input_log}" OUTPUT_DIR="${OUTPUT_DIR}" bash "${REPO_ROOT}/scripts/ops/cloud-run-readiness-check.sh"
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

require_command jq

case "${LIVE_CHECKS}" in
  off)
    write_plan
    echo "[warn] Live Cloud Run validation was not run. Set LIVE_CHECKS=on and BASE_URL to the public control plane entrypoint." >&2
    exit 0
    ;;
  on)
    ;;
  *)
    echo "LIVE_CHECKS must be off or on" >&2
    exit 1
    ;;
esac

require_command curl

if [[ -z "${BASE_URL}" ]]; then
  echo "BASE_URL is required when LIVE_CHECKS=on" >&2
  exit 1
fi

if ! curl -sS --connect-timeout 5 --max-time 10 -o /dev/null "${BASE_URL}/api/catalog/workshops"; then
  echo "Cloud Run control plane entrypoint is not reachable at ${BASE_URL}/api/catalog/workshops" >&2
  exit 1
fi

mkdir -p "${OUTPUT_DIR}"
manifest_file="${OUTPUT_DIR}/${RUN_ID}-manifest.json"

jq -n \
  --arg runId "${RUN_ID}" \
  --arg baseUrl "${BASE_URL}" \
  --arg workshops "${WORKSHOPS}" \
  --arg outputDir "${OUTPUT_DIR}" \
  --argjson targetConcurrentUsers "${TARGET_CONCURRENT_USERS}" \
  --argjson steadyIterations "${STEADY_ITERATIONS}" \
  --argjson burstSize "${BURST_SIZE}" \
  --argjson soakCycles "${SOAK_CYCLES}" \
  --argjson iterationsPerCycle "${ITERATIONS_PER_CYCLE}" \
  '{
    runId: $runId,
    mode: "live-cloud-run",
    runtime: "cloud-run",
    baseUrl: $baseUrl,
    workshops: ($workshops | split(",")),
    outputDir: $outputDir,
    targetConcurrentUsers: $targetConcurrentUsers,
    steadyIterations: $steadyIterations,
    burstSize: $burstSize,
    soakCycles: $soakCycles,
    iterationsPerCycle: $iterationsPerCycle
  }' > "${manifest_file}"

echo "[info] Starting Cloud Run 100 user evidence window ${RUN_ID}" >&2
echo "[info] Manifest: ${manifest_file}" >&2

BASE_URL="${BASE_URL}" \
WORKSHOPS="${WORKSHOPS}" \
ITERATIONS="${STEADY_ITERATIONS}" \
OUTPUT_DIR="${OUTPUT_DIR}" \
ACTOR_ID_PREFIX="${ACTOR_ID_PREFIX}-steady" \
TERMINATE_AFTER_READY="${TERMINATE_AFTER_READY}" \
bash "${SCRIPT_DIR}/public-steady-launch.sh"
steady_log="$(latest_log)"

BASE_URL="${BASE_URL}" \
WORKSHOPS="${WORKSHOPS}" \
BURST_SIZE="${BURST_SIZE}" \
OUTPUT_DIR="${OUTPUT_DIR}" \
ACTOR_ID_PREFIX="${ACTOR_ID_PREFIX}-burst" \
TERMINATE_AFTER_READY="${TERMINATE_AFTER_READY}" \
bash "${SCRIPT_DIR}/public-burst-launch.sh"
burst_log="$(latest_log)"

BASE_URL="${BASE_URL}" \
WORKSHOPS="${WORKSHOPS}" \
SOAK_CYCLES="${SOAK_CYCLES}" \
ITERATIONS_PER_CYCLE="${ITERATIONS_PER_CYCLE}" \
OUTPUT_DIR="${OUTPUT_DIR}" \
TERMINATE_AFTER_READY="${TERMINATE_AFTER_READY}" \
bash "${SCRIPT_DIR}/public-soak-launch.sh"
soak_log="$(latest_log)"

run_cleanup_and_summary "${steady_log}"
run_cleanup_and_summary "${burst_log}"
run_cleanup_and_summary "${soak_log}"

CONTROL_PLANE_URL="${BASE_URL}" \
HUB_URL="${BASE_URL}" \
bash "${REPO_ROOT}/scripts/ops/public-rollback-validation.sh" --validate

echo "[info] Cloud Run 100 user evidence window completed: ${RUN_ID}" >&2
