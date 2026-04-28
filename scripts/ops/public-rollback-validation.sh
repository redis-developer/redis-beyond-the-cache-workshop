#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/common.sh"

usage() {
  cat <<EOF
Usage:
  ./scripts/ops/public-rollback-validation.sh
  ./scripts/ops/public-rollback-validation.sh --validate

Default mode prints the rollback checklist.
Validate mode checks that the live release defaults match the currently active release default rules.
EOF
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if [[ "${1:-}" != "--validate" ]]; then
  exec bash "${SCRIPT_DIR}/pilot-rollback-checklist.sh" "$@"
fi

require_command curl
require_command jq
require_file "${PILOT_RULES_FILE}"

catalog_body="$(mktemp)"
trap 'rm -f "${catalog_body}"' EXIT

status_code="$(api_get "${CONTROL_PLANE_URL}/api/catalog/workshops" "${catalog_body}")"
[[ "${status_code}" == "200" ]] || die "Control plane catalog request failed with status ${status_code}"

for workshop_id in "${PUBLIC_WORKSHOPS[@]}"; do
  expected_release="current"
  rule_enabled="$(yaml_workshop_field "${PILOT_RULES_FILE}" "${workshop_id}" "enabled")"
  if [[ "${rule_enabled}" == "true" ]] && yaml_workshop_has_environment "${PILOT_RULES_FILE}" "${workshop_id}" "local"; then
    expected_release="$(yaml_workshop_field "${PILOT_RULES_FILE}" "${workshop_id}" "releaseVersion")"
  fi

  live_release_version="$(json_query "${catalog_body}" --arg workshop_id "${workshop_id}" '.[] | select(.workshopId == $workshop_id) | .defaultReleaseVersion')"
  [[ "${live_release_version}" == "${expected_release}" ]] || die "Rollback surface validation failed for ${workshop_id}: expected ${expected_release}, got ${live_release_version}"
  pass "${workshop_id} live release matches the current rollback target model (${expected_release})"
done

echo
pass "public rollback validation completed"
