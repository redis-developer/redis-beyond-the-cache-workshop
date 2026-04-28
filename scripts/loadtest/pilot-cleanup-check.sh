#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:9000}"
DEFAULT_ACTOR_TYPE="${ACTOR_TYPE:-LEARNER}"
DEFAULT_ACTOR_ROLES="${ACTOR_ROLES:-learner}"
WAIT_SECONDS="${WAIT_SECONDS:-0}"
OUTPUT_DIR="${OUTPUT_DIR:-scripts/loadtest/output}"
INPUT="${INPUT:-}"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

latest_run_log() {
  find "$OUTPUT_DIR" -maxdepth 1 -type f -name '*.jsonl' | sort | tail -n 1
}

require_command curl
require_command jq

if [ -z "$INPUT" ]; then
  INPUT="$(latest_run_log)"
fi

if [ -z "$INPUT" ] || [ ! -f "$INPUT" ]; then
  echo "Could not find a run log. Set INPUT to a steady or burst JSONL file." >&2
  exit 1
fi

if [ "$WAIT_SECONDS" -gt 0 ]; then
  echo "Waiting $WAIT_SECONDS seconds before cleanup verification" >&2
  sleep "$WAIT_SECONDS"
fi

checked=0
attention=0

while IFS= read -r record; do
  [ -n "$record" ] || continue

  actor_id="$(printf '%s' "$record" | jq -r '.actorId // ""')"
  actor_type="$(printf '%s' "$record" | jq -r '.actorType // ""')"
  actor_roles="$(printf '%s' "$record" | jq -r '.actorRoles // ""')"
  record_base_url="$(printf '%s' "$record" | jq -r '.baseUrl // ""')"
  session_id="$(printf '%s' "$record" | jq -r '.sessionId // ""')"
  workshop_id="$(printf '%s' "$record" | jq -r '.workshopId // ""')"

  if [ -z "$actor_type" ]; then
    actor_type="$DEFAULT_ACTOR_TYPE"
  fi

  if [ -z "$actor_roles" ]; then
    actor_roles="$DEFAULT_ACTOR_ROLES"
  fi

  [ -n "$actor_id" ] || continue
  [ -n "$session_id" ] || continue

  checked=$(( checked + 1 ))
  response_file="$(mktemp "${TMPDIR:-/tmp}/pilot-cleanup.XXXXXX")"
  request_base_url="$BASE_URL"

  if [ -n "$record_base_url" ]; then
    request_base_url="$record_base_url"
  fi

  if http_status=$(curl -sS -o "$response_file" -w "%{http_code}" \
      -H "X-Platform-Actor-Id: $actor_id" \
      -H "X-Platform-Actor-Type: $actor_type" \
      -H "X-Platform-Roles: $actor_roles" \
      "$request_base_url/api/sessions/$session_id"); then
    :
  else
    echo "ATTENTION $workshop_id $session_id actor=$actor_id fetch_failed" >&2
    attention=$(( attention + 1 ))
    rm -f "$response_file"
    continue
  fi

  if [ "$http_status" -ge 400 ]; then
    echo "ATTENTION $workshop_id $session_id actor=$actor_id http_status=$http_status" >&2
    attention=$(( attention + 1 ))
    rm -f "$response_file"
    continue
  fi

  state="$(jq -r '.state // ""' "$response_file")"
  cleanup_status="$(jq -r '.workspaceCleanupStatus // ""' "$response_file")"

  case "$state" in
    REQUESTED|ADMITTED|PROVISIONING|INITIALIZING|READY|DEGRADED|TERMINATING)
      echo "ATTENTION $workshop_id $session_id actor=$actor_id state=$state cleanup=$cleanup_status" >&2
      attention=$(( attention + 1 ))
      ;;
    *)
      case "$cleanup_status" in
        ACTIVE|PENDING|FAILED)
          echo "ATTENTION $workshop_id $session_id actor=$actor_id state=$state cleanup=$cleanup_status" >&2
          attention=$(( attention + 1 ))
          ;;
        *)
          echo "OK $workshop_id $session_id actor=$actor_id state=$state cleanup=$cleanup_status" >&2
          ;;
      esac
      ;;
  esac

  rm -f "$response_file"
done < "$INPUT"

echo "Checked $checked sessions from $INPUT" >&2

if [ "$attention" -gt 0 ]; then
  echo "Cleanup verification found $attention session(s) that still need attention." >&2
  exit 1
fi

echo "Cleanup verification passed." >&2
