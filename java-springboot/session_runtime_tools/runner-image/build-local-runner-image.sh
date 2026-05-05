#!/usr/bin/env bash
set -euo pipefail

usage() {
  cat <<'USAGE'
usage: build-local-runner-image.sh --workshop-id ID [options]

Builds a Cloud Run session runner image from one selected workshop and its
frontend manager module without changing source files in the repository.

Options:
  --workshop-id ID                 Workshop backend module, for example 1_session_management.
  --manager-module MODULE          Frontend manager module. Defaults to ID_frontend.
  --workshop-source-dir PATH       Backend source directory. Defaults to the Gradle project dir for ID.
  --manager-source-dir PATH        Frontend source directory. Defaults to the Gradle project dir for MODULE.
  --image-tag TAG                  Local image tag. Defaults to redis-workshop-session-runner:ID.
  --variant NAME                   local-redis-insight. Defaults to local-redis-insight.
  --source-revision REVISION       Source revision label. Defaults to current git SHA or local.
  --redis-insight-archive PATH     Optional Redis Insight archive for local-redis-insight images.
  --platform PLATFORM              Docker target platform. Defaults to linux/amd64.
  --output PATH                    Metadata JSON output. Defaults to build/session-runner-image.json.
  --context-dir PATH               Keep the generated Docker context at PATH instead of a temp dir.
  --no-build                       Validate and prepare context, but do not run docker build.
  -h, --help                       Show this help.
USAGE
}

fail() {
  printf '%s\n' "$*" >&2
  exit 1
}

script_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
repo_root="$(cd "${script_dir}/../../.." && pwd)"

workshop_id=""
manager_module=""
workshop_source_dir=""
manager_source_dir=""
image_tag=""
variant="local-redis-insight"
source_revision=""
redis_insight_archive=""
image_platform="linux/amd64"
output_path=""
context_dir=""
build_image=true

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
    --workshop-source-dir)
      workshop_source_dir="${2:-}"
      shift 2
      ;;
    --manager-source-dir)
      manager_source_dir="${2:-}"
      shift 2
      ;;
    --image-tag)
      image_tag="${2:-}"
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
    --redis-insight-archive)
      redis_insight_archive="${2:-}"
      shift 2
      ;;
    --platform)
      image_platform="${2:-}"
      shift 2
      ;;
    --output)
      output_path="${2:-}"
      shift 2
      ;;
    --context-dir)
      context_dir="${2:-}"
      shift 2
      ;;
    --no-build)
      build_image=false
      shift
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
[ "$variant" = "local-redis-insight" ] || fail "Unsupported --variant: $variant"

manager_module="${manager_module:-${workshop_id}_frontend}"
image_tag="${image_tag:-redis-workshop-session-runner:${workshop_id}}"
output_path="${output_path:-${repo_root}/build/session-runner-image.json}"

if [ -z "$source_revision" ]; then
  source_revision="$(git -C "$repo_root" rev-parse --short=12 HEAD 2>/dev/null || printf 'local')"
fi

settings_path="${repo_root}/java-springboot/settings.gradle.kts"
java_dir="${repo_root}/java-springboot"
infra_dir="${repo_root}/java-springboot/workshop-infrastructure"

absolute_input_path() {
  path="$1"
  case "$path" in
    /*)
      printf '%s\n' "$path"
      ;;
    *)
      printf '%s/%s\n' "$repo_root" "$path"
      ;;
  esac
}

module_project_dir_from_settings() {
  module_name="$1"
  if [ ! -f "$settings_path" ]; then
    return 0
  fi
  sed -nE "s#^[[:space:]]*project\\(\":${module_name}\"\\)\\.projectDir[[:space:]]*=[[:space:]]*file\\(\"([^\"]+)\"\\).*#\\1#p" "$settings_path" | head -n 1
}

resolve_module_dir() {
  module_name="$1"
  explicit_dir="$2"

  if [ -n "$explicit_dir" ]; then
    absolute_input_path "$explicit_dir"
    return 0
  fi

  default_dir="${java_dir}/${module_name}"
  if [ -d "$default_dir" ]; then
    printf '%s\n' "$default_dir"
    return 0
  fi

  mapped_dir="$(module_project_dir_from_settings "$module_name")"
  if [ -n "$mapped_dir" ]; then
    printf '%s/%s\n' "$java_dir" "$mapped_dir"
    return 0
  fi

  printf '%s\n' "$default_dir"
}

relative_to_java_dir() {
  absolute_dir="$(cd "$1" && pwd -P)"
  java_dir_abs="$(cd "$java_dir" && pwd -P)"
  case "$absolute_dir" in
    "$java_dir_abs"/*)
      printf '%s\n' "${absolute_dir#"$java_dir_abs"/}"
      ;;
    *)
      fail "Module source directory must be under java-springboot: $1"
      ;;
  esac
}

workshop_dir="$(resolve_module_dir "$workshop_id" "$workshop_source_dir")"
manager_dir="$(resolve_module_dir "$manager_module" "$manager_source_dir")"

[ -d "$workshop_dir" ] || fail "Workshop module not found: ${workshop_dir}"
[ -d "$manager_dir" ] || fail "Manager module not found: ${manager_dir}"
[ -d "$infra_dir" ] || fail "Infrastructure module not found: ${infra_dir}"

workshop_project_dir="$(relative_to_java_dir "$workshop_dir")"
manager_project_dir="$(relative_to_java_dir "$manager_dir")"

if [ -n "$redis_insight_archive" ]; then
  [ -f "$redis_insight_archive" ] || fail "Redis Insight archive not found: ${redis_insight_archive}"
fi

cleanup_context=""
if [ -z "$context_dir" ]; then
  context_dir="$(mktemp -d "${TMPDIR:-/tmp}/session-runner-image.XXXXXX")"
  cleanup_context="$context_dir"
else
  mkdir -p "$context_dir"
fi

cleanup() {
  if [ -n "$cleanup_context" ]; then
    rm -rf "$cleanup_context"
  fi
}
trap cleanup EXIT

copy_dir() {
  src="$1"
  dest="$2"
  mkdir -p "$dest"
  (
    cd "$src"
    tar \
      --exclude='.gradle' \
      --exclude='build' \
      --exclude='node_modules' \
      --exclude='.DS_Store' \
      -cf - .
  ) | (
    cd "$dest"
    tar -xf -
  )
}

mkdir -p "${context_dir}/java-springboot" "${context_dir}/redis-insight"
cp "${script_dir}/Dockerfile" "${context_dir}/Dockerfile"
cp "${repo_root}/java-springboot/build.gradle.kts" "${context_dir}/java-springboot/build.gradle.kts"
cp "${repo_root}/java-springboot/gradlew" "${context_dir}/java-springboot/gradlew"
copy_dir "${repo_root}/java-springboot/gradle" "${context_dir}/java-springboot/gradle"
copy_dir "$infra_dir" "${context_dir}/java-springboot/workshop-infrastructure"
copy_dir "$workshop_dir" "${context_dir}/java-springboot/${workshop_project_dir}"
copy_dir "$manager_dir" "${context_dir}/java-springboot/${manager_project_dir}"
copy_dir "${repo_root}/workshop-frontend-shared" "${context_dir}/workshop-frontend-shared"
cp "${repo_root}/workshops.yaml" "${context_dir}/workshops.yaml"
touch "${context_dir}/redis-insight/.keep"

cat > "${context_dir}/java-springboot/settings.gradle.kts" <<SETTINGS
rootProject.name = "redis-session-runner-image"

include("workshop-infrastructure")
include("${workshop_id}")
project(":${workshop_id}").projectDir = file("${workshop_project_dir}")
include("${manager_module}")
project(":${manager_module}").projectDir = file("${manager_project_dir}")
SETTINGS

cat > "${context_dir}/.dockerignore" <<'DOCKERIGNORE'
**/.gradle
**/build
**/node_modules
**/.DS_Store
DOCKERIGNORE

redis_insight_archive_name=""
if [ -n "$redis_insight_archive" ]; then
  redis_insight_archive_name="$(basename "$redis_insight_archive")"
  cp "$redis_insight_archive" "${context_dir}/redis-insight/${redis_insight_archive_name}"
fi

mkdir -p "$(dirname "$output_path")"

if [ "$build_image" = true ]; then
  docker build \
    --platform "$image_platform" \
    --build-arg "WORKSHOP_ID=${workshop_id}" \
    --build-arg "MANAGER_MODULE=${manager_module}" \
    --build-arg "WORKSHOP_SOURCE_DIR=${workshop_project_dir}" \
    --build-arg "MANAGER_SOURCE_DIR=${manager_project_dir}" \
    --build-arg "RUNNER_VARIANT=${variant}" \
    --build-arg "SOURCE_REVISION=${source_revision}" \
    --build-arg "REDIS_INSIGHT_ARCHIVE_NAME=${redis_insight_archive_name}" \
    -t "$image_tag" \
    "$context_dir"

  image_id="$(docker image inspect --format '{{.Id}}' "$image_tag")"
  repo_digest="$(docker image inspect --format '{{if .RepoDigests}}{{index .RepoDigests 0}}{{end}}' "$image_tag" 2>/dev/null || true)"
else
  image_id=""
  repo_digest=""
fi

cat > "$output_path" <<JSON
{
  "workshop_id": "${workshop_id}",
  "manager_module": "${manager_module}",
  "workshop_source_dir": "${workshop_project_dir}",
  "manager_source_dir": "${manager_project_dir}",
  "variant": "${variant}",
  "image_tag": "${image_tag}",
  "image_id": "${image_id}",
  "repo_digest": "${repo_digest}",
  "source_revision": "${source_revision}",
  "platform": "${image_platform}",
  "context_dir": "${context_dir}",
  "built": ${build_image}
}
JSON

printf 'Session runner image metadata written to %s\n' "$output_path"
