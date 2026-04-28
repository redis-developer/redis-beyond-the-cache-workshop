#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:9000}"
WORKSHOPS_CSV="${WORKSHOPS:-1_session_management,2_full_text_search,3_distributed_locks,4_agent_memory}"
BURST_SIZE="${BURST_SIZE:-10}"
ACTOR_ID_PREFIX="${ACTOR_ID_PREFIX:-burst-learner}"
ACTOR_TYPE="${ACTOR_TYPE:-LEARNER}"
ACTOR_ROLES="${ACTOR_ROLES:-learner}"
POLL_INTERVAL_SECONDS="${POLL_INTERVAL_SECONDS:-2}"
READY_TIMEOUT_SECONDS="${READY_TIMEOUT_SECONDS:-240}"
TERMINATION_TIMEOUT_SECONDS="${TERMINATION_TIMEOUT_SECONDS:-180}"
CLEANUP_TIMEOUT_SECONDS="${CLEANUP_TIMEOUT_SECONDS:-180}"
TERMINATE_AFTER_READY="${TERMINATE_AFTER_READY:-1}"
OUTPUT_DIR="${OUTPUT_DIR:-scripts/loadtest/output}"

require_command() {
  command -v "$1" >/dev/null 2>&1 || {
    echo "Missing required command: $1" >&2
    exit 1
  }
}

http_get_session() {
  local actor_id="$1"
  local session_id="$2"
  local output_file="$3"

  curl -sS -o "$output_file" -w "%{http_code}" \
    -H "X-Platform-Actor-Id: $actor_id" \
    -H "X-Platform-Actor-Type: $ACTOR_TYPE" \
    -H "X-Platform-Roles: $ACTOR_ROLES" \
    "$BASE_URL/api/sessions/$session_id"
}

poll_session() {
  local actor_id="$1"
  local session_id="$2"
  local timeout_seconds="$3"
  local phase="$4"
  local output_file="$5"
  local deadline
  local http_status
  local state
  local cleanup_status
  local cleanup_completed_at

  deadline=$(( $(date +%s) + timeout_seconds ))
  while [ "$(date +%s)" -le "$deadline" ]; do
    if ! http_status=$(http_get_session "$actor_id" "$session_id" "$output_file"); then
      return 1
    fi
    if [ "$http_status" -ge 400 ]; then
      return 1
    fi

    state="$(jq -r '.state // ""' "$output_file")"
    cleanup_status="$(jq -r '.workspaceCleanupStatus // ""' "$output_file")"
    cleanup_completed_at="$(jq -r '.cleanupCompletedAt // ""' "$output_file")"
    case "$phase:$state" in
      launch:READY|launch:DEGRADED|launch:FAILED|launch:EXPIRED|launch:TERMINATED|launch:CLEANUP_PENDING)
        return 0
        ;;
      terminate:TERMINATED|terminate:CLEANUP_PENDING|terminate:EXPIRED|terminate:FAILED)
        return 0
        ;;
    esac

    if [ "$phase" = "cleanup" ]; then
      case "$cleanup_status" in
        COMPLETED|FAILED|NONE)
          return 0
          ;;
      esac
      if [ -n "$cleanup_completed_at" ]; then
        return 0
      fi
    fi

    sleep "$POLL_INTERVAL_SECONDS"
  done

  return 1
}

write_record() {
  local output_file="$1"
  local run_id="$2"
  local actor_id="$3"
  local workshop_id="$4"
  local create_http_status="$5"
  local session_id="$6"
  local launch_outcome="$7"
  local launch_state="$8"
  local terminate_http_status="$9"
  local final_state="${10}"
  local cleanup_status="${11}"
  local public_entry_url="${12}"
  local error_code="${13}"
  local error_message="${14}"
  local launch_duration_seconds="${15}"
  local total_duration_seconds="${16}"
  local termination_requested="${17}"
  local terminated_at="${18}"
  local cleanup_completed_at="${19}"
  local cleanup_lag_seconds="${20}"

  jq -cn \
    --arg runId "$run_id" \
    --arg script "$(basename "$0")" \
    --arg baseUrl "$BASE_URL" \
    --arg actorId "$actor_id" \
    --arg actorType "$ACTOR_TYPE" \
    --arg actorRoles "$ACTOR_ROLES" \
    --arg workshopId "$workshop_id" \
    --arg sessionId "$session_id" \
    --arg launchOutcome "$launch_outcome" \
    --arg launchState "$launch_state" \
    --arg finalState "$final_state" \
    --arg workspaceCleanupStatus "$cleanup_status" \
    --arg publicEntryUrl "$public_entry_url" \
    --arg errorCode "$error_code" \
    --arg errorMessage "$error_message" \
    --arg terminatedAt "$terminated_at" \
    --arg cleanupCompletedAt "$cleanup_completed_at" \
    --argjson createHttpStatus "$create_http_status" \
    --argjson terminateHttpStatus "$terminate_http_status" \
    --argjson launchDurationSeconds "$launch_duration_seconds" \
    --argjson totalDurationSeconds "$total_duration_seconds" \
    --argjson terminationRequested "$termination_requested" \
    --argjson cleanupLagSeconds "$cleanup_lag_seconds" \
    '{
      runId: $runId,
      script: $script,
      baseUrl: $baseUrl,
      actorId: $actorId,
      actorType: $actorType,
      actorRoles: $actorRoles,
      workshopId: $workshopId,
      createHttpStatus: $createHttpStatus,
      sessionId: $sessionId,
      launchOutcome: $launchOutcome,
      launchState: $launchState,
      terminateHttpStatus: $terminateHttpStatus,
      finalState: $finalState,
      workspaceCleanupStatus: $workspaceCleanupStatus,
      publicEntryUrl: $publicEntryUrl,
      errorCode: $errorCode,
      errorMessage: $errorMessage,
      terminatedAt: $terminatedAt,
      cleanupCompletedAt: $cleanupCompletedAt,
      cleanupLagSeconds: $cleanupLagSeconds,
      launchDurationSeconds: $launchDurationSeconds,
      totalDurationSeconds: $totalDurationSeconds,
      terminationRequested: $terminationRequested
    }' > "$output_file"
}

run_worker() {
  local index="$1"
  local workshop_id="$2"
  local actor_id="${ACTOR_ID_PREFIX}-${index}"
  local create_file="$TMP_DIR/create-$index.json"
  local launch_file="$TMP_DIR/launch-$index.json"
  local final_file="$TMP_DIR/final-$index.json"
  local output_file="$TMP_DIR/result-$index.json"
  local start_time
  local create_http_status=0
  local terminate_http_status=0
  local session_id=""
  local launch_outcome=""
  local launch_state=""
  local final_state=""
  local cleanup_status=""
  local public_entry_url=""
  local error_code=""
  local error_message=""
  local termination_requested=false
  local launch_end_time
  local now_time
  local launch_duration_seconds
  local total_duration_seconds
  local terminated_at=""
  local cleanup_completed_at=""
  local cleanup_lag_seconds="null"

  start_time="$(date +%s)"
  launch_end_time="$start_time"

  if create_http_status=$(curl -sS -o "$create_file" -w "%{http_code}" \
      -X POST \
      -H "Content-Type: application/json" \
      -H "X-Platform-Actor-Id: $actor_id" \
      -H "X-Platform-Actor-Type: $ACTOR_TYPE" \
      -H "X-Platform-Roles: $ACTOR_ROLES" \
      -d "{\"workshopId\":\"$workshop_id\"}" \
      "$BASE_URL/api/sessions"); then
    :
  else
    error_code="curl_error"
    error_message="Failed to create session"
    launch_outcome="create_request_failed"
  fi

  if [ "$create_http_status" -eq 202 ]; then
    session_id="$(jq -r '.sessionId // ""' "$create_file")"
    if [ -n "$session_id" ] && poll_session "$actor_id" "$session_id" "$READY_TIMEOUT_SECONDS" launch "$launch_file"; then
      launch_state="$(jq -r '.state // ""' "$launch_file")"
      public_entry_url="$(jq -r '.publicEntryUrl // ""' "$launch_file")"
      case "$launch_state" in
        READY)
          launch_outcome="ready"
          ;;
        DEGRADED)
          launch_outcome="degraded"
          ;;
        FAILED)
          launch_outcome="launch_failed"
          ;;
        EXPIRED)
          launch_outcome="launch_expired"
          ;;
        TERMINATED)
          launch_outcome="launch_terminated"
          ;;
        CLEANUP_PENDING)
          launch_outcome="launch_cleanup_pending"
          ;;
        *)
          launch_outcome="launch_unknown"
          ;;
      esac
    else
      if [ -f "$launch_file" ]; then
        launch_state="$(jq -r '.state // ""' "$launch_file")"
        public_entry_url="$(jq -r '.publicEntryUrl // ""' "$launch_file")"
      fi
      launch_outcome="launch_timeout"
      error_code="launch_timeout"
      error_message="Session did not reach a settled launch state before timeout"
    fi
  elif [ "$create_http_status" -eq 409 ]; then
    launch_outcome="create_conflict"
    error_code="$(jq -r '.code // ""' "$create_file")"
    error_message="$(jq -r '.detail // .message // ""' "$create_file")"
    session_id="$(jq -r '.existingSessionId // ""' "$create_file")"
    launch_state="$(jq -r '.existingSession.state // ""' "$create_file")"
    public_entry_url="$(jq -r '.existingSession.publicEntryUrl // ""' "$create_file")"
  elif [ "$create_http_status" -gt 0 ]; then
    launch_outcome="create_http_error"
    error_code="$(jq -r '.code // ""' "$create_file" 2>/dev/null || true)"
    error_message="$(jq -r '.detail // .message // ""' "$create_file" 2>/dev/null || true)"
  fi

  launch_end_time="$(date +%s)"

  if [ "$TERMINATE_AFTER_READY" = "1" ] && [ -n "$session_id" ] && { [ "$launch_state" = "READY" ] || [ "$launch_state" = "DEGRADED" ]; }; then
    termination_requested=true
    if terminate_http_status=$(curl -sS -o "$TMP_DIR/terminate-$index.json" -w "%{http_code}" \
        -X DELETE \
        -H "X-Platform-Actor-Id: $actor_id" \
        -H "X-Platform-Actor-Type: $ACTOR_TYPE" \
        -H "X-Platform-Roles: $ACTOR_ROLES" \
        "$BASE_URL/api/sessions/$session_id"); then
      :
    else
      terminate_http_status=0
      error_code="${error_code:-terminate_curl_error}"
      error_message="${error_message:-Failed to terminate session}"
    fi
  fi

  if [ -n "$session_id" ]; then
    if [ "$termination_requested" = true ]; then
      if poll_session "$actor_id" "$session_id" "$TERMINATION_TIMEOUT_SECONDS" terminate "$final_file"; then
        :
      fi
      if [ -f "$final_file" ]; then
        cleanup_status="$(jq -r '.workspaceCleanupStatus // ""' "$final_file")"
        if [ "$cleanup_status" = "ACTIVE" ] || [ "$cleanup_status" = "PENDING" ]; then
          poll_session "$actor_id" "$session_id" "$CLEANUP_TIMEOUT_SECONDS" cleanup "$final_file" || true
        fi
      fi
    else
      if [ -f "$launch_file" ]; then
        cp "$launch_file" "$final_file"
      elif http_get_session "$actor_id" "$session_id" "$final_file" >/dev/null 2>&1; then
        :
      fi
    fi
  fi

  if [ -f "$final_file" ]; then
    final_state="$(jq -r '.state // ""' "$final_file")"
    cleanup_status="$(jq -r '.workspaceCleanupStatus // ""' "$final_file")"
    public_entry_url="${public_entry_url:-$(jq -r '.publicEntryUrl // ""' "$final_file")}"
    terminated_at="$(jq -r '.terminatedAt // ""' "$final_file")"
    cleanup_completed_at="$(jq -r '.cleanupCompletedAt // ""' "$final_file")"
    cleanup_lag_seconds="$(jq -nr \
      --arg terminatedAt "$terminated_at" \
      --arg cleanupCompletedAt "$cleanup_completed_at" '
      def to_epoch:
        if . == "" then null
        else (sub("\\.[0-9]+Z$"; "Z") | fromdateiso8601)
        end;
      if ($terminatedAt | length) == 0 or ($cleanupCompletedAt | length) == 0 then null
      else (($cleanupCompletedAt | to_epoch) - ($terminatedAt | to_epoch))
      end
    ')"
  else
    final_state="$launch_state"
  fi

  now_time="$(date +%s)"
  launch_duration_seconds=$(( launch_end_time - start_time ))
  total_duration_seconds=$(( now_time - start_time ))

  write_record \
    "$output_file" \
    "$RUN_ID" \
    "$actor_id" \
    "$workshop_id" \
    "$create_http_status" \
    "$session_id" \
    "$launch_outcome" \
    "$launch_state" \
    "$terminate_http_status" \
    "$final_state" \
    "$cleanup_status" \
    "$public_entry_url" \
    "$error_code" \
    "$error_message" \
    "$launch_duration_seconds" \
    "$total_duration_seconds" \
    "$termination_requested" \
    "$terminated_at" \
    "$cleanup_completed_at" \
    "$cleanup_lag_seconds"
}

require_command curl
require_command jq

IFS=',' read -r -a WORKSHOPS <<< "$WORKSHOPS_CSV"
if [ "${#WORKSHOPS[@]}" -eq 0 ]; then
  echo "WORKSHOPS must contain at least one workshop id" >&2
  exit 1
fi

mkdir -p "$OUTPUT_DIR"
RUN_ID="burst-$(date -u +%Y%m%dT%H%M%SZ)"
OUTPUT_FILE="$OUTPUT_DIR/$RUN_ID.jsonl"
TMP_DIR="$(mktemp -d "${TMPDIR:-/tmp}/pilot-burst.XXXXXX")"
trap 'rm -rf "$TMP_DIR"' EXIT

echo "Writing burst launch results to $OUTPUT_FILE" >&2
echo "Launching $BURST_SIZE concurrent sessions" >&2

pids=()
index=1
while [ "$index" -le "$BURST_SIZE" ]; do
  workshop_index=$(( (index - 1) % ${#WORKSHOPS[@]} ))
  workshop_id="${WORKSHOPS[$workshop_index]}"
  run_worker "$index" "$workshop_id" &
  pids+=("$!")
  index=$(( index + 1 ))
done

for pid in "${pids[@]}"; do
  wait "$pid" || true
done

find "$TMP_DIR" -maxdepth 1 -type f -name 'result-*.json' | sort | while IFS= read -r file; do
  cat "$file"
  printf '\n'
done > "$OUTPUT_FILE"

echo "Burst launch run complete: $OUTPUT_FILE" >&2
