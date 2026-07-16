#!/usr/bin/env bash
# scripts/release-audit/lib/common.sh
# Shared helpers for the release-audit pipeline.
# Loaded via `source` by release-audit.sh and the per-story lib files.
#
# Constitution Principle II (Evidence Over Invention): every helper below
# is grounded in the bash 4+, jq 1.6+, gh 2.x, git 2.30+ tools already
# present in this repository. No invented APIs.

set -euo pipefail

# ---------- Output helpers ----------

log_info()  { printf '[INFO]  %s\n' "$*" >&2; }
log_warn()  { printf '[WARN]  %s\n' "$*" >&2; }
log_error() { printf '[ERROR] %s\n' "$*" >&2; }

# die <exit-code> <message...>
die() {
  local code="$1"; shift
  log_error "$@"
  exit "${code}"
}

# ---------- Tooling checks ----------

# require_origin: ensure `origin` resolves and `gh auth status` succeeds.
# Exit codes per contracts/audit-output-schemas.md §"CLI surface":
#   4 = gh not authenticated or origin unreachable
require_origin() {
  if ! git remote get-url origin >/dev/null 2>&1; then
    die 4 "origin remote is not configured"
  fi
  if ! timeout 10 git ls-remote origin >/dev/null 2>&1; then
    die 4 "origin remote is unreachable (timeout or network error)"
  fi
  if ! (gh auth status || true) 2>&1 | grep -q "Active account: true"; then
    die 4 "gh CLI active account is not authenticated (run: gh auth login --hostname github.com)"
  fi
}

# require_tag <tag>: ensure <tag> resolves on origin; echo the commit SHA.
# Exit code 3 = invalid arguments / tag unknown on origin.
require_tag() {
  local tag="$1"
  if [[ -z "${tag}" ]]; then
    die 3 "require_tag: tag argument is empty"
  fi
  local sha
  sha="$(timeout 10 git ls-remote origin "refs/tags/${tag}" 2>/dev/null | awk '{print $1}' | head -n1)"
  if [[ -z "${sha}" ]]; then
    die 3 "tag '${tag}' does not resolve on origin"
  fi
  printf '%s\n' "${sha}"
}

# ---------- Atomic writer ----------

# write_atomic <path> <content>
# Writes content to <path> via a temp file then mv, so partial writes
# never replace an existing good file.
write_atomic() {
  local path="$1"; shift
  local content="$1"
  local dir tmp
  dir="$(dirname "${path}")"
  ensure_output_dir "${dir}"
  tmp="$(mktemp "${dir}/.audit.XXXXXX")"
  printf '%s\n' "${content}" > "${tmp}"
  mv "${tmp}" "${path}"
}

# ensure_output_dir <dir>
ensure_output_dir() {
  local dir="$1"
  if [[ -d "${dir}" ]]; then return 0; fi
  if ! mkdir -p "${dir}" 2>/dev/null; then
    die 2 "cannot create output directory: ${dir}"
  fi
}

# ---------- JSON helpers ----------

# jq_safe <jq-filter> <file>
# Run jq but fail with exit 3 on parse error.
jq_safe() {
  local filter="$1"; shift
  local file="$1"; shift || true
  if [[ ! -f "${file}" ]]; then
    die 3 "jq_safe: file not found: ${file}"
  fi
  jq "${filter}" "${file}"
}