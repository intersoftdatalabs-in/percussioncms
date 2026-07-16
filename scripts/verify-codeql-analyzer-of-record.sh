#!/usr/bin/env bash
# Verify CodeQL analyzer-of-record policy for intersoftdatalabs-in/percussioncms.
# Fails non-zero if default setup is re-enabled or required files are missing.
#
# Usage: scripts/verify-codeql-analyzer-of-record.sh
# See: docs/ai-generated/tasks/gh-codeql-alerts/codeql-pr-playbook.md

set -euo pipefail

REPO="${CODEQL_REPO:-intersoftdatalabs-in/percussioncms}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
ERR=0

echo "== CodeQL analyzer-of-record check (repo=$REPO) =="

if [[ ! -f "$ROOT/.github/workflows/codeql.yml" ]]; then
  echo "FAIL: missing .github/workflows/codeql.yml"
  ERR=1
else
  if grep -qE '^[[:space:]]*pull_request:' "$ROOT/.github/workflows/codeql.yml"; then
    echo "OK: advanced workflow has pull_request trigger"
  else
    echo "FAIL: advanced workflow missing pull_request trigger (PR analyzer will not run with config)"
    ERR=1
  fi
  if grep -q 'config-file: ./.github/codeql/codeql-config.yml' "$ROOT/.github/workflows/codeql.yml"; then
    echo "OK: advanced workflow uses codeql-config.yml"
  else
    echo "FAIL: advanced workflow not wired to codeql-config.yml"
    ERR=1
  fi
fi

for f in \
  "$ROOT/.github/codeql/codeql-config.yml" \
  "$ROOT/.github/codeql/models/codeql-pack.yml" \
  "$ROOT/docs/ai-generated/tasks/gh-codeql-alerts/codeql-pr-playbook.md"
do
  if [[ -f "$f" ]]; then
    echo "OK: present ${f#"$ROOT"/}"
  else
    echo "FAIL: missing ${f#"$ROOT"/}"
    ERR=1
  fi
done

if grep -q '^\s*packs:' "$ROOT/.github/codeql/codeql-config.yml" 2>/dev/null; then
  echo "OK: codeql-config.yml declares packs"
else
  echo "FAIL: codeql-config.yml missing packs: (model pack not loaded)"
  ERR=1
fi

if command -v gh >/dev/null 2>&1; then
  STATE="$(gh api "repos/${REPO}/code-scanning/default-setup" --jq .state 2>/dev/null || echo "error")"
  if [[ "$STATE" == "not-configured" ]]; then
    echo "OK: default CodeQL setup is not-configured"
  else
    echo "FAIL: default CodeQL setup state is '${STATE}' (expected not-configured)"
    echo "      Fix: gh api --method PATCH repos/${REPO}/code-scanning/default-setup -f state=not-configured"
    ERR=1
  fi
else
  echo "WARN: gh not installed; skipped default-setup API check"
fi

if [[ "$ERR" -ne 0 ]]; then
  echo "== RESULT: FAIL =="
  exit 1
fi
echo "== RESULT: PASS =="
exit 0
