#!/usr/bin/env bash
# Timer entry point for the opencode night-issue-prs workflow: one bounded
# cycle per invocation (default --max-issues 5). Safe to trigger every 2h via
# a systemd timer — flock on tmp/night-issue-prs.lock makes overlapping
# triggers (timer vs timer, timer vs manual) exit immediately instead of
# stacking.
# Usage: scripts/night-timer-run.sh [--max-issues N] [launcher args...]
# Env: MAX_ISSUES overrides the default; MODEL overrides the pinned model
# (default opencode-go/muse-spark-1.3-contributor#medium);
# NIGHT_REPORT_PATH is stamped per run
# so consecutive cycles don't overwrite scratch/night-report.md.
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
LOCK_FILE="${LOCK_FILE:-${REPO_ROOT}/tmp/night-issue-prs.lock}"
MAX_ISSUES="${MAX_ISSUES:-5}"
# Pinned model for unattended runs: the default model slot is too slow
# (single inference step per ~5 min; whole cycles take 5h+). Override with
# MODEL="<provider>/<model>#<variant>" (format of `opencode run --model`).
MODEL="${MODEL:-opencode-go/muse-spark-1.3-contributor#medium}"

mkdir -p "${REPO_ROOT}/tmp"

# Report dir: the dedicated worktree's scratch/ (same place the manual-cycle
# reports landed), so timer reports never pollute the primary checkout.
WORKTREE_DEFAULT="${HOME}/.opencode/worktrees/intersoft-workspace-percussioncms/night-issue-prs"

# NOTE: no "--" separator — older util-linux flock takes <file> <command>
# directly. The trailing "$@" is expanded once by this shell into separate
# words for the inner sh, whose "$@" is then the launcher's argument list.
exec flock -n "${LOCK_FILE}" sh -c '
  STAMP=$(date -u +%Y%m%dT%H%M%SZ)
  WORKTREE="${NIGHT_WORKTREE_PATH:-'"${WORKTREE_DEFAULT}"'}"
  export NIGHT_REPORT_PATH="${WORKTREE}/scratch/night-report-${STAMP}.md"
  cd "'"${REPO_ROOT}"'"
  exec python3 scripts/opencode-night-issue-prs.py --max-issues "'"${MAX_ISSUES}"'" --model "'"${MODEL}"'" "$@"
' sh "$@"
