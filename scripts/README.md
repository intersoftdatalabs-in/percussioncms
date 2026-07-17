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

### `erlang-harvest-review-patterns.py` (+ `.sh` / `.bat`)

Harvest GitHub PR **line review comments** (including closed/merged PRs) from
`kilo-code-bot[bot]` (and optional other authors), cluster them into generalized
themes, write a candidate report, and optionally auto-merge multi-PR themes into
Erlang review pattern memory.

- **Purpose**: Keep Erlang's institutional review memory (`patterns.md`) fed from
  real Kilo/GitHub review history without hand-copying every comment. Short-handed
  teams re-run this instead of maintaining patterns only by hand.
- **Usage** (repo root; needs `gh auth login` + network):
  ```text
  # Candidates only (safe default)
  python3 scripts/erlang-harvest-review-patterns.py
  # or: scripts/erlang-harvest-review-patterns.sh
  # or: scripts\erlang-harvest-review-patterns.bat

  # Merge multi-PR themes into patterns.md
  python3 scripts/erlang-harvest-review-patterns.py --apply

  # Also promote single-PR CRITICAL hard-gate themes (noisier)
  python3 scripts/erlang-harvest-review-patterns.py --apply --promote-critical
  ```
- **Outputs**:
  - `docs/ai-generated/code-reviews/harvest-candidates-YYYY-MM-DD.md` — full cluster report + evidence links
  - With `--apply`: appends selected bullets to
    `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md`
    (marked `_(harvested, seen N×)_`)
- **Prereqs**: Python 3.9+, `gh` CLI authenticated. No `jq` required (Python parses JSON).
- **Cross-platform**: Python core; Unix wrapper `.sh` and Windows wrapper `.bat`.
- **Tests**: `python3 scripts/test_erlang_harvest_review_patterns.py` (offline; uses `--fixture`).
- **Notes**:
  - Default authors: `kilo-code-bot[bot]`. Options: `--include-security-bots`, `--include-humans`.
  - Default `--apply` promotes **multi-PR** themes only (count ≥ 2 and ≥ 2 distinct PRs).
  - Mitigation / reply threads are skipped (`in_reply_to_id`).
  - Review the candidates report (and the patterns.md diff) before committing.

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
| `verify-no-jqplot-vendor-refs.sh` | Guards the `WebUI/.../lib/jqplot` vendor-removal (dead Shindig-gadget-era chart library, confirmed via `git grep` to have zero live JSP/JS/HTML/XML references) against regression: fails if the removed directories reappear or any file references `jqplot` again. | T021-class US2 |
| `test-verify-no-jqplot-vendor-refs.sh` | Self-test for `verify-no-jqplot-vendor-refs.sh`: drives the real script through its PASS case (current, already-clean repo state) and two FAIL cases (a reintroduced vendor directory; a stray `jqplot` reference in a `git add`ed tracked file), cleaning up all scratch state on exit. | T021-class US2 |

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

- Prefer portable entry points: POSIX `sh`/`bash` **and** Windows `.bat` when operators need both, or a single Python/Java tool with thin wrappers (see `erlang-harvest-review-patterns`).
- Shell scripts MUST set `-euo pipefail` (or `set -eu` for pure `sh`) and document purpose + usage in this README.
- Scripts MUST NOT write to `%TEMP%` or `$TMPDIR`; use `./tmp` for scratch.
- Scripts MUST NOT invent third-party APIs or extension points — see root AGENTS.md "Evidence Over Invention".
