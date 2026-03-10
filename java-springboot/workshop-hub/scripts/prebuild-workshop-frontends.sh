#!/bin/sh

set -eu

usage() {
    echo "Usage: $0 [--dry-run] <workshops-file> <workspace-root> <staging-root>" >&2
    exit 1
}

DRY_RUN=false
if [ "${1:-}" = "--dry-run" ]; then
    DRY_RUN=true
    shift
fi

[ "$#" -eq 3 ] || usage

WORKSHOPS_FILE=$1
WORKSPACE_ROOT=$2
STAGING_ROOT=$3

list_prebuild_frontend_dockerfiles() {
    awk '
        function trim(value) {
            gsub(/^[[:space:]]+|[[:space:]]+$/, "", value)
            return value
        }

        function emit() {
            if (tolower(frontend_prebuild) == "true" && frontend_dockerfile != "" && !seen[frontend_dockerfile]++) {
                print frontend_dockerfile
            }
        }

        /^[[:space:]]*-[[:space:]]*id:/ {
            emit()
            frontend_dockerfile = ""
            frontend_prebuild = "false"
            next
        }

        /^[[:space:]]*frontendDockerfile:/ {
            frontend_dockerfile = trim(substr($0, index($0, ":") + 1))
            next
        }

        /^[[:space:]]*frontendPrebuild:/ {
            frontend_prebuild = trim(substr($0, index($0, ":") + 1))
            next
        }

        END {
            emit()
        }
    ' "$WORKSHOPS_FILE"
}

module_path_from_dockerfile() {
    frontend_dockerfile=$1
    case "$frontend_dockerfile" in
        */*)
            printf '%s\n' "${frontend_dockerfile%/*}"
            ;;
        *)
            printf '.\n'
            ;;
    esac
}

normalize_relative_path() {
    path=$1
    case "$path" in
        .)
            printf '\n'
            ;;
        ./*)
            printf '%s\n' "${path#./}"
            ;;
        *)
            printf '%s\n' "$path"
            ;;
    esac
}

run_npm_install() {
    npm install --no-fund --no-audit
}

mkdir -p "$STAGING_ROOT"

MODULES=$(list_prebuild_frontend_dockerfiles)

if [ -z "$MODULES" ]; then
    echo "No workshop frontends are marked with frontendPrebuild=true in $WORKSHOPS_FILE; skipping."
    exit 0
fi

printf '%s\n' "$MODULES" | while IFS= read -r frontend_dockerfile; do
    [ -n "$frontend_dockerfile" ] || continue

    module_path=$(module_path_from_dockerfile "$frontend_dockerfile")
    relative_module_path=$(normalize_relative_path "$module_path")

    if [ -n "$relative_module_path" ]; then
        module_dir="$WORKSPACE_ROOT/$relative_module_path"
        staged_module_dir="$STAGING_ROOT/$relative_module_path/src/main/resources"
    else
        module_dir="$WORKSPACE_ROOT"
        staged_module_dir="$STAGING_ROOT/src/main/resources"
    fi

    frontend_dir="$module_dir/frontend"
    static_dir="$module_dir/src/main/resources/static"

    if [ ! -d "$frontend_dir" ]; then
        echo "Missing frontend directory: $frontend_dir" >&2
        exit 1
    fi

    if [ "$DRY_RUN" = true ]; then
        echo "would prebuild $module_path"
        continue
    fi

    echo "Prebuilding frontend: $module_path"
    (
        cd "$frontend_dir"
        run_npm_install
        VUE_APP_BASE_PATH=/ npm run build
    )

    if [ ! -d "$static_dir" ]; then
        echo "Expected built static assets in $static_dir" >&2
        exit 1
    fi

    rm -rf "$staged_module_dir/static"
    mkdir -p "$staged_module_dir"
    cp -R "$static_dir" "$staged_module_dir/static"
done
