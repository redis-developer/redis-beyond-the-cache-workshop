#!/usr/bin/env bash

set -euo pipefail

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

require_command jq

if [ -z "$INPUT" ]; then
  INPUT="$(latest_run_log)"
fi

if [ -z "$INPUT" ] || [ ! -f "$INPUT" ]; then
  echo "Could not find a run log. Set INPUT to a steady or burst JSONL file." >&2
  exit 1
fi

echo "Summary for $INPUT"
echo

jq -s '
  def count_by(key):
    sort_by(. [key]) |
    group_by(. [key]) |
    map({key: (.[0][key] // "unknown"), count: length});

  def average_of(values):
    if (values | length) == 0 then null else ((values | add) / (values | length)) end;

  def percentile_of(values; percentile):
    if (values | length) == 0 then null
    else
      (values | sort) as $sorted |
      ($sorted | length) as $count |
      (((($count - 1) * percentile) / 100) | ceil) as $index |
      $sorted[$index]
    end;

  def metric_block:
    {
      launchRequests: length,
      launchSuccesses: (map(select(.launchOutcome == "ready" or .launchOutcome == "degraded")) | length),
      launchFailures: (map(select(.launchOutcome != "ready" and .launchOutcome != "degraded")) | length),
      terminationRequests: (map(select(.terminationRequested == true)) | length),
      averageLaunchDurationSeconds: average_of(map(select(.launchDurationSeconds != null) | .launchDurationSeconds)),
      startupLatencyP50Seconds: percentile_of(map(select(.launchDurationSeconds != null) | .launchDurationSeconds); 50),
      startupLatencyP95Seconds: percentile_of(map(select(.launchDurationSeconds != null) | .launchDurationSeconds); 95),
      startupLatencyMaxSeconds: (map(select(.launchDurationSeconds != null) | .launchDurationSeconds) | max?),
      cleanupLagSamples: (map(select(.cleanupLagSeconds != null) | .cleanupLagSeconds) | length),
      cleanupLagP50Seconds: percentile_of(map(select(.cleanupLagSeconds != null) | .cleanupLagSeconds); 50),
      cleanupLagP95Seconds: percentile_of(map(select(.cleanupLagSeconds != null) | .cleanupLagSeconds); 95),
      cleanupLagMaxSeconds: (map(select(.cleanupLagSeconds != null) | .cleanupLagSeconds) | max?),
      byLaunchOutcome: count_by("launchOutcome"),
      byFinalState: count_by("finalState")
    };

  {
    totalRecords: length,
    successfulLaunches: (map(select(.launchOutcome == "ready" or .launchOutcome == "degraded")) | length),
    failedLaunches: (map(select(.launchOutcome != "ready" and .launchOutcome != "degraded")) | length),
    averageLaunchDurationSeconds: average_of(map(select(.launchDurationSeconds != null) | .launchDurationSeconds)),
    cleanupLagSamples: (map(select(.cleanupLagSeconds != null) | .cleanupLagSeconds) | length),
    cleanupLagP50Seconds: percentile_of(map(select(.cleanupLagSeconds != null) | .cleanupLagSeconds); 50),
    cleanupLagP95Seconds: percentile_of(map(select(.cleanupLagSeconds != null) | .cleanupLagSeconds); 95),
    byWorkshop: count_by("workshopId"),
    byWorkshopMetrics: (
      sort_by(.workshopId) |
      group_by(.workshopId) |
      map({
        workshopId: (.[0].workshopId // "unknown"),
        metrics: metric_block
      })
    ),
    byLaunchOutcome: count_by("launchOutcome"),
    byFinalState: count_by("finalState")
  }' "$INPUT"
