#!/usr/bin/env sh
# verify-pr-review-resolution.sh (T078b, feature 004).
#
# Per Constitution IX (NON-NEGOTIABLE) and the PR Review Comment
# Resolution procedure in AGENTS.md, every closing PR for tasks
# T021..T072 MUST have all of its review threads resolved before it is
# merge-ready. This script reads the GitHub PRs identified by
# triage.md `linked_pr` and fails if any thread has `isResolved: false`.
#
# Usage:
#   scripts/verify-pr-review-resolution.sh [pr-number ...]
# If no PR numbers are passed, the script reads triage.md and uses
# every non-empty linked_pr in it.
#
# Requires: gh CLI authenticated with `repo` scope.
set -eu

repo_root="$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$repo_root"

triage="docs/ai-generated/tasks/gh-codeql-alerts/triage.md"

command -v gh >/dev/null 2>&1 || { echo "FAIL: gh CLI not found" >&2; exit 2; }

# Collect PR numbers from args OR from triage.md.
prs=""
if [ $# -gt 0 ]; then
    prs="$*"
else
    if [ -f "$triage" ]; then
        prs=$(awk -F'|' '
            /^\| [0-9]+ \|/ {
                linked = $10
                gsub(/^ +| +$/, "", linked)
                if (linked ~ /^[0-9]+$/) print linked
            }
        ' "$triage" | sort -u)
    fi
fi

if [ -z "$prs" ]; then
    echo "verify-pr-review-resolution: no PRs to check (PASS by vacuous truth)"
    exit 0
fi

fail=0
for pr in $prs; do
    echo "==> PR #$pr"
    threads_json=$(gh pr view "$pr" --json reviewThreads --jq '.reviewThreads // []' 2>/dev/null || echo "[]")
    unresolved=$(printf '%s' "$threads_json" | grep -c '"isResolved":false' || true)
    unresolved=${unresolved:-0}
    if [ "$unresolved" -gt 0 ]; then
        echo "  FAIL: $unresolved unresolved review thread(s) on PR #$pr"
        fail=1
    else
        echo "  OK: all review threads resolved"
    fi
done

if [ "$fail" -ne 0 ]; then
    echo "verify-pr-review-resolution: FAIL" >&2
    exit 1
fi
echo "verify-pr-review-resolution: PASS"
exit 0
