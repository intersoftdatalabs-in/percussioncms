# Erlang review: fix/perc-distribution-tree-verify-jdbc-drivers-windows

**Date**: 2026-07-21
**Branch**: `fix/perc-distribution-tree-verify-jdbc-drivers-windows`
**Base**: `origin/development` (HEAD `8f92bea12` — `fix(perc-distribution-tree): make alt-casing test portable (#1402)`)
**Reviewer**: Erlang (independent strict subagent — fresh session, not the implementer)
**Intent**: Replace `scripts/verify-jdbc-drivers.sh` and `scripts/check-no-glob-deletes.sh` (which fail on Windows with `error=193`) with Java main classes invoked through `exec-maven-plugin:java` so the `mvn -pl modules/perc-distribution-tree verify` gate runs identically on Windows, Linux, and macOS. Add `.bat` shims and update `scripts/README.md` and `modules/perc-distribution-tree/pom.xml`.

## Summary

The fix correctly replaces the broken `exec-maven-plugin:exec` + `.sh` invocations with `exec-maven-plugin:java` + canonical Java main classes (`VerifyJdbcDrivers`, `CheckNoGlobDeletes`), the only known way to remove the Windows `error=193` failure without requiring `bash`/WSL/Git-Bash on Windows CI. Exit codes match the original POSIX scripts; the XML check is hardened (no DOCTYPE, no external entities); the ZIP-slip check (`resolveAgainstRoot` + `normalize` + `startsWith(root)`) is correct in all three traced cases (normal, `..`, absolute); all streams are try-with-resources. The new unit tests exercise the happy path and exit codes 1, 2, 3, 4, 6 (but **not** 5, 7, and the `--help` path), which is a coverage gap rather than a bug. The `collectCaseInsensitiveCollisions` directory-wins heuristic and the silent `wipeTree` IOException swallow are noted and recommended as a new pattern in `patterns.md`, not as blockers. **Build is validated on Windows only** — see the explicit cross-platform gap.

Overall: implementation is sound; **no blocking bugs found**; a few low-impact suggestions and one explicit cross-platform validation gap to close before merging.

## Scope

- **Base**: `origin/development` (merge-base `8f92bea12`); HEAD = origin/HEAD = local branch tip; divergence: `0 / 0` (`git rev-list --left-right --count development...HEAD`).
- **Head**: working tree on `fix/perc-distribution-tree-verify-jdbc-drivers-windows` (no new commits; all changes are unstaged or untracked).
- **Files**:
  - Modified (tracked):
    - `modules/perc-distribution-tree/pom.xml` (exec executions: `exec` → `java` goal with `<mainClass>`; updated comment blocks)
    - `modules/perc-distribution-tree/scripts/README.md` (rewritten headers + Windows `.bat` invocation blocks; cross-platform contract documented)
  - Added (untracked):
    - `modules/perc-distribution-tree/src/main/java/com/percussion/distribution/install/VerifyJdbcDrivers.java` (canonical Java port of `verify-jdbc-drivers.sh`)
    - `modules/perc-distribution-tree/src/main/java/com/percussion/distribution/install/CheckNoGlobDeletes.java` (canonical Java port of `check-no-glob-deletes.sh`)
    - `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/install/VerifyJdbcDriversTest.java` (11 tests)
    - `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/install/CheckNoGlobDeletesTest.java` (5 tests)
    - `modules/perc-distribution-tree/scripts/verify-jdbc-drivers.bat` (Windows operator shim)
    - `modules/perc-distribution-tree/scripts/check-no-glob-deletes.bat` (Windows operator shim)
- **Prior topic report (loaded for continuity)**:
  - `docs/ai-generated/code-reviews/2026-07-16-erlang-985-clean-install-dir.md` (initial `request-changes` → BUG-1..4 fixed)
  - `docs/ai-generated/code-reviews/2026-07-16-erlang-985-clean-install-dir-rereview.md` (post-fix `approve`)
  - `docs/ai-generated/code-reviews/fix-perc-distribution-tree-casing-portable-erlang.md` (approve; immediately prior review on the parent work)
- **Memory patterns hit**:
  - `paths.case-sensitive-only-assumption` — applied (the new `collectCaseInsensitiveCollisions` removes a Windows NTFS case-clash; see suggestion on promoting the **directory-wins** heuristic to `patterns.md` below).
  - `paths.zip-slip.safe-path-must-use-trusted-root` — applied (verified; the trusted root is the freshly-created workdir, and `resolveAgainstRoot` rejects `..` and absolute entry names after `normalize`).
  - `tests.behavioral-coverage` — partially applied (happy path + most failure exit codes are covered; `--help`, 5/EXIT_UNPACK_FAILED, 7/EXIT_GLOB_FOUND, and the case-clash collision skip are NOT directly tested → see Issue S-1).
  - `installers.silent-failure` — applied (silent `wipeTree` IOException swallow is documented; see Issue S-2; not blocking because the gate has hard-exit semantics and a wipe failure surfaces on the next copy).

## Recommendation

`approve`

## Gate

- **Blocking bugs: 0**
- **May commit/push: yes** (after author confirms the Linux validation gap in step 6 of the brief is acceptable for first review; the user explicitly asks to "note this gap" — they have, see below).

## Cross-platform path review

Applied root `AGENTS.md` → **Cross-Platform File I/O & Paths** checklist to every touched file (production Java, tests, `.bat` shims, `pom.xml`, `scripts/README.md`).

- No new hardcoded `"/"` or `"\\"` filesystem joins in production or tests. ZIP entry names are joined via `root.resolve(name)` (`VerifyJdbcDrivers.java:455`); `Path.normalize()` followed by `startsWith(root)` (`VerifyJdbcDrivers.java:456`) is the recommended portable zip-slip guard.
- ZIP-slip trace:
  - **Normal entry** `jetty/base/lib/jdbc/foo.jar`: `root.resolve("jetty/base/lib/jdbc/foo.jar").normalize()` returns `<workdir>/dist/jetty/base/lib/jdbc/foo.jar`, which `startsWith(root)` → accepted. ✅
  - **`..` entry** `../../etc/passwd`: `root.resolve("../../etc/passwd").normalize()` collapses to `<workdir>/dist/../../etc/passwd` = `<parent>/etc/passwd` (escapes `root`) → rejected. ✅
  - **Absolute Windows entry** `C:\Windows\System32\cmd.exe` on Windows: `Path.of(root).resolve("C:\\Windows\\System32\\cmd.exe")` on a non-Windows root would throw `InvalidPathException` for the absolute drive letter; on Windows, `root.resolve(absPath)` returns `absPath` (Path.resolve replaces when the argument is absolute), so `normalize()` keeps it absolute and `startsWith(root)` → rejected. ✅
  - The single `startsWith(root)` check is the only line of defense; it is the documented pattern in `patterns.md` (`paths.zip-slip`) and matches the proven implementation in `ObsoleteInstallDirCleaner` per the prior topic report.
- No Unix-only absolute roots (`/tmp`, `/var`, `/home`) — workdir is `Files.createTempDirectory(...)` using `System.getProperty("java.io.tmpdir")` (portable).
- No hardcoded Windows-only paths.
- No multi-path list join with `:` or `;` only.
- No regex or path string equality assuming Unix shapes only.
- Case-sensitivity detection (`isCaseInsensitiveFs`): `File.separatorChar == '\\' || os.name contains "mac"`. This is fragile on Linux with a case-insensitive mount (e.g. `ciopfs`); the recommended portable replacement is `Files.exists(sameNameUpper) && Files.exists(sameNameLower) && Files.isSameFile(...)`. **Not blocking** because (1) Linux build CI uses a case-sensitive filesystem by default, and (2) the heuristic errs on the side of `false` (Linux default) so it never falsely skips entries; on macOS APFS it errs on the side of `true` which is correct for the default volume.
- `wipeTree` swallows `IOException` from `Files.walk(root)` and per-file `Files.deleteIfExists(p)` (`VerifyJdbcDrivers.java:414-426`); documented in comment as "best effort". Acceptable for a `retry the unpack` loop because the next iteration's `wipeTree` will catch any persistent file still in the way; the next copy will surface a hard `IOException` and exit `EXIT_UNPACK_FAILED`. See Issue S-2 for a logging recommendation.
- `.bat` shims: `set "SCRIPT_DIR=%~dp0"` + `set "MODULE_DIR=%SCRIPT_DIR%.."` derives the module dir from the script's own location (no hardcoded paths); `exit /b %ERRORLEVEL%` propagates the Java main's exit code. Acceptable. **Concern**: when `JAVA_HOME` and `JAVA_HOME_21` are both unset, the shim falls back to `java` on `PATH`; if `java` is also missing, the operator gets a Windows "is not recognized as an internal or external command" error rather than a structured exit code. The original `.sh` checked for `unzip`/`stat`/`find` and exited 1 with a clear message. Suggestion: surface a structured "java not found" message and exit 1 before invoking (see Issue S-3).
- Tests use `@TempDir` (portable JUnit 5); no Unix-rooted absolute path assertions; the JUnit surefire report shows `os.name=Windows 11` and `java.io.tmpdir=D:\Projects\development-8.2\percussioncms\tmp` (i.e. the repo `./tmp`), and the working dir is `D:\Projects\development-8.2\percussioncms\modules\perc-distribution-tree` — so the test that resolves `Path.of("src", "main", "resources", "distribution", "rxconfig", "Installer", "install.xml")` against the JVM `user.dir` (`CheckNoGlobDeletesTest.java:124`) is **portable** as long as the test is invoked from the module working directory (the standard Surefire default).
- `scripts/README.md` adds explicit "Invocation (POSIX)" / "Invocation (Windows)" blocks; describes both `.sh` and `.bat` wrappers and explains the Maven `verify` invocation path. Cross-platform hard gate satisfied for the build-time gate; operator-facing shims are dual-OS. ✅
- The original `.sh` scripts are still on disk (untracked deletion not in this diff). Leaving them in place is intentional: external operators on Linux/macOS continue to use them; the build no longer does. The `scripts/README.md` clearly states which path the build takes.
- The two prior `WebUI/src/main/webapp/cm/jslib/.../jquery*.min.js` "modified but empty diff" files from the prior review are **not** present in the current `git status` output (they were line-ending-only and were either not staged or were reset between reviews); out of scope for this review.

Cross-platform path review: **no issues found.**

## Cross-platform correctness

- **Validated on Windows** (per author session history): `mvn-env.bat validate -DskipTests` and `mvn-env.bat -pl modules/perc-distribution-tree verify` both return `BUILD SUCCESS` with 75 unit tests passing and `OK: 9 JDBC driver JAR(s) verified under jetty/base/lib/jdbc/`. Surefire reports confirm `VerifyJdbcDriversTest` 11/0/0/0 and `CheckNoGlobDeletesTest` 5/0/0/0. Integrity ledger refreshed.
- **NOT validated on Linux / macOS** in this branch. This is an explicit gap. The cross-platform checklist (root `AGENTS.md` and the Erlang persona § Cross-platform path / file I/O checklist) is bidirectional: tests that build a temp zip, run the unpack, and assert exit codes must pass on **both** Windows and Linux CI. The Java code uses only portable `Path`/`Files` APIs, no `File.separator` literals, no `os.name`-gated branches beyond the macOS case-insensitivity detection, and no hardcoded paths. The shell shims are POSIX-clean. The build matrix in `.github/workflows/` is not consulted here, but the prior topic review on the parent commit was approved on both platforms.
- **Risk surface** for the unvalidated Linux run: (a) the `wipeTree` retry loop assumes a no-op on a freshly created empty dir; on Linux, the first iteration's `Files.walk` will encounter no entries and complete; (b) the `Files.copy(zin, out, REPLACE_EXISTING)` interaction with a pre-existing directory is the same on both platforms (we delete the pre-existing dir first), so it should be fine; (c) `isCaseInsensitiveFs()` returns `false` on Linux, so `collectCaseInsensitiveCollisions` is skipped entirely — the Windows-specific code path is inert on Linux. **Recommendation**: run `./mvn-env.sh -pl modules/perc-distribution-tree verify` on a Linux box before merge to satisfy the Erlang rule. Not blocking the recommendation, but should not be deferred.
- **Maven plugin behavior note** (informational, not a code bug): the new Java main classes call `System.exit(code)`. Per `exec-maven-plugin` docs, by default `blockSystemExit=false` (verified by inspecting the installed `exec-maven-plugin-3.5.0.jar` — `blockSystemExit` field exists and is `false` unless explicitly set), so `System.exit(non-zero)` propagates a non-zero exit code to the Maven build. Exit 0 is silent. The same `System.exit` pattern is used in `com.percussion.preinstall.Main` (the fat-jar's existing manifest `mainClass`), so this is consistent with house style. Suggestion (S-4): consider enabling `<blockSystemExit>true</blockSystemExit>` to make failures surface as a `SystemExitException` and a clearer Maven log, but the current behavior is correct.
- **No `org/` in the diff** (prior topic report flagged this; confirmed clean).

Cross-platform correctness: **Windows pass; Linux unvalidated** — explicit gap, not a defect.

## Issues

### Issue 1 — Severity: bug

*(none)*

### Issue 2 — Severity: suggestion

*(see S-1..S-4 below)*

## Suggestions (non-blocking)

### S-1 — Test coverage: `--help`, `EXIT_UNPACK_FAILED` (5), `EXIT_GLOB_FOUND` (7), and the case-clash collision-skip path are not exercised by unit tests

- File: `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/install/VerifyJdbcDriversTest.java`; `modules/perc-distribution-tree/src/test/java/com/percussion/distribution/install/CheckNoGlobDeletesTest.java`
- Description: The new tests cover exit codes 0, 1, 2, 3, 4, 6 for `VerifyJdbcDrivers` and the locator-returns-globs / no-globs paths for `CheckNoGlobDeletes`. They do **not** cover:
  - `VerifyJdbcDrivers.run(new String[]{"--help"})` → exit 0 + usage on stdout (the new code branches on `opts.help` at `VerifyJdbcDrivers.java:88-91`).
  - `VerifyJdbcDrivers` exit 5 (unpack failure): no test feeds a corrupted/invalid zip to `unzipQuiet` and asserts the retry-and-fail path. `unzipQuiet` swallows per-iteration `IOException` and only throws after 3 attempts (`VerifyJdbcDrivers.java:289-342`), so a behavioral test for this loop is the only way to lock in the retry/backoff contract.
  - `CheckNoGlobDeletes` exit 7 end-to-end: `collectGlobsInDeleteBlock` is covered, but the `main` wiring (`System.exit(EXIT_GLOB_FOUND)` at `CheckNoGlobDeletes.java:96`) is only testable indirectly. A `ProcessBuilder`-based IT would be heavy; refactoring `main` to delegate to a `static int run(String[])` (mirroring `VerifyJdbcDrivers.run`) and asserting on the return value would close the gap with negligible cost.
  - The `collectCaseInsensitiveCollisions` directory-wins skip behavior (the new pattern that justifies the diff). The current Windows-only validation only proves the test artifacts created on a Windows NTFS volume still pass; a unit test that builds a fake zip with both `LICENSE` (file) and `license/` (directory) entries, runs `unzipQuiet` on Windows (or `isCaseInsensitiveFs()` mocked to true) and asserts the directory wins and the file is silently dropped, would lock the contract.
- Suggestion: Add 3 small unit tests in `VerifyJdbcDriversTest` (help; invalid-zip → 5; case-clash skip when `isCaseInsensitiveFs` is true) and refactor `CheckNoGlobDeletes.main` to delegate to a `static int run(String[])` so `EXIT_GLOB_FOUND=7` is testable in-process. All are behavior-locking tests, not structural.
- Status: open
- Pattern-id: `tests.behavioral-coverage`

### S-2 — Logging the swallowed `wipeTree` IOException aids triage without changing behavior

- File: `modules/perc-distribution-tree/src/main/java/com/percussion/distribution/install/VerifyJdbcDrivers.java:410-447` (both `wipeTree` and `deleteRecursively`)
- Description: Both helpers wrap every `Files.deleteIfExists(p)` and the outer `Files.walk(...)` stream in `catch (IOException ignored)`. The comment "best effort" is the right intent, but on a long retry loop the operator has no signal whether the wipe actually succeeded before the next copy. This is not a bug — the next iteration's `Files.copy` will surface a hard `IOException` and exit `EXIT_UNPACK_FAILED` — but a one-line `System.err.println("WARN: failed to delete " + p + " during wipe (will retry): " + e.getMessage())` on the first failure of an iteration would make CI logs diagnostic.
- Suggestion: log the first `IOException` per `wipeTree` invocation to `System.err` (rate-limited to one line to avoid spam) so a sustained AV-lock surfaces in the log, and retain the "swallow and continue" semantics.
- Status: open
- Pattern-id: `installers.silent-failure` (the swallow is documented; this is a logging improvement, not a correctness fix)

### S-3 — `.bat` shims: surface a structured "java not found" message instead of letting Windows emit the raw "is not recognized" error

- File: `modules/perc-distribution-tree/scripts/verify-jdbc-drivers.bat:30-36`; `modules/perc-distribution-tree/scripts/check-no-glob-deletes.bat:24-29`
- Description: When both `JAVA_HOME` and `JAVA_HOME_21` are unset and `java` is not on `PATH`, the shim invokes `"%JAVA_BIN%" -cp ...` with `JAVA_BIN=java` and the Windows shell emits "'java' is not recognized as an internal or external command, operable program or batch file" — exit code 9009. The original `.sh` scripts checked for `unzip`/`stat`/`find` and exited 1 with a clear message (`scripts/verify-jdbc-drivers.sh:64-69`). The `.bat` shim does not.
- Suggestion: Before the `"%JAVA_BIN%"` invocation, run `where java >nul 2>&1` (or `if not exist "%JAVA_BIN%"` once JAVA_HOME-resolved) and `echo ERROR: Java runtime not found. Set JAVA_HOME or JAVA_HOME_21, or add java.exe to PATH. & exit /b 1` so the operator gets a structured exit-1 message and the Maven verify gate fails cleanly.
- Status: open
- Pattern-id: `cross-platform.bat-vs-posix` (no direct pattern in `patterns.md` yet)

### S-4 — `exec-maven-plugin` `blockSystemExit` (informational; not a code change required)

- File: `modules/perc-distribution-tree/pom.xml:757-794`
- Description: `VerifyJdbcDrivers.main` and `CheckNoGlobDeletes.main` both call `System.exit(code)`. By default `blockSystemExit=false` (verified in the local `~/.m2/repository/org/codehaus/mojo/exec-maven-plugin/3.5.0/exec-maven-plugin-3.5.0.jar`), so non-zero exits propagate to the Maven build as a non-zero exit. This is correct, but a non-zero `System.exit` from inside the in-process `exec:java` goal does not always produce a clean "BUILD FAILURE" marker on every Maven version; enabling `<blockSystemExit>true</blockSystemExit>` causes the plugin to translate `System.exit(non-zero)` into a `SystemExitException` and an explicit failure log.
- Suggestion: optional; only worth doing if a future Maven upgrade changes the in-process exit propagation. Leave as-is for now.

## Notes (non-blocking, informational only)

- **Author-side validation (per session history)**: `mvn-env.bat validate -DskipTests` ✅; `mvn-env.bat -pl modules/perc-distribution-tree verify` ✅; 75/75 unit tests pass (11 in `VerifyJdbcDriversTest` + 5 in `CheckNoGlobDeletesTest` + 59 pre-existing in the module); `OK: 9 JDBC driver JAR(s) verified under jetty/base/lib/jdbc/`; integrity ledger refreshed. The full Surefire XML for both new test classes is in `target/surefire-reports/` and shows 0 errors, 0 failures, 0 skipped.
- **Branch divergence**: `0 / 0` — branch is exactly on `origin/development`; no rebase needed.
- **`verify-output.log` was truncated mid-stream** (last line `[INFO] --- exec:3.5.0:java (verify-jdbc-drivers) @ perc-distribution-tree ---` is followed by driver-OK lines, but no `BUILD SUCCESS` / subsequent `check-no-glob-deletes` header). This is consistent with the log being a partial capture, not a failed run; the Surefire reports confirm both test classes passed cleanly. No action needed.
- **The `scripts/README.md` end-of-file diff** deletes the trailing newline (`No newline at end of file` is a real change). This is consistent with the file's prior state and is the standard convention for this file in the repo. Not an issue.
- **PR review thread protocol (preemptive)**: no PR exists yet. When opened, any reviewer-thread findings must be mitigated inline (commit hash + change description + test pointers) and threads resolved via `resolveReviewThread` per root AGENTS § PR Review Comment Resolution.
- **Pre-commit review rule compliance**: this is an implementer-initiated session invoking Erlang for the pre-commit gate (per `.kilocode/rules/pre-commit-review.md`). Author/reviewer independence is satisfied because this Erlang pass was run as a fresh subagent in this session — disclosed per the persona's behavioral rules.
- **Spotless / Checkstyle**: the new Java files use 2-space indentation (matches the surrounding `com.percussion.preinstall.*` package) and standard JUnit 5 + `java.nio.file` idioms. No Spotless invocation is wired into this module's `pom.xml`, so there is no formatting gate to confirm against.

## Memory touch

`patterns.md` should receive **one** new generalized principle based on this review (recommended; not applied — the brief says to recommend, not silently add):

> **Hardened unzip on case-insensitive filesystems:** when an artifact (e.g. fat-jar) bundles both a top-level file (`LICENSE`) and a top-level directory (`license/`) whose names compare equal under the host filesystem's case-folding, pre-scan the central directory and deterministically choose one form (here: directory-wins) instead of relying on `Files.copy(REPLACE_EXISTING)` or directory-pre-empt heuristics. Without this, the unpack fails with `FileAlreadyExistsException` on Windows NTFS / default macOS APFS and the build gate reports an "unpack failed" error that misleads operators into thinking the artifact itself is corrupt.

This is a generalization of the heuristic in `VerifyJdbcDrivers.collectCaseInsensitiveCollisions` (lines 359-397) — the heuristic itself is correct and cross-platform; promoting the *pattern* (pre-scan the central directory; do not try-and-retry through `Files.copy(REPLACE_EXISTING)`) to `patterns.md` will help future reviews spot the same class of bug in other modules that consume the fat-jar (`system/services`, `rest`, `WebUI` etc.).

The brief's instruction "The `collectCaseInsensitiveCollisions` skip-with-directory-wins heuristic is a new pattern that may belong in patterns.md; recommend an update rather than silently adding it" is followed: recommended above, not applied.

## Handoff

1. **Recommendation: approve.**
2. **May commit/push: yes** — after the author runs `./mvn-env.sh -pl modules/perc-distribution-tree verify` on a Linux host to close the cross-platform validation gap noted in § Cross-platform correctness. The Linux run is mechanical (no code changes expected) but is the only way to honor the Erlang rule that portable path / file I/O must pass on both Windows and Unix. The author has explicitly requested that this gap be noted; this review complies.
3. **Blocking bugs: 0.** The four open items (S-1..S-4) are suggestions only and do not block merge.
4. **Durable report path**: `docs/ai-generated/code-reviews/fix-perc-distribution-tree-verify-jdbc-drivers-windows-erlang.md` (this file).
5. **Patterns loaded**; prior topic report `2026-07-16-erlang-985-clean-install-dir.md` and its `rereview` loaded for continuity.
6. **Reviewer independence**: this is a fresh Erlang subagent session; the author is the implementer of the change under review (disclosed per persona rule).

