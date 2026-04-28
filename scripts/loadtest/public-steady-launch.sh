#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

BASE_URL="${BASE_URL:-http://localhost:9000}"
WORKSHOPS="${WORKSHOPS:-1_session_management,2_full_text_search,3_distributed_locks,4_agent_memory}"
ITERATIONS="${ITERATIONS:-20}"
OUTPUT_DIR="${OUTPUT_DIR:-scripts/loadtest/output/public}"
ACTOR_ID_PREFIX="${ACTOR_ID_PREFIX:-public-steady}"

if curl -sS --connect-timeout 2 --max-time 5 -o /dev/null "${BASE_URL}/api/catalog/workshops" >/dev/null 2>&1; then
  OUTPUT_DIR="${OUTPUT_DIR}" \
  WORKSHOPS="${WORKSHOPS}" \
  ITERATIONS="${ITERATIONS}" \
  ACTOR_ID_PREFIX="${ACTOR_ID_PREFIX}" \
  bash "${SCRIPT_DIR}/pilot-steady-launch.sh"
  exit 0
fi

echo "[warn] Public stack is not reachable at ${BASE_URL}. Skipping live steady launch run." >&2
echo "[info] Re run with the platform up to execute the steady scenario." >&2
