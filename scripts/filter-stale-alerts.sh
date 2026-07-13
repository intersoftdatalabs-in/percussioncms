#!/usr/bin/env bash
# Stale scanner-cache filter for the GH code-scanning fetch script.
#
# A "stale" alert is one whose most_recent_instance.location.path no longer
# exists in the current branch's working tree (the file was removed, renamed,
# or never committed). The CodeQL dashboard sometimes caches these for a
# while; filtering them out of the open-alert count is required for a true
# "0 active alerts" release-readiness measure.
#
# Usage:
#   scripts/filter-stale-alerts.sh <alerts.md> <stale-out.md>
#
# Writes one row per filtered alert to <stale-out.md> in the form:
#   alert_id|rule_id|path|last_seen_branch
#
# Notes:
#   * Uses git ls-files to determine the set of tracked files. Untracked
#     files (work-in-progress) are NOT considered present; the goal is the
#     "what will the next push scan" answer, not the worktree.
#   * POSIX-ish; requires bash + awk. Used by verify scripts and run
#     weekly per the cadence in
#     docs/ai-generated/tasks/gh-codeql-alerts/README.md.
set -euo pipefail

if [[ $# -ne 2 ]]; then
    echo "usage: $0 <alerts.md> <stale-out.md>" >&2
    exit 2
fi

alerts_md="$1"
stale_out="$2"

if [[ ! -f "$alerts_md" ]]; then
    echo "alerts file not found: $alerts_md" >&2
    exit 2
fi

# Current branch (best-effort; default to "unknown" if not a git checkout).
branch="unknown"
if git rev-parse --abbrev-ref HEAD >/dev/null 2>&1; then
    branch="$(git rev-parse --abbrev-ref HEAD)"
fi

# Build a sorted set of all tracked files for O(log n) lookups.
tracked="$(mktemp)"
trap 'rm -f "$tracked"' EXIT
git ls-files | sort -u > "$tracked"

# Header on the stale-out file.
{
    echo "# Stale Scanner-Cache Alerts"
    echo ""
    echo "**Repository**: $(git config --get remote.origin.url 2>/dev/null || echo unknown)"
    echo "**Branch**: ${branch}"
    echo "**Generated**: $(date -u +"%Y-%m-%dT%H:%M:%SZ") (UTC)"
    echo ""
    echo "These alerts reference a file path that is no longer present in this"
    echo "branch (per \`git ls-files\`). The CodeQL dashboard can cache alerts for"
    echo "deleted files; these rows are EXCLUDED from the open-alert count for"
    echo "release readiness. See"
    echo "\`specs/004-zero-code-scanning-alerts/contracts/README.md\` C1."
    echo ""
    echo "| alert_id | rule_id | path | last_seen_branch |"
    echo "|----------|---------|------|------------------|"
} > "$stale_out"

# Parse alerts.md (raw fetch output) and emit a stale row for any path that
# is not in `git ls-files`. Format of alerts.md (per the fetch script):
#   - **Alert #<id>** — `<rule>` (severity, CodeQL)
#     - **Location:** <path>:<line>
# The first pass writes a TSV of unique (alert_id, rule, path) tuples;
# the second pass uses `comm -23` to subtract the tracked paths.

# The awk function above is a placeholder; we re-do the check with comm
# below for portability.  This second pass is O(n log n) and produces the
# final stale file.
{
    # Extract unique (alert_id, rule, path) tuples from alerts.md.
    awk '
        /^- \*\*Alert #/ {
            line = $0
            sub(/.*Alert #/, "", line)
            split(line, a, " ")
            alert_id = a[1]
            gsub(/^\*+|\*+$/, "", alert_id)
            if (match($0, /`[^`]+`/)) {
                rule = substr($0, RSTART + 1, RLENGTH - 2)
            } else {
                rule = "<unknown>"
            }
        }
        /\*\*Location:\*\*/ {
            loc = $0
            sub(/.*Location:\*\* */, "", loc)
            split(loc, parts, ":")
            path = parts[1]
            sub(/^\.\//, "", path)
            if (path != "" && path != "<no-path>") {
                printf "%s\t%s\t%s\n", alert_id, rule, path
            }
        }
    ' "$alerts_md" | sort -u > "${tracked}.alerts.tsv"

    # List of paths that are tracked.
    cut -f3 "${tracked}.alerts.tsv" | sort -u > "${tracked}.paths.tsv"

    # Paths in alerts that are NOT tracked (i.e., stale).
    comm -23 "${tracked}.paths.tsv" "$tracked" > "${tracked}.stale.tsv"

    # Emit a row per (alert_id, rule, stale path).
    if [[ -s "${tracked}.stale.tsv" ]]; then
        while IFS= read -r stale_path; do
            awk -F'\t' -v sp="$stale_path" -v br="$branch" '
                $3 == sp {
                    printf "| %s | `%s` | `%s` | %s |\n", $1, $2, $3, br
                }
            ' "${tracked}.alerts.tsv"
        done < "${tracked}.stale.tsv" >> "$stale_out"
        rm -f "${tracked}.alerts.tsv" "${tracked}.paths.tsv" "${tracked}.stale.tsv"
    fi
}

# Summary on stderr for callers.
count="$(grep -c '^|' "$stale_out" 2>/dev/null || true)"
count="${count:-0}"
# Subtract the two header rows.
count=$((count > 2 ? count - 2 : 0))
echo "stale-alerts: ${count}  (file: ${stale_out})" >&2
