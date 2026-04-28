#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

LIVE_CHECKS="${LIVE_CHECKS:-off}"
BILLING_EXPORT_TABLE="${BILLING_EXPORT_TABLE:-}"
COST_START_DATE="${COST_START_DATE:-}"
COST_END_DATE="${COST_END_DATE:-}"
QUERY_FILE="${QUERY_FILE:-${REPO_ROOT}/infra/terraform/cloudrun/queries/cloud-run-cost-by-label.sql}"
OUTPUT_FORMAT="${OUTPUT_FORMAT:-prettyjson}"

usage() {
  cat <<EOF
Usage: ./scripts/ops/cloud-run-cost-report.sh

Dry run is the default. Set LIVE_CHECKS=on to execute the BigQuery query.

Environment:
  LIVE_CHECKS           off or on. Default: ${LIVE_CHECKS}
  BILLING_EXPORT_TABLE  Required. Fully qualified BigQuery export table as project.dataset.table
  COST_START_DATE       Required. Inclusive start date, YYYY-MM-DD
  COST_END_DATE         Required. Exclusive end date, YYYY-MM-DD
  QUERY_FILE            Default: ${QUERY_FILE}
  OUTPUT_FORMAT         bq output format. Default: ${OUTPUT_FORMAT}
EOF
}

die() {
  echo "ERROR: $*" >&2
  exit 1
}

require_command() {
  command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"
}

require_file() {
  local file_path="$1"
  [[ -f "${file_path}" ]] || die "Missing required file: ${file_path}"
}

validate_date() {
  local name="$1"
  local value="$2"
  [[ "${value}" =~ ^[0-9]{4}-[0-9]{2}-[0-9]{2}$ ]] || die "${name} must use YYYY-MM-DD"
}

validate_table() {
  [[ "$1" =~ ^[A-Za-z0-9_-]+[.][A-Za-z0-9_]+[.][A-Za-z0-9_]+$ ]] \
    || die "BILLING_EXPORT_TABLE must use project.dataset.table"
}

render_query() {
  sed "s|{{BILLING_EXPORT_TABLE}}|${BILLING_EXPORT_TABLE}|g" "${QUERY_FILE}"
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

require_file "${QUERY_FILE}"
require_command sed

[[ -n "${BILLING_EXPORT_TABLE}" ]] || die "BILLING_EXPORT_TABLE is required"
[[ -n "${COST_START_DATE}" ]] || die "COST_START_DATE is required"
[[ -n "${COST_END_DATE}" ]] || die "COST_END_DATE is required"

validate_table "${BILLING_EXPORT_TABLE}"
validate_date COST_START_DATE "${COST_START_DATE}"
validate_date COST_END_DATE "${COST_END_DATE}"

case "${LIVE_CHECKS}" in
  off)
    echo "[dry-run] Cloud Run cost report query was not executed." >&2
    echo "[dry-run] Set LIVE_CHECKS=on after confirming Billing export access." >&2
    echo "[dry-run] Command:" >&2
    printf 'bq query --use_legacy_sql=false --format=%q --parameter=start_date:DATE:%q --parameter=end_date:DATE:%q < rendered-query.sql\n' \
      "${OUTPUT_FORMAT}" "${COST_START_DATE}" "${COST_END_DATE}" >&2
    render_query
    ;;
  on)
    require_command bq
    render_query | bq query \
      --use_legacy_sql=false \
      --format="${OUTPUT_FORMAT}" \
      --parameter="start_date:DATE:${COST_START_DATE}" \
      --parameter="end_date:DATE:${COST_END_DATE}"
    ;;
  *)
    die "LIVE_CHECKS must be off or on"
    ;;
esac
