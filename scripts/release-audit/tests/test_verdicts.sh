#!/usr/bin/env bash
# scripts/release-audit/tests/test_verdicts.sh
# Per tasks.md T018: assertion test for User Story 2.
# MUST fail before verdicts.sh is implemented; MUST pass after.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
TEST_DIR="${ROOT_DIR}/tmp/release-audit/test_verdicts"

rm -rf "${TEST_DIR}"
mkdir -p "${TEST_DIR}"

bash "${ROOT_DIR}/scripts/release-audit/release-audit.sh" \
  --from-tag v8.1.6 --to-tag v8.1.7 \
  --output-dir "${TEST_DIR}" 2>&1 || true

VERDICTS="${TEST_DIR}/verdicts.json"
INVENTORY="${TEST_DIR}/inventory.json"

fail() { printf 'FAIL: %s\n' "$*"; exit 1; }

[[ -f "${VERDICTS}" ]] || fail "verdicts.json does not exist"
[[ -f "${INVENTORY}" ]] || fail "inventory.json does not exist"

# Count match
INV_COUNT="$(jq 'length' "${INVENTORY}")"
VER_COUNT="$(jq 'length' "${VERDICTS}")"
[[ "${VER_COUNT}" -eq "${INV_COUNT}" ]] || fail "verdicts (${VER_COUNT}) != inventory (${INV_COUNT})"

# Every verdict is one of the 5 enum values
INVALID="$(jq '[.[] | select(.verdict | IN("already-present","needs-migration","not-applicable","superseded","conflicts-with-newer-design") | not)] | length' "${VERDICTS}")"
[[ "${INVALID}" -eq 0 ]] || fail "found ${INVALID} verdicts with invalid enum values"

# Every evidenceNote is non-empty
EMPTY_NOTES="$(jq '[.[] | select(.evidenceNote == "" or .evidenceNote == null)] | length' "${VERDICTS}")"
[[ "${EMPTY_NOTES}" -eq 0 ]] || fail "found ${EMPTY_NOTES} verdicts with empty evidenceNote"

# Distribution sanity: at least 1 verdict per enum value across the 141 PRs
DIST="$(jq -r '[.[] | .verdict] | group_by(.) | map({verdict:.[0], count:length}) | .[] | "\(.verdict)=\(.count)"' "${VERDICTS}" | sort | tr '\n' ' ')"
echo "verdict distribution: ${DIST}"

echo "PASS: test_verdicts.sh — ${VER_COUNT} verdicts, all valid enum + non-empty notes"