#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

usage() {
  cat <<EOF
Usage: ./scripts/ops/pilot-launch-rules-check.sh

Checks the local workshop registry release defaults and compares them with the live control plane catalog.

Environment:
  CONTROL_PLANE_URL      Default: ${CONTROL_PLANE_URL}
  HUB_URL                Default: ${HUB_URL}
  PILOT_RULES_FILE       Legacy alias for WORKSHOPS_FILE. Default: ${PILOT_RULES_FILE}
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

catalog_body="$(mktemp)"
hub_catalog_body="$(mktemp)"
trap 'rm -f "${catalog_body}" "${hub_catalog_body}"' EXIT

status_code="$(api_get "${CONTROL_PLANE_URL}/api/catalog/workshops" "${catalog_body}")"
[[ "${status_code}" == "200" ]] || die "Control plane catalog request failed with status ${status_code}"

status_code="$(api_get "${HUB_URL}/api/catalog/workshops" "${hub_catalog_body}")"
[[ "${status_code}" == "200" ]] || die "Hub catalog request failed with status ${status_code}"

for workshop_id in "${PILOT_WORKSHOPS[@]}"; do
  print_workshop_header "${workshop_id}"

  catalog_block="$(yaml_registry_default_release_block "${RELEASE_CATALOG_FILE}" "${workshop_id}")"
  [[ -n "${catalog_block}" ]] || die "No default release entry found in release source for ${workshop_id}"

  catalog_release_id="$(yaml_registry_default_release_field "${RELEASE_CATALOG_FILE}" "${workshop_id}" "releaseId")"
  catalog_release_version="$(yaml_registry_default_release_field "${RELEASE_CATALOG_FILE}" "${workshop_id}" "releaseVersion")"
  catalog_enabled="$(yaml_registry_default_release_field "${RELEASE_CATALOG_FILE}" "${workshop_id}" "enabled")"

  [[ -n "${catalog_release_id}" ]] || die "Default release releaseId missing for ${workshop_id}"
  [[ -n "${catalog_release_version}" ]] || die "Default release releaseVersion missing for ${workshop_id}"
  [[ "${catalog_enabled}" == "true" ]] || die "Default release is not enabled for ${workshop_id}"
  yaml_registry_default_release_has_environment "${RELEASE_CATALOG_FILE}" "${workshop_id}" "local" || die "Default release for ${workshop_id} is not active for local environment"

  live_release_version="$(json_query "${catalog_body}" --arg workshop_id "${workshop_id}" '.[] | select(.workshopId == $workshop_id) | .defaultReleaseVersion')"
  live_mode="$(json_query "${catalog_body}" --arg workshop_id "${workshop_id}" '.[] | select(.workshopId == $workshop_id) | .defaultMode')"
  hub_release_version="$(json_query "${hub_catalog_body}" --arg workshop_id "${workshop_id}" '.[] | select(.workshopId == $workshop_id) | .defaultReleaseVersion')"

  [[ -n "${live_release_version}" ]] || die "Control plane catalog did not return ${workshop_id}"
  [[ "${live_release_version}" == "${catalog_release_version}" ]] || die "Live control plane defaultReleaseVersion mismatch for ${workshop_id}: expected ${catalog_release_version}, got ${live_release_version}"
  [[ "${hub_release_version}" == "${catalog_release_version}" ]] || die "Live hub defaultReleaseVersion mismatch for ${workshop_id}: expected ${catalog_release_version}, got ${hub_release_version}"

  echo "catalog releaseId:     ${catalog_release_id}"
  echo "catalog releaseVersion:${catalog_release_version}"
  echo "live defaultRelease:   ${live_release_version}"
  echo "live mode:             ${live_mode}"
  pass "registry release default and live catalog agree for ${workshop_id}"
done

echo
pass "release default registry check completed"
