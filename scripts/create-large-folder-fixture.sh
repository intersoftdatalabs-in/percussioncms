#!/usr/bin/env bash
#
# scripts/create-large-folder-fixture.sh
#
# Feature: 992-react-content-explorer — SC-005 perf fixture (tasks.md T012b).
# Creates a single CMS folder with >=500 children for the UAT perf scenario
# (quickstart.md Scenario B).
#
# Cross-platform: Windows users should run the .cmd counterpart
# `scripts/create-large-folder-fixture.cmd` (add when needed; this .sh is
# portable to Linux/macOS). The script is opt-in UAT scaffolding, not a
# CI-required workflow.
#
# Usage:
#   CMS_BASE_URL=https://cms.local:8443 \
#   CMS_USER=admin1 CMS_PASS=<redacted> \
#   FIXTURE_PATH=/Sites/PerfFixture FIXTURE_COUNT=500 \
#   ./scripts/create-large-folder-fixture.sh
#
# Security:
#   Uses a temporary netrc file (mode 0600) for curl credentials so the
#   password is NOT placed on the process command line (Erlang hard gate).
#   The netrc file is removed on EXIT via trap.
#
# Failure tracking:
#   Each child creation is checked against the returned HTTP status. A run
#   with any non-2xx / non-409 status exits non-zero (Erlang hard gate:
#   no false-green on ignored child failures).
#
# Records evidence in:
#   specs/992-react-content-explorer/checklists/sc005-perf-evidence.md
#   (append a row per run with: fixture size, p50/p95/max ms, pass/fail)

set -euo pipefail

CMS_BASE_URL="${CMS_BASE_URL:-http://localhost:8080}"
CMS_USER="${CMS_USER:?CMS_USER is required}"
CMS_PASS="${CMS_PASS:?CMS_PASS is required}"
FIXTURE_PATH="${FIXTURE_PATH:-/Sites/PerfFixture}"
FIXTURE_COUNT="${FIXTURE_COUNT:-500}"

# Parse host out of CMS_BASE_URL for the netrc "machine" field.
CMS_HOST="${CMS_BASE_URL#*://}"
CMS_HOST="${CMS_HOST%%/*}"
CMS_HOST="${CMS_HOST%%:*}"

# Portable date (Linux + macOS); Windows users should use the .cmd counterpart.
ts="$(date -u +'%Y-%m-%dT%H:%M:%SZ')"

echo "[$ts] Creating fixture folder ${FIXTURE_PATH} with ${FIXTURE_COUNT} children at ${CMS_BASE_URL}"

# Temporary netrc keeps the password off the process command line.
netrc_file="$(mktemp)"
printf 'machine %s login %s password %s\n' "${CMS_HOST}" "${CMS_USER}" "${CMS_PASS}" > "${netrc_file}"
chmod 600 "${netrc_file}"
trap 'rm -f "${netrc_file}"' EXIT

# 1. Create the parent folder. 2xx or 409 (already exists) are both acceptable.
create_code="$(curl -sS -k --netrc-file "${netrc_file}" \
  -X GET "${CMS_BASE_URL}/Rhythmyx/services/pathmanagement/path/addNewFolder/${FIXTURE_PATH}?name=PerfFixtureRoot" \
  -o /dev/null -w '%{http_code}')"
printf 'createRoot=%s\n' "${create_code}"
case "${create_code}" in
  2*|409) ;;
  *) echo "[$ts] FAILED: parent folder creation returned ${create_code}" >&2; exit 1 ;;
esac

# 2. Create N children using pathmanagement addFolder. Track failures; exit non-zero on any.
failures=0
for i in $(seq 1 "${FIXTURE_COUNT}"); do
  child="child_$(printf '%04d' "$i")"
  code="$(curl -sS -k --netrc-file "${netrc_file}" \
    -X GET "${CMS_BASE_URL}/Rhythmyx/services/pathmanagement/path/addNewFolder/${FIXTURE_PATH}/PerfFixtureRoot?name=${child}" \
    -o /dev/null -w '%{http_code}')"
  printf '%s=%s\n' "${child}" "${code}"
  case "${code}" in
    2*) ;;   # 2xx — created
    409) ;;  # 409 — already exists (idempotent re-run)
    *)   failures=$((failures + 1)) ;;
  esac
done

if [ "${failures}" -gt 0 ]; then
  echo "[$ts] FAILED: ${failures}/${FIXTURE_COUNT} child creations did not return 2xx or 409. See above." >&2
  exit 1
fi

echo "[$ts] Done. Verify with:"
echo "  curl -sS -k --netrc-file <creds> \"${CMS_BASE_URL}/Rhythmyx/services/pathmanagement/path/folder/${FIXTURE_PATH}/PerfFixtureRoot\" | jq 'length'"
echo "Then run quickstart.md Scenario B and append results to"
echo "  specs/992-react-content-explorer/checklists/sc005-perf-evidence.md"