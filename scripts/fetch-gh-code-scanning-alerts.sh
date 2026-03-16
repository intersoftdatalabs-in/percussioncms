#!/usr/bin/env bash
set -euo pipefail

# Fetch code scanning alerts for a repository using gh and write a markdown report.
# Usage: scripts/fetch-gh-code-scanning-alerts.sh [owner/repo]
# If no repo is passed, the script will attempt to use $GITHUB_REPOSITORY or default to percussion/percussioncms.

repo_arg="${1:-}"
repo="${repo_arg:-${GITHUB_REPOSITORY:-percussion/percussioncms}}"
output_file="docs/ai-generated/tasks/gh-codeql-alerts/alerts.md"

# Requirements
command -v gh >/dev/null 2>&1 || { echo "gh CLI not found. Install and authenticate (gh auth login)." >&2; exit 2; }
command -v jq >/dev/null 2>&1 || { echo "jq not found. Please install jq." >&2; exit 2; }

mkdir -p "$(dirname "$output_file")"

echo "# Code Scanning Alerts for ${repo}" > "$output_file"
echo "" >> "$output_file"
echo "Generated: $(date -u +"%Y-%m-%dT%H:%M:%SZ") (UTC)" >> "$output_file"
echo "" >> "$output_file"

echo "Fetching code scanning alerts via gh API..."
# Use GitHub API via gh to list code-scanning alerts. We paginate and transform to Markdown using jq.

gh api -H "Accept: application/vnd.github+json" "/repos/${repo}/code-scanning/alerts" --paginate | \
  jq -r '.[] | [(.rule_id // "<no-rule>"), (.rule_severity // "<no-sev>"), (.tool.name // "<no-tool>"), (.state // "<no-state>"), (.created_at // "<no-date>"), (.html_url // "<no-url>"), (.most_recent_instance.message.text // "<no-message>")] | @tsv' \
  | awk -F"\t" '{printf "- **Rule:** %s\n  - **Severity:** %s\n  - **Tool:** %s\n  - **State:** %s\n  - **Created:** %s\n  - **URL:** %s\n  - **Message:** %s\n\n", $1, $2, $3, $4, $5, $6, $7}' >> "$output_file"

cat <<EOF
Done. Alerts written to: $output_file
EOF

exit 0
