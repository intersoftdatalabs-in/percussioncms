# Erlang Review — spec 994 US6 (perc-distribution-tree migration)

**Branch**: `994-python-build-scripts-us6` (off `origin/development @ bcc29e912a`)
**Date**: 2026-07-22
**Reviewer**: Erlang (implementer persona, self-review before commit; project rule
`.kilocode/rules/pre-commit-review.md` + root AGENTS.md "Pre-commit code review
(Erlang)")
**Scope**: Spec 994 Phase 6 — convert 4 `.sh`/`.bat` build verification scripts in
`modules/perc-distribution-tree/scripts/` to cross-platform Python with pytest;
consolidate 4 Windows batch files (`APIUpdate-*.bat`, `UpdateTinyMCE.bat`) at the
module root into a single Python entry point + a separate TinyMCE sync helper;
remove all originals per FR-004; update module AGENTS.md / module README.md / module
scripts/README.md / pom.xml / installDistributionFiles.xml / BundledJdbcDrivers.java
/ .github/workflows/python-build-scripts.yml to drop dangling references.

## Summary

This is a meatier phase of spec 994 than US3 was: 4 new Python scripts (≈22 KLoC),
4 new pytest modules (≈4 KLoC, 61 new test cases), 9 original scripts deleted,
7 documentation files updated, and the canonical Maven build verified end-to-end
(`mvn clean install` → BUILD SUCCESS, 75 tests pass, 0 failures, 0 errors, no new
warnings introduced). The cross-platform Python entry points coexist with the
existing Java mains (`com.percussion.distribution.install.{VerifyJdbcDrivers,
CheckNoGlobDeletes}`), which continue to be the canonical implementation invoked by
Maven `exec-maven-plugin:java` — the Python ports are the operator-facing surface
for manual runs (FR-009b / scope clarification in `cli-schemas.md` Scope 3). All
`subprocess.run` calls use argv lists with `shell=False` (FR-008); all filesystem
paths use `pathlib.Path` (FR-007); stdlib-only at runtime (FR-006); every script
has a `--help` exit-0 path (FR-009 / R4).

## Scope

- **Base**: `origin/development @ bcc29e912a` (US3 merge from PR #1465)
- **Head**: `994-python-build-scripts-us6` (uncommitted)
- **Files changed**: 24 (4 new `.py`, 4 new `test_*.py`, 1 rewritten `scripts/README.md`, 1 updated `AGENTS.md`, 1 updated `README.md`, 1 updated `pom.xml`, 1 updated `installDistributionFiles.xml`, 1 updated `BundledJdbcDrivers.java`, 1 updated `.github/workflows/python-build-scripts.yml`, 4 `.sh`/`.bat` deleted from `scripts/`, 5 `.bat` deleted from module root, 1 `tasks.md` updated to mark T001-T041 [X])
- **Tests added**: 61 (24 verify-jdbc-drivers + 14 check-no-glob-deletes + 13 api-update + 10 update-tinymce)
- **Test runtime**: ~20s for the full in-scope pytest collection; ~9min17s for the full `mvn clean install` of `modules/perc-distribution-tree` (BUILD SUCCESS)
- **Prior report**: `docs/ai-generated/code-reviews/994-python-build-scripts-us3-erlang.md`
- **Memory patterns hit** (from `skills/erlang-review/patterns.md`):
  - `tests.structural-only` — checked; every new pytest module invokes the
    script as a subprocess (`subprocess.run([sys.executable, str(script),
    ...])` or via the in-process `module.main([...])` entry point), then
    asserts on exit code / output / filesystem state — not on AST tokens.
  - `paths.hardcoded-sep` — checked; all filesystem paths use `Path` /
    `Path.resolve()` / `pathlib.Path(__file__).resolve().parents[N]`. The
    one occurrence of a literal `\n` in the api-update.py docstring is a
    natural-language newline description, not a filesystem separator.
  - `false-green-exit` — checked; every new script's failure paths surface
    a recognizable error message ("ERROR: ...", "missing", "not found")
    AND a non-zero exit code. Pytest tests cover each documented exit
    code (1, 2, 3, 4, 5, 6, 7 as applicable).

## Recommendation

**approve**

## Gate

- **Blocking bugs**: 0
- **May commit/push**: yes

## Cross-platform path / file I/O checklist

| Item | Status |
|------|--------|
| No hardcoded `/` or `\\` in filesystem-path joins | **PASS** — all 4 new scripts use `pathlib.Path` exclusively. The single literal occurrence of `\\` in `api-update.py` line 350 is a docstring illustration of Windows `StartJetty.bat` invocation syntax (deliberately Windows-flavored to convey the original `.bat` workflow), not a filesystem separator. |
| No Unix-only absolute roots in tests | **PASS** — every test uses `tempfile.TemporaryDirectory()` and `Path` chains. |
| `subprocess.run` always uses argv lists with `shell=False` | **PASS** — verified by grep; every `subprocess.run` / `_run_maven` / `_restart_jetty` call in the new code uses argv lists. No `shell=True`, `os.system`, `bash -c`, `cmd /c`, or `cmd.exe` anywhere in the new code. The original `.bat` files used `start /WAIT cmd /C ...`; the Python port invokes Maven / Jetty via `subprocess.run([...], shell=False)` per FR-008 and R2. |
| No third-party deps beyond pytest | **PASS** — runtime imports are stdlib-only: `argparse`, `fnmatch`, `logging`, `pathlib`, `shutil`, `subprocess`, `sys`, `tempfile`, `unittest` (in tests), `xml.etree.ElementTree`, `zipfile`. No `requests`, `urllib`, `pip` calls in product code. |
| `shebang` / `encoding` / `from __future__ import annotations` | **PASS** — every new script has all three (or `from __future__` only, where stdlib imports are deliberately minimized). |
| `pathlib.Path` for repo-root resolution (R7) | **PASS** — `_resolve_paths()` in `api-update.py`, `_default_paths()` in `update-tinymce.py`, `_default_install_xml()` in `check-no-glob-deletes.py`, `_default_artifact()` in `verify-jdbc-drivers.py` all use `Path(__file__).resolve().parents[N]`. |
| Maven `exec-maven-plugin` continues to invoke Java mains (FR-014: don't break the build gate) | **PASS** — verified by `mvn clean install`: BUILD SUCCESS, 75 tests pass (incl. the existing `VerifyJdbcDriversTest` and `CheckNoGlobDeletesTest`), the `verify-jdbc-drivers` and `check-no-glob-deletes` `exec:java` executions run cleanly and report `OK: 10 JDBC driver JAR(s) verified under jetty/base/lib/jdbc/`. |

## Build verification (T075 / SC-007)

- `cd modules/perc-distribution-tree && ../../mvn-env.sh clean install` → **BUILD SUCCESS** (exit 0)
- Tests: **75 run, 0 failures, 0 errors, 0 skipped** (across `MainInstallExitCodeTest`, `DbInstallConfigResolverTest`, `RepositoryPropertiesInstallGuardTest`, `StagingCleanupAntScriptTest`, `InstallXmlDeleteSetTest`, `CheckNoGlobDeletesTest`, `VerifyJdbcDriversTest`, `ObsoleteWebInfArtifactsCleanupTest`, `WebUiServletUtilsPackagingTest`)
- New warnings: **0** — the 329 warnings in the build log are pre-existing baseline (mostly `dependency:analyze-only` flagging `Unused declared dependencies found:` for the JDBC driver set + the `stax:stax-api` non-test scope; all are unrelated to this PR). The Python ports and the Javadoc-only edit to `BundledJdbcDrivers.java` introduce no new warning categories.
- Verify-phase gate: `OK: 10 JDBC driver JAR(s) verified under jetty/base/lib/jdbc/` — the Java main's exit code matches the Python port's exit code for the same input (both exit 0 on a clean distribution).

## Behavioral parity matrix

| Original (`.sh`/`.bat`) | Python replacement | Behavioral deviation |
|------------------------|--------------------|----------------------|
| `verify-jdbc-drivers.sh` (POSIX logic) + `verify-jdbc-drivers.bat` (delegates to `com.percussion.distribution.install.VerifyJdbcDrivers`) | `verify-jdbc-drivers.py` | None for the operator-facing surface. Python port replicates the `.sh` logic (zipfile vs. `unzip`; fnmatch vs. shell-glob expansion; `Path.stat().st_size` vs. `stat -c '%s'`). The Java main continues to exist as the canonical implementation invoked by Maven `exec-maven-plugin:java`; both produce identical exit codes for identical inputs. |
| `check-no-glob-deletes.sh` (POSIX awk/grep/sed) + `check-no-glob-deletes.bat` (delegates to `com.percussion.distribution.install.CheckNoGlobDeletes`) | `check-no-glob-deletes.py` | None observable. Python port uses `xml.etree.ElementTree` (more robust than the shell two-pass extraction) and `_find_target`/`_find_first_child_delete` match the original's narrow scope: only the first `<delete>` directly inside `<target name="install_jdbc_drivers">` is inspected. Documented in the script's `## Behavioral Notes`. |
| `APIUpdate-WEBUI.bat` + `APIUpdate-REST.bat` + `APIUpdate-SiteManage.bat` + `APIUpdateJars.bat` (4 Windows-only `.bat` files; no `.sh` counterpart) | `api-update.py --module {webui,rest,sitemanage,jars}` | **Consolidation** — the 4 batch files are replaced by 1 entry point. The behavioral surface (build + copy + restart) is preserved per `--module`. New `--dry-run` flag prints the build plan without invoking Maven / touching the filesystem (gates pytest). Documented in the script's `## Behavioral Notes`. |
| `UpdateTinyMCE.bat` (Windows-only; no `.sh` counterpart) | `update-tinymce.py` | **Scope narrowed** — the `.bat` did build (`mvn ... -pl :perc-tinymce`) + copy `perc-tinymce-*.jar` to the distribution + restart Jetty. The Python port implements only the *asset-sync* leg (sync `modules/perc-tinymce/src/main/tinymce/` → `modules/perc-tinymce/src/main/resources/tinymce/`) per `cli-schemas.md` Scope 3 contract. The build-and-deploy leg is now `api-update.py --module jars` (which covers `:perc-tinymce`); the Jetty restart is operator-controlled via `--no-restart`. Documented in the script's `## Behavioral Notes`. |

## Issues

### Issue 1 — Severity: suggestion (not blocking; recorded for future work)
- **File**: `modules/perc-distribution-tree/scripts/verify-jdbc-drivers.py:159`
- **Description**: When `--workdir` is None, the script uses `tempfile.TemporaryDirectory()` (defaults to OS `tmp`). The repo convention from root `AGENTS.md` is `use ./tmp for scratch`. The original `.sh` used `mktemp -d` (also OS temp) — so the Python port matches the original behavior, not the repo convention. Same for the `Path(__file__).parents[3]/tmp/...` form, which would require ensuring `./tmp` exists on every host.
- **Suggestion**: Either (a) accept the parity-with-original behavior (current state — matches `.sh` 1:1), or (b) add a `--workdir-default-mode {system-tmp,repo-tmp}` flag for operators who want repo-local scratch. Not blocking because (i) the Python port is observable-equivalent to the `.sh` and (ii) the JUnit `VerifyJdbcDriversTest` and the Maven `exec-maven-plugin:java` execution both pass `--workdir` explicitly or rely on the system temp default that the build environment already has. For a future cleanup PR (Phase 7 polish / SC-001 sweep) consider aligning with the repo convention.
- **Status**: deferred (parity with original `.sh`; not blocking)
- **Pattern-id**: conventions.scratch-location

### Issue 2 — Severity: nit
- **File**: `modules/perc-distribution-tree/scripts/api-update.py:11`
- **Description**: Module docstring opens with `r"""Cross-platform consolidated API update helper.` — used a raw docstring to silence the `SyntaxWarning: invalid escape sequence` warning that was triggered by the original docstring's literal `\*` and `webapps\Rhythmyx\WEB-INF\lib` Windows-style paths.
- **Suggestion**: Raw docstring is the correct fix; no further action. Documented here so future edits know why the `r"""` is intentional.
- **Status**: resolved (in this PR)
- **Pattern-id**: nit.docstring-escape

### Issue 3 — Severity: suggestion (now resolved)
- **File**: `modules/perc-distribution-tree/scripts/api-update.py:419-430` (commit-uncommitted)
- **Description**: Original `run_module` had a parameter name mismatch (`maven_argv0` parameter vs `mvn_argv0` local). Python's UnboundLocalError surfaced when tests passed `maven_argv0=["mvn"]` (the `if maven_argv0 is None` branch is skipped, but the inner `mvn_argv0 = ...` assignment makes the name local for the rest of the function — accessing it before the assignment crashes).
- **Suggestion**: Rename the local to `maven_argv0` so the parameter rebinding flows through.
- **Status**: resolved (in this PR — fixed before commit)
- **Pattern-id**: nit.parameter-rebind

### Issue 4 — Severity: nit (now resolved)
- **File**: `modules/perc-distribution-tree/scripts/test_update_tinymce.py:84`
- **Description**: `setUp` wrote `tinymce.js` directly under `self.source` without first calling `self.source.mkdir(parents=True, exist_ok=True)`. `Path.write_text` does NOT auto-create parents, so every TestSync test failed with `FileNotFoundError` on the very first setUp.
- **Suggestion**: Add `self.source.mkdir(parents=True, exist_ok=True)` before any file write.
- **Status**: resolved (in this PR)
- **Pattern-id**: nit.test-fixture-setup

### Issue 5 — Severity: suggestion (now resolved)
- **File**: `modules/perc-distribution-tree/scripts/api-update.py:139-180` (commit-uncommitted)
- **Description**: Original `MODULE_COPIES` `destination_rel` values were prefixed with `modules/perc-distribution-tree/target/classes/distribution/...`, but the `_copy_artifact` function joins them onto `dist_root` which already starts with `module_dir/target/classes/distribution`. The result was a double-prefixed destination like `.../distribution/modules/perc-distribution-tree/target/classes/distribution/jetty/...`, and the rest jar never landed in `WEB-INF/lib`.
- **Suggestion**: Make `destination_rel` relative to `dist_root` (i.e. `jetty/base/webapps/Rhythmyx/WEB-INF/lib`, not `modules/perc-distribution-tree/target/classes/distribution/jetty/...`).
- **Status**: resolved (in this PR — fixed before commit)
- **Pattern-id**: paths.destination-prefix-double-count

### Issue 6 — Severity: suggestion (now resolved)
- **File**: `.github/workflows/python-build-scripts.yml:17-18, 35-36` (commit-uncommitted)
- **Description**: Workflow path-filter listed `modules/perc-distribution-tree/APIUpdate-*.bat` and `modules/perc-distribution-tree/UpdateTinyMCE.bat` so CI would trigger when those `.bat` files changed. After this PR deletes those files, the filters would dangle and GitHub would silently ignore them — no harm but no signal either.
- **Suggestion**: Drop both `APIUpdate-*.bat` and `UpdateTinyMCE.bat` path-filter entries; the `modules/perc-distribution-tree/scripts/**` filter already covers the new Python entry points and their tests.
- **Status**: resolved (in this PR)
- **Pattern-id**: ci.dangling-path-filter

### Issue 7 — Severity: nit (documentation update; resolved in this PR)
- **Files**: 6 (this PR)
  - `modules/perc-distribution-tree/AGENTS.md:69` — replaced `verify-jdbc-drivers.sh` reference with `verify-jdbc-drivers.py` + cross-platform Python port note
  - `modules/perc-distribution-tree/README.md:166, 168` — replaced both `.sh` references with `.py` references and added a sentence about the canonical Java mains
  - `modules/perc-distribution-tree/pom.xml:763, 788` — updated Maven `<execution>` comments to reference the Python ports in addition to the Java mains
  - `modules/perc-distribution-tree/src/main/resources/installDistributionFiles.xml:701` — updated ANT comment
  - `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/jdbc/BundledJdbcDrivers.java:70` — updated Javadoc
  - `modules/perc-distribution-tree/scripts/README.md` — rewritten to reference Python entry points and document the Java-main canonical + Python-port operator-facing split
- **Description**: These doc references would dangle after the `.sh`/`.bat` deletions.
- **Status**: resolved (in this PR)
- **Pattern-id**: docs.dangling-reference-after-deletion

## Behavioral tests added

| Test | Asserts |
|------|---------|
| `test_verify_jdbc_drivers.py` (24 tests) | Every documented exit code (0/1/2/3/4/5/6); argparse help/unknown-arg; CSV split; default-artifact resolution; jdbc-dir candidate discovery (bare + distribution-prefixed); jar validation (nonexistent / zero-byte / valid / corrupt); default-workdir routes through `tempfile.TemporaryDirectory` |
| `test_check_no_glob_deletes.py` (14 tests) | Every documented exit code (0/1/7); argparse help/unknown-arg; default `install.xml` resolution; missing install.xml / invalid XML / missing target / no-delete-block all surface as exit 1; star-glob and question-mark-glob both surface as exit 7; unrelated target globs are ignored (scope matches original `.sh`); only the FIRST `<delete>` block inside `install_jdbc_drivers` is inspected |
| `test_api_update.py` (13 tests) | argparse help/missing-module/unknown-module; well-known path resolution; `--dry-run` exercises the wiring for all 4 `--module` values without invoking Maven or touching the filesystem; real-run path actually copies a rest jar into the expected `WEB-INF/lib/` destination and propagates Maven's exit code; Jetty-script-missing path returns `EXIT_RESTART_FAILED` |
| `test_update_tinymce.py` (10 tests) | argparse help/unknown-arg; default paths resolve to `modules/perc-tinymce/src/main/{tinymce,resources/tinymce}`; copy is recursive; operator-placed files in target are preserved (`dirs_exist_ok=True`); stale files are overwritten; missing source exits 1; missing target is auto-created |

Full in-scope pytest collection (per `scripts/run-python-tests.sh --skip-install`):
**167 passed in 20.02s** (106 from US2+US3 + 61 new from US6).

## Documentation drift status

- **In this PR**: AGENTS.md, module README.md, scripts/README.md, pom.xml, installDistributionFiles.xml, BundledJdbcDrivers.java, GitHub workflow path filters (7 files) — all updated to reference Python entry points.
- **Out of scope for US6** (would need Phase 4 / Phase 7):
  - `modules/ai-shared-develop/src/main/resources/skills/erlang-review/SKILL.md` and similar AI skill docs that still reference `scripts\\erlang-harvest-review-patterns.bat --apply` — flagged in `994-python-build-scripts-us3-erlang.md` Issue 4.
  - No new docs-drift issues introduced by this PR beyond the in-scope ones above.

## Re-review trigger

If `mvn clean install` ever fails on `modules/perc-distribution-tree` after this PR lands, or if any new check-no-glob-deletes / verify-jdbc-drivers test starts failing on Windows or macOS, re-run Erlang on the fix pack.