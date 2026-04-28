#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

BASE_URL="${BASE_URL:-http://localhost:9000}"
WORKSHOPS="${WORKSHOPS:-1_session_management,2_full_text_search,3_distributed_locks,4_agent_memory}"
BURST_SIZE="${BURST_SIZE:-12}"
OUTPUT_DIR="${OUTPUT_DIR:-scripts/loadtest/output/public}"
ACTOR_ID_PREFIX="${ACTOR_ID_PREFIX:-public-burst}"

if curl -sS --connect-timeout 2 --max-time 5 -o /dev/null "${BASE_URL}/api/catalog/workshops" >/dev/null 2>&1; then
  OUTPUT_DIR="${OUTPUT_DIR}" \
  WORKSHOPS="${WORKSHOPS}" \
  BURST_SIZE="${BURST_SIZE}" \
  ACTOR_ID_PREFIX="${ACTOR_ID_PREFIX}" \
  bash "${SCRIPT_DIR}/pilot-burst-launch.sh"
  exit 0
fi

echo "[warn] Public stack is not reachable at ${BASE_URL}. Skipping live burst run." >&2
echo "[info] Re run with the platform up to execute the burst scenario." >&2
