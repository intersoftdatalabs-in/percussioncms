# scripts/

Repository-wide operational and helper scripts. Per the project's AGENTS.md, generated scripts MUST live under this directory (or the owning module's script directory). Scratch work uses `./tmp`; do not use system temp dirs.

## Scripts

### `fetch-gh-code-scanning-alerts.sh`

Fetch code scanning (CodeQL) alerts for a repository using the `gh` CLI and write a markdown report.

- **Purpose**: Reusable enumerator for the `004-zero-code-scanning-alerts` triage workflow and any future release-readiness check.
- **Usage**:
  ```bash
  scripts/fetch-gh-code-scanning-alerts.sh [owner/repo] [state]
  # state: open | dismissed | fixed | all (default: open)
  ```
- **Output**: `docs/ai-generated/tasks/gh-codeql-alerts/alerts.md` — markdown list of alerts including alert number, rule ID, severity, file path + line, and message.
- **Prereqs**: `gh` CLI authenticated (`gh auth login`), `jq` installed.
- **Notes**:
  - The GitHub REST API nests `rule.id` and `rule.security_severity_level` under `.rule.*`. Earlier versions of this script used flat field names and produced `<no-rule>` placeholders; fixed 2026-07-11.
  - Pagination is handled automatically (`--paginate`, `per_page=100`).
  - For the `004-zero-code-scanning-alerts` workflow, use this script to (re)generate `alerts.md`, then seed/triage `triage.md` per `specs/004-zero-code-scanning-alerts/contracts/README.md` C1.

### Other scripts in this directory

- `authenticate-sigstore.sh` — sigstore / cosign authentication helper for artifact verification.
- `gh-preflight.sh` — pre-flight checks for `gh` CLI usage.
- `hot-deploy-local.sh` — local hot-deploy helper for the CMS.
- `resolve-conflicts.sh` — git conflict resolution helper.

## Conventions

- All scripts in this directory MUST be POSIX `sh` or `bash`, set `-euo pipefail`, and document purpose + usage in this README.
- Scripts MUST NOT write to `%TEMP%` or `$TMPDIR`; use `./tmp` for scratch.
- Scripts MUST NOT invent third-party APIs or extension points — see root AGENTS.md "Evidence Over Invention".
