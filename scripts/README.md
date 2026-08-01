# scripts/

Repository-wide operational and helper scripts. Per the project's AGENTS.md, generated scripts MUST live under this directory (or the owning module's script directory). Scratch work uses `./tmp`; do not use system temp dirs.

## Scope (per spec 994-python-build-scripts)

All build-time scripts in this directory are cross-platform Python 3.9+ (FR-001). The migration delivers per-directory PRs (FR-001a); the `scripts/` directory is **Scope 1** and landed in **US2**. See [`specs/994-python-build-scripts/spec.md`](../../specs/994-python-build-scripts/spec.md) for the full in-scope/out-of-scope split and [`specs/994-python-build-scripts/contracts/cli-schemas.md`](../../specs/994-python-build-scripts/contracts/cli-schemas.md) for the CLI contract of each script.

Out of scope for spec 994 (must NOT be touched):
- Repo-root Maven wrapper (`./mvnw` / `mvnw.cmd`) — already cross-platform; use it for builds.
- Anything under `system/release/`, `system/installResources/`, `system/Tools/`, etc. — runtime scripts deployed with customer installations (FR-013).

## Scripts

### `prune-stale-worktrees.py` / `prune-stale-worktrees.bat`

List or remove **stale git worktrees** left by agent sessions (Kilo / Grok / etc.).

- **Purpose**: Free disk after PRs merge. Full monorepo worktrees under `.kilo/worktrees/`, `~/.grok/worktrees/`, etc. fill disks quickly when not cleaned up. Complements root `AGENTS.md` → **Git worktree hygiene (HARD GATE)** and `.kilo/rules/worktree-hygiene.md`.
- **Usage**:

  ```bash
  # Dry-run (default): show keep vs remove using gh PR state
  python3 scripts/prune-stale-worktrees.py

  # Remove worktrees whose branches have MERGED or CLOSED PRs
  python3 scripts/prune-stale-worktrees.py --apply --force --delete-local-branches

  # Also drop worktrees with no linked PR
  python3 scripts/prune-stale-worktrees.py --apply --force --include-no-pr --delete-local-branches
  ```

  Windows:

  ```bat
  scripts\prune-stale-worktrees.bat
  scripts\prune-stale-worktrees.bat --apply --force --delete-local-branches
  ```

- **Keeps**: main worktree, current cwd worktree, locked worktrees, branches with **open** PRs (unless `--include-open`).
- **Prereqs**: `git`; `gh` authenticated (unless `--skip-gh` with `--include-no-pr` only).
- **Tests**: `python3 -m pytest scripts/test_prune_stale_worktrees.py -v` (or `python3 scripts/test_prune_stale_worktrees.py`).

### `derby-surface-inventory.py` / `derby-surface-inventory.bat`

Repo-wide inventory of Apache Derby surface area for feature **#548** (default embedded DB migration).

- **Purpose**: QC-001 / tasks T004–T005 — produce a dispositionable checklist of every `derby` / `sqlDerby` / NetworkServer / Liquibase `dbms=derby` / etc. hit for triage before GA.
- **Usage**:

  ```bash
  python3 scripts/derby-surface-inventory.py
  # Windows:
  scripts\derby-surface-inventory.bat
  ```
- **Output**: `specs/548-derby-embedded-migration/checklists/derby-surface-inventory.md`
- **Prereqs**: Python 3.9+
- **Notes**: Excludes `target/`, `node_modules/`, `.git`, `*.log`, and common binary suffixes. Assigns dispositions (`port`, `migration-only`, `docs-only`, `test-only`, `false-positive`, …). Re-run after large tree changes. Use `--fail-on-unknown` for QC-001 freeze checks (must exit 0 with zero `unknown` rows).

### `fetch-gh-code-scanning-alerts.py`

Fetch code scanning (CodeQL) alerts for a repository using the `gh` CLI and write a markdown report.

- **Purpose**: Reusable enumerator for the `004-zero-code-scanning-alerts` triage workflow and any future release-readiness check.
- **Usage**:

  ```bash
  python3 scripts/fetch-gh-code-scanning-alerts.py [--repo OWNER/REPO] [--state open|dismissed|fixed|all]
  # state: open | dismissed | fixed | all (default: open)
  python scripts/fetch-gh-code-scanning-alerts.py        # Windows
  ```
- **Output**: `docs/ai-generated/tasks/gh-codeql-alerts/alerts.md` — markdown list of alerts including alert number, rule ID, severity, file path + line, and message.
- **Prereqs**: `gh` CLI authenticated (`gh auth login`).
- **Notes**:
  - Pagination is handled automatically (`--paginate`, `per_page=100`).
  - After fetching, the script invokes `filter_stale_alerts.py` to write `alerts-stale-cache.md` (T007b).
- **Tests**: `python3 -m pytest scripts/test_fetch_gh_code_scanning_alerts.py -v`

### Matrix install smoke (docker/scripts — not this directory)

Ephemeral CMS/DTS install matrix for #1500 lives under **`docker/scripts/matrix-install-smoke.py`** (not here). It mounts `perc-distribution-tree*.jar` / `delivery-tier-distribution*.jar`, runs silent install, starts the product, probes login/health, records JSON under `docker/logs/`, and destroys the cell. See `docker/README.md`.

### `install-cms-dev.py`

Run the Percussion CMS installer ONCE on the host into a persistent `install_root/` directory. The docker **dev** runtime bind-mounts that directory into the `cms-dts` container at `/opt/Percussion/` so:

- the container's only job is to run `StartJetty.sh` (no in-container install);
- container restarts do **not** re-install (the install persists on the host);
- hot-deploys (jar swaps, config edits) are local file edits in `install_root/`, picked up by the container on the next `docker compose restart`.
- **Purpose**: One-time CMS install into `./docker/dev-data/cms-dts/install_root/` (default). Idempotent — skips install if the marker file is present.
- **Usage**:

  ```bash
  python3 scripts/install-cms-dev.py                  # one-time install
  python3 scripts/install-cms-dev.py --reset          # force reinstall
  python3 scripts/install-cms-dev.py --install-root /tmp/cms-install
  ```
- **Prereqs**:
  - JDK 21 on the host.
  - Built artifacts: `modules/perc-distribution-tree/target/perc-distribution-tree.jar` and `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/target/delivery-tier-distribution.jar` (run `./mvnw clean install -DskipTests=true`).
  - For MySQL installs: the `mysql` compose service must be running and reachable on `localhost:3306`.
- **Output**: `RESULT:OK STEP:install LOG:<path>` or `RESULT:FAIL STEP:install LOG:<path>`.
- **Tests**: `python3 -m pytest scripts/test_install_cms_dev.py -v`

### `create-large-folder-fixture.py`

Create a single CMS folder with ≥500 children for the SC-005 perf UAT scenario of feature `992-react-content-explorer`.

- **Purpose**: Tasks.md T012b perf fixture scaffolding. Run on a test CMS instance to seed the fixture used for the SC-005 pass criterion (`p95 ≤ 10 s` on standard office network).
- **Usage**:

  ```bash
  python3 scripts/create-large-folder-fixture.py \
      --base-url https://cms.local:8443 \
      --user admin1 --password <redacted> \
      --fixture-path /Sites/PerfFixture --fixture-count 500
  ```
- **Output**: A folder `FIXTURE_PATH/PerfFixtureRoot` with `FIXTURE_COUNT` children (default `/Sites/PerfFixture/PerfFixtureRoot` × 500).
- **Prereqs**: `curl`, network reachability to a running CMS instance with admin credentials.
- **Tests**: `python3 -m pytest scripts/test_create_large_folder_fixture.py -v`

### `erlang-harvest-review-patterns.py`

Harvest GitHub PR **line review comments** (including closed/merged PRs) from `kilo-code-bot[bot]` (and optional other authors), cluster them into generalized themes, write a candidate report, and optionally auto-merge multi-PR themes into Erlang review pattern memory.

- **Purpose**: Keep Erlang's institutional review memory (`patterns.md`) fed from real Kilo/GitHub review history.
- **Usage**:

  ```bash
  python3 scripts/erlang-harvest-review-patterns.py [--apply] [--promote-critical]
  ```
- **Outputs**: `docs/ai-generated/code-reviews/harvest-candidates-YYYY-MM-DD.md` and, with `--apply`, appends selected bullets to `modules/ai-shared-develop/src/main/resources/skills/erlang-review/patterns.md`.
- **Prereqs**: Python 3.9+, `gh` CLI authenticated.
- **Tests**: `python3 -m pytest scripts/test_erlang_harvest_review_patterns.py -v`

### Other scripts in this directory

Each entry below has been ported to cross-platform Python 3.9+ with pytest coverage under `scripts/test_<name>.py`.

- `authenticate-sigstore.py` — Sigstore OIDC token retrieval + cache. Test: `test_authenticate_sigstore.py`.
- `gh-preflight.py` — pre-flight checks for `gh` CLI usage. Test: `test_gh_preflight.py`.
- `hot-deploy-local.py` — local hot-deploy helper for the CMS (jar modules + webui). Test: `test_hot_deploy_local.py`.
- `resolve-conflicts.py` — git conflict resolution helper (ours / theirs / manual). Test: `test_resolve_conflicts.py`.
- `verify-no-finder-jsp-references.py` — CI-gate artifact-grep for spec 992 / FR-019a (modern Track B shell, hard-cut in PR #1390). Test: `test_verify_no_finder_jsp_references.py`.
- `verify-no-jqplot-vendor-refs.py` — CI-gate guard that the removed jqplot vendor library stays gone. Test: `test_verify_no_jqplot_vendor_refs.py`.
- `verify-codeql-analyzer-of-record.py` — asserts the advanced CodeQL workflow + config + playbook are in place and that the default-setup is `not-configured`. Test: `test_verify_codeql_analyzer_of_record.py`.

### `004-zero-code-scanning-alerts` workflow scripts

All converted to cross-platform Python 3.9+ (US2). All run from the repo root.

|           Python script           |                                                                                                       Purpose                                                                                                       | Spec ref |
|-----------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------|
| `filter-stale-alerts.py`          | Filter out alerts whose file path is no longer in `git ls-files`; write the stale rows to `alerts-stale-cache.md` for audit. Invoked automatically by `fetch-gh-code-scanning-alerts.py` at the end of every fetch. | T007b    |
| `verify-triage-inventory.py`      | CI-lite check on `triage.md`: row count == open-alert count, every `false-positive`/`accepted-risk` row has non-empty `notes`, every `module_owner` is a path under `AGENTS.md`.                                    | T012     |
| `verify-distribution-archive.py`  | Rebuild `modules/perc-distribution-tree` (and `modules/perc-packages`) and assert none of the files listed in `tmp/gh-codeql-alerts/removed-files.txt` appear in the resulting JARs or `.ppkg` installer.           | T019     |
| `verify-valid-fixes.py`           | Assert every `triage.md` row with `disposition == valid` has a non-empty `linked_pr`.                                                                                                                               | T035     |
| `verify-suppressions.py`          | For every row in `suppressions.md`, grep the cited source line for the matching `// codeql[…] comment and `justification:` text.                                                                                    | T064     |
| `verify-pr-review-resolution.py`  | For every `linked_pr` in `triage.md`, query `gh pr view --json reviewThreads` and fail if any thread has `isResolved: false` (Constitution IX, `SC-007`).                                                           | T078b    |
| `test-verify-triage-inventory.py` | Self-test for `verify-triage-inventory.py` against `scripts/test-fixtures/triage-good.md` and `triage-bad.md`.                                                                                                      | T013     |

#### Usage

```sh
# Re-fetch and re-triage (weekly cadence per the alerts dir README).
python3 scripts/fetch-gh-code-scanning-alerts.py --repo intersoftdatalabs-in/percussioncms

# Pre-merge gates (run before merging a closing PR).
python3 scripts/verify-triage-inventory.py
python3 scripts/verify-valid-fixes.py
python3 scripts/verify-suppressions.py
python3 scripts/verify-distribution-archive.py
python3 scripts/verify-pr-review-resolution.py
```

#### Test fixtures

`scripts/test-fixtures/triage-{good,bad}.md` are minimal 4-row triage inventories used by `test-verify-triage-inventory.py`. Companion `scripts/test-fixtures/alerts-{good,bad}.md` files provide the alerts.md content for the row-count check. The "bad" fixture exercises the empty-notes and unknown-module_owner failure modes; the "good" fixture is the expected clean state.

### `release-audit/` package

Cross-platform Python port of the v8.1.x → 8.2 migration audit pipeline (spec 005-migrate-8.1.7-changes). Replaces the previous bash `release-audit.sh` + `lib/*.sh` + `tests/test_*.sh` layout with a Python package (`scripts/release-audit/*.py` + `scripts/release-audit/tests/*.py`).

- **Usage**:

  ```bash
  python3 scripts/release-audit/__main__.py --help
  python3 scripts/release-audit/__main__.py --from-tag v8.1.6 --to-tag v8.1.7 \
      --target-branch development --output-dir ./tmp/release-audit/v8.1.6..v8.1.7
  ```
- **Subcommands**: `inventory`, `verdicts`, `backlog`, `report`, `port`, `all` (default).
- **Tests**: `python3 -m pytest scripts/release-audit/tests/ -v`
- **Note**: The directory name `release-audit/` contains a dash, which Python cannot import as a package name. Users invoke the entry point by file path (`python3 scripts/release-audit/__main__.py`) rather than via `python -m release_audit`.

## Conventions

- **Cross-platform Python only.** All scripts in this directory are Python 3.9+ per spec 994 FR-001. The legacy "Windows users run the `.cmd` counterpart" guidance has been retired (FR-011).
- **Stdlib only at runtime.** No third-party imports beyond pytest (which is declared in `scripts/requirements-dev.txt`, FR-006).
- **`pathlib.Path` everywhere.** No hardcoded `/` or `\\` separators in filesystem paths (FR-007; root AGENTS.md Cross-Platform File I/O & Paths).
- **`subprocess.run([...], shell=False, check=False, timeout=N)`** for every external invocation (FR-008). Never `shell=True`, `os.system`, `bash -c`, `cmd /c`.
- **Colocated pytest module** (`test_<name>.py`) per script (FR-009). Tests invoke scripts via `subprocess.run([sys.executable, str(script_path), ...])` per R4.
- **`## Behavioral Notes`** section in every script's module docstring enumerating deviations from the shell original (FR-009b).
- **`logging.getLogger(__name__)`** with format `%(asctime)s %(levelname)s %(message)s`.
- **Scripts MUST NOT write to `%TEMP%` or `$TMPDIR`**; use `./tmp` for scratch.
- **Scripts MUST NOT invent third-party APIs or extension points** — see root AGENTS.md "Evidence Over Invention".

