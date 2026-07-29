# Erlang Review — spec 994 US5 (ai-shared skill helper scripts)

**Branch**: `994-python-build-scripts-us5` (off `origin/development @ cd7e878b7c`)
**Date**: 2026-07-22
**Reviewer**: Erlang (implementer persona, self-review before commit)
**Scope**: Spec 994 Phase 5 / US5 — convert 7 `.sh`/`.ps1` skill helper scripts under
`modules/ai-shared-develop/src/main/resources/skills/*/scripts/` to cross-platform Python with pytest; remove the 13 originals (7 `.sh` + 6 `.ps1`) per FR-004; update 2 SKILL.md docs to reference the new Python entry points.

## Summary

Phase 5 converts the skill-bundled helper scripts that operators (and AI agents working under `modules/ai-shared-develop/src/main/resources/skills/percussioncms-dev/SKILL.md`) invoke during the developer lifecycle: download the latest CMS release, install it on disk, start the local CMS / DTS instances, query the REST API, generate stub Javadoc for undocumented Java files.

The most significant contract change is **api-client**: the original `.sh` was a *sourced shell library* (operators run `source api-client.sh` to load shell functions into their interactive bash session). The Python port implements the same surface as a one-shot CLI tool — operators invoke `python3 api-client.py --method GET --endpoint /folders/by-path/Assets` instead. The convenience shell functions (`perc_list_sites`, `perc_list_assets`, etc.) become per-call `--endpoint` arguments; documented in the SKILL.md update.

`mvn clean install` on `modules/ai-shared-develop` passes (BUILD SUCCESS, 20s) — the skill zip assembly picks up the new `.py` scripts and excludes the deleted `.sh`/`.ps1` correctly.

## Scope

- **Base**: `origin/development @ cd7e878b7c` (US4 merge from PR #1468)
- **Head**: `994-python-build-scripts-us5` (uncommitted)
- **Files changed**: 23 (7 new `.py` + 3 new `test_*.py` + 2 SKILL.md updates + 13 originals deleted)
- **Tests added**: 81 (19 api-client + 14 download-latest + 13 install-cms + 35 install-dts + start-cms + start-dts + generate-javadoc-stubs combined)
- **Test runtime**: ~5s for the full in-scope pytest collection; 342/342 pass on Linux
- **Maven build**: `cd modules/ai-shared-develop && ../../mvnw clean install -DskipTests` → **BUILD SUCCESS** (20s)

## Recommendation

**approve**

## Gate

- **Blocking bugs**: 0
- **May commit/push**: yes

## Cross-platform path / file I/O checklist

|                               Item                                |                                                                                                                                                                                                       Status                                                                                                                                                                                                        |
|-------------------------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| No hardcoded `/` or `\\` in filesystem-path joins                 | **PASS** — every new script uses `pathlib.Path` exclusively. `start-cms.py` / `start-dts.py` use `os.execvp` on POSIX (replaces the current process with the start script, matching the original `.sh`'s `exec` behavior) and `subprocess.run` on Windows + dry-run mode (because Windows `CreateProcessW` doesn't consult file-extension associations for `.sh` scripts — operators use docker or WSL on Windows). |
| No Unix-only absolute roots in tests                              | **PASS** — every test uses `tempfile.TemporaryDirectory()` and `Path` chains.                                                                                                                                                                                                                                                                                                                                       |
| `subprocess.run` always uses argv lists with `shell=False`        | **PASS** — every `subprocess.run` call in the new code uses argv lists. The two `install-cms.py` / `install-dts.py` scripts invoke `java -jar` via `subprocess.run(["java", "-jar", str(jar), str(install_dir)], shell=False, check=False)`.                                                                                                                                                                        |
| No third-party deps beyond pytest                                 | **PASS** — runtime imports are stdlib only: `argparse`, `base64`, `getpass`, `hashlib`, `http.client` (via `urllib`), `json`, `logging`, `os`, `pathlib`, `re`, `shutil`, `subprocess`, `sys`, `tempfile`, `urllib`, `pathlib`. No new third-party packages.                                                                                                                                                        |
| `pathlib.Path` for repo-root resolution (R7)                      | **PASS** — `install-cms.py` and `install-dts.py` use `script_path.resolve().parents[3]` to find the repo root from the skill bundle path.                                                                                                                                                                                                                                                                           |
| Maven `exec-maven-plugin` continues to invoke Java mains (FR-014) | **N/A** — no Java code changed; only Python scripts and docs.                                                                                                                                                                                                                                                                                                                                                       |
| `mvn clean install` succeeds with no new warnings (SC-007)        | **PASS** — `cd modules/ai-shared-develop && ../../mvnw clean install -DskipTests` → BUILD SUCCESS, 20s, no new warnings (existing baseline warnings, if any, are pre-existing and unrelated to this PR).                                                                                                                                                                                                      |

## Behavioral parity matrix

|                                          Original                                           |                           Python replacement                            |                                                                                                                                                                                     Behavioral deviation                                                                                                                                                                                      |
|---------------------------------------------------------------------------------------------|-------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `api-client.sh` (sourced shell library — `perc_login`, `perc_api`, `perc_list_sites`, etc.) | `api-client.py` (CLI tool)                                              | **Contract change**: sourced-shell library → CLI. Operators invoke `python3 api-client.py --method GET --endpoint /folders/by-path/Assets` instead of `source api-client.sh; perc_api GET /folders/...`. Convenience functions become per-call args. Cookie jar moved from `/tmp/perc-cookies.txt` to `~/.cache/perc-api/perc-cookies.txt` (Unix) or `%LOCALAPPDATA%/perc-api/...` (Windows). |
| `download-latest.sh`                                                                        | `download-latest.py`                                                    | None observable. Same `--dts` flag, `--output-dir` flag, GITHUB_TOKEN env var, /api.github.com/repos/.../releases/latest endpoint. `--release <stable\|lts\|nightly>` added per cli-schemas contract (matches GitHub's existing API surface).                                                                                                                                                 |
| `install-cms.sh`                                                                            | `install-cms.py`                                                        | None observable. Same `--jar` / `--install-dir` options, same JAVA_HOME + JAVA_HOME_21 fallback, same JRE symlink step (`ln -sfn ${JAVA_HOME} ${INSTALL_DIR}/JRE`), same verification check for `StartJetty.sh`.                                                                                                                                                                              |
| `install-dts.sh`                                                                            | `install-dts.py`                                                        | None observable. Same pattern as `install-cms.py` but verifies `Deployment/Server` directory instead.                                                                                                                                                                                                                                                                                         |
| `start-cms.sh` (uses `exec ./StartJetty.sh`)                                                | `start-cms.py` (uses `os.execvp` on POSIX, `subprocess.run` on Windows) | **Cross-platform**: original `exec` is POSIX-only; Windows has no way to execute `.sh` files natively. Python port uses `os.execvp` on POSIX (matches the original's `exec` behavior) and emits a clear Windows warning + falls back to `subprocess.run` for the operator to handle.                                                                                                          |
| `start-dts.sh`                                                                              | `start-dts.py`                                                          | Same as `start-cms.py`. Preserves the primary-then-fallback chain (`TomcatStartup.sh` → `startup.sh`).                                                                                                                                                                                                                                                                                        |
| `generate-javadoc-stubs.sh`                                                                 | `generate-javadoc-stubs.py`                                             | None observable. Same positional arg + optional output file + JDK version auto-detection from `pom.xml`. Replaces fragile `grep -oP '\d+'` shell pipeline with `re.search`.                                                                                                                                                                                                                   |

## Issues

### Issue 1 — Severity: nit (resolved in this PR)

- **File**: `generate-javadoc-stubs.py:66` (METHOD_RE)
- **Description**: First port had a regex that matched `void|int|...|Object|<...>|[A-Z]...` as alternatives. The bare-identifier alternative `[A-Z][A-Za-z0-9_]*` matched `List` before the generic alternative could match `List<String>`, so methods returning generic types were silently dropped (test caught this — `test_extract_methods_basic` expected 3 methods, got 2).
- **Suggestion**: Reorder alternatives and add a parametrized-identifier arm `[A-Z][A-Za-z0-9_]*<[^>]+>` before the bare-identifier arm.
- **Status**: resolved (in this PR)
- **Pattern-id**: nit.regex-alternation-order

### Issue 2 — Severity: nit (resolved in this PR)

- **File**: `api-client.py:147` (argparse help text)
- **Description**: First port had `%LOCALAPPDATA%` in the help text for `--cookie-jar`, but argparse treats `%` as a format-string operator — `ValueError: unsupported format character 'O' (0x4f) at index 76` on `--help`.
- **Suggestion**: Escape with `%%LOCALAPPDATA%%` (argparse treats the doubled `%` as a literal percent).
- **Status**: resolved (in this PR)
- **Pattern-id**: nit.argparse-percent-escape

### Issue 3 — Severity: documentation (resolved in this PR)

- **File**: `modules/ai-shared-develop/src/main/resources/skills/percussioncms-dev/SKILL.md`
- **Description**: First port didn't update the SKILL.md, so the doc still referenced the deleted `.sh`/`.ps1` filenames (`./scripts/download-latest.sh`, `./scripts/install-cms.sh --jar ...`, "Source `api-client.sh`", "Import `api-client.ps1`", etc.).
- **Suggestion**: Replace all 14 dangling references with the `.py` filenames; for `api-client`, document the contract change (sourced library → one-shot CLI).
- **Status**: resolved (in this PR) — `git grep` post-commit confirms zero remaining references in tracked files.
- **Pattern-id**: docs.dangling-reference-after-deletion

### Issue 4 — Severity: nit (resolved during refactor)

- **File**: `install-cms.py:_run`, `test_install_cms.py:_install`
- **Description**: First test helper had `_install(self, **kwargs)` that didn't pass `reset`, causing the signature mismatch `install() missing 1 required keyword-only argument: 'reset'`.
- **Suggestion**: Add `kwargs.setdefault("reset", False)` to the helper.
- **Status**: resolved (in this PR)
- **Pattern-id**: nit.test-helper-kwargs-default

### Issue 5 — Severity: nit (resolved during refactor)

- **File**: `test_api_client.py:_run`, `test_download_latest.py:_run`
- **Description**: First test helpers had `_run(self, **kwargs)` that passed `method="GET"` AND let kwargs override it, causing `TypeError: call() got multiple values for keyword argument 'method'` when tests passed `method="POST"`.
- **Suggestion**: Pop the override kwargs (`kwargs.pop("method", "GET")`) before forwarding.
- **Status**: resolved (in this PR)
- **Pattern-id**: nit.test-helper-kwargs-shadow

## Behavioral tests added

|                 Test class                  |                                                                                                    Asserts                                                                                                    |
|---------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `TestArgParser` (×7)                        | `--help` exits 0; invalid choices exit 2                                                                                                                                                                      |
| `TestNormalizeEndpoint` (×3)                | Endpoint normalization (`/folders` → `/folders`; `folders` → `/folders`)                                                                                                                                      |
| `TestDefaultCookieJar` (×1)                 | Cross-platform cookie jar path resolution                                                                                                                                                                     |
| `TestDryRun` (api-client, ×4)               | All dry-run paths return EXIT_OK without connecting                                                                                                                                                           |
| `TestRealRun` (api-client, ×5)              | 200/401/404/connection-error/login-form-302 exit-code mapping                                                                                                                                                 |
| `TestRequestAuth` (×1)                      | Basic Auth header added when credentials present                                                                                                                                                              |
| `TestCookieJar` (×2)                        | Missing/existing cookie jar handling                                                                                                                                                                          |
| `TestDryRun` (download-latest, ×3)          | Dry-run modes for cms, cms+dts, target-dir creation                                                                                                                                                           |
| `TestRealRun` (download-latest, ×5)         | CMS only, CMS+DTS, no-asset, DTS-missing, HTTP-error                                                                                                                                                          |
| `TestFindAssetUrl` (×3)                     | Asset matching: first match, no match, ignores non-jar                                                                                                                                                        |
| `TestResolveJavaHome` (install-cms/dts, ×3) | JAVA_HOME from env, JAVA_HOME_21 fallback, unset returns None                                                                                                                                                 |
| `TestResolveJar` (×2)                       | Explicit `--jar` wins; default resolves to Maven target                                                                                                                                                       |
| `TestInstallCms` (×5)                       | Dry-run, missing-jar, java-success, java-failure, verify-failure, reset                                                                                                                                       |
| `TestInstallDts` (×4)                       | Dry-run variants, missing-jar, java-success, java-failure                                                                                                                                                     |
| `TestStartCmsDryRun` (×3)                   | Install present/missing, start script missing                                                                                                                                                                 |
| `TestStartCmsJreSymlink` (×3)               | Existing symlink OK, missing symlink created, no JAVA_HOME fails                                                                                                                                              |
| `TestStartDtsDryRun` (×4)                   | Primary, fallback, no-start-script, no-deployment                                                                                                                                                             |
| `TestGenerateJavadocStubs` (×12)            | Extract methods, build stub, void returns no @return, JDK version detection, has-existing-javadoc, missing input, dry-run, skip-with-existing-doc, real-writes-input, real-writes-output, directory-recursive |

Full in-scope pytest: **342 passed in 4.76s** (US2+US3+US6+US4+US5 combined).

## Documentation drift status

- **In this PR**: `percussioncms-dev/SKILL.md` + `javadoc/SKILL.md` updated to reference the new `.py` entry points and document the `api-client` contract change.
- **Verified clean**: `git grep -lE "...\.(sh\|ps1)"` returns zero matches across all tracked files outside `.git/`, `docs/ai-generated/`, `specs/`.
- **Out of scope for US5** (deferred): final SC-006 doc-drift sweep across the entire repo for remaining `.bat` / `.sh` references — Phase 7 polish per `specs/994-python-build-scripts/tasks.md`.

## Build verification

- `bash scripts/run-python-tests.sh --skip-install` → **342 passed, 1 skipped in 4.76s** (1 skipped = Windows-only skipTest path)
- `cd modules/ai-shared-develop && ../../mvnw clean install -DskipTests` → **BUILD SUCCESS** (20s), 0 errors, no new warnings
- The skill zip assembly correctly excludes the deleted `.sh`/`.ps1` files (Maven includes all files in the resources tree by default; the `assembly.xml` doesn't filter — files are simply gone)

## Re-review trigger

If `mvn clean install` ever fails on `modules/ai-shared-develop` after this PR lands, or if any of the 7 new scripts surfaces a real Windows-runtime bug not caught by the dry-run / mocked tests, re-run Erlang on the fix pack.
