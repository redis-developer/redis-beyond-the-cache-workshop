#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
usage: publish-cloud-run-smoke-images.sh --project-id PROJECT [options]

Builds the images needed for a one workshop Cloud Run smoke test and writes
digest references that can be passed to infra/terraform/cloudrun.

Options:
  --project-id PROJECT             Google Cloud project, for example personal-rdlts.
  --region REGION                  Artifact Registry region. Defaults to europe-west4.
  --repository-id ID               Artifact Registry repository id. Defaults to session-runners.
  --workshop-id ID                 Workshop runner to publish. Defaults to 1_session_management.
  --runner-variant NAME            Runner variant. Defaults to local-redis-insight.
  --redis-insight-archive PATH     Optional Redis Insight archive passed to the runner image builder.
  --platform PLATFORM              Docker target platform. Defaults to linux/amd64.
  --source-revision REVISION       Source revision. Defaults to current git SHA or local.
  --push                           Push images and emit digest pinned references.
  --output PATH                    Metadata JSON output. Defaults to build/cloud-run-smoke-images.json.
  --tfvars-output PATH             Terraform tfvars snippet output.
  -h, --help                       Show this help.
USAGE
}

fail() {
  printf '%s\n' "$*" >&2
  exit 1
}

json_escape() {
  printf '%s' "$1" | sed 's/\\/\\\\/g; s/"/\\"/g'
}

json_value() {
  key="$1"
  file="$2"
  sed -n "s/.*\"${key}\": \"\\([^\"]*\\)\".*/\\1/p" "$file" | head -n 1
}

image_reference_after_push() {
  tag="$1"
  repository="$2"
  repo_digest="$(docker image inspect --format '{{if .RepoDigests}}{{index .RepoDigests 0}}{{end}}' "$tag" 2>/dev/null || true)"
  if [ -z "$repo_digest" ]; then
    repo_digest="$(docker buildx imagetools inspect "$tag" --format '{{json .Manifest.Digest}}' 2>/dev/null | tr -d '"' || true)"
    if [ -n "$repo_digest" ]; then
      repo_digest="${repository}@${repo_digest}"
    fi
  fi
  [ -n "$repo_digest" ] || fail "Image was pushed but no digest was available: $tag"
  printf '%s' "$repo_digest"
}

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../.." && pwd)"
runner_publisher="${repo_root}/scripts/ci/publish-session-runner-image.sh"

project_id=""
region="europe-west4"
repository_id="session-runners"
workshop_id="1_session_management"
runner_variant="local-redis-insight"
redis_insight_archive=""
image_platform="linux/amd64"
source_revision=""
push_images=false
output_path=""
tfvars_output=""

while [ "$#" -gt 0 ]; do
  case "$1" in
    --project-id)
      project_id="${2:-}"
      shift 2
      ;;
    --region)
      region="${2:-}"
      shift 2
      ;;
    --repository-id)
      repository_id="${2:-}"
      shift 2
      ;;
    --workshop-id)
      workshop_id="${2:-}"
      shift 2
      ;;
    --runner-variant)
      runner_variant="${2:-}"
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
    --source-revision)
      source_revision="${2:-}"
      shift 2
      ;;
    --push)
      push_images=true
      shift
      ;;
    --output)
      output_path="${2:-}"
      shift 2
      ;;
    --tfvars-output)
      tfvars_output="${2:-}"
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

[ -n "$project_id" ] || fail "Missing --project-id"
[ -x "$runner_publisher" ] || fail "Runner publisher is not executable: $runner_publisher"

if [ -z "$source_revision" ]; then
  source_revision="$(git -C "$repo_root" rev-parse --short=12 HEAD 2>/dev/null || printf 'local')"
fi

safe_revision="$(printf '%s' "$source_revision" | tr -c 'A-Za-z0-9_.-' '-')"
registry="${region}-docker.pkg.dev/${project_id}/${repository_id}"
runner_repository="${registry}/${workshop_id}"
control_plane_tag="${registry}/platform-control-plane:${safe_revision}"
execution_plane_tag="${registry}/platform-execution-plane:${safe_revision}"
output_path="${output_path:-${repo_root}/build/cloud-run-smoke-images.json}"
tfvars_output="${tfvars_output:-${repo_root}/build/cloud-run-smoke-images.auto.tfvars}"
runner_output="$(mktemp "${TMPDIR:-/tmp}/cloud-run-runner.XXXXXX")"

runner_args=(
  --workshop-id "$workshop_id"
  --repository "$runner_repository"
  --variant "$runner_variant"
  --source-revision "$source_revision"
  --platform "$image_platform"
  --output "$runner_output"
)

if [ -n "$redis_insight_archive" ]; then
  runner_args+=(--redis-insight-archive "$redis_insight_archive")
fi

if [ "$push_images" = true ]; then
  runner_args+=(--push)
fi

"$runner_publisher" "${runner_args[@]}"

runner_image_reference="$(json_value image_reference "$runner_output")"
[ -n "$runner_image_reference" ] || fail "Runner publisher did not emit image_reference"

docker build \
  --platform "$image_platform" \
  --build-arg "SOURCE_REVISION=${source_revision}" \
  --build-arg "SESSION_MANAGEMENT_RUNNER_IMAGE=${runner_image_reference}" \
  -t "$control_plane_tag" \
  -f "${repo_root}/java-springboot/platform_control_plane/Dockerfile" \
  "$repo_root"

docker build \
  --platform "$image_platform" \
  --build-arg "SOURCE_REVISION=${source_revision}" \
  -t "$execution_plane_tag" \
  -f "${repo_root}/java-springboot/platform_execution_plane/Dockerfile" \
  "$repo_root"

control_plane_reference="$control_plane_tag"
execution_plane_reference="$execution_plane_tag"

if [ "$push_images" = true ]; then
  docker push "$control_plane_tag"
  control_plane_reference="$(image_reference_after_push "$control_plane_tag" "${registry}/platform-control-plane")"
  docker push "$execution_plane_tag"
  execution_plane_reference="$(image_reference_after_push "$execution_plane_tag" "${registry}/platform-execution-plane")"
fi

mkdir -p "$(dirname "$output_path")" "$(dirname "$tfvars_output")"

cat > "$output_path" <<JSON
{
  "project_id": "$(json_escape "$project_id")",
  "region": "$(json_escape "$region")",
  "repository_id": "$(json_escape "$repository_id")",
  "source_revision": "$(json_escape "$source_revision")",
  "workshop_id": "$(json_escape "$workshop_id")",
  "runner_variant": "$(json_escape "$runner_variant")",
  "platform": "$(json_escape "$image_platform")",
  "runner_image": "$(json_escape "$runner_image_reference")",
  "control_plane_image": "$(json_escape "$control_plane_reference")",
  "execution_plane_image": "$(json_escape "$execution_plane_reference")",
  "pushed": ${push_images}
}
JSON

cat > "$tfvars_output" <<TFVARS
control_plane_image   = "${control_plane_reference}"
execution_plane_image = "${execution_plane_reference}"
TFVARS

rm -f "$runner_output"
printf 'Cloud Run smoke image metadata written to %s\n' "$output_path"
printf 'Terraform image variables written to %s\n' "$tfvars_output"
