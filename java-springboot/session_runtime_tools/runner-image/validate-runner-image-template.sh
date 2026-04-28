#!/usr/bin/env bash
set -euo pipefail

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
dockerfile="${script_dir}/Dockerfile"
builder="${script_dir}/build-local-runner-image.sh"

[ -f "$dockerfile" ] || {
  printf 'Missing Dockerfile template: %s\n' "$dockerfile" >&2
  exit 1
}

[ -f "$builder" ] || {
  printf 'Missing local builder: %s\n' "$builder" >&2
  exit 1
}

bash -n "$builder"

grep -q 'com.redis.workshop.runner.workshop-id' "$dockerfile"
grep -q 'WORKSHOP_SESSION_RUNNER_AUTO_START=true' "$dockerfile"
grep -q 'WORKSHOP_CHILD_COMMAND' "$dockerfile"
grep -q 'WORKSHOP_CHILD_REBUILD_COMMAND' "$dockerfile"
grep -q 'WORKSHOP_REBUILT_CHILD_JAR' "$dockerfile"
grep -q 'WORKSHOP_LOCAL_REDIS_INSIGHT_COMMAND' "$dockerfile"
grep -q 'RI_PROXY_PATH=/redis-insight' "$dockerfile"
grep -q 'exec java' "$dockerfile"
grep -q 'HEALTHCHECK' "$dockerfile"
grep -q 'local-redis-insight' "$dockerfile"
grep -q 'redis/redisinsight' "$dockerfile"

printf 'Runner image template validation passed\n'
