#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

LIVE_CHECKS="${LIVE_CHECKS:-auto}"

usage() {
  cat <<EOF
Usage: ./scripts/ops/public-launch-preflight.sh

Environment:
  CONTROL_PLANE_URL      Default: ${CONTROL_PLANE_URL}
  HUB_URL                Default: ${HUB_URL}
  LIVE_CHECKS            auto, on, or off. Default: ${LIVE_CHECKS}
  PILOT_ACTOR_ID         Optional learner or admin actor id
  PILOT_ACTOR_TYPE       Default: ${PILOT_ACTOR_TYPE}
  PILOT_ACTOR_ROLES      Default: ${PILOT_ACTOR_ROLES}
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

note "Checking public launch operator surfaces"
pass "results template present: ${PUBLIC_RESULTS_TEMPLATE}"
pass "runbook present: ${PUBLIC_RUNBOOK_FILE}"
pass "baselines present: ${PUBLIC_BASELINES_FILE}"
pass "readiness checklist present: ${PUBLIC_READINESS_FILE}"
pass "alert inventory present: ${PUBLIC_ALERTS_FILE}"

alert_count="$(jq -r '.alerts | length' "${PUBLIC_ALERTS_FILE}")"
[[ "${alert_count}" == "7" ]] || die "Expected 7 public launch alerts, found ${alert_count}"
pass "alert inventory includes the required seven critical failure classes"

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

if [[ "${should_run_live_checks}" == "0" ]]; then
  warn "Live public launch checks were skipped. Set LIVE_CHECKS=on when the stack is running."
  pass "public launch preflight completed"
  exit 0
fi

init_actor_headers

catalog_body="$(mktemp)"
hub_catalog_body="$(mktemp)"
legacy_body="$(mktemp)"
trap 'rm -f "${catalog_body}" "${hub_catalog_body}" "${legacy_body}"' EXIT

status_code="$(api_get "${CONTROL_PLANE_URL}/api/catalog/workshops" "${catalog_body}")"
[[ "${status_code}" == "200" ]] || die "Control plane catalog check failed with status ${status_code}"
status_code="$(api_get "${HUB_URL}/api/catalog/workshops" "${hub_catalog_body}")"
[[ "${status_code}" == "200" ]] || die "Hub catalog check failed with status ${status_code}"
pass "control plane and hub catalog surfaces are reachable"

for workshop_id in "${PUBLIC_WORKSHOPS[@]}"; do
  match_count="$(json_query "${hub_catalog_body}" --arg workshop_id "${workshop_id}" '[.[] | select(.workshopId == $workshop_id)] | length')"
  [[ "${match_count}" == "1" ]] || die "Workshop ${workshop_id} missing from hub catalog"
done
pass "all release backed workshops are visible through the hub catalog"

legacy_status="$(curl -sS -o "${legacy_body}" -w "%{http_code}" "${HUB_URL}/manager/api/workshops")"
if [[ "${legacy_status}" != "404" ]]; then
  die "Expected retired legacy route /manager/api/workshops to return 404, got ${legacy_status}"
fi
pass "legacy public manager routes are retired"

pass "public launch preflight completed"
