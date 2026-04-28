#!/usr/bin/env bash

set -euo pipefail

OUTPUT_DIR="${OUTPUT_DIR:-scripts/loadtest/output/cloudrun}"
INPUT="${INPUT:-}"
LIVE_CHECKS="${LIVE_CHECKS:-off}"
BASE_URL="${BASE_URL:-}"
DEFAULT_ACTOR_TYPE="${ACTOR_TYPE:-LEARNER}"
DEFAULT_ACTOR_ROLES="${ACTOR_ROLES:-learner}"
GCP_PROJECT="${GCP_PROJECT:-}"
GCP_REGION="${GCP_REGION:-}"
WORKSPACE_BUCKET="${WORKSPACE_BUCKET:-}"
EXPECT_WORKSPACE_PREFIX_DELETED="${EXPECT_WORKSPACE_PREFIX_DELETED:-0}"

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

session_ids_from_log() {
  jq -r 'select(.sessionId != null and .sessionId != "") | .sessionId' "$1" | sort -u
}

check_route_metadata() {
  local input_file="$1"
  local attention=0
  require_command curl

  while IFS= read -r record; do
    [[ -n "${record}" ]] || continue
    local actor_id
    local actor_type
    local actor_roles
    local record_base_url
    local request_base_url
    local response_file
    local http_status
    local state
    local cleanup_status
    local public_entry_url
    local session_id

    actor_id="$(printf '%s' "${record}" | jq -r '.actorId // ""')"
    actor_type="$(printf '%s' "${record}" | jq -r '.actorType // ""')"
    actor_roles="$(printf '%s' "${record}" | jq -r '.actorRoles // ""')"
    record_base_url="$(printf '%s' "${record}" | jq -r '.baseUrl // ""')"
    session_id="$(printf '%s' "${record}" | jq -r '.sessionId // ""')"
    [[ -n "${actor_id}" && -n "${session_id}" ]] || continue

    request_base_url="${BASE_URL}"
    if [[ -z "${request_base_url}" && -n "${record_base_url}" ]]; then
      request_base_url="${record_base_url}"
    fi
    if [[ -z "${request_base_url}" ]]; then
      echo "[warn] Route metadata check skipped for ${session_id}. BASE_URL was not available." >&2
      continue
    fi
    if [[ -z "${actor_type}" ]]; then
      actor_type="${DEFAULT_ACTOR_TYPE}"
    fi
    if [[ -z "${actor_roles}" ]]; then
      actor_roles="${DEFAULT_ACTOR_ROLES}"
    fi

    response_file="$(mktemp "${TMPDIR:-/tmp}/cloud-run-route.XXXXXX")"
    if http_status=$(curl -sS -o "${response_file}" -w "%{http_code}" \
        -H "X-Platform-Actor-Id: ${actor_id}" \
        -H "X-Platform-Actor-Type: ${actor_type}" \
        -H "X-Platform-Roles: ${actor_roles}" \
        "${request_base_url}/api/sessions/${session_id}"); then
      :
    else
      echo "ATTENTION route metadata fetch failed for ${session_id}" >&2
      attention=$((attention + 1))
      rm -f "${response_file}"
      continue
    fi

    if [[ "${http_status}" == "404" ]]; then
      rm -f "${response_file}"
      continue
    fi
    if [[ "${http_status}" -ge 400 ]]; then
      echo "ATTENTION route metadata fetch returned ${http_status} for ${session_id}" >&2
      attention=$((attention + 1))
      rm -f "${response_file}"
      continue
    fi

    state="$(jq -r '.state // ""' "${response_file}")"
    cleanup_status="$(jq -r '.workspaceCleanupStatus // ""' "${response_file}")"
    public_entry_url="$(jq -r '.publicEntryUrl // ""' "${response_file}")"
    rm -f "${response_file}"

    case "${state}" in
      TERMINATED|CLEANUP_PENDING|EXPIRED|FAILED)
        if [[ "${cleanup_status}" == "COMPLETED" && -n "${public_entry_url}" && "${public_entry_url}" != "null" ]]; then
          echo "ATTENTION ${session_id} still has route metadata after cleanup: ${public_entry_url}" >&2
          attention=$((attention + 1))
        fi
        ;;
    esac
  done < "${input_file}"

  [[ "${attention}" -eq 0 ]]
}

check_cloud_run_services() {
  local input_file="$1"
  local services_file
  local session_ids_file
  local attention=0

  services_file="$(mktemp "${TMPDIR:-/tmp}/cloud-run-services.XXXXXX")"
  session_ids_file="$(mktemp "${TMPDIR:-/tmp}/cloud-run-sessions.XXXXXX")"
  trap 'rm -f "${services_file}" "${session_ids_file}"' RETURN

  session_ids_from_log "${input_file}" > "${session_ids_file}"
  gcloud run services list \
    --project "${GCP_PROJECT}" \
    --region "${GCP_REGION}" \
    --format=json > "${services_file}"

  while IFS= read -r service; do
    [[ -n "${service}" ]] || continue
    local service_name
    local session_id
    service_name="$(printf '%s' "${service}" | jq -r '.metadata.name // ""')"
    session_id="$(printf '%s' "${service}" | jq -r '.metadata.labels["workshop-session-id"] // ""')"
    [[ -n "${session_id}" ]] || continue
    if grep -qx "${session_id}" "${session_ids_file}"; then
      echo "ATTENTION Cloud Run session service still exists after cleanup: ${service_name} session=${session_id}" >&2
      attention=$((attention + 1))
    fi
  done < <(jq -c '.[] | select(.metadata.labels["managed-by"] == "workshop-execution-plane")' "${services_file}")

  [[ "${attention}" -eq 0 ]]
}

check_workspace_prefixes() {
  local input_file="$1"
  local attention=0
  while IFS= read -r session_id; do
    [[ -n "${session_id}" ]] || continue
    if gcloud storage ls "gs://${WORKSPACE_BUCKET}/sessions/${session_id}/**" >/dev/null 2>&1; then
      echo "ATTENTION workspace prefix still exists for cleaned session: gs://${WORKSPACE_BUCKET}/sessions/${session_id}/" >&2
      attention=$((attention + 1))
    fi
  done < <(session_ids_from_log "${input_file}")

  [[ "${attention}" -eq 0 ]]
}

require_command jq

if [[ -z "${INPUT}" ]]; then
  INPUT="$(latest_log || true)"
fi

if [[ -z "${INPUT}" || ! -f "${INPUT}" ]]; then
  echo "[warn] No Cloud Run run log was found under ${OUTPUT_DIR}. Cleanup checks were skipped." >&2
  exit 0
fi

attention=0
check_route_metadata "${INPUT}" || attention=$((attention + 1))

case "${LIVE_CHECKS}" in
  off)
    echo "[warn] Live Cloud Run service and workspace checks were skipped. Set LIVE_CHECKS=on with GCP_PROJECT and GCP_REGION." >&2
    ;;
  on)
    require_command gcloud
    [[ -n "${GCP_PROJECT}" ]] || {
      echo "GCP_PROJECT is required when LIVE_CHECKS=on" >&2
      exit 1
    }
    [[ -n "${GCP_REGION}" ]] || {
      echo "GCP_REGION is required when LIVE_CHECKS=on" >&2
      exit 1
    }

    check_cloud_run_services "${INPUT}" || attention=$((attention + 1))

    if [[ "${EXPECT_WORKSPACE_PREFIX_DELETED}" == "1" ]]; then
      [[ -n "${WORKSPACE_BUCKET}" ]] || {
        echo "WORKSPACE_BUCKET is required when EXPECT_WORKSPACE_PREFIX_DELETED=1" >&2
        exit 1
      }
      check_workspace_prefixes "${INPUT}" || attention=$((attention + 1))
    else
      echo "[warn] Workspace prefix deletion check skipped. Set EXPECT_WORKSPACE_PREFIX_DELETED=1 and WORKSPACE_BUCKET to enforce it." >&2
    fi
    ;;
  *)
    echo "LIVE_CHECKS must be off or on" >&2
    exit 1
    ;;
esac

if [[ "${attention}" -gt 0 ]]; then
  echo "Cloud Run cleanup checks found ${attention} issue(s)." >&2
  exit 1
fi

echo "[ok] Cloud Run cleanup checks completed" >&2
