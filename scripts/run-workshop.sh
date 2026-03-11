#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
JAVA_DIR="${REPO_ROOT}/java-springboot"
GRADLEW="${JAVA_DIR}/gradlew"
STATE_ROOT="${TMPDIR:-/tmp}/redis-workshops"

usage() {
  cat <<'EOF'
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

start_gradle_app() {
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

  (
    cd "${REPO_ROOT}"
    nohup env "$@" "${GRADLEW}" -p "${JAVA_DIR}" --no-daemon ":${project}:bootRun" </dev/null >> "${log_file}" 2>&1 &
    echo $! > "${pid_file}"
  )

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
  echo "Frontend: ${frontend_url}"
  echo "Backend:  ${backend_url}"
  echo "Redis Insight: http://localhost:5540/"
  if [[ -n "${extra_url:-}" ]]; then
    echo "${extra_url}"
  fi
}

start_backend() {
  echo "Starting ${display_name} backend..."
  start_gradle_app \
    "backend" \
    "${backend_project}" \
    "${backend_pid_file}" \
    "${backend_log_file}" \
    SERVER_PORT="${backend_port}" \
    WORKSHOP_SOURCE_PATH="${source_path}" \
    WORKSHOP_BASE_PATH="${source_path}"
  wait_for_port "${backend_port}" "backend" "${backend_log_file}"
}

start_frontend() {
  echo "Starting ${display_name} frontend..."
  start_gradle_app \
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

  unset workshop_id display_name compose_file backend_project frontend_project source_path frontend_url backend_url extra_url
  unset frontend_port backend_port
  infra_services=()

  case "${selection}" in
    1|1_session_management|session-management)
      workshop_id="1_session_management"
      display_name="Session Management"
      compose_file="${JAVA_DIR}/1_session_management/docker-compose.yml"
      backend_project="1_session_management"
      frontend_project="1_session_management_frontend"
      source_path="${JAVA_DIR}/1_session_management"
      frontend_port="8080"
      backend_port="18080"
      frontend_url="http://localhost:${frontend_port}/"
      backend_url="http://localhost:${backend_port}"
      infra_services=(redis redis-insight)
      ;;
    2|2_full_text_search|full-text-search)
      workshop_id="2_full_text_search"
      display_name="Full-Text Search"
      compose_file="${JAVA_DIR}/2_full_text_search/docker-compose.yml"
      backend_project="2_full_text_search"
      frontend_project="2_full_text_search_frontend"
      source_path="${JAVA_DIR}/2_full_text_search"
      frontend_port="8081"
      backend_port="18081"
      frontend_url="http://localhost:${frontend_port}/"
      backend_url="http://localhost:${backend_port}"
      infra_services=(redis redis-insight)
      ;;
    3|3_distributed_locks|distributed-locks)
      workshop_id="3_distributed_locks"
      display_name="Distributed Locks"
      compose_file="${JAVA_DIR}/3_distributed_locks/docker-compose.yml"
      backend_project="3_distributed_locks"
      frontend_project="3_distributed_locks_frontend"
      source_path="${JAVA_DIR}/3_distributed_locks"
      frontend_port="8082"
      backend_port="18082"
      frontend_url="http://localhost:${frontend_port}/"
      backend_url="http://localhost:${backend_port}"
      infra_services=(redis postgres redis-insight)
      ;;
    4|4_agent_memory|agent-memory)
      workshop_id="4_agent_memory"
      display_name="Agent Memory"
      compose_file="${JAVA_DIR}/4_agent_memory/docker-compose.yml"
      backend_project="4_agent_memory"
      frontend_project="4_agent_memory_frontend"
      source_path="${JAVA_DIR}/4_agent_memory"
      frontend_port="8083"
      backend_port="18083"
      frontend_url="http://localhost:${frontend_port}/"
      backend_url="http://localhost:${backend_port}"
      infra_services=(redis redis-insight agent-memory-server)
      extra_url="Agent Memory Server: http://localhost:8000/"
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
}

check_local_ports() {
  local blocked=0
  cleanup_stale_pidfile "${backend_pid_file}"
  cleanup_stale_pidfile "${frontend_pid_file}"
  if port_in_use "${backend_port}" && [[ ! -f "${backend_pid_file}" ]]; then
    echo "Backend port ${backend_port} is already in use." >&2
    blocked=1
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

  check_local_ports

  echo "Starting ${display_name} infrastructure..."
  docker compose -f "${compose_file}" up -d "${infra_services[@]}"

  start_backend
  start_frontend

  echo
  echo "${display_name} is ready."
  print_urls
  echo "Backend log:  ${backend_log_file}"
  echo "Frontend log: ${frontend_log_file}"
}

down() {
  stop_service "${frontend_port}" "${frontend_pid_file}" "frontend"
  stop_service "${backend_port}" "${backend_pid_file}" "backend"
  echo "Stopping infrastructure..."
  docker compose -f "${compose_file}" down
}

restart() {
  local target="${1:-all}"

  ensure_java_home
  mkdir -p "${state_dir}"

  case "${target}" in
    backend)
      stop_service "${backend_port}" "${backend_pid_file}" "backend"
      start_backend
      ;;
    frontend)
      stop_service "${frontend_port}" "${frontend_pid_file}" "frontend"
      start_frontend
      ;;
    all)
      stop_service "${frontend_port}" "${frontend_pid_file}" "frontend"
      stop_service "${backend_port}" "${backend_pid_file}" "backend"
      start_backend
      start_frontend
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

  echo "${display_name}"
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
  if ! docker compose -f "${compose_file}" ps "${infra_services[@]}"; then
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
