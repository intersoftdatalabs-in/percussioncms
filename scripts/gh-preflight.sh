#!/usr/bin/env bash
set -euo pipefail

EXPECTED_REPO="intersoftdatalabs-in/percussioncms"

usage() {
  cat <<'EOF'
Usage: scripts/gh-preflight.sh [--fix]

Checks that GitHub actions target the intersoft fork by default:
  1) gh default repo is intersoftdatalabs-in/percussioncms
  2) git origin remote points to intersoftdatalabs-in/percussioncms

Options:
  --fix   Automatically set gh default repo and git remote.pushDefault=origin
EOF
}

FIX_MODE="false"
if [[ "${1:-}" == "--help" || "${1:-}" == "-h" ]]; then
  usage
  exit 0
elif [[ "${1:-}" == "--fix" ]]; then
  FIX_MODE="true"
elif [[ -n "${1:-}" ]]; then
  echo "ERROR: unknown argument '${1}'" >&2
  usage
  exit 2
fi

if ! command -v gh >/dev/null 2>&1; then
  echo "ERROR: gh CLI is required but not found in PATH." >&2
  exit 1
fi

if ! command -v git >/dev/null 2>&1; then
  echo "ERROR: git is required but not found in PATH." >&2
  exit 1
fi

current_repo=""
if ! current_repo="$(gh repo set-default --view 2>/dev/null)"; then
  current_repo=""
fi

origin_url="$(git remote get-url origin 2>/dev/null || true)"

repo_ok="false"
origin_ok="false"

if [[ "${current_repo}" == "${EXPECTED_REPO}" ]]; then
  repo_ok="true"
fi

if [[ "${origin_url}" == *"github.com/intersoftdatalabs-in/percussioncms"* ]]; then
  origin_ok="true"
fi

if [[ "${FIX_MODE}" == "true" ]]; then
  if [[ "${repo_ok}" != "true" ]]; then
    gh repo set-default "${EXPECTED_REPO}" >/dev/null
    current_repo="$(gh repo set-default --view)"
    [[ "${current_repo}" == "${EXPECTED_REPO}" ]] && repo_ok="true"
  fi

  git config remote.pushDefault origin
fi

if [[ "${repo_ok}" != "true" ]]; then
  echo "ERROR: gh default repo is '${current_repo:-<unset>}' (expected '${EXPECTED_REPO}')." >&2
  echo "Run: gh repo set-default ${EXPECTED_REPO}" >&2
  exit 1
fi

if [[ "${origin_ok}" != "true" ]]; then
  echo "ERROR: git origin points to '${origin_url:-<unset>}' (expected intersoft fork)." >&2
  echo "Run: git remote set-url origin https://github.com/intersoftdatalabs-in/percussioncms.git" >&2
  exit 1
fi

push_default="$(git config --get remote.pushDefault || true)"
if [[ "${push_default}" != "origin" ]]; then
  echo "WARNING: remote.pushDefault is '${push_default:-<unset>}' (recommended: origin)." >&2
  echo "Run: git config remote.pushDefault origin" >&2
fi

echo "OK: GitHub target preflight passed (${EXPECTED_REPO})."
