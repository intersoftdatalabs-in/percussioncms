# Erlang review: #2220 perc-doctor dist packaging + install guide

**Branch:** `feat/issue-2220-perc-doctor-dist-packaging` (stacked on `feat/issue-2218-clean-logs`)  
**Date:** 2026-08-06  
**Reviewer persona:** Erlang (pre-commit gate)

## Summary

Adds operator distribution packaging for `perc-doctor`: Unix + Windows `bin` launchers, stable `bin/perc-doctor.jar`, Maven assembly classifier `dist` zip, `perc-distribution-tree` unpack wiring, `install.xml` upgrade.overwrite lockstep for extensionless Unix launcher + jar, operator install guide with dry-run-first examples, and structural packaging tests in both modules.

## Scope

|         Area          |                                            Paths                                            |
|-----------------------|---------------------------------------------------------------------------------------------|
| perc-doctor packaging | `modules/perc-doctor/pom.xml`, `src/main/scripts/*`, `src/main/assembly/dist-bin.xml`       |
| perc-doctor docs      | `modules/perc-doctor/README.md`, `docs/operator-install-guide.md`                           |
| perc-doctor tests     | `PercDoctorPackagingTest.java`                                                              |
| dist-tree             | `modules/perc-distribution-tree/pom.xml`, `install.xml`, `PercDoctorDistPackagingTest.java` |

Prior report / Memory: no prior #2220 report. Patterns: packaging lockstep tests (JettyServiceDualShip, BundledGcmNatives), cross-platform paths, no hardcoded user homes.

**Cross-platform path review:** Launchers resolve install root from script directory (`%~dp0` / `BASH_SOURCE`), not hardcoded homes. Docs use generic `/opt/Percussion` and `C:\Percussion`. Assembly sets Unix line endings + `0755` on shell script and Windows line endings on `.bat`. Packaging tests forbid personal profile path shapes. Install tree layout is `<install-root>/bin/*` on both platforms.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

- No bug findings
- Behavioral/structural packaging tests present for new packaging logic
- Portable path I/O in launchers and docs
- Module clean installs: `modules/perc-doctor` BUILD SUCCESS (Tests run: 53); `modules/perc-distribution-tree` BUILD SUCCESS including `PercDoctorDistPackagingTest` and full assembly with `bin/perc-doctor{.bat,.jar}` present

## Issues

_None at bug severity._

### suggestion

1. **`modules/perc-doctor/src/main/scripts/perc-doctor.bat`** — `for %%A in (%*)` flag scan can mishandle unusual quoting; acceptable for operator flags like `--install-root`. No change required for v1.

2. **dependency:analyze unused `perc-doctor` zip** — same class as existing `perc-service-wrapper` provided packaging deps; pre-existing warn style.

### nit

1. Assembly `dir` format cannot be attached to install (Maven warning only); zip classifier is what dist-tree consumes.

## Evidence

```text
cd modules/perc-doctor && ../../mvnw clean install
# Tests run: 53, Failures: 0 — BUILD SUCCESS
# attaches perc-doctor-*-dist.zip

cd modules/perc-distribution-tree && ../../mvnw clean install
# BUILD SUCCESS; target/classes/distribution/bin/{perc-doctor,perc-doctor.bat,perc-doctor.jar}
# java -jar …/perc-doctor.jar --dry-run -v clean-heap-dumps → WOULD_DELETE (smoke)
```

