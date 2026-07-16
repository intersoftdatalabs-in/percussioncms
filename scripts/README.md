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

### `004-zero-code-scanning-alerts` workflow scripts

Added for the `004-zero-code-scanning-alerts` feature. All are POSIX `sh` (or portable `bash`) per `AGENTS.md`; all run from the repo root.

| Script | Purpose | Spec ref |
|--------|---------|----------|
| `filter-stale-alerts.sh` | Filter out alerts whose file path is no longer in `git ls-files`; write the stale rows to `alerts-stale-cache.md` for audit. Invoked automatically by `fetch-gh-code-scanning-alerts.sh` at the end of every fetch. | T007b |
| `verify-triage-inventory.sh` | CI-lite check on `triage.md`: row count == open-alert count, every `false-positive`/`accepted-risk` row has non-empty `notes`, every `module_owner` is a path under `AGENTS.md`. | T012 |
| `verify-distribution-archive.sh` | Rebuild `modules/perc-distribution-tree` (and `modules/perc-packages`) and assert none of the files listed in `tmp/gh-codeql-alerts/removed-files.txt` appear in the resulting JARs or `.ppkg` installer. | T019 |
| `verify-valid-fixes.sh` | Assert every `triage.md` row with `disposition == valid` has a non-empty `linked_pr`. | T035 |
| `verify-codeql-analyzer-of-record.sh` | Assert advanced CodeQL is PR-wired, model pack/config/playbook exist, and **default CodeQL setup is `not-configured`** (stops residual thrashing). | codeql-pr-playbook |
| `verify-suppressions.sh` | For every row in `suppressions.md`, grep the cited source line for the matching `// codeql[…]` comment and `justification:` text. | T064 |
| `verify-pr-review-resolution.sh` | For every `linked_pr` in `triage.md`, query `gh pr view --json reviewThreads` and fail if any thread has `isResolved: false` (Constitution IX, `SC-007`). | T078b |
| `test-verify-triage-inventory.sh` | Self-test for `verify-triage-inventory.sh` against `scripts/test-fixtures/triage-good.md` and `triage-bad.md`. | T013 |

#### Usage

```sh
# Re-fetch and re-triage (weekly cadence per the alerts dir README).
scripts/fetch-gh-code-scanning-alerts.sh intersoftdatalabs-in/percussioncms

# Pre-merge gates (run before merging a closing PR).
scripts/verify-triage-inventory.sh
scripts/verify-valid-fixes.sh
scripts/verify-suppressions.sh
scripts/verify-distribution-archive.sh
scripts/verify-pr-review-resolution.sh
```

#### Test fixtures

`scripts/test-fixtures/triage-good.md` and `triage-bad.md` are minimal 4-row
triage inventories used by `test-verify-triage-inventory.sh`. The "bad"
fixture exercises the empty-notes and unknown-module_owner failure modes;
the "good" fixture is the expected clean state.

## Conventions

- All scripts in this directory MUST be POSIX `sh` or `bash`, set `-euo pipefail`, and document purpose + usage in this README.
- Scripts MUST NOT write to `%TEMP%` or `$TMPDIR`; use `./tmp` for scratch.
- Scripts MUST NOT invent third-party APIs or extension points — see root AGENTS.md "Evidence Over Invention".
