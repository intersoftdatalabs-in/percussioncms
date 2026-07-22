# CLI Contracts: Cross-Platform Python Build Scripts

**Branch**: `994-python-build-scripts` | **Date**: 2026-07-21 | **Spec**: [spec.md](spec.md)

This document captures the CLI surface contract for each in-scope script. The contract is the **port contract** — what each Python replacement MUST accept as CLI args, env vars, and exit codes — derived from the existing `.sh`/`.bat` it replaces (FR-002: CLI parity).

Conventions:
- **Argument form**: argparse long-form (`--flag`) unless the original used short-form; if the original accepted both, both are preserved
- **Environment variables**: named in `SCREAMING_SNAKE_CASE`; defaults shown
- **Exit codes**:
  - `0` — success
  - `1` — usage error (unknown flag, missing required arg)
  - `2` — prerequisite/IO error (Python missing, file not found, network unreachable)
  - `>2` — logic failure (specific to each script; documented below)
- **Output**: stdout/stderr captured by pytest via `subprocess.run([sys.executable, str(script_path), ...])` per R4

The contracts below enumerate the in-scope scripts grouped by directory. The actual per-script implementation is locked in `/speckit.tasks`.

---

## Scope 1 — repo-root `scripts/`

### `scripts/install-cms-dev.py`
Replaces: `scripts/install-cms-dev.sh` (no `.bat` exists; the original was bash-only and relied on WSL/Git Bash on Windows).

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--install-root <path>` | str | `/opt/Percussion` (Linux) / `C:\opt\Percussion` (Windows, via env) | — | Host-side install root; must match the in-container path the docker-compose bind-mount exposes at `/opt/Percussion` |
| `--reset` | flag | `False` | — | Reinstall even if marker present |
| `--no-bootstrap` | flag | `False` | — | Do not seed from `docker/dev-data/cms-dts/` |
| `--skip-dts` | flag | `True` | — | Run CMS installer only |
| `--install-dts` | flag | `False` | — | Run DTS installer |
| `-h`, `--help` | flag | — | — | Show usage |

Env vars (read from `.env.compose`): `PERC_DB_TYPE`, `PERC_DB_HOST`, `PERC_DB_PORT`, `PERC_DB_NAME`, `PERC_DB_USER`, `PERC_DB_PASSWORD`, `PERC_DB_SSL_*`, `PERC_DB_TRUSTSTORE_*`, `PERC_DB_KEYSTORE_*`.

Stdout: `RESULT:OK STEP:install LOG:<path>` on success; `RESULT:FAIL STEP:install LOG:<path>` on failure. Log file under `docker/logs/install-<ts>.log`.

Behavioral Notes (FR-009b):
- Original uses bash `trap` for cleanup of partial install on failure; Python uses `try`/`finally` (R2). On Windows interactive console, signal handlers are no-ops — cleanup still runs via `finally`.
- Path defaults differ by OS (no hardcoded `/opt/Percussion` on Windows).

### `scripts/authenticate-sigstore.py`
Replaces: `scripts/authenticate-sigstore.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--identity <token>` | str | — | `SIGSTORE_IDENTITY_TOKEN` | OIDC identity token |
| `--cache-path <path>` | str | `~/.sigstore-token` | — | Cache file path |
| `-h`, `--help` | flag | — | — | Show usage |

Behavioral Notes: original used `[[ -f ~/.sigstore-token ]] && [[ -z "${SIGSTORE_IDENTITY_TOKEN}" ]]` for cache-or-env resolution. Python ports to `Path(cache_path).is_file()` and `os.environ.get(...)`; same semantics, no observable change.

### `scripts/gh-preflight.py`
Replaces: `scripts/gh-preflight.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--repo <owner/repo>` | str | `intersoftdatalabs-in/percussioncms` | `GITHUB_REPOSITORY` | — |
| `--require <tool>` | repeated str | `["gh", "jq"]` | — | Tool names that must be on PATH |
| `-h`, `--help` | flag | — | — | Show usage |

Exit code: `2` if any required tool is missing; `0` if all present.

### `scripts/hot-deploy-local.py`
Replaces: `scripts/hot-deploy-local.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--jar <path>` | str (required) | — | — | Path to built jar |
| `--target <cms\|dts\|both\|/abs/path>` | str | `both` | — | Deploy target |
| `--restart` | flag | `False` | — | Restart container after deploy |
| `--verify` | flag | `False` | — | Run verify after deploy |
| `--timeout-seconds <N>` | int | `60` | — | Verify timeout |
| `-h`, `--help` | flag | — | — | Show usage |

### `scripts/resolve-conflicts.py`
Replaces: `scripts/resolve-conflicts.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--strategy <ours\|theirs\|manual>` | str | `manual` | — | Conflict resolution strategy |
| `--dry-run` | flag | `False` | — | Show what would be done |
| `-h`, `--help` | flag | — | — | Show usage |

### `scripts/fetch-gh-code-scanning-alerts.py`
Replaces: `scripts/fetch-gh-code-scanning-alerts.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--repo <owner/repo>` | str | `intersoftdatalabs-in/percussioncms` | `GITHUB_REPOSITORY` | — |
| `--state <open\|dismissed\|fixed\|all>` | str | `open` | — | Alert state filter |
| `--output <path>` | str | `docs/ai-generated/tasks/gh-codeql-alerts/alerts.md` | — | Output markdown path |
| `-h`, `--help` | flag | — | — | Show usage |

Exit code: `0` on success; `2` if `gh` not authenticated or network fails.

### `scripts/generate-umbrella-issues.py`
Replaces: `scripts/generate-umbrella-issues.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--input <path>` | str (required) | — | — | Input triage markdown |
| `--output-dir <path>` | str | `docs/ai-generated/tasks/gh-codeql-alerts/` | — | Output dir for umbrella issues |
| `--dry-run` | flag | `False` | — | Show what would be created |
| `-h`, `--help` | flag | — | — | Show usage |

### `scripts/stage-triage-cluster.py`
Replaces: `scripts/stage-triage-cluster.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--cluster-name <name>` | str (required) | — | — | Cluster name |
| `--max-prs <N>` | int | `10` | — | Max PRs per cluster |
| `-h`, `--help` | flag | — | — | Show usage |

### `scripts/filter-stale-alerts.py`
Replaces: `scripts/filter-stale-alerts.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--input <path>` | str (required) | — | — | Input alerts markdown |
| `--stale-output <path>` | str | `<input>.stale.md` | — | Stale rows output |
| `-h`, `--help` | flag | — | — | Show usage |

### `scripts/verify-triage-inventory.py`
Replaces: `scripts/verify-triage-inventory.sh` (+ paired `test-verify-triage-inventory.sh`)

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--triage <path>` | str | `docs/ai-generated/tasks/gh-codeql-alerts/triage.md` | — | Triage markdown path |
| `--alerts <path>` | str | `docs/ai-generated/tasks/gh-codeql-alerts/alerts.md` | — | Alerts markdown path |
| `--strict` | flag | `False` | — | Treat warnings as failures |
| `-h`, `--help` | flag | — | — | Show usage |

Exit codes: `0` all rules satisfied; `1` rule violation; `2` IO error.

### `scripts/verify-valid-fixes.py`
Replaces: `scripts/verify-valid-fixes.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--triage <path>` | str | `docs/ai-generated/tasks/gh-codeql-alerts/triage.md` | — | — |
| `-h`, `--help` | flag | — | — | Show usage |

### `scripts/verify-suppressions.py`
Replaces: `scripts/verify-suppressions.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--suppressions <path>` | str | `docs/ai-generated/tasks/gh-codeql-alerts/suppressions.md` | — | — |
| `--source-root <path>` | str | repo root | — | — |
| `-h`, `--help` | flag | — | — | Show usage |

### `scripts/verify-distribution-archive.py`
Replaces: `scripts/verify-distribution-archive.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--removed-files <path>` | str | `tmp/gh-codeql-alerts/removed-files.txt` | — | — |
| `--distribution-jar <path>` | str | `modules/perc-distribution-tree/target/perc-distribution-tree-*.jar` | — | — |
| `-h`, `--help` | flag | — | — | Show usage |

### `scripts/verify-pr-review-resolution.py`
Replaces: `scripts/verify-pr-review-resolution.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--triage <path>` | str | `docs/ai-generated/tasks/gh-codeql-alerts/triage.md` | — | — |
| `--repo <owner/repo>` | str | `intersoftdatalabs-in/percussioncms` | `GITHUB_REPOSITORY` | — |
| `-h`, `--help` | flag | — | — | Show usage |

### `scripts/verify-no-finder-jsp-references.py`
Replaces: `scripts/verify-no-finder-jsp-references.sh` (+ `.bat`; + paired `test-verify-no-finder-jsp-references.sh`)

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--target <path>` | str | `WebUI/src/main/webapp/cm/app/webmgt.jsp` | — | — |
| `--allow-include <substr>` | repeated str | `["finder_js.jsp"]` | — | Carve-outs |
| `--allow-track-a` | flag | `False` | — | Allow `cm/pages/app/webmgt.jsp` |
| `-h`, `--help` | flag | — | — | Show usage |

### `scripts/verify-no-jqplot-vendor-refs.py`
Replaces: `scripts/verify-no-jqplot-vendor-refs.sh` (+ paired `test-verify-no-jqplot-vendor-refs.sh`)

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--repo-root <path>` | str | repo root | — | — |
| `-h`, `--help` | flag | — | — | Show usage |

### `scripts/verify-codeql-analyzer-of-record.py`
Replaces: `scripts/verify-codeql-analyzer-of-record.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--repo <owner/repo>` | str | `intersoftdatalabs-in/percussioncms` | `GITHUB_REPOSITORY` | — |
| `--workflow <path>` | str | `.github/workflows/codeql.yml` | — | — |
| `-h`, `--help` | flag | — | — | Show usage |

### `scripts/create-large-folder-fixture.py`
Replaces: `scripts/create-large-folder-fixture.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--fixture-path <path>` | str | `/Sites/PerfFixture` | `FIXTURE_PATH` | CMS folder path |
| `--fixture-count <N>` | int | `500` | `FIXTURE_COUNT` | Number of children |
| `--base-url <url>` | str | `https://localhost:8443` | `CMS_BASE_URL` | — |
| `--user <name>` | str | — | `CMS_USER` | — |
| `--password <pwd>` | str | — | `CMS_PASS` | — |
| `-h`, `--help` | flag | — | — | Show usage |

### `scripts/erlang-harvest-review-patterns.py`
Already exists as Python. **No conversion needed.** Paired `.sh`/`.bat` removed (FR-004). Existing CLI surface unchanged.

### `scripts/test-verify-triage-inventory.py`
Replaces: `scripts/test-verify-triage-inventory.sh` (the bash self-test becomes a pytest self-test).

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--fixture-good <path>` | str | `scripts/test-fixtures/triage-good.md` | — | — |
| `--fixture-bad <path>` | str | `scripts/test-fixtures/triage-bad.md` | — | — |
| `--script-under-test <path>` | str | `scripts/verify-triage-inventory.py` | — | — |
| `-h`, `--help` | flag | — | — | Show usage |

### `scripts/release-audit/*.py`
Replaces: `scripts/release-audit/*.sh` (release-audit.sh, lib/*.sh, tests/test_*.sh, release-audit subcommands).

| Argument (top-level) | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--report <path>` | str | `docs/ai-generated/release-audit.md` | — | Output report |
| `--inventory <path>` | str | `docs/ai-generated/release-inventory.md` | — | Inventory file |
| `--strict` | flag | `False` | — | Treat warnings as failures |
| `<subcommand>` | choice | `inventory\|verdicts\|backlog\|report\|port` | — | Subcommand (mirrors the bash `case "$1"` structure) |
| `-h`, `--help` | flag | — | — | Show usage |

Behavioral Notes: bash `lib/*.sh` source-include pattern becomes Python module imports inside the `release_audit/` package (still under `scripts/`).

---

## Scope 2 — docker dev tooling

### `docker/scripts/hot-deploy-jar.py`
Replaces: `docker/scripts/hot-deploy-jar.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--jar <path>` | str (required) | — | — | — |
| `--target <cms\|dts\|both\|/abs/path>` | str | `both` | — | — |
| `--restart` | flag | `False` | — | — |
| `--timeout-seconds <N>` | int | `60` | — | — |
| `-h`, `--help` | flag | — | — | Show usage |

### `docker/scripts/perc-devctl.py`
Replaces: `docker/scripts/perc-devctl.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `<subcommand>` | choice (required) | — | — | `install\|up\|down\|status\|verify\|it-verify\|deploy-jar\|verify-fix\|logs-path\|inspect-install\|show-generated-passwords` |
| Per-subcommand flags | varies | — | — | Each subcommand preserves the bash flags (e.g. `install --reset --no-bootstrap --install-root <path>`, `up --build`, `down --volumes`, `deploy-jar --jar <path> --target <x> --restart --verify`, etc.) |
| `-h`, `--help` | flag | — | — | Show usage |

Behavioral Notes: the bash trap-based cleanup (`trap 'cleanup_on_error' ERR`) is replaced with `try`/`finally` blocks per R2.

### `docker/entrypoint/install-update.py`
Replaces: `docker/entrypoint/install-update.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--update-url <url>` | str (required) | — | `PERC_UPDATE_URL` | — |
| `--install-root <path>` | str | `/opt/Percussion` | `PERC_INSTALL_ROOT` | — |
| `-h`, `--help` | flag | — | — | Show usage |

---

## Scope 3 — `modules/perc-distribution-tree` build scripts

### `modules/perc-distribution-tree/scripts/verify-jdbc-drivers.py`
Replaces: `modules/perc-distribution-tree/scripts/verify-jdbc-drivers.sh` (+ `.bat`)

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--distribution-dir <path>` | str | `modules/perc-distribution-tree/target/distribution` | — | Built distribution dir |
| `--expected-drivers <name>` | repeated str | parent-POM JDBC driver coords | — | — |
| `-h`, `--help` | flag | — | — | Show usage |

### `modules/perc-distribution-tree/scripts/check-no-glob-deletes.py`
Replaces: `modules/perc-distribution-tree/scripts/check-no-glob-deletes.sh` (+ `.bat`)

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--repo-root <path>` | str | repo root | — | — |
| `-h`, `--help` | flag | — | — | Show usage |

### `modules/perc-distribution-tree/scripts/api-update.py` (consolidated)
Replaces: `modules/perc-distribution-tree/APIUpdate-WEBUI.bat`, `APIUpdate-REST.bat`, `APIUpdate-SiteManage.bat`, `APIUpdateJars.bat`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--module <webui\|rest\|sitemanage\|jars>` | choice (required) | — | — | Which API update to run |
| `--skip-tests` | flag | `False` | — | Pass `-DskipTests=true` to Maven |
| `--no-restart` | flag | `False` | — | Do not restart Jetty after update |
| `-h`, `--help` | flag | — | — | Show usage |

Behavioral Notes: the original `.bat` files hardcoded `start /WAIT cmd /C ...` Windows-process invocations; the Python port invokes Maven via `subprocess.run([mvn_path, ...], shell=False)` (R2). The `--module jars` case still copies jars into the distribution tree.

### `modules/perc-distribution-tree/scripts/update-tinymce.py`
Replaces: `modules/perc-distribution-tree/UpdateTinyMCE.bat`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--source <path>` | str | `modules/perc-tinymce/src/main/tinymce` | — | TinyMCE source dir |
| `--target <path>` | str | `modules/perc-tinymce/src/main/resources/tinymce` | — | Target dir |
| `-h`, `--help` | flag | — | — | Show usage |

---

## Scope 4 — `modules/ai-shared-develop` dev + skill scripts

### `modules/ai-shared-develop/scripts/sign-ai-resources.py`
Replaces: `modules/ai-shared-develop/scripts/sign-ai-resources.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--key <path>` | str | `~/.sigstore-key` | `SIGSTORE_KEY` | — |
| `--target <path>` | str (repeated) | — | — | Files to sign |
| `-h`, `--help` | flag | — | — | Show usage |

### `modules/ai-shared-develop/scripts/verify-signatures-hook.py`
Replaces: `modules/ai-shared-develop/scripts/verify-signatures-hook.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--hook-input <path>` | str | stdin | — | GitHub webhook payload |
| `--key <path>` | str | `~/.sigstore-key.pub` | `SIGSTORE_PUBLIC_KEY` | — |
| `-h`, `--help` | flag | — | — | Show usage |

### `modules/ai-shared-develop/scripts/build-integrity-check.py`
Replaces: `modules/ai-shared-develop/scripts/build-integrity-check.sh`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--manifest <path>` | str (required) | — | — | Manifest of expected artifacts |
| `--repo-root <path>` | str | repo root | — | — |
| `-h`, `--help` | flag | — | — | Show usage |

### Skill scripts under `modules/ai-shared-develop/src/main/resources/skills/percussioncms-dev/scripts/`

Each skill script gets a Python equivalent; the CLI contracts below are derived from the existing `.sh` files in the skill bundle.

| Python script | CLI surface (parity with `.sh`) |
|---------------|-------------------------------|
| `api-client.py` | `--base-url <url> --user <name> --password <pwd> --endpoint <path> [--method GET\|POST\|...] [--data <json>]` |
| `download-latest.py` | `--release <stable\|lts\|nightly> --target-dir <path>` |
| `install-cms.py` | `--install-root <path> --db-type <derby\|mysql\|...> [--reset]` |
| `install-dts.py` | `--install-root <path> [--skip-cms]` |
| `start-cms.py` | `--install-root <path> [--timeout-seconds N]` |
| `start-dts.py` | `--install-root <path> [--timeout-seconds N]` |
| `generate-javadoc-stubs.py` (under `skills/javadoc/scripts/`) | `--module <name> --output <path>` |

All skill scripts include `-h, --help`.

---

## Cross-Cutting Contract: `scripts/run-python-tests.{sh,cmd}`

| Argument | Type | Default | Env var | Notes |
|----------|------|---------|---------|-------|
| `--pytest-args <args...>` | str (variadic) | — | — | Forwarded to `python -m pytest` (e.g. `-k "verify_triage"`) |
| `--skip-install` | flag | `False` | — | Skip the `pip install` step |
| `--requirements <path>` | str | `scripts/requirements-dev.txt` | — | Override requirements file path |
| `-h`, `--help` | flag | — | — | Show usage |

Exit codes: propagates pytest exit code; `2` if `pip install` fails.

---

## Cross-Cutting Contract: Python invocation

All Python scripts in this spec follow the same invocation contract (FR-003):

```text
# Linux / macOS
python3 <script>.py [args]

# Windows
python <script>.py [args]

# Unix with chmod +x
./<script>.py [args]    # uses shebang #!/usr/bin/env python3
```

Each script's `--help` output lists the script's purpose and usage in a `Usage: <script>.py [options]` block (per FR-009 help path test).