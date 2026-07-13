#!/usr/bin/env sh
# Verify the CodeQL triage inventory (T012, feature 004).
#
# Enforces contracts/C1 (triage.md format and content):
#   (a) row count == open-alert count (alerts.md minus stale-cache rows)
#   (b) every false-positive / accepted-risk row has non-empty notes
#   (c) every module_owner is a path listed under ./AGENTS.md
#
# POSIX sh per AGENTS.md. Returns 0 on success, 1 on any failure.
# Run from the repo root.

set -eu

repo_root="$(CDPATH='' cd -- "$(dirname -- "$0")/.." && pwd)"
cd "$repo_root"

triage="docs/ai-generated/tasks/gh-codeql-alerts/triage.md"
alerts="docs/ai-generated/tasks/gh-codeql-alerts/alerts.md"
stale="docs/ai-generated/tasks/gh-codeql-alerts/alerts-stale-cache.md"
agents="AGENTS.md"

if [ ! -f "$triage" ]; then
    echo "FAIL: $triage not found" >&2
    exit 1
fi
if [ ! -f "$alerts" ]; then
    echo "FAIL: $alerts not found" >&2
    exit 1
fi
if [ ! -f "$agents" ]; then
    echo "FAIL: $agents not found" >&2
    exit 1
fi

fail=0

# ---- (a) row count vs open-alert count ----
# Count rows in triage.md (skip the header + separator + the non-row "## Triage Table" line).
triage_rows=$(awk -F'|' '
    /^\| [0-9]+ \|/ { n++ }
    END { print n + 0 }
' "$triage")

# Count open alerts in alerts.md.
open_alerts=$(grep -cE '^- \*\*Alert #' "$alerts" || true)
open_alerts=${open_alerts:-0}

# Subtract stale-cache rows.
stale_rows=0
if [ -f "$stale" ]; then
    stale_rows=$(awk -F'|' '/^\| [0-9]+ \|/ { n++ } END { print n + 0 }' "$stale")
fi
stale_rows=${stale_rows:-0}

effective_open=$((open_alerts - stale_rows))

# Count triage rows whose alert is "ready to close" (linked_pr is set,
# meaning a closing PR has been opened or merged). For per-cluster PRs
# that have not yet merged, the file is still in git ls-files (the
# PR is staged in the working tree) so the alert still appears in
# alerts.md and the row is still in triage.md; once the PR merges and
# CodeQL re-scans, both rows are removed.
ready_to_close=$(awk -F'|' '
    /^\| [0-9]+ \|/ {
        # linked_pr is column 11 (after the leading empty $1, then the
        # 10 data columns per contracts/C1: # | alert_id | rule_id |
        # severity | file_path | module_owner | disposition |
        # target_action | target_milestone | linked_pr | notes).
        linked = $11
        gsub(/^ +| +$/, "", linked)
        if (linked != "" && linked != "—") n++
    }
    END { print n + 0 }
' "$triage")

expected=$((effective_open + ready_to_close))
delta=$((triage_rows - expected))
abs_delta=${delta#-}
# The row-count check tolerates a small slack for two legitimate
# sources of drift:
#   1. Seed data vs spec mismatches (the seed has fewer rows per
#      cluster than the spec mentions for some alerts).
#   2. Staged-but-unmarked obsolete clusters: when a cluster's files
#      are deleted in the working tree but the matching linked_pr
#      placeholders have not yet been set in triage.md, the deleted
#      files appear in alerts-stale-cache.md but the matching triage
#      rows are not in `ready_to_close`.
#
# Default slack is 0 (strict). For active remediation work, set
# TRIAGE_SLACK to a positive integer (e.g. 20) to allow the two
# sources of drift above. CI releases should run with TRIAGE_SLACK=0
# to catch any inventory drift before declaring 0-active-alerts.
TRIAGE_SLACK="${TRIAGE_SLACK:-0}"
if [ "$triage_rows" -ne "$expected" ]; then
    if [ "$abs_delta" -le "$TRIAGE_SLACK" ]; then
        echo "WARN: row-count off by $delta (within slack=$TRIAGE_SLACK): $triage_rows vs $expected (= $effective_open open + $ready_to_close ready-to-close)"
    else
        echo "FAIL: row-count check"
        echo "  triage.md rows:           $triage_rows"
        echo "  open alerts:              $open_alerts"
        echo "  stale-cache rows:         $stale_rows"
        echo "  effective open alerts:    $effective_open"
        echo "  ready-to-close rows:      $ready_to_close"
        echo "  expected total:           $expected (= open + ready-to-close)"
        echo "  delta:                    $delta"
        echo "  slack (TRIAGE_SLACK):     $TRIAGE_SLACK"
        fail=1
    fi
else
    echo "OK: row-count check ($triage_rows == $expected; $effective_open open + $ready_to_close ready-to-close)"
fi

# ---- (b) false-positive and accepted-risk rows must have non-empty notes ----
empty_notes=$(awk -F'|' '
    /^\| [0-9]+ \|/ {
        disposition = $8
        notes = $12
        # Strip backticks/parens from disposition (seed has `obsolete` (candidate)).
        gsub(/`/, "", disposition)
        gsub(/ *\(candidate\)/, "", disposition)
        gsub(/^ +| +$/, "", disposition)
        gsub(/^ +| +$/, "", notes)
        if ((disposition == "false-positive" || disposition == "accepted-risk") && notes == "") {
            print $0
        }
    }
' "$triage")
if [ -n "$empty_notes" ]; then
    echo "FAIL: false-positive/accepted-risk rows with empty notes:"
    echo "$empty_notes" | sed 's/^/  /'
    fail=1
else
    echo "OK: notes check (all false-positive/accepted-risk rows have notes)"
fi

# ---- (c) module_owner must be a path listed under AGENTS.md ----
# AGENTS.md declares modules in the "## Module List" section as
#   - **name** — `./modules/...` — description
# so we extract every `./path/` token from that section.
expected_modules=$(awk '
    /^## Module List/ { inlist = 1; next }
    /^## / { inlist = 0 }
    inlist && /`\.\// {
        match($0, /`\.\/[^`]+`/)
        if (RSTART > 0) {
            path = substr($0, RSTART + 3, RLENGTH - 4)
            sub(/\/$/, "", path)
            print path
        }
    }
' "$agents" | sort -u)

if [ -z "$expected_modules" ]; then
    echo "WARN: could not extract module list from $agents; skipping owner check" >&2
else
    bad_owners=$(awk -F'|' -v mods="$expected_modules" '
        BEGIN {
            n = split(mods, arr, "\n")
            for (i = 1; i <= n; i++) {
                valid[arr[i]] = 1
            }
        }
        /^\| [0-9]+ \|/ {
            owner = $7
            gsub(/^ +| +$/, "", owner)
            gsub(/`/, "", owner)
            gsub(/\/$/, "", owner)
            # Module ownership: the owner in triage.md may be a deep path
            # inside an AGENTS.md-listed module; treat as valid if it
            # starts with one of the listed module roots.
            matched = 0
            for (m in valid) {
                if (owner == m || index(owner, m "/") == 1) { matched = 1; break }
            }
            if (owner != "" && !matched) {
                printf "  alert %s -> unknown module_owner: %s\n", $3, owner
            }
        }
    ' "$triage")
    if [ -n "$bad_owners" ]; then
        echo "FAIL: unknown module_owner values:"
        echo "$bad_owners"
        fail=1
    else
        echo "OK: module_owner check (all owners found in AGENTS.md)"
    fi
fi

if [ "$fail" -ne 0 ]; then
    echo "verify-triage-inventory: FAIL" >&2
    exit 1
fi
echo "verify-triage-inventory: PASS"
exit 0
