#!/usr/bin/env bash
# scripts/release-audit/release-audit.sh
# Main driver for the v8.1.7 → 8.2 migration audit pipeline.
# See ../specs/005-migrate-8.1.7-changes/contracts/audit-output-schemas.md
# for the CLI surface contract and exit codes.
#
# Usage:
#   bash scripts/release-audit/release-audit.sh \
#       --from-tag v8.1.6 --to-tag v8.1.7 \
#       --target-branch development \
#       --output-dir ./tmp/release-audit/v8.1.6..v8.1.7
#
# Exit codes:
#   0 = success; all expected output files written
#   2 = partial failure; some output files may be present
#   3 = invalid arguments (e.g. tag range unresolvable)
#   4 = gh CLI not authenticated or origin unreachable

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
# shellcheck source=lib/common.sh
source "${SCRIPT_DIR}/lib/common.sh"
# shellcheck source=lib/inventory.sh
source "${SCRIPT_DIR}/lib/inventory.sh"
# shellcheck source=lib/verdicts.sh
source "${SCRIPT_DIR}/lib/verdicts.sh"
# shellcheck source=lib/backlog.sh
source "${SCRIPT_DIR}/lib/backlog.sh"
# shellcheck source=lib/report.sh
source "${SCRIPT_DIR}/lib/report.sh"

# ---------- Defaults ----------

FROM_TAG="v8.1.6"
TO_TAG="v8.1.7"
TARGET_BRANCH="development"
OUTPUT_DIR=""
INCLUDE_DEPENDABOT="false"

# ---------- Argument parsing ----------

usage() {
  cat <<'USAGE'
Usage: release-audit.sh [options]

Options:
  --from-tag <TAG>           Lower bound of tag range (default: v8.1.6)
  --to-tag <TAG>             Upper bound of tag range (default: v8.1.7)
  --target-branch <BRANCH>   Branch to compare against (default: development)
  --output-dir <DIR>         Output directory (default: ./tmp/release-audit/<from>..<to>)
  --include-dependabot       Include dependabot PRs in the inventory (default: excluded)
  -h, --help                 Show this help and exit

Exit codes:
  0 success, 2 partial failure, 3 invalid args, 4 gh/origin unreachable
USAGE
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --from-tag)          FROM_TAG="$2"; shift 2 ;;
    --to-tag)            TO_TAG="$2"; shift 2 ;;
    --target-branch)     TARGET_BRANCH="$2"; shift 2 ;;
    --output-dir)        OUTPUT_DIR="$2"; shift 2 ;;
    --include-dependabot) INCLUDE_DEPENDABOT="true"; shift ;;
    -h|--help)           usage; exit 0 ;;
    *)                   die 3 "unknown argument: $1" ;;
  esac
done

# ---------- Validate ----------

[[ -n "${FROM_TAG}" ]] || die 3 "--from-tag is required"
[[ -n "${TO_TAG}" ]]   || die 3 "--to-tag is required"
[[ -n "${TARGET_BRANCH}" ]] || die 3 "--target-branch is required"

if [[ -z "${OUTPUT_DIR}" ]]; then
  OUTPUT_DIR="./tmp/release-audit/${FROM_TAG}..${TO_TAG}"
fi

log_info "audit config: ${FROM_TAG}..${TO_TAG} against ${TARGET_BRANCH} → ${OUTPUT_DIR}"
log_info "include-dependabot: ${INCLUDE_DEPENDABOT}"

# ---------- Phase 2 wiring smoke check ----------
# Per tasks.md T012: this section proves CLI parsing + require_origin +
# require_tag + ensure_output_dir + write_atomic all work end-to-end.
# Story phases (US1+) extend the pipeline below this smoke check.

require_origin
FROM_SHA="$(require_tag "${FROM_TAG}")"
TO_SHA="$(require_tag "${TO_TAG}")"

ensure_output_dir "${OUTPUT_DIR}"
write_atomic "${OUTPUT_DIR}/_audit_config.json" "$(jq -n \
  --arg from_tag "${FROM_TAG}" \
  --arg to_tag "${TO_TAG}" \
  --arg from_sha "${FROM_SHA}" \
  --arg to_sha "${TO_SHA}" \
  --arg target_branch "${TARGET_BRANCH}" \
  --arg include_dependabot "${INCLUDE_DEPENDABOT}" \
  --arg output_dir "${OUTPUT_DIR}" \
  '{fromTag:$from_tag,toTag:$to_tag,fromSha:$from_sha,toSha:$to_sha,targetBranch:$target_branch,includeDependabot:($include_dependabot=="true"),outputDir:$output_dir,runTimestamp:now|todate}')"

log_info "smoke check OK: config written to ${OUTPUT_DIR}/_audit_config.json"

# ---------- Pipeline hooks ----------

run_inventory_phase "${OUTPUT_DIR}" "${FROM_TAG}" "${TO_TAG}" "${INCLUDE_DEPENDABOT}"
run_verdicts_phase "${OUTPUT_DIR}" "${TARGET_BRANCH}"
run_backlog_phase "${OUTPUT_DIR}"
run_report_phase "${OUTPUT_DIR}"

# US4 — Port workflow is invoked per-item from the command line, not here.
# US5 — Re-runnability is exercised by re-running with --from-tag/--to-tag.

log_info "US1+US2+US3 complete; outputs in ${OUTPUT_DIR}/"
exit 0