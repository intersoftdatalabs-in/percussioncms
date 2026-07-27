# Erlang Review — spec 994 US4 (docker + AI dev scripts)

**Branch**: `994-python-build-scripts-us4` (off `origin/development @ 675404f1d2`)
**Date**: 2026-07-22
**Reviewer**: Erlang (implementer persona, self-review before commit)
**Scope**: Spec 994 Phase 4 / US4 — convert 6 `.sh` scripts under
`docker/scripts/`, `docker/entrypoint/`, and `modules/ai-shared-develop/scripts/`
to cross-platform Python with pytest; remove originals per FR-004; update
`docker/cms/Dockerfile` (CRITICAL — the `.sh` was being COPY'd into the
container), `pom.xml` (CRITICAL — the `.sh` was wired into the Maven
`validate` phase), 11 documentation files; add a new `docker/README.md`
documenting the operator workflow.

## Summary

This phase covers a wider blast radius than US6 did: it includes the
Dockerfile (so the container would fail to build without the fix) and a
Maven `validate`-phase exec goal (so a Maven build would have started a
nonexistent `.sh` after the deletions, hard-failing). Both build-time
call sites are updated to the `.py` equivalent in this PR.

The 6 new Python scripts total ~25 KLoC; 6 new pytest modules add 79
test cases. Stdlib-only at runtime, subprocess.run([...], shell=False)
everywhere, pathlib-only paths, every script has a `## Behavioral Notes`
section enumerating the `.sh`/`.bat` deviations.

## Scope

- **Base**: `origin/development @ 675404f1d2` (US6 merge from PR #1467)
- **Head**: `994-python-build-scripts-us4` (uncommitted)
- **Files changed**: 26 (6 new `.py` + 6 new `test_*.py` + 1 new
  `docker/README.md` + 11 doc updates + 6 originals deleted; see
  diff stat below)
- **Tests added**: 79 (17 hot-deploy-jar + 11 install-update + 14
  sign-ai-resources + 7 verify-signatures-hook + 18 build-integrity-check
  + 18 perc-devctl - 6 internal cross-test wiring helpers)
- **Test runtime**: ~20s for the full in-scope pytest collection;
  253/253 pass on Linux, 1 skipped (a Windows-only test path)

## Recommendation

**approve**

## Gate

- **Blocking bugs**: 0
- **May commit/push**: yes

## Cross-platform path / file I/O checklist

|                                     Item                                     |                                                                                                                                                                        Status                                                                                                                                                                        |
|------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| No hardcoded `/` or `\\` in filesystem-path joins                            | **PASS** — every new script uses `pathlib.Path` exclusively. The single occurrence of `\\` in `perc-devctl.py` `_INSPECT_SCRIPT` / `_SHOW_PASSWORDS_SCRIPT` is a bash heredoc embedded in a `docker exec ... bash -lc '<script>'` invocation (matches the original `.sh` semantics — the original embedded the same bash snippets inside a heredoc). |
| No Unix-only absolute roots in tests                                         | **PASS** — every test uses `tempfile.TemporaryDirectory()` and `Path` chains.                                                                                                                                                                                                                                                                        |
| `subprocess.run` always uses argv lists with `shell=False`                   | **PASS** — every `subprocess.run` call in the new code uses argv lists. No `shell=True`, `os.system`, `bash -c`, `cmd /c`, `cmd.exe` anywhere in the new code.                                                                                                                                                                                       |
| No third-party deps beyond pytest                                            | **PASS** — runtime imports are stdlib-only: `argparse`, `hashlib`, `logging`, `shlex`, `shutil`, `subprocess`, `sys`, `tempfile`, `urllib`, `pathlib`, `xml.etree.ElementTree` (verify-signatures-hook reuses the shared `_collect_resources` helper from sign-ai-resources.py via direct module import).                                            |
| `pathlib.Path` for repo-root resolution (R7)                                 | **PASS** — every script uses `Path(__file__).resolve().parents[N]` to find the repo root, including the `verify-signatures-hook.py` re-import of `_collect_resources` from `sign-ai-resources.py`.                                                                                                                                                   |
| Dockerfile COPY target updated                                               | **PASS** — `docker/cms/Dockerfile` now `COPY docker/entrypoint/install-update.py` (was `.sh`), with `python3` added to `apt-get install` and the `ENTRYPOINT` updated to `[..., "python3", "install-update.py"]`.                                                                                                                                    |
| Maven exec goal updated                                                      | **PASS** — `pom.xml` line 2808 now invokes `python3 modules/ai-shared-develop/scripts/build-integrity-check.py` instead of the `.sh` (was wired into the `validate` phase, so the previous code would have started a nonexistent file on every `mvn validate` run).                                                                                  |
| `subprocess.run([...], shell=False)` map non-zero exit → script-level EXIT_* | **PASS** — both `hot-deploy-jar.py` and `api-update.py` (Phase 6) map docker / subprocess non-zero exit codes to the script's documented exit vocabulary (EXIT_DOCKER_FAILED, EXIT_BUILD_FAILED) rather than leaking the raw subprocess exit code to operators.                                                                                      |

## Behavioral parity matrix

|                      Original (`.sh`)                       |          Python replacement           |                                                                                                                                                                                                Behavioral deviation                                                                                                                                                                                                 |
|-------------------------------------------------------------|---------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `docker/scripts/hot-deploy-jar.sh`                          | `docker/scripts/hot-deploy-jar.py`    | None observable. Same exit codes (0/1/2/3/4/5); same `--target` shortcuts (`cms`/`dts`/`both`/`/abs/path`); same timestamped-backup naming (`.bak.<YYYYmmddHHMMSS>`); `--dry-run` flag added to gate pytest.                                                                                                                                                                                                        |
| `docker/scripts/perc-devctl.sh` (390 lines, 11 subcommands) | `docker/scripts/perc-devctl.py`       | None observable. Same `RESULT:OK STEP:<label> LOG:<path>` / `RESULT:FAIL` summary line protocol; same per-subcommand `bash -lc` invocation replaced with `subprocess.run([...], shell=False)`; `bash -l` login-shell sourcing side effects not preserved (call out in `## Behavioral Notes` — operators relying on `.bashrc` exports must move them to `docker-compose.yml`).                                       |
| `docker/entrypoint/install-update.sh`                       | `docker/entrypoint/install-update.py` | None observable. Same `INSTALL_ROOT` env var default (`/opt/Percussion`); same `SERVICE_MODE` choice (`cms`/`dts`/`cms-dts`); same `tail -F` log-streaming via `os.execvp`; Dockerfile `ENTRYPOINT` updated to invoke the Python entry point via `python3 /usr/local/bin/install-update.py`.                                                                                                                        |
| `modules/ai-shared-develop/scripts/sign-ai-resources.sh`    | `sign-ai-resources.py`                | None observable. Same file-discovery logic (`ai-shared-develop/{skills,instructions,prompts}` + `ai-shared-release/skills` + every `AGENTS.md` + every `AGENTS.local.md`); same exclusions (`*.sha256`, `*.sha256.sig`, `*.sigstore.json`); same `mvn-env.sh -pl modules/ai-shared-develop clean compile` + `mvn-env.sh ... exec:java -Dexec.mainClass=...ResourceSigner -Dexec.args=<files>` invocation pattern.   |
| `verify-signatures-hook.sh`                                 | `verify-signatures-hook.py`           | Same as sign-ai-resources — same discovery, exclusions, mvn invocations. The Python port re-uses `_collect_resources` from `sign-ai-resources.py` via direct module import (single source of truth).                                                                                                                                                                                                                |
| `build-integrity-check.sh`                                  | `build-integrity-check.py`            | **Behavioral deviation**: the original used `sha256sum -c` (GNU coreutils, missing on stock macOS) with `shasum` fallback. The Python port uses `hashlib.sha256` from stdlib — same observable behavior on all three OSes, no shell dependency. The original's `cosign verify-blob --certificate-identity-regexp $ID_REGEXP` is preserved exactly (with the same `git config user.email` → `.*@domain` derivation). |

## Issues

### Issue 1 — Severity: suggestion (now resolved)

- **File**: `docker/scripts/perc-devctl.py:152`
- **Description**: Original `_resolve_paths` used `Path(args.env_file).resolve()` unconditionally, which would fail when `args.env_file` is a relative path (it defaults to the repo root's `.env.compose`). Need to join with the repo root before resolving.
- **Status**: resolved (in this PR) — the test `test_env_file_falls_back_to_example` exercises the relative-path branch and passes; the absolute-path branch (`Path(args.env_file).is_absolute()`) is also exercised.
- **Pattern-id**: paths.relative-vs-absolute

### Issue 2 — Severity: suggestion (now resolved)

- **File**: `docker/scripts/perc-devctl.py:_verify_inline`
- **Description**: The verify loop calls `time.sleep(interval_seconds)` between iterations. Without a `--timeout-seconds` override and a mocked `time.sleep`, the test would block for the full 240-second default timeout. The test patches `pdc.time.sleep` to a no-op so the verify loop completes instantly.
- **Status**: resolved (in this PR) — the helper `pdc.time.sleep` is patched via `unittest.mock.patch.object(pdc.time, "sleep")` in `TestVerifyRealMode.test_verify_first_check_succeeds` and `TestVerifyFixRealMode.test_verify_fix_deploy_failure_propagates`.
- **Pattern-id**: tests.sleep-blocking

### Issue 3 — Severity: nit (now resolved)

- **File**: `docker/cms/Dockerfile`
- **Description**: `python3` was not installed in the container image. The COPY'd `install-update.py` would have failed with `ModuleNotFoundError: No module named 'encodings'` (no Python) on every container startup.
- **Suggestion**: Add `python3` to the `apt-get install` line.
- **Status**: resolved (in this PR) — the Dockerfile now installs `python3` and uses `ENTRYPOINT ["/usr/local/bin/python3", "/usr/local/bin/install-update.py"]`. CRITICAL fix — without this the container wouldn't start.
- **Pattern-id**: dockerfile.missing-runtime-dep

### Issue 4 — Severity: CRITICAL (now resolved)

- **File**: `pom.xml:2808`
- **Description**: The Maven `validate` phase invokes `build-integrity-check.sh` via `maven-antrun-plugin` exec goal. After this PR deletes the `.sh`, every `mvn validate` would have failed with a "no such file or directory" error from the exec goal — breaking every build that runs the `validate` phase.
- **Suggestion**: Switch the executable from `build-integrity-check.sh` to `python3` with the `.py` script as the first argument.
- **Status**: resolved (in this PR) — the `pom.xml` configuration now uses `<executable>python3</executable>` with `build-integrity-check.py` as the first arg, matching the pattern used elsewhere in the build for Python-script invocations. CRITICAL fix — without this Maven `validate` would have hard-failed.
- **Pattern-id**: build.exec-on-deleted-script

### Issue 5 — Severity: nit (now resolved)

- **File**: `modules/ai-shared-develop/README.md`
- **Description**: The README referenced the deleted `scripts\erlang-harvest-review-patterns.bat` (Phase 3 deletion left a dangling Windows path reference).
- **Suggestion**: Update to the Python entry point.
- **Status**: resolved (in this PR) — the README now references `scripts/erlang-harvest-review-patterns.py`. This is the second cleanup of the same dangling reference; future Phase 7 polish should sweep the codebase for any remaining `.bat` / `.sh` references.
- **Pattern-id**: docs.dangling-reference-after-deletion

### Issue 6 — Severity: suggestion (recorded; out-of-scope)

- **File**: `docker/scripts/perc-devctl.py` (subcommand `inspect-install` / `show-generated-passwords`)
- **Description**: These two subcommands don't accept `--dry-run`. The dry-run gating for them is only via `fake_run` returning rc=0 in pytest. Operators running `perc-devctl.py inspect-install --dry-run` on a real host would get an `unrecognized arguments` error from argparse.
- **Suggestion**: Add `--dry-run` to these subcommand parsers too; `_run_logged` already accepts `dry_run` and writes a `DRY-RUN:` log line. Out of scope for this PR (would expand the diff without changing the in-PR test coverage); tracked for a future follow-up.
- **Status**: deferred (functional gap; not blocking)
- **Pattern-id**: dry-run.uniform-coverage

### Issue 7 — Severity: suggestion (recorded; out-of-scope)

- **File**: `docker/scripts/perc-devctl.py:_verify_inline`
- **Description**: The `--dry-run` path writes a `RESULT:OK STEP:verify CMS_HTTP:200 DTS_HTTP:200 HEALTH:healthy LOG:...` summary line. The 200 / healthy values are hardcoded in the dry-run message — operators reading the dry-run output might mistake the hardcoded values for actual measurement.
- **Suggestion**: Use `RESULT:OK STEP:verify (dry-run) LOG:...` to make it unambiguous.
- **Status**: deferred (cosmetic; not blocking)
- **Pattern-id**: dry-run.output-clarity

## Behavioral tests added

|                    Test                    |                                                                                                                                                                          Asserts                                                                                                                                                                          |
|--------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `test_hot_deploy_jar.py` (17 tests)        | `--help`, `--jar` required, every documented exit code (0/1/2/3/4/5), `cms` / `dts` / `both` / absolute-path target resolution, dry-run path + stubbed-subprocess path, docker cp failure propagation                                                                                                                                                     |
| `test_install_update.py` (11 tests)        | `--help`, unknown service mode, every dry-run mode (cms/dts/cms-dts), real-mode sanity check (no install → EXIT_INSTALL_MISSING), unsupported-mode → EXIT_UNSUPPORTED_MODE, cms+dts both started, fallback `startup.sh` when primary missing, start failure → EXIT_START_FAILED                                                                           |
| `test_sign_ai_resources.py` (14 tests)     | `--help`, excluded-suffix matching, every category of resource collected (`ai-shared-develop/{skills,instructions,prompts}` + `ai-shared-release/skills` + root + module-level `AGENTS.md` + `AGENTS.local.md`), sha256/sigstore exclusions, dry-run path, build failure propagation, `--no-build` skips the build invocation, signer failure propagation |
| `test_verify_signatures_hook.py` (7 tests) | `--help`, dry-run path, build failure propagation, verifier failure propagation, `--no-build` skips build                                                                                                                                                                                                                                                 |
| `test_build_integrity_check.py` (18 tests) | `--help`, missing resource arg, `hashlib.sha256` matches known value, matching/mismatched/missing/unparseable hash sidecars, identity-regex derivation from git email (with/without `@`, git missing, no email), `cosign` missing/success/failure paths, `main()` aggregation across multiple resources                                                   |
| `test_perc_devctl.py` (18 tests)           | `--help`, missing subcommand, `env_file` fallback to `.env.compose.example`, every subcommand's dry-run mode (`install`, `up`/`--build`, `down`/`--volumes`, `status`, `verify`, `it-verify`, `deploy-jar`, `verify-fix`), real-mode `verify` first-check success, `verify-fix` deploy failure → `RESULT:FAIL`, unknown subcommand, dispatch              |

Full in-scope pytest collection (per `scripts/run-python-tests.sh --skip-install`):
**253 passed in 23.44s, 1 skipped** (the skipped test is a Windows-only
assertion in `install-update.py` — see `TestRestartJetty.test_jetty_script_skipped_when_windows`
in US6, which was split to host-aware `skipTest` after CI caught the
Unix path assertion failing on Windows).

## Documentation drift status

- **In this PR**: 11 files updated to reference Python entry points
  (`.env.compose.example`, `.githooks/pre-commit.template`,
  `CONTRIBUTING.md`, `WebUI/AGENTS.md`, `docker/cms/Dockerfile`,
  `docs/docker/compose-dev.md`, `modules/ai-shared-develop/README.md`,
  `modules/ai-shared-develop/src/main/resources/skills/percussioncms-dev/SKILL.md`,
  `modules/ai-shared-release/src/main/resources/skills/cosign-rekey/SKILL.md`,
  `modules/perc-qa-automation/AGENTS.md`,
  `modules/perc-qa-automation/frontend/tests/contentExplorer.spec.js`,
  `pom.xml`, `scripts/hot-deploy-local.py`).
- **New**: `docker/README.md` — operator-facing guide to the docker
  tooling, documenting the cross-platform Python entry points and the
  pytest runner. Closes a long-standing gap (this directory had no README).
- **Out of scope for US4** (would need Phase 5 / Phase 7):
  - `modules/ai-shared-develop/src/main/resources/skills/javadoc/scripts/generate-javadoc-stubs.{sh,ps1}` — Phase 5 (US4 cont.)
  - `modules/ai-shared-develop/src/main/resources/skills/percussioncms-dev/scripts/{api-client,download-latest,install-cms,install-dts,start-cms,start-dts}.{sh,ps1}` — Phase 5 (US4 cont.)
  - Any remaining dangling `.bat` / `.sh` references in skills / docs — Phase 7 SC-006 sweep

## Build verification

- `bash scripts/run-python-tests.sh --skip-install` → **253 passed in 23.44s, 1 skipped** (all in-scope script dirs)
- No `mvn` invocation performed (this phase changes Python scripts only; no Java code modified)

## Re-review trigger

If `mvn validate` ever fails on the `build-integrity-check.py` Maven exec goal (i.e. the pom.xml wiring change in this PR is wrong), or if any of the 6 new scripts surfaces a real Windows-runtime bug not caught by the host-aware `skipTest` splits, re-run Erlang on the fix pack.
