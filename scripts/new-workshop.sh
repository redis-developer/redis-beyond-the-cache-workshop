#!/usr/bin/env bash
set -euo pipefail

if [ $# -lt 4 ]; then
  echo "Usage: $0 <id> <title> <serviceName> <port>" >&2
  echo "Example: $0 3_rate_limiting \"Rate Limiting\" rate-limiting 8082" >&2
  exit 1
fi

ID="$1"
TITLE="$2"
SERVICE_NAME="$3"
PORT="$4"

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REGISTRY_PATH="$ROOT_DIR/workshops.yaml"
MODULE_DIR="$ROOT_DIR/java-springboot/$ID"

if [ -e "$MODULE_DIR" ]; then
  echo "Module directory already exists: $MODULE_DIR" >&2
  exit 1
fi

if [ ! -f "$REGISTRY_PATH" ]; then
  echo "Registry not found: $REGISTRY_PATH" >&2
  exit 1
fi

if command -v rg >/dev/null 2>&1; then
  if rg -n "id: $ID" "$REGISTRY_PATH" >/dev/null; then
    echo "Workshop id already exists in registry: $ID" >&2
    exit 1
  fi
else
  if grep -q "id: $ID" "$REGISTRY_PATH"; then
    echo "Workshop id already exists in registry: $ID" >&2
    exit 1
  fi
fi

mkdir -p "$MODULE_DIR/src/main/java" "$MODULE_DIR/src/main/resources"

cat <<'DOC' > "$MODULE_DIR/README.md"
# Workshop

TODO: Describe the workshop goals, prerequisites, and steps.
DOC

cat <<'DOC' > "$MODULE_DIR/Dockerfile"
# TODO: Add workshop build steps here
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY . /app
CMD ["/bin/sh", "-c", "echo TODO: implement workshop Dockerfile"]
DOC

cat <<DOC >> "$REGISTRY_PATH"
  - id: $ID
    title: $TITLE
    description: >-
      TODO: Add workshop description.
    difficulty: Beginner
    estimatedMinutes: 30
    serviceName: $SERVICE_NAME
    port: $PORT
    url: /workshop/$SERVICE_NAME/
    dockerfile: java-springboot/$ID/Dockerfile
    topics:
      - TODO
DOC

SETTINGS_FILE="$ROOT_DIR/java-springboot/settings.gradle.kts"
if [ -f "$SETTINGS_FILE" ]; then
  if command -v rg >/dev/null 2>&1; then
    if ! rg -n "include\(\"$ID\"\)" "$SETTINGS_FILE" >/dev/null; then
      echo "include(\"$ID\")" >> "$SETTINGS_FILE"
    fi
  else
    if ! grep -q "include(\"$ID\")" "$SETTINGS_FILE"; then
      echo "include(\"$ID\")" >> "$SETTINGS_FILE"
    fi
  fi
fi

echo "Workshop scaffold created: $MODULE_DIR"
echo "Registry updated: $REGISTRY_PATH"
echo "Next: run ./java-springboot/gradlew :workshop-hub:generateCompose to refresh compose files."
