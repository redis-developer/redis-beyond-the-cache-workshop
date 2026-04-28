#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

validate_only=0

usage() {
  cat <<EOF
Usage:
  ./scripts/ops/pilot-rollback-checklist.sh
  ./scripts/ops/pilot-rollback-checklist.sh --validate

Default mode prints the current release default state and a local development checklist.
Validate mode checks that the release backed workshops now use current.

Environment:
  CONTROL_PLANE_URL      Default: ${CONTROL_PLANE_URL}
  HUB_URL                Default: ${HUB_URL}
  PILOT_ACTOR_ID         Historical variable name for optional learner or admin actor id
  PILOT_ACTOR_TYPE       Default: ${PILOT_ACTOR_TYPE}
  PILOT_ACTOR_ROLES      Default: ${PILOT_ACTOR_ROLES}
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --validate)
      validate_only=1
      shift
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      die "Unknown argument: $1"
      ;;
  esac
done

require_command curl
require_command jq
require_file "${PILOT_RULES_FILE}"

init_actor_headers

catalog_body="$(mktemp)"
sessions_body="$(mktemp)"
trap 'rm -f "${catalog_body}" "${sessions_body}"' EXIT

status_code="$(api_get "${CONTROL_PLANE_URL}/api/catalog/workshops" "${catalog_body}")"
[[ "${status_code}" == "200" ]] || die "Control plane catalog request failed with status ${status_code}"

if [[ "${validate_only}" == "1" ]]; then
  for workshop_id in "${PILOT_WORKSHOPS[@]}"; do
    live_release_version="$(json_query "${catalog_body}" --arg workshop_id "${workshop_id}" '.[] | select(.workshopId == $workshop_id) | .defaultReleaseVersion')"
    [[ "${live_release_version}" == "current" ]] || die "Local development validation failed for ${workshop_id}: expected current, got ${live_release_version}"
    pass "${workshop_id} now uses current"
  done

  if actor_configured; then
    status_code="$(api_get "${HUB_URL}/api/sessions" "${sessions_body}")"
    [[ "${status_code}" == "200" ]] || die "Authenticated session listing failed with status ${status_code}"
    for workshop_id in "${PILOT_WORKSHOPS[@]}"; do
      active_count="$(json_query "${sessions_body}" --arg workshop_id "${workshop_id}" '[.[] | select(.workshopId == $workshop_id and (.state != "TERMINATED" and .state != "FAILED" and .state != "EXPIRED"))] | length')"
      if [[ "${active_count}" != "0" ]]; then
        warn "${workshop_id} still has ${active_count} active sessions after switching to current"
      else
        pass "${workshop_id} has no active sessions after switching to current"
      fi
    done
  fi

  echo
  pass "local development validation completed"
  exit 0
fi

echo "Current release defaults"
for workshop_id in "${PILOT_WORKSHOPS[@]}"; do
  print_workshop_header "${workshop_id}"
  live_release_version="$(json_query "${catalog_body}" --arg workshop_id "${workshop_id}" '.[] | select(.workshopId == $workshop_id) | .defaultReleaseVersion')"
  live_mode="$(json_query "${catalog_body}" --arg workshop_id "${workshop_id}" '.[] | select(.workshopId == $workshop_id) | .defaultMode')"
  echo "live defaultReleaseVersion: ${live_release_version}"
  echo "live defaultMode:           ${live_mode}"
done

if actor_configured; then
  status_code="$(api_get "${HUB_URL}/api/sessions" "${sessions_body}")"
  [[ "${status_code}" == "200" ]] || die "Authenticated session listing failed with status ${status_code}"
  echo
  echo "Active release backed sessions"
  for workshop_id in "${PILOT_WORKSHOPS[@]}"; do
    print_workshop_header "${workshop_id}"
    session_lines="$(json_query "${sessions_body}" --arg workshop_id "${workshop_id}" '
      [.[] | select(.workshopId == $workshop_id and (.state != "TERMINATED" and .state != "FAILED" and .state != "EXPIRED"))
       | "\(.sessionId) \(.state) \(.publicEntryUrl // "-")"] | .[]
    ')"
    if [[ -n "${session_lines}" ]]; then
      echo "${session_lines}"
    else
      echo "none"
    fi
  done
else
  warn "No actor headers configured. Active session listing was skipped."
fi

echo
echo "Local development checklist"
echo "1. Edit ${PILOT_RULES_FILE}."
echo "2. For each release backed workshop, set enabled: false or remove local from environments."
echo "3. Restart the control plane."
echo "4. Run ./scripts/ops/pilot-rollback-checklist.sh --validate"
echo "5. If actor headers are configured, terminate any remaining release backed sessions through the control plane UI or DELETE /api/sessions/{sessionId}."
echo "6. Launch one fresh session for each release backed workshop and confirm the current local development path behaves as expected."
echo "7. Re run ./scripts/ops/pilot-preflight.sh before continuing normal operator work."
