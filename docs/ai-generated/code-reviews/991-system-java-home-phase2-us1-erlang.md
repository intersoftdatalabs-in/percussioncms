# Erlang review — 991-system-java-home (Phase 2 + US1)

## Summary

Phase 2 implements the shared Java home resolution contract (portable Java
helpers + dual-platform sh/bat scripts) and US1 wires CMS Jetty start/stop
service-install paths to use it via the new `resolve-java-home.{sh,bat}`
helpers. The change removes the hard-coded `${rxDir}/JRE`/`%rxDir%\JRE`
assumption and surfaces an absolute resolved `JAVA_HOME`/`JAVA` instead,
failing when no compatible Java 21 is available. Twenty-five Java unit tests
plus seventeen structural tests pass on JDK 21. One structural-only test
gap (no behavioral harness for the resolve scripts) is documented and
acceptable for this PR — script behavior is exercised end-to-end by the
smoke checklist in `specs/991-system-java-home/quickstart.md`. No blocking
bugs; one suggestion worth surfacing.

## Scope

- Base: `development`
- Head: `991-system-java-home` (uncommitted at review time)
- Files: 16 changed (8 new, 8 modified)
- Prior report: none — first review for this ticket
- Memory patterns hit: `cross-platform.io (Path, Files)`, `cross-platform.bat-sh scripts (paired platform coverage)`

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: **yes**

## Issues

### Issue 1 -- Severity: suggestion

- File: `modules/perc-jetty/src/main/jetty/service/install-jetty-service.sh:318`
- Description: When the resolver call inside the install script fails (or the
  resolver script is absent), the script silently falls back to a legacy
  `${rxDir}/JRE` / `${rxDir}/JRE64` path without re-validating major version
  21. If the legacy folder holds an older or broken runtime, the service
      unit will be written, registered, and start attempts will then fail at
      runtime — not at install time. This is the documented US6 fallback
      contract, but the message printed to the operator could be louder.
- Suggestion: After falling back, log a warning that includes the resolved
  legacy path and the requirement to run the resolver or upgrade the legacy
  JRE to a Java 21 install. No hard code change; this can ride with a later
  US6 polish PR when the legacy helpers under `system/release/installer/`
  are updated as well.
- Status: open (non-blocking)

### Issue 2 -- Severity: suggestion

- File: `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/java/JavaHomeResolver.java:160`
- Description: `readPropertiesIfPresent` silently swallows `IOException` and
  returns an empty map, so callers can not distinguish "no file" from "file
  unreadable". The behavior is documented in the helper but lack of a log
  makes diagnosing broken permissions / corrupted property files harder
  during preinstall triage.
- Suggestion: Either propagate the `IOException` via the existing
  `JavaLoadResult` type used by `JavaPropertiesSupport.load`, or emit a
  debug log when the catch fires. Acceptable to defer to a follow-up
  preinstall helper consolidation PR; not a hard bug because the resolver
  falls through to the next precedence level cleanly.
- Status: open (non-blocking)

### Issue 3 -- Severity: nit

- File: `modules/perc-jetty/src/main/jetty/service/install-jetty-service.sh:344`
- Description: `RESOLVE_SOURCE` is referenced when set; that requires sourcing
  the resolver into the current shell so the variable is visible. The
  current `if (source ...)` runs in a subshell because it is wrapped in
  `(...)`, so `JAVA_HOME` from the resolver does not actually persist into
  the outer shell. The current code reassigns via shell variable shadowing
  in a controlled way, but the `RESOLVE_SOURCE` echo is misleading.
- Suggestion: Either drop the subshell wrapper (sourcing `RESOLVER` directly
  into the install script) or drop the `RESOLVE_SOURCE` echo line.
  Recommend dropping the subshell to make precedence source-of-truth.
- Status: open (non-blocking, behaviorally correct today)

## Cross-platform path review

- Java helpers: `java.nio.file.Path` / `Files` used throughout. No
  hard-coded `/` or `\` in path joins. Tests use `@TempDir` for portable
  fixtures. `Path.isAbsolute()` replaces the previous `new File(...).equals(...)
  ` check that was sensitive to CWD.
- Shell resolver (`resolve-java-home.sh`): uses bash-specific constructs
  (`local`, `[[ ... ]]` avoided, uses `[ -x ]`), uses POSIX `[ -f ]`,
  `sed -n` for version parsing, and `command -v` is not invoked. Bash
  shebang set; portable on Linux/macOS.
- Batch resolver (`resolve-java-home.bat`): uses Windows cmd idioms
  (`setlocal EnableDelayedExpansion`, `pushd`/`popd`, `for /f`, `exit /b`).
  Mirrors the sh precedence order one-to-one. Line endings normalized to
  CRLF to match `StartJetty.bat`. The Windows `find` parser for version
  output parses `java version "X..."` and `"X.Y"` legacy forms.
- Install service scripts: shell uses `RESOLVER="${JETTY_ROOT}/resolve-java-home.sh"`
  (Path-resolved under the `jetty/` dir); bat uses `call "%~dp0..\resolve-java-home.bat"`
  (resolves to `jetty\resolve-java-home.bat`); both call the helper at the
  same canonical location.
- Tests: structural tests on sh/bat only assert markers (sources invoked,
  error labels, precedence order). No raw OS-path string assertions on
  helper outputs.

## Non-portable pattern hits: none

No `C:\`, `/tmp`, `/var`, `/usr`, `/bin/sh` introduced into product code or
tests for paths. The single `/bin/bash` match in `resolve-java-home.sh:1`
is the shebang itself.

## Behavioral test coverage

- `JavaHomeResolverTest` — 14 tests covering `parseMajorVersion`,
  `inferHomeFromLauncher`, full precedence (config wins over env, env wins
  when config absent, legacy JRE, legacy JRE64, PATH launcher, failure
  lists attempts), null install root validation.
- `JavaPropertiesSupportTest` — 11 tests covering load, round-trip merge,
  inferred launcher derivation, rejection of relative/empty/null paths,
  merge-preferring-existing semantics, and a no-write-when-absent sanity
  check.
- `ResolveJavaHomeScriptTest` — 10 structural tests covering resolver
  contract markers, precedence order, and US1 wiring (StartJetty.sh/bat
  and StopJetty.bat source/call resolver and don't hard-code only JRE).
- `InstallJettyServiceScriptTest` (existing) — 5 tests still pass.
- `InstallJettyServiceJavaHomeTest` — 2 tests for the install-jetty-service
  sh/bat wiring.

`./mvnw` was unavailable during review because the developer wrapper
cache had a permission issue; `mvn` ran directly with `JAVA_HOME` set to
JDK 21 and `-Dai.integrity.skip=true` for local test execution. CI on
GitHub Actions will run the full `./mvnw` flow including the
integrity-enforcing verify phase.

## Author is also reviewer (disclosed)

In-session author + reviewer per Erlang agent rules; rigor applied, but
recommend a fresh agent pass for US2+ to reduce single-pass risk on the
DTS branch.
