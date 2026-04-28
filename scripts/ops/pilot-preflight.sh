#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

usage() {
  cat <<EOF
Usage: ./scripts/ops/pilot-preflight.sh

Environment:
  CONTROL_PLANE_URL      Default: ${CONTROL_PLANE_URL}
  HUB_URL                Default: ${HUB_URL}
  PILOT_ACTOR_ID         Historical variable name for optional learner or admin actor id
  PILOT_ACTOR_TYPE       Default: ${PILOT_ACTOR_TYPE}
  PILOT_ACTOR_ROLES      Default: ${PILOT_ACTOR_ROLES}
  WORKSHOPS_FILE         Default: ${WORKSHOPS_FILE}
  RELEASE_CATALOG_FILE   Legacy override for release source. Default: ${RELEASE_CATALOG_FILE}
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

require_command curl
require_command jq
require_file "${RELEASE_CATALOG_FILE}"

init_actor_headers

note "Checking local release default operator prerequisites"
pass "release source file present: ${RELEASE_CATALOG_FILE}"

catalog_body="$(mktemp)"
hub_catalog_body="$(mktemp)"
sessions_body="$(mktemp)"
release_body="$(mktemp)"
admin_overview_body="$(mktemp)"
trap 'rm -f "${catalog_body}" "${hub_catalog_body}" "${sessions_body}" "${release_body}" "${admin_overview_body}"' EXIT

status_code="$(api_get "${CONTROL_PLANE_URL}/api/catalog/workshops" "${catalog_body}")"
[[ "${status_code}" == "200" ]] || die "Control plane catalog check failed with status ${status_code}"
control_plane_count="$(json_get "${catalog_body}" 'length')"
pass "control plane catalog reachable with ${control_plane_count} workshops"

status_code="$(api_get "${HUB_URL}/api/catalog/workshops" "${hub_catalog_body}")"
[[ "${status_code}" == "200" ]] || die "Hub catalog proxy check failed with status ${status_code}"
hub_count="$(json_get "${hub_catalog_body}" 'length')"
pass "hub catalog proxy reachable with ${hub_count} workshops"

for workshop_id in "${PILOT_WORKSHOPS[@]}"; do
  cp_match="$(json_query "${catalog_body}" --arg workshop_id "${workshop_id}" '[.[] | select(.workshopId == $workshop_id)] | length')"
  [[ "${cp_match}" == "1" ]] || die "Release backed workshop ${workshop_id} missing from control plane catalog"
  hub_match="$(json_query "${hub_catalog_body}" --arg workshop_id "${workshop_id}" '[.[] | select(.workshopId == $workshop_id)] | length')"
  [[ "${hub_match}" == "1" ]] || die "Release backed workshop ${workshop_id} missing from hub catalog"
done
pass "release backed workshops visible through both catalog surfaces"

if actor_configured; then
  note "Actor headers enabled for ${PILOT_ACTOR_ID} (${PILOT_ACTOR_TYPE}) roles=${PILOT_ACTOR_ROLES}"
  status_code="$(api_get "${HUB_URL}/api/sessions" "${sessions_body}")"
  [[ "${status_code}" == "200" ]] || die "Authenticated session listing failed with status ${status_code}"
  session_count="$(json_get "${sessions_body}" 'length')"
  pass "session listing reachable with ${session_count} visible sessions"

  for workshop_id in "${PILOT_WORKSHOPS[@]}"; do
    active_count="$(json_query "${sessions_body}" --arg workshop_id "${workshop_id}" '[.[] | select(.workshopId == $workshop_id and (.state != "TERMINATED" and .state != "FAILED" and .state != "EXPIRED"))] | length')"
    note "${workshop_id} active sessions: ${active_count}"
  done
else
  warn "No actor headers configured. Authenticated session checks were skipped."
  warn "Set PILOT_ACTOR_ID and related env vars to include learner or admin checks."
fi

if actor_configured && actor_has_admin_role; then
  status_code="$(api_get "${CONTROL_PLANE_URL}/api/sessions/admin/releases/overview" "${release_body}")"
  [[ "${status_code}" == "200" ]] || die "Admin release overview failed with status ${status_code}"
  catalog_available="$(json_get "${release_body}" '.catalogAvailable')"
  release_count="$(json_get "${release_body}" '.releases | length')"
  pass "admin release overview reachable, catalogAvailable=${catalog_available}, releases=${release_count}"

  status_code="$(api_get "${CONTROL_PLANE_URL}/api/sessions/admin/overview" "${admin_overview_body}")"
  [[ "${status_code}" == "200" ]] || die "Admin session overview failed with status ${status_code}"
  pass "admin session overview reachable"
elif actor_configured; then
  warn "Actor does not have admin role. Admin release checks were skipped."
fi

echo
echo "Workshop release defaults"
for workshop_id in "${PILOT_WORKSHOPS[@]}"; do
  default_release="$(json_query "${catalog_body}" --arg workshop_id "${workshop_id}" '.[] | select(.workshopId == $workshop_id) | .defaultReleaseVersion')"
  default_mode="$(json_query "${catalog_body}" --arg workshop_id "${workshop_id}" '.[] | select(.workshopId == $workshop_id) | .defaultMode')"
  echo "  ${workshop_id}: release=${default_release} mode=${default_mode}"
done

echo
pass "release default preflight completed"
