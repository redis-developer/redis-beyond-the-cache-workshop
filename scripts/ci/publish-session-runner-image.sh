#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
usage: publish-session-runner-image.sh --workshop-id ID --repository REPOSITORY [options]

Builds a session runner image and optionally pushes it so CI can publish a
digest pinned image reference for the workshop release metadata.

Options:
  --workshop-id ID                 Workshop backend module, for example 1_session_management.
  --manager-module MODULE          Frontend manager module. Defaults to ID_frontend.
  --repository REPOSITORY          Registry repository without tag.
  --variant NAME                   local-redis-insight. Defaults to local-redis-insight.
  --source-revision REVISION       Source revision. Defaults to current git SHA or local.
  --tag TAG                        Full image tag. Defaults to REPOSITORY:ID-REVISION-VARIANT.
  --redis-insight-archive PATH     Optional Redis Insight archive for local-redis-insight images.
  --platform PLATFORM              Docker target platform. Defaults to linux/amd64.
  --push                           Push the image and record a digest pinned reference.
  --output PATH                    Metadata JSON output.
  -h, --help                       Show this help.
USAGE
}

fail() {
  printf '%s\n' "$*" >&2
  exit 1
}

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
builder="${repo_root}/java-springboot/session_runtime_tools/runner-image/build-local-runner-image.sh"

workshop_id=""
manager_module=""
repository=""
variant="local-redis-insight"
source_revision=""
image_tag=""
redis_insight_archive=""
image_platform="linux/amd64"
output_path=""
push_image=false

while [ "$#" -gt 0 ]; do
  case "$1" in
    --workshop-id)
      workshop_id="${2:-}"
      shift 2
      ;;
    --manager-module)
      manager_module="${2:-}"
      shift 2
      ;;
    --repository)
      repository="${2:-}"
      shift 2
      ;;
    --variant)
      variant="${2:-}"
      shift 2
      ;;
    --source-revision)
      source_revision="${2:-}"
      shift 2
      ;;
    --tag)
      image_tag="${2:-}"
      shift 2
      ;;
    --redis-insight-archive)
      redis_insight_archive="${2:-}"
      shift 2
      ;;
    --platform)
      image_platform="${2:-}"
      shift 2
      ;;
    --push)
      push_image=true
      shift
      ;;
    --output)
      output_path="${2:-}"
      shift 2
      ;;
    -h|--help)
      usage
      exit 0
      ;;
    *)
      fail "Unknown argument: $1"
      ;;
  esac
done

[ -n "$workshop_id" ] || fail "Missing --workshop-id"
[ -n "$repository" ] || fail "Missing --repository"
[ -x "$builder" ] || fail "Runner image builder is not executable: $builder"

if [ -z "$source_revision" ]; then
  source_revision="$(git -C "$repo_root" rev-parse --short=12 HEAD 2>/dev/null || printf 'local')"
fi

safe_revision="$(printf '%s' "$source_revision" | tr -c 'A-Za-z0-9_.-' '-')"
safe_variant="$(printf '%s' "$variant" | tr -c 'A-Za-z0-9_.-' '-')"
image_tag="${image_tag:-${repository}:${workshop_id}-${safe_revision}-${safe_variant}}"
output_path="${output_path:-${repo_root}/build/session-runner-image-${workshop_id}-${safe_variant}.json}"
local_output="$(mktemp "${TMPDIR:-/tmp}/session-runner-publish.XXXXXX")"

builder_args=(
  --workshop-id "$workshop_id"
  --image-tag "$image_tag"
  --variant "$variant"
  --source-revision "$source_revision"
  --platform "$image_platform"
  --output "$local_output"
)

if [ -n "$manager_module" ]; then
  builder_args+=(--manager-module "$manager_module")
fi

if [ -n "$redis_insight_archive" ]; then
  builder_args+=(--redis-insight-archive "$redis_insight_archive")
fi

"$builder" "${builder_args[@]}"

digest=""
image_reference="$image_tag"

if [ "$push_image" = true ]; then
  docker push "$image_tag"
  repo_digest="$(docker image inspect --format '{{if .RepoDigests}}{{index .RepoDigests 0}}{{end}}' "$image_tag" 2>/dev/null || true)"
  if [ -z "$repo_digest" ]; then
    repo_digest="$(docker buildx imagetools inspect "$image_tag" --format '{{json .Manifest.Digest}}' 2>/dev/null | tr -d '"' || true)"
    if [ -n "$repo_digest" ]; then
      repo_digest="${repository}@${repo_digest}"
    fi
  fi
  [ -n "$repo_digest" ] || fail "Image was pushed but no digest was available: $image_tag"
  digest="${repo_digest#*@}"
  image_reference="${repository}@${digest}"
fi

mkdir -p "$(dirname "$output_path")"
cat > "$output_path" <<JSON
{
  "workshop_id": "${workshop_id}",
  "manager_module": "${manager_module:-${workshop_id}_frontend}",
  "variant": "${variant}",
  "tag": "${image_tag}",
  "repository": "${repository}",
  "digest": "${digest}",
  "image_reference": "${image_reference}",
  "source_revision": "${source_revision}",
  "platform": "${image_platform}",
  "published": ${push_image}
}
JSON

rm -f "$local_output"
printf 'Session runner publish metadata written to %s\n' "$output_path"
