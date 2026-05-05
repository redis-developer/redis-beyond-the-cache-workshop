#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
JAVA_DIR="${REPO_ROOT}/java-springboot"
GRADLEW="${JAVA_DIR}/gradlew"
STATE_ROOT="${TMPDIR:-/tmp}/redis-workshops"
PORT_MIN=20000
PORT_SPAN=40000

usage() {
  cat <<'EOF'
Local maintainer helper only.
This script is not the public runtime path.

Usage:
  ./scripts/run-workshop.sh up <workshop>
  ./scripts/run-workshop.sh down <workshop>
  ./scripts/run-workshop.sh status <workshop>
  ./scripts/run-workshop.sh restart <workshop> [frontend|backend|all]

Workshops:
  1 | 1_session_management | session-management
  2 | 2_full_text_search   | full-text-search
  3 | 3_distributed_locks  | distributed-locks
  4 | 4_agent_memory       | agent-memory
  5 | 1_spring_ai_fundamentals | spring-ai-fundamentals
EOF
}

ensure_java_home() {
  if [[ -n "${JAVA_HOME:-}" ]]; then
    return
  fi

  if [[ -x "/usr/libexec/java_home" ]]; then
    local detected
    detected=$(/usr/libexec/java_home -v 21 2>/dev/null || /usr/libexec/java_home 2>/dev/null || true)
    if [[ -n "${detected}" ]]; then
      export JAVA_HOME="${detected}"
    fi
  fi
}

is_pid_running() {
  local pid="$1"
  [[ -n "${pid}" ]] && kill -0 "${pid}" 2>/dev/null
}

cleanup_stale_pidfile() {
  local pid_file="$1"
  if [[ ! -f "${pid_file}" ]]; then
    return
  fi

  local pid
  pid="$(cat "${pid_file}")"
  if ! is_pid_running "${pid}"; then
    rm -f "${pid_file}"
  fi
}

port_in_use() {
  local port="$1"
  if ! command -v lsof >/dev/null 2>&1; then
    return 1
  fi
  lsof -tiTCP:"${port}" -sTCP:LISTEN >/dev/null 2>&1
}

allocate_free_port() {
  local excluded_port="${1:-}"
  local candidate

  for _ in $(seq 1 200); do
    candidate=$((PORT_MIN + RANDOM % PORT_SPAN))
    if [[ "${candidate}" == "${excluded_port}" ]]; then
      continue
    fi
    if ! port_in_use "${candidate}"; then
      echo "${candidate}"
      return
    fi
  done

  echo "Could not find a free local port." >&2
  exit 1
}

listening_pid() {
  local port="$1"
  if ! command -v lsof >/dev/null 2>&1; then
    return 1
  fi
  lsof -tiTCP:"${port}" -sTCP:LISTEN 2>/dev/null | head -n 1
}

stop_pid_file() {
  local pid_file="$1"
  local name="$2"

  cleanup_stale_pidfile "${pid_file}"
  if [[ ! -f "${pid_file}" ]]; then
    echo "${name}: not running"
    return
  fi

  local pid
  pid="$(cat "${pid_file}")"
  if is_pid_running "${pid}"; then
    kill "${pid}"
    for _ in $(seq 1 20); do
      if ! is_pid_running "${pid}"; then
        break
      fi
      sleep 1
    done
    if is_pid_running "${pid}"; then
      kill -9 "${pid}" 2>/dev/null || true
    fi
  fi

  rm -f "${pid_file}"
  echo "${name}: stopped"
}

stop_service() {
  local port="$1"
  local pid_file="$2"
  local name="$3"

  local pid=""
  pid="$(listening_pid "${port}" || true)"
  if [[ -n "${pid}" ]]; then
    kill "${pid}" 2>/dev/null || true
    for _ in $(seq 1 20); do
      if ! port_in_use "${port}"; then
        break
      fi
      sleep 1
    done
    if port_in_use "${port}"; then
      pid="$(listening_pid "${port}" || true)"
      if [[ -n "${pid}" ]]; then
        kill -9 "${pid}" 2>/dev/null || true
      fi
    fi
    echo "${name}: stopped"
  else
    echo "${name}: not running"
  fi

  if [[ -f "${pid_file}" ]]; then
    stop_pid_file "${pid_file}" "${name} launcher"
  fi
}

java_executable() {
  if [[ -n "${JAVA_HOME:-}" && -x "${JAVA_HOME}/bin/java" ]]; then
    echo "${JAVA_HOME}/bin/java"
    return
  fi

  if command -v java >/dev/null 2>&1; then
    command -v java
    return
  fi

  echo "Java is required but was not found." >&2
  exit 1
}

find_boot_jar() {
  local project="$1"
  local project_dir="${JAVA_DIR}/${project}"
  if [[ "${project}" == "${backend_project:-}" && -n "${backend_project_dir:-}" ]]; then
    project_dir="${backend_project_dir}"
  elif [[ "${project}" == "${frontend_project:-}" && -n "${frontend_project_dir:-}" ]]; then
    project_dir="${frontend_project_dir}"
  fi
  local libs_dir="${project_dir}/build/libs"
  local jar_file=""

  if [[ ! -d "${libs_dir}" ]]; then
    echo "No build output found for ${project}." >&2
    exit 1
  fi

  while IFS= read -r candidate; do
    jar_file="${candidate}"
    break
  done < <(find "${libs_dir}" -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | sort)

  if [[ -z "${jar_file}" ]]; then
    echo "No boot jar found for ${project}." >&2
    exit 1
  fi

  echo "${jar_file}"
}

start_boot_app() {
  local name="$1"
  local project="$2"
  local pid_file="$3"
  local log_file="$4"
  shift 4

  cleanup_stale_pidfile "${pid_file}"
  if [[ -f "${pid_file}" ]]; then
    echo "${name}: already running (pid $(cat "${pid_file}"))"
    return
  fi

  mkdir -p "$(dirname "${pid_file}")"
  : > "${log_file}"

  echo "${name}: building boot jar..."
  if ! env "$@" "${GRADLEW}" -p "${JAVA_DIR}" --no-daemon ":${project}:bootJar" </dev/null >> "${log_file}" 2>&1; then
    echo "${name}: build failed" >&2
    tail -n 80 "${log_file}" >&2 || true
    exit 1
  fi

  local jar_file
  jar_file="$(find_boot_jar "${project}")"
  local java_cmd
  java_cmd="$(java_executable)"

  nohup env "$@" "${java_cmd}" -jar "${jar_file}" </dev/null >> "${log_file}" 2>&1 &
  echo $! > "${pid_file}"

  echo "${name}: started (pid $(cat "${pid_file}"))"
}

wait_for_port() {
  local port="$1"
  local name="$2"
  local log_file="$3"
  local timeout="${4:-90}"

  for _ in $(seq 1 "${timeout}"); do
    if port_in_use "${port}"; then
      echo "${name}: ready on port ${port}"
      return
    fi
    sleep 1
  done

  echo "${name}: failed to become ready on port ${port}" >&2
  if [[ -f "${log_file}" ]]; then
    echo "--- ${name} log ---" >&2
    tail -n 80 "${log_file}" >&2 || true
  fi
  exit 1
}

print_urls() {
  if [[ -n "${frontend_url:-}" ]]; then
    echo "Frontend: ${frontend_url}"
  else
    echo "Frontend: not assigned"
  fi
  if [[ -n "${backend_url:-}" ]]; then
    echo "Backend:  ${backend_url}"
  else
    echo "Backend:  not assigned"
  fi
  echo "Local Redis Insight: http://localhost:5540/"
  if [[ -n "${extra_url:-}" ]]; then
    echo "${extra_url}"
  fi
}

keep_alive_if_requested() {
  if [[ "${WORKSHOP_KEEP_ALIVE:-false}" != "true" ]]; then
    return
  fi

  echo
  echo "Keeping local workflow attached. Stop it with scripts/run-workshop.sh down ${workshop_id}."
  while true; do
    sleep 3600
  done
}

load_assigned_ports() {
  frontend_port=""
  backend_port=""

  if [[ ! -f "${ports_file}" ]]; then
    return 1
  fi

  # shellcheck disable=SC1090
  source "${ports_file}"
  if [[ ! "${frontend_port:-}" =~ ^[0-9]+$ || ! "${backend_port:-}" =~ ^[0-9]+$ ]]; then
    frontend_port=""
    backend_port=""
    return 1
  fi

  configure_urls
}

write_assigned_ports() {
  mkdir -p "${state_dir}"
  {
    echo "frontend_port=${frontend_port}"
    echo "backend_port=${backend_port}"
  } > "${ports_file}"
}

assign_new_ports() {
  backend_port="$(allocate_free_port)"
  frontend_port="$(allocate_free_port "${backend_port}")"
  configure_urls
  write_assigned_ports
}

configure_urls() {
  frontend_url="http://localhost:${frontend_port}/"
  backend_url="http://localhost:${backend_port}"
}

ensure_ports_for_up() {
  cleanup_stale_pidfile "${backend_pid_file}"
  cleanup_stale_pidfile "${frontend_pid_file}"

  if [[ -f "${backend_pid_file}" || -f "${frontend_pid_file}" ]]; then
    load_assigned_ports || {
      echo "A local process is running but ${ports_file} is missing or invalid." >&2
      exit 1
    }
    return
  fi

  assign_new_ports
}

load_ports_for_existing_run() {
  if load_assigned_ports; then
    return 0
  fi

  frontend_port="${legacy_frontend_port}"
  backend_port="${legacy_backend_port}"
  configure_urls
  return 1
}

start_backend() {
  echo "Starting ${display_name} backend..."
  start_boot_app \
    "backend" \
    "${backend_project}" \
    "${backend_pid_file}" \
    "${backend_log_file}" \
    SERVER_PORT="${backend_port}" \
    WORKSHOP_SOURCE_PATH="${source_path}" \
    WORKSHOP_BASE_PATH="${source_path}"
  wait_for_port "${backend_port}" "backend" "${backend_log_file}"
}

managed_runner_enabled() {
  [[ "${workshop_id}" == "1_session_management" || "${workshop_id}" == "1_spring_ai_fundamentals" ]]
}

build_backend_for_runner() {
  echo "Building ${display_name} backend child..."
  : > "${backend_log_file}"
  if ! "${GRADLEW}" -p "${JAVA_DIR}" --no-daemon -PskipFrontendBuild=true ":${backend_project}:bootJar" </dev/null >> "${backend_log_file}" 2>&1; then
    echo "backend child: build failed" >&2
    tail -n 80 "${backend_log_file}" >&2 || true
    exit 1
  fi
}

managed_child_command() {
  local jar_file
  jar_file="$(find_boot_jar "${backend_project}")"
  local java_cmd
  java_cmd="$(java_executable)"
  printf 'exec "%s" -jar "%s"' "${java_cmd}" "${jar_file}"
}

managed_child_rebuild_command() {
  printf '"%s" -p "%s" --no-watch-fs -PskipFrontendBuild=true ":%s:bootJar" -x test' \
    "${GRADLEW}" \
    "${JAVA_DIR}" \
    "${backend_project}"
}

managed_code_editor_command() {
  if [[ -n "${WORKSHOP_LOCAL_CODE_EDITOR_COMMAND:-}" ]]; then
    printf '%s' "${WORKSHOP_LOCAL_CODE_EDITOR_COMMAND}"
    return
  fi

  if ! command -v code-server >/dev/null 2>&1; then
    return
  fi

  local code_server
  local quoted_code_server
  local quoted_data_dir
  local quoted_extensions_dir
  code_server="$(command -v code-server)"
  printf -v quoted_code_server '%q' "${code_server}"
  printf -v quoted_data_dir '%q' "${state_dir}/code-server"
  printf -v quoted_extensions_dir '%q' "${state_dir}/code-server/extensions"
  printf 'SHELL=/bin/false exec %s --auth none --bind-addr "127.0.0.1:${WORKSHOP_LOCAL_CODE_EDITOR_PORT:-39000}" --disable-proxy --disable-telemetry --disable-update-check --disable-workspace-trust --user-data-dir %s --extensions-dir %s "${WORKSHOP_SESSION_WORKSPACE_PATH}"' \
    "${quoted_code_server}" \
    "${quoted_data_dir}" \
    "${quoted_extensions_dir}"
}

managed_code_editor_workspace_path() {
  if [[ -n "${WORKSHOP_LOCAL_CODE_EDITOR_WORKSPACE_PATH:-}" ]]; then
    printf '%s' "${WORKSHOP_LOCAL_CODE_EDITOR_WORKSPACE_PATH}"
    return
  fi

  if [[ -n "${WORKSHOP_CODE_EDITOR_WORKSPACE_PATH:-}" ]]; then
    printf '%s' "${WORKSHOP_CODE_EDITOR_WORKSPACE_PATH}"
    return
  fi

  if [[ -z "${code_editor_command:-}" ]] && port_in_use "${WORKSHOP_LOCAL_CODE_EDITOR_PORT:-39000}"; then
    printf '/home/coder/project'
    return
  fi

  printf '%s' "${source_path}"
}

wait_for_runner_ready() {
  local timeout="${1:-120}"
  local runner_status_url="http://localhost:${frontend_port}/internal/session-runner/status"
  local status_body=""

  for _ in $(seq 1 "${timeout}"); do
    status_body="$(curl -sS "${runner_status_url}" 2>/dev/null || true)"
    if [[ "${status_body}" == *'"state":"CHILD_READY"'* ]]; then
      echo "backend child: ready on port ${backend_port}"
      return
    fi
    if [[ "${status_body}" == *'"state":"CHILD_FAILED"'* ]]; then
      echo "backend child: failed" >&2
      echo "${status_body}" >&2
      tail -n 80 "${frontend_log_file}" >&2 || true
      exit 1
    fi
    sleep 1
  done

  echo "backend child: failed to become ready on port ${backend_port}" >&2
  tail -n 80 "${frontend_log_file}" >&2 || true
  exit 1
}

restart_managed_backend() {
  if ! port_in_use "${frontend_port}"; then
    start_frontend
    wait_for_runner_ready
    return
  fi

  local response
  response="$(curl -sS \
    -X POST \
    -H 'Content-Type: application/json' \
    -d '{"rebuild":false,"async":true}' \
    "http://localhost:${frontend_port}/internal/session-runner/restart" 2>/dev/null || true)"

  if [[ "${response}" != *'"enabled":true'* ]]; then
    echo "backend child: restart request failed" >&2
    echo "${response}" >&2
    exit 1
  fi
  wait_for_runner_ready
}

start_frontend() {
  echo "Starting ${display_name} frontend..."
  if managed_runner_enabled; then
    local code_editor_command
    local code_editor_workspace_path
    local -a frontend_env
    code_editor_command="$(managed_code_editor_command)"
    code_editor_workspace_path="$(managed_code_editor_workspace_path)"
    frontend_env=(
      SERVER_PORT="${frontend_port}"
      WORKSHOP_SESSION_RUNNER_ENABLED="true"
      WORKSHOP_SESSION_RUNNER_AUTO_START="true"
      WORKSHOP_CHILD_PORT="${backend_port}"
      WORKSHOP_CHILD_WORKING_DIRECTORY="${JAVA_DIR}"
      WORKSHOP_CHILD_COMMAND="$(managed_child_command)"
      WORKSHOP_CHILD_REBUILD_COMMAND="$(managed_child_rebuild_command)"
      WORKSHOP_CHILD_HEALTH_PATH="/login"
      WORKSHOP_LOCAL_REDIS_INSIGHT_COMMAND="sleep 2147483647"
      WORKSHOP_LOCAL_REDIS_INSIGHT_PORT="5540"
      WORKSHOP_LOCAL_CODE_EDITOR_PORT="${WORKSHOP_LOCAL_CODE_EDITOR_PORT:-39000}"
      WORKSHOP_LOCAL_CODE_EDITOR_HEALTH_PATH="${WORKSHOP_LOCAL_CODE_EDITOR_HEALTH_PATH:-/healthz}"
      WORKSHOP_SOURCE_PATH="${source_path}"
      WORKSHOP_BASE_PATH="${source_path}"
      WORKSHOP_SESSION_WORKSPACE_PATH="${source_path}"
      WORKSHOP_LOCAL_CODE_EDITOR_WORKSPACE_PATH="${code_editor_workspace_path}"
    )
    if [[ -n "${code_editor_command}" ]]; then
      frontend_env+=(WORKSHOP_LOCAL_CODE_EDITOR_COMMAND="${code_editor_command}")
    elif port_in_use "${WORKSHOP_LOCAL_CODE_EDITOR_PORT:-39000}"; then
      echo "code editor: using existing process on port ${WORKSHOP_LOCAL_CODE_EDITOR_PORT:-39000}"
    else
      echo "code editor: code-server not found; embedded VS Code process will not start locally"
    fi

    start_boot_app \
      "frontend" \
      "${frontend_project}" \
      "${frontend_pid_file}" \
      "${frontend_log_file}" \
      "${frontend_env[@]}"
    wait_for_port "${frontend_port}" "frontend" "${frontend_log_file}"
    return
  fi

  start_boot_app \
    "frontend" \
    "${frontend_project}" \
    "${frontend_pid_file}" \
    "${frontend_log_file}" \
    SERVER_PORT="${frontend_port}" \
    WORKSHOP_BACKEND_URL="${backend_url}" \
    WORKSHOP_SOURCE_PATH="${source_path}" \
    WORKSHOP_BASE_PATH="${source_path}"
  wait_for_port "${frontend_port}" "frontend" "${frontend_log_file}"
}

resolve_workshop() {
  local selection="$1"

  unset workshop_id display_name compose_file backend_project frontend_project backend_project_dir frontend_project_dir source_path frontend_url backend_url extra_url
  unset frontend_port backend_port legacy_frontend_port legacy_backend_port ports_file
  infra_services=()

  case "${selection}" in
    1|1_session_management|session-management)
      workshop_id="1_session_management"
      display_name="Session Management"
      compose_file="${JAVA_DIR}/1_session_management/docker-compose.yml"
      backend_project="1_session_management"
      frontend_project="1_session_management_frontend"
      source_path="${JAVA_DIR}/1_session_management"
      legacy_frontend_port="8080"
      legacy_backend_port="18080"
      infra_services=(redis redis-insight)
      ;;
    2|2_full_text_search|full-text-search)
      workshop_id="2_full_text_search"
      display_name="Full-Text Search"
      compose_file="${JAVA_DIR}/2_full_text_search/docker-compose.yml"
      backend_project="2_full_text_search"
      frontend_project="2_full_text_search_frontend"
      source_path="${JAVA_DIR}/2_full_text_search"
      legacy_frontend_port="8081"
      legacy_backend_port="18081"
      infra_services=(redis redis-insight)
      ;;
    3|3_distributed_locks|distributed-locks)
      workshop_id="3_distributed_locks"
      display_name="Distributed Locks"
      compose_file="${JAVA_DIR}/3_distributed_locks/docker-compose.yml"
      backend_project="3_distributed_locks"
      frontend_project="3_distributed_locks_frontend"
      source_path="${JAVA_DIR}/3_distributed_locks"
      legacy_frontend_port="8082"
      legacy_backend_port="18082"
      infra_services=(redis postgres redis-insight)
      ;;
    4|4_agent_memory|agent-memory)
      workshop_id="4_agent_memory"
      display_name="Agent Memory"
      compose_file="${JAVA_DIR}/4_agent_memory/docker-compose.yml"
      backend_project="4_agent_memory"
      frontend_project="4_agent_memory_frontend"
      source_path="${JAVA_DIR}/4_agent_memory"
      legacy_frontend_port="8083"
      legacy_backend_port="18083"
      infra_services=(redis redis-insight agent-memory-server)
      extra_url="Agent Memory Server: http://localhost:8000/"
      ;;
    5|1_spring_ai_fundamentals|spring-ai-fundamentals)
      workshop_id="1_spring_ai_fundamentals"
      display_name="Spring AI Fundamentals"
      compose_file="${JAVA_DIR}/spring_ai_multi_agent_course/1_spring_ai_fundamentals/docker-compose.yml"
      backend_project="1_spring_ai_fundamentals"
      frontend_project="1_spring_ai_fundamentals_frontend"
      source_path="${JAVA_DIR}/spring_ai_multi_agent_course/1_spring_ai_fundamentals"
      backend_project_dir="${source_path}"
      frontend_project_dir="${JAVA_DIR}/spring_ai_multi_agent_course/1_spring_ai_fundamentals_frontend"
      legacy_frontend_port="8084"
      legacy_backend_port="18084"
      infra_services=(redis redis-insight)
      ;;
    *)
      echo "Unknown workshop: ${selection}" >&2
      usage
      exit 1
      ;;
  esac

  state_dir="${STATE_ROOT}/${workshop_id}"
  backend_pid_file="${state_dir}/backend.pid"
  frontend_pid_file="${state_dir}/frontend.pid"
  backend_log_file="${state_dir}/backend.log"
  frontend_log_file="${state_dir}/frontend.log"
  ports_file="${state_dir}/ports.env"
}

check_local_ports() {
  local blocked=0
  cleanup_stale_pidfile "${backend_pid_file}"
  cleanup_stale_pidfile "${frontend_pid_file}"
  if port_in_use "${backend_port}" && [[ ! -f "${backend_pid_file}" ]]; then
    if managed_runner_enabled && [[ -f "${frontend_pid_file}" ]]; then
      :
    else
      echo "Backend port ${backend_port} is already in use." >&2
      blocked=1
    fi
  fi
  if port_in_use "${frontend_port}" && [[ ! -f "${frontend_pid_file}" ]]; then
    echo "Frontend port ${frontend_port} is already in use." >&2
    blocked=1
  fi
  if [[ "${blocked}" -ne 0 ]]; then
    exit 1
  fi
}

up() {
  ensure_java_home
  mkdir -p "${state_dir}"

  echo "Running local maintainer workflow for ${display_name}."
  ensure_ports_for_up
  check_local_ports

  echo "Starting ${display_name} infrastructure..."
  if [[ "${#infra_services[@]}" -gt 0 ]]; then
    docker compose -f "${compose_file}" up -d "${infra_services[@]}"
  else
    echo "No infrastructure services for ${display_name}."
  fi

  if managed_runner_enabled; then
    build_backend_for_runner
  else
    start_backend
  fi
  start_frontend
  if managed_runner_enabled; then
    wait_for_runner_ready
  fi

  echo
  echo "${display_name} is ready."
  print_urls
  echo "Backend log:  ${backend_log_file}"
  echo "Frontend log: ${frontend_log_file}"
  keep_alive_if_requested
}

down() {
  echo "Stopping local maintainer workflow for ${display_name}."
  load_ports_for_existing_run || true
  stop_service "${frontend_port}" "${frontend_pid_file}" "frontend"
  stop_service "${backend_port}" "${backend_pid_file}" "backend"
  rm -f "${ports_file}"
  echo "Stopping infrastructure..."
  if [[ "${#infra_services[@]}" -gt 0 ]]; then
    docker compose -f "${compose_file}" down
  else
    echo "No infrastructure services for ${display_name}."
  fi
}

restart() {
  local target="${1:-all}"

  ensure_java_home
  mkdir -p "${state_dir}"
  load_assigned_ports || assign_new_ports

  case "${target}" in
    backend)
      if managed_runner_enabled; then
        restart_managed_backend
      else
        stop_service "${backend_port}" "${backend_pid_file}" "backend"
        start_backend
      fi
      ;;
    frontend)
      stop_service "${frontend_port}" "${frontend_pid_file}" "frontend"
      start_frontend
      if managed_runner_enabled; then
        wait_for_runner_ready
      fi
      ;;
    all)
      stop_service "${frontend_port}" "${frontend_pid_file}" "frontend"
      stop_service "${backend_port}" "${backend_pid_file}" "backend"
      if managed_runner_enabled; then
        build_backend_for_runner
      else
        start_backend
      fi
      start_frontend
      if managed_runner_enabled; then
        wait_for_runner_ready
      fi
      ;;
    *)
      echo "Unknown restart target: ${target}" >&2
      echo "Use one of: frontend, backend, all" >&2
      exit 1
      ;;
  esac

  echo
  echo "${display_name} restart complete."
  print_urls
}

status() {
  cleanup_stale_pidfile "${frontend_pid_file}"
  cleanup_stale_pidfile "${backend_pid_file}"

  echo "${display_name} local maintainer workflow"
  if ! load_assigned_ports; then
    echo "ports: not assigned"
    print_urls
    echo
    if [[ "${#infra_services[@]}" -eq 0 ]]; then
      echo "Infrastructure: none"
    elif ! docker compose -f "${compose_file}" ps "${infra_services[@]}"; then
      echo "Infrastructure status unavailable. Check Docker permissions or start Docker Desktop."
    fi
    return
  fi

  if port_in_use "${frontend_port}"; then
    echo "frontend: running on port ${frontend_port}"
  else
    echo "frontend: stopped"
  fi
  if port_in_use "${backend_port}"; then
    echo "backend: running on port ${backend_port}"
  else
    echo "backend: stopped"
  fi
  print_urls
  echo
  if [[ "${#infra_services[@]}" -eq 0 ]]; then
    echo "Infrastructure: none"
  elif ! docker compose -f "${compose_file}" ps "${infra_services[@]}"; then
    echo "Infrastructure status unavailable. Check Docker permissions or start Docker Desktop."
  fi
}

if [[ $# -lt 1 ]]; then
  usage
  exit 1
fi

if [[ $# -eq 1 ]]; then
  command_name="up"
  resolve_workshop "$1"
else
  command_name="$1"
  shift
  resolve_workshop "$1"
fi

case "${command_name}" in
  up)
    up
    ;;
  down)
    down
    ;;
  status)
    status
    ;;
  restart)
    restart "${2:-all}"
    ;;
  *)
    echo "Unknown command: ${command_name}" >&2
    usage
    exit 1
    ;;
esac
