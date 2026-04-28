#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

BASE_URL="${BASE_URL:-http://localhost:9000}"
WORKSHOPS="${WORKSHOPS:-1_session_management,2_full_text_search,3_distributed_locks,4_agent_memory}"
SOAK_CYCLES="${SOAK_CYCLES:-3}"
ITERATIONS_PER_CYCLE="${ITERATIONS_PER_CYCLE:-8}"
COOLDOWN_SECONDS="${COOLDOWN_SECONDS:-10}"
OUTPUT_DIR="${OUTPUT_DIR:-scripts/loadtest/output/public}"
RUN_ID="public-soak-$(date -u +%Y%m%dT%H%M%SZ)"

if ! curl -sS --connect-timeout 2 --max-time 5 -o /dev/null "${BASE_URL}/api/catalog/workshops" >/dev/null 2>&1; then
  echo "[warn] Public stack is not reachable at ${BASE_URL}. Skipping live soak run." >&2
  echo "[info] Re run with the platform up to execute the soak scenario." >&2
  exit 0
fi

mkdir -p "${OUTPUT_DIR}"
combined_output="${OUTPUT_DIR}/${RUN_ID}.jsonl"
tmp_dir="$(mktemp -d "${TMPDIR:-/tmp}/public-soak.XXXXXX")"
trap 'rm -rf "${tmp_dir}"' EXIT

cycle=1
while [[ "${cycle}" -le "${SOAK_CYCLES}" ]]; do
  cycle_output_dir="${tmp_dir}/cycle-${cycle}"
  mkdir -p "${cycle_output_dir}"

  echo "[info] Starting soak cycle ${cycle}/${SOAK_CYCLES}" >&2
  BASE_URL="${BASE_URL}" \
  WORKSHOPS="${WORKSHOPS}" \
  ITERATIONS="${ITERATIONS_PER_CYCLE}" \
  OUTPUT_DIR="${cycle_output_dir}" \
  ACTOR_ID_PREFIX="public-soak-${cycle}" \
  bash "${SCRIPT_DIR}/pilot-steady-launch.sh" >/dev/null

  cycle_log="$(find "${cycle_output_dir}" -maxdepth 1 -type f -name '*.jsonl' | sort | tail -n 1)"
  [[ -n "${cycle_log}" ]] || {
    echo "[warn] No output log was generated for soak cycle ${cycle}" >&2
    cycle=$(( cycle + 1 ))
    continue
  }

  cat "${cycle_log}" >> "${combined_output}"

  if [[ "${cycle}" -lt "${SOAK_CYCLES}" && "${COOLDOWN_SECONDS}" -gt 0 ]]; then
    sleep "${COOLDOWN_SECONDS}"
  fi

  cycle=$(( cycle + 1 ))
done

echo "Wrote soak launch results to ${combined_output}" >&2
