#!/usr/bin/env sh
# verify-valid-fixes.sh (T035, feature 004).
#
# Iterates triage.md rows whose disposition is "valid" and confirms each
# has a non-empty linked_pr (set when the closing PR merges). Fails
# otherwise. Run weekly and at release sign-off.
#
# Usage:
#   scripts/verify-valid-fixes.sh
#
# Returns 0 on success, 1 if any valid row lacks a linked_pr.
set -eu

repo_root="$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$repo_root"

triage="docs/ai-generated/tasks/gh-codeql-alerts/triage.md"

if [ ! -f "$triage" ]; then
    echo "FAIL: $triage not found" >&2
    exit 1
fi

# linked_pr is column 11 (awk $11) in contracts/C1; "—" or empty is unlinked.
missing=$(awk -F'|' '
    /^\| [0-9]+ \|/ {
        disposition = $8
        gsub(/^ +| +$/, "", disposition)
        if (disposition == "valid") {
            linked = $11
            gsub(/^ +| +$/, "", linked)
            if (linked == "" || linked == "—") {
                printf "  alert %s  rule %s  path %s\n", $3, $4, $6
            }
        }
    }
' "$triage")

if [ -n "$missing" ]; then
    echo "FAIL: valid alerts without a linked_pr:" >&2
    echo "$missing" >&2
    exit 1
fi
echo "verify-valid-fixes: PASS"
exit 0
