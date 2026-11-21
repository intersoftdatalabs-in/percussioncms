#!/usr/bin/env bash
# Seed gitignored AGENTS.local.md from the primary checkout into a worktree.
# Usage: scripts/night-seed-worktree.sh [--check] [--overwrite] <worktree>
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
exec python3 "${REPO_ROOT}/scripts/night-seed-worktree.py" "$@"
