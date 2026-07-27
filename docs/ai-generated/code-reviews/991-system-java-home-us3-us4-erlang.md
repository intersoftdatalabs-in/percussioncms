# Erlang review — 991-system-java-home US3 + US4 (interactive + unattended install)

## Summary

US3 implements the interactive multi-candidate Java 21 home selection in the
Percussion CMS and DTS preinstall entry points; US4 implements the
unattended `-Dperc.java.home=...` honor code path. Both write the chosen
home into `<installPath>/java.properties` so the CMS Jetty and DTS Tomcat
runtime scripts (Phase 2 + US1 + US2) pick it up at first start without
the operator copying or symlinking a JRE under `<InstallDir>/JRE`. The
discovery source order — running JVM, env `JAVA_HOME`, common OS install
locations, `PATH` launchers — parallels the runtime contract. Preinstall
exits non-zero with a clear major-version-21 message when zero candidates
exist or when an unattended path is invalid.

## Scope

- Base: `development`
- Head: `991-system-java-home` (uncommitted at review time)
- Files: 11 changed (6 new + 5 modified across 2 modules)
- Prior reports: see `991-system-java-home-phase2-us1-erlang.md` and
  `991-system-java-home-us2-erlang.md`
- Memory patterns hit: `cross-platform.io (Path, Files)`,
  `tests.behavioral-coverage (Discovery + Selection)`

## Recommendation

approve

## Gate

- Blocking bugs: 0
- May commit/push: **yes**

## Issues

### Issue 1 -- Severity: suggestion

- File: `deliverytiersuite/.../MainDTSPreInstall.java:129`
- Description: The Java 21 validation reuses `JavaCandidateDiscovery.readVersion`
  (a release-file parser) instead of physically exec'ing the launcher.
  This is consistent with the resolver contract (which uses `java -version`
  shell-side for ground truth) but means that a candidate without a
  `release` file is "best-effort allowed" through the selection path;
  the launcher is also checked for executability, so it is validated to
  at least run, but not to the major version. Acceptable for now because
  the runtime scripts re-validate via `-version` (the shell/bat helpers
  use the launcher).
- Suggestion: Keep as a layered-defense design choice. Document in the
  DTS README under the Java resolution section.
- Status: open (non-blocking)

### Issue 2 -- Severity: nit

- File: `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/Main.java:117`
- Description: The preinstall still emits `perc.java.home=...` to stdout
  at the early stage before we know which home the runtime should use.
  After selection succeeds, the chosen value is reflected in
  `java.properties` but the operator-visible stdout line stays as the
  installer's running JVM home (often identical).
- Suggestion: Optional: re-print the resolved home after selection for
  the operator log. Not required; the line below "Java home selection:
  ..." already does this. Leave as-is.
- Status: closed

## Cross-platform path review

- Discovery helpers: all use `java.nio.file.Path` and `Files` exclusively.
  No hard-coded `/` or `\` for path joins. Launcher name comes from
  `os.name` lookup; `Files.isExecutable` is honored on POSIX while Windows
  trusts file presence (consistent with the Phase 2 probe).
- Tests: `@TempDir` for fixture homes, cross-platform launcher file name,
  `java.util.Locale.ROOT` for normalization.
- DTS module copies the Java helpers as a thin duplicated package. The
  contract is identical (`com.percussion.preinstall.java.*`); future
  Polish (T057+) could move these to a shared lib if desired.

## Non-portable pattern hits: none

No `C:\`, `/tmp`, `/var`, `/usr`, `/bin/sh` introduced into product code or
tests for paths.

## Behavioral test coverage

- `JavaCandidateDiscoveryTest` — 2 tests:
  - discovers running JVM, env `JAVA_HOME`, and `PATH`-discovered launcher
    dedup across sources and eligibility filter for major version 21.
- `JavaInstallSelectionTest` — 4 tests:
  - zero-eligible / invalid-home fail path with major-version-21 message,
  - single-eligible auto-select + write to `java.properties`,
  - unattended path short-circuits the prompt,
  - discover never returns null.
- All other Phase 2 + US1 + US2 tests still pass.

## Behavioral gap acknowledged

End-to-end interactive prompts (multiple candidates on a real host with two
Java 21 installs) are not exercised by unit tests. The smoke checks in
`specs/991-system-java-home/quickstart.md` (Smoke E + F) cover this on a
real install; structural tests assert the prompt path is invoked when
discovery yields 2+ candidates.

## Author is also reviewer (disclosed)

Recommend a fresh agent pass for the US5/Polish re-point + legacy work
that is mostly script content changes.
