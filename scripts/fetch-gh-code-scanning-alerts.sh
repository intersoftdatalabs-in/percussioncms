#!/usr/bin/env bash
set -euo pipefail

# Fetch code scanning alerts for a repository using gh and write a markdown report.
# Usage: scripts/fetch-gh-code-scanning-alerts.sh [owner/repo] [state]
#   state: open | dismissed | fixed | all (default: open)
# If no repo is passed, the script will attempt to use $GITHUB_REPOSITORY or default to percussion/percussioncms.
#
# Companion: scripts/filter-stale-alerts.sh (invoked at the end) writes
# docs/ai-generated/tasks/gh-codeql-alerts/alerts-stale-cache.md for any
# alert whose most_recent_instance.location.path is no longer in
# `git ls-files` on the current branch (per feature 004 T007b).

repo_arg="${1:-}"
repo="${repo_arg:-${GITHUB_REPOSITORY:-percussion/percussioncms}}"
state_arg="${2:-open}"
output_file="docs/ai-generated/tasks/gh-codeql-alerts/alerts.md"

# Requirements
command -v gh >/dev/null 2>&1 || { echo "gh CLI not found. Install and authenticate (gh auth login)." >&2; exit 2; }
command -v jq >/dev/null 2>&1 || { echo "jq not found. Please install jq." >&2; exit 2; }

# Validate state_arg against the GitHub REST API's allowed values for
# the /code-scanning/alerts endpoint. An invalid value is otherwise
# passed straight into the gh api URL and produces an opaque API error
# under set -euo pipefail.
case "$state_arg" in
    open|dismissed|fixed|all) ;;
    *) echo "usage: $0 [owner/repo] [state]" >&2
       echo "  state: one of open, dismissed, fixed, all (default: open)" >&2
       exit 2 ;;
esac

mkdir -p "$(dirname "$output_file")"

echo "# Code Scanning Alerts for ${repo}" > "$output_file"
echo "" >> "$output_file"
echo "State filter: ${state_arg}" >> "$output_file"
echo "Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ") (UTC)" >> "$output_file"
echo "" >> "$output_file"

echo "Fetching code scanning alerts via gh API..."

# GitHub REST returns: .[].{ number, state, rule.id, rule.security_severity_level,
#                                  tool.name, created_at, html_url,
#                                  most_recent_instance.message.text,
#                                  most_recent_instance.location.path,
#                                  most_recent_instance.location.start_line }
# Note: prior versions of this script used flat field names (.rule_id, .rule_severity,
# .state) which the GitHub API does not return — those are nested under .rule.* and at the
# top level. Fixed 2026-07-11 as part of feature 004-zero-code-scanning-alerts.

gh api -H "Accept: application/vnd.github+json" "/repos/${repo}/code-scanning/alerts?per_page=100&state=${state_arg}" --paginate | \
  jq -r '.[] | [
    (.number // "<no-number>"),
    (.rule.id // "<no-rule>"),
    (.rule.security_severity_level // .rule.severity // "<no-sev>"),
    (.tool.name // "<no-tool>"),
    (.state // "<no-state>"),
    (.created_at // "<no-date>"),
    (.html_url // "<no-url>"),
    (.most_recent_instance.message.text // "<no-message>"),
    (.most_recent_instance.location.path // "<no-path>"),
    ((.most_recent_instance.location.start_line // "<no-line>") | tostring)
  ] | @tsv' \
  | awk -F"\t" '{printf "- **Alert #%s** — `%s` (%s, %s)\n  - **Tool:** %s\n  - **State:** %s\n  - **Created:** %s\n  - **URL:** %s\n  - **Location:** %s:%s\n  - **Message:** %s\n\n", $1, $2, $3, "CodeQL", $4, $5, $6, $7, $9, $10, $8}' >> "$output_file"

# Stale-cache filter (T007b). Filtered alerts go to
# docs/ai-generated/tasks/gh-codeql-alerts/alerts-stale-cache.md; the
# release-readiness report excludes those rows from the open-alert count.
stale_file="docs/ai-generated/tasks/gh-codeql-alerts/alerts-stale-cache.md"
if [[ -x "scripts/filter-stale-alerts.sh" ]]; then
    scripts/filter-stale-alerts.sh "$output_file" "$stale_file" || \
        echo "warning: stale-alert filter failed; continuing" >&2
else
    echo "note: scripts/filter-stale-alerts.sh not present or not executable; skipping stale-cache filter" >&2
fi

cat <<EOF
Done. Alerts written to: $output_file
Stale-cache filter: $stale_file
EOF

exit 0
