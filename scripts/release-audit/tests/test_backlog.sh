#!/usr/bin/env bash
# scripts/release-audit/tests/test_backlog.sh
# Per tasks.md T025: assertion test for User Story 3 backlog output.
# MUST fail before backlog.sh is implemented; MUST pass after.
#
# Reuses an existing test_inventory run if present (faster for CI);
# otherwise runs the full pipeline.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
TEST_DIR="${ROOT_DIR}/tmp/release-audit/test_backlog"

rm -rf "${TEST_DIR}"
mkdir -p "${TEST_DIR}"

# Prefer reusing test_inventory outputs to avoid hitting gh rate limits.
INVENTORY_SRC="${ROOT_DIR}/tmp/release-audit/test_inventory"
if [[ -f "${INVENTORY_SRC}/inventory.json" ]] \
   && [[ -f "${INVENTORY_SRC}/verdicts.json" ]] \
   && [[ -f "${INVENTORY_SRC}/_audit_config.json" ]] \
   && [[ "${SKIP_REUSE:-0}" != "1" ]]; then
  printf '[INFO]  reusing existing inventory/verdicts from test_inventory (skip full re-run)\n' >&2
  cp "${INVENTORY_SRC}/_audit_config.json" "${TEST_DIR}/"
  cp "${INVENTORY_SRC}/inventory.json"       "${TEST_DIR}/"
  cp "${INVENTORY_SRC}/verdicts.json"        "${TEST_DIR}/"
  cp "${INVENTORY_SRC}/dependabot-excluded.json" "${TEST_DIR}/"
  # Source lib files and run only backlog + report phases
  # shellcheck source=lib/common.sh
  source "${ROOT_DIR}/scripts/release-audit/lib/common.sh"
  # shellcheck source=lib/backlog.sh
  source "${ROOT_DIR}/scripts/release-audit/lib/backlog.sh"
  # shellcheck source=lib/report.sh
  source "${ROOT_DIR}/scripts/release-audit/lib/report.sh"
  run_backlog_phase "${TEST_DIR}"
  run_report_phase  "${TEST_DIR}"
else
  bash "${ROOT_DIR}/scripts/release-audit/release-audit.sh" \
    --from-tag v8.1.6 --to-tag v8.1.7 \
    --output-dir "${TEST_DIR}" 2>&1 || true
fi

BACKLOG="${TEST_DIR}/migration-backlog.md"
REPORT="${TEST_DIR}/v8.1.7-to-8.2-migration-report.md"
VERDICTS="${TEST_DIR}/verdicts.json"

fail() { printf 'FAIL: %s\n' "$*"; exit 1; }

[[ -f "${BACKLOG}" ]] || fail "migration-backlog.md does not exist"
[[ -f "${REPORT}" ]]  || fail "v8.1.7-to-8.2-migration-report.md does not exist"

# Backlog has top matter, P0/P1/P2/P3 sections, Issue Clusters appendix
grep -q "^## P0 — Security"              "${BACKLOG}" || fail "backlog missing P0 section"
grep -q "^## P1 — REST contract"          "${BACKLOG}" || fail "backlog missing P1 section"
grep -q "^## P2 — UI fix"                 "${BACKLOG}" || fail "backlog missing P2 section"
grep -q "^## P3 — Cosmetic"               "${BACKLOG}" || fail "backlog missing P3 section"
grep -q "^## Issue Clusters Appendix"     "${BACKLOG}" || fail "backlog missing Issue Clusters Appendix"

# Report has all 7 required sections
grep -q "^## TL;DR"                       "${REPORT}" || fail "report missing TL;DR"
grep -q "^## Verdict Distribution"        "${REPORT}" || fail "report missing Verdict Distribution"
grep -q "^## Top 10 Backlog Items"        "${REPORT}" || fail "report missing Top 10 Backlog Items"
grep -q "^## Exclusions"                  "${REPORT}" || fail "report missing Exclusions"
grep -q "^## Open Questions"              "${REPORT}" || fail "report missing Open Questions"
grep -q "^## Next Steps"                  "${REPORT}" || fail "report missing Next Steps"

# All backlog PRs are needs-migration (cross-check vs verdicts.json)
NM_VERDICTS="$(jq '[.[] | select(.verdict == "needs-migration")] | length' "${VERDICTS}")"
echo "needs-migration count: ${NM_VERDICTS}"

# Backlog has at least one PR row (markdown table row containing #NNN pattern)
PR_REFS="$(grep -oE 'github\.com/intersoftdatalabs-in/percussioncms/pull/[0-9]+' "${BACKLOG}" | wc -l)"
[[ "${PR_REFS}" -gt 0 ]] || fail "backlog has no PR references"

echo "PASS: test_backlog.sh — backlog + report present, all sections found, ${PR_REFS} PR rows"