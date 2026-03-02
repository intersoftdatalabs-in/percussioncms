# GH Code Scanning Alerts (CodeQL)

This task folder contains a small helper script and a target markdown file to list current code scanning (CodeQL) alerts for a repository using the `gh` CLI.

Purpose
- Provide a reproducible command to fetch code scanning alerts from GitHub and write a readable markdown report.

Prerequisites
- `gh` (GitHub CLI) installed and authenticated (`gh auth login`).
- `jq` installed for JSON processing.

Files created
- `scripts/fetch-gh-code-scanning-alerts.sh` — helper script that queries the GitHub API via `gh` and generates `alerts.md`.
- `docs/ai-generated/tasks/gh-codeql-alerts/alerts.md` — placeholder output file created by the script.

Usage
1. From the repository root, run:

```bash
# Use default repo from GITHUB_REPOSITORY or pass owner/repo as first arg
scripts/fetch-gh-code-scanning-alerts.sh percussion/percussioncms
```

2. The script will write `docs/ai-generated/tasks/gh-codeql-alerts/alerts.md` containing a human-readable list of alerts.

Notes
- The script uses the GitHub REST API endpoint `repos/{owner}/{repo}/code-scanning/alerts` via `gh api`.
- If you need a different format (CSV, JSON), the script can be adapted easily.

Next steps
- Run the script locally (requires authenticated `gh`) and review `alerts.md`.
- I can then open individual alerts and propose fixes or create PRs for high-priority items.
