#!/usr/bin/env bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

CONTROL_PLANE_URL="${CONTROL_PLANE_URL:-http://localhost:8080}"
HUB_URL="${HUB_URL:-http://localhost:9000}"
PILOT_ACTOR_ID="${PILOT_ACTOR_ID:-}"
PILOT_ACTOR_TYPE="${PILOT_ACTOR_TYPE:-LEARNER}"
PILOT_ACTOR_ROLES="${PILOT_ACTOR_ROLES:-learner}"
WORKSHOPS_FILE="${WORKSHOPS_FILE:-${REPO_ROOT}/workshops.yaml}"
PILOT_RULES_FILE="${PILOT_RULES_FILE:-${WORKSHOPS_FILE}}"
RELEASE_CATALOG_FILE="${RELEASE_CATALOG_FILE:-${WORKSHOPS_FILE}}"

readonly REPO_ROOT
readonly CONTROL_PLANE_URL
readonly HUB_URL
readonly PILOT_ACTOR_ID
readonly PILOT_ACTOR_TYPE
readonly PILOT_ACTOR_ROLES
readonly PILOT_RULES_FILE
readonly WORKSHOPS_FILE
readonly RELEASE_CATALOG_FILE
readonly PILOT_WORKSHOPS=(
  "1_session_management"
  "2_full_text_search"
  "3_distributed_locks"
  "4_agent_memory"
)
readonly PUBLIC_WORKSHOPS=(
  "1_session_management"
  "2_full_text_search"
  "3_distributed_locks"
  "4_agent_memory"
)
readonly PUBLIC_RESULTS_TEMPLATE="${REPO_ROOT}/docs/pilot/public-launch-results-template.md"
readonly PUBLIC_RUNBOOK_FILE="${REPO_ROOT}/docs/operations/public-launch-runbook.md"
readonly PUBLIC_BASELINES_FILE="${REPO_ROOT}/docs/operations/public-launch-baselines.md"
readonly PUBLIC_READINESS_FILE="${REPO_ROOT}/docs/operations/public-launch-readiness-checklist.md"
readonly PUBLIC_ALERTS_FILE="${REPO_ROOT}/scripts/ops/public-launch-alerts.json"

declare -a ACTOR_HEADER_ARGS=()

die() {
  echo "ERROR: $*" >&2
  exit 1
}

note() {
  echo "[info] $*"
}

pass() {
  echo "[ok] $*"
}

warn() {
  echo "[warn] $*" >&2
}

require_command() {
  local command_name="$1"
  command -v "${command_name}" >/dev/null 2>&1 || die "Missing required command: ${command_name}"
}

require_file() {
  local file_path="$1"
  [[ -f "${file_path}" ]] || die "Missing required file: ${file_path}"
}

init_actor_headers() {
  ACTOR_HEADER_ARGS=()
  if [[ -n "${PILOT_ACTOR_ID}" ]]; then
    ACTOR_HEADER_ARGS+=(-H "X-Platform-Actor-Id: ${PILOT_ACTOR_ID}")
    ACTOR_HEADER_ARGS+=(-H "X-Platform-Actor-Type: ${PILOT_ACTOR_TYPE}")
    ACTOR_HEADER_ARGS+=(-H "X-Platform-Roles: ${PILOT_ACTOR_ROLES}")
  fi
}

actor_configured() {
  [[ -n "${PILOT_ACTOR_ID}" ]]
}

actor_has_admin_role() {
  [[ ",${PILOT_ACTOR_ROLES}," == *",admin,"* ]]
}

json_get() {
  local file_path="$1"
  local jq_filter="$2"
  jq -r "${jq_filter}" "${file_path}"
}

json_query() {
  local file_path="$1"
  shift
  jq -r "$@" "${file_path}"
}

api_request() {
  local method="$1"
  local url="$2"
  local body_file="$3"
  local payload="${4:-}"
  local curl_args=(
    curl
    -sS
    -X "${method}"
    -H "Accept: application/json"
    -o "${body_file}"
    -w "%{http_code}"
  )

  if [[ ${#ACTOR_HEADER_ARGS[@]} -gt 0 ]]; then
    curl_args+=("${ACTOR_HEADER_ARGS[@]}")
  fi

  if [[ -n "${payload}" ]]; then
    curl_args+=(-H "Content-Type: application/json" --data "${payload}")
  fi

  "${curl_args[@]}" "${url}"
}

api_get() {
  local url="$1"
  local body_file="$2"
  api_request GET "${url}" "${body_file}"
}

api_post_json() {
  local url="$1"
  local body_file="$2"
  local payload="$3"
  api_request POST "${url}" "${body_file}" "${payload}"
}

api_delete() {
  local url="$1"
  local body_file="$2"
  api_request DELETE "${url}" "${body_file}"
}

http_probe() {
  local url="$1"
  curl -sS --connect-timeout 2 --max-time 5 -o /dev/null "${url}" >/dev/null 2>&1
}

yaml_workshop_block() {
  local file_path="$1"
  local workshop_id="$2"
  awk -v workshop_id="${workshop_id}" '
    /^  - / {
      if (block != "" && matched) {
        printf "%s", block
        exit
      }
      block = $0 ORS
      matched = 0
      if ($1 == "-" && $2 == "workshopId:" && $3 == workshop_id) {
        matched = 1
      }
      next
    }

    block != "" {
      block = block $0 ORS
    }

    $1 == "workshopId:" && $2 == workshop_id {
      matched = 1
    }

    END {
      if (block != "" && matched) {
        printf "%s", block
      }
    }
  ' "${file_path}"
}

yaml_workshop_field() {
  local file_path="$1"
  local workshop_id="$2"
  local field_name="$3"
  yaml_workshop_block "${file_path}" "${workshop_id}" | awk -v field_name="${field_name}" '
    $1 == "-" && $2 == field_name ":" {
      print $3
      exit
    }
    $1 == field_name ":" {
      print $2
      exit
    }
  '
}

yaml_workshop_has_environment() {
  local file_path="$1"
  local workshop_id="$2"
  local environment_name="$3"
  yaml_workshop_block "${file_path}" "${workshop_id}" | awk -v environment_name="${environment_name}" '
    $1 == "-" && $2 == environment_name {
      found = 1
    }
    END {
      exit(found ? 0 : 1)
    }
  '
}

yaml_registry_workshop_block() {
  local file_path="$1"
  local workshop_id="$2"
  awk -v workshop_id="${workshop_id}" '
    /^  - id:/ {
      if (block != "" && matched) {
        printf "%s", block
        exit
      }
      block = $0 ORS
      matched = ($3 == workshop_id)
      next
    }

    block != "" {
      block = block $0 ORS
    }

    END {
      if (block != "" && matched) {
        printf "%s", block
      }
    }
  ' "${file_path}"
}

yaml_registry_default_release_block() {
  local file_path="$1"
  local workshop_id="$2"
  local workshop_block
  workshop_block="$(yaml_registry_workshop_block "${file_path}" "${workshop_id}")"

  if [[ -z "${workshop_block}" ]]; then
    yaml_workshop_block "${file_path}" "${workshop_id}"
    return
  fi

  printf '%s' "${workshop_block}" | awk '
    /^      - releaseId:/ {
      if (release != "" && default_found) {
        printf "%s", release
        exit
      }
      release = $0 ORS
      default_found = 0
      next
    }

    release != "" {
      release = release $0 ORS
      if ($1 == "defaultForWorkshop:" && $2 == "true") {
        default_found = 1
      }
    }

    END {
      if (release != "" && default_found) {
        printf "%s", release
      }
    }
  '
}

yaml_registry_default_release_field() {
  local file_path="$1"
  local workshop_id="$2"
  local field_name="$3"
  yaml_registry_default_release_block "${file_path}" "${workshop_id}" | awk -v field_name="${field_name}" '
    $1 == "-" && $2 == field_name ":" {
      print $3
      exit
    }
    $1 == field_name ":" {
      print $2
      exit
    }
  '
}

yaml_registry_default_release_has_environment() {
  local file_path="$1"
  local workshop_id="$2"
  local environment_name="$3"
  yaml_registry_default_release_block "${file_path}" "${workshop_id}" | awk -v environment_name="${environment_name}" '
    $1 == "-" && $2 == environment_name {
      found = 1
    }
    END {
      exit(found ? 0 : 1)
    }
  '
}

print_workshop_header() {
  local workshop_id="$1"
  echo
  echo "== ${workshop_id} =="
}
