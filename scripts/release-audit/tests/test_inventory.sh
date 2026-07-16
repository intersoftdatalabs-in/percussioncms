#!/usr/bin/env bash
# scripts/release-audit/tests/test_inventory.sh
# Per tasks.md T013: assertion test for User Story 1.
# MUST fail before inventory.sh is implemented; MUST pass after.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "${SCRIPT_DIR}/../../.." && pwd)"
TEST_DIR="${ROOT_DIR}/tmp/release-audit/test_inventory"

# Reset test dir
rm -rf "${TEST_DIR}"
mkdir -p "${TEST_DIR}"

# Run the audit driver with current implementation
bash "${ROOT_DIR}/scripts/release-audit/release-audit.sh" \
  --from-tag v8.1.6 --to-tag v8.1.7 \
  --output-dir "${TEST_DIR}" 2>&1 || true

INVENTORY="${TEST_DIR}/inventory.json"
EXCLUDED="${TEST_DIR}/dependabot-excluded.json"

# Assertions
fail() { printf 'FAIL: %s\n' "$*"; exit 1; }

[[ -f "${INVENTORY}" ]] || fail "inventory.json does not exist (T014/T015 not implemented yet)"
[[ -f "${EXCLUDED}" ]]  || fail "dependabot-excluded.json does not exist"

# SC-001: 100% inventoried (lower-bound check)
COUNT="$(jq 'length' "${INVENTORY}")"
[[ "${COUNT}" -gt 100 ]] || fail "expected > 100 inventory entries, got ${COUNT}"

# SC-002: zero dependabot in inventory
DEP_IN_INV="$(jq '[.[] | select(.author | test("dependabot"; "i"))] | length' "${INVENTORY}")"
[[ "${DEP_IN_INV}" -eq 0 ]] || fail "expected 0 dependabot PRs in inventory, got ${DEP_IN_INV}"

# Excluded file: all entries are dependabot (dynamic checks)
EX_COUNT="$(jq 'length' "${EXCLUDED}")"
[[ "${EX_COUNT}" -gt 150 ]] || fail "expected > 150 dependabot entries in excluded file, got ${EX_COUNT}"

DEP_IN_EX="$(jq '[.[] | select(.author | test("dependabot"; "i"))] | length' "${EXCLUDED}")"
[[ "${DEP_IN_EX}" -eq "${EX_COUNT}" ]] || fail "expected all excluded entries to be dependabot, got ${DEP_IN_EX}/${EX_COUNT}"

echo "PASS: test_inventory.sh — inventory ${COUNT}, excluded ${EX_COUNT}, 0 dependabot leakage"