# Quickstart / Validation Guide: System Java Home (991)

**Goal**: Prove CMS/DTS run with **no** manual `<InstallDir>/JRE` copy/symlink when a valid Java 21 is configured or on the environment.

**Spec**: [spec.md](./spec.md) · **Contracts**: [java-home-resolution.md](./contracts/java-home-resolution.md), [java-properties-contract.md](./contracts/java-properties-contract.md)

## Prerequisites

- Branch `991-system-java-home` built/installed (or scripts patched in a test install tree)
- Host with **JDK/JRE 21** available (system package, Temurin, etc.)
- **Do not** create `<InstallDir>/JRE` for positive path tests
- JDK 21 for unit tests via `./mvn-env.sh` / `mvn-env.bat` (build only; not product runtime)

## Automated checks (CI / local)

```bash
# From repo root — module list finalized in tasks.md; examples:
./mvn-env.sh -pl modules/perc-jetty -am test -Dtest='*JavaHome*,*ResolveJava*,InstallJetty*'
./mvn-env.sh -pl modules/perc-distribution-tree -am test -Dtest='*JavaHome*,*Preinstall*Java*'
# DTS module tests similarly once added under delivery-tier-distribution
```

**Expect**: Resolution precedence tests pass; invalid major versions rejected; property write/read round-trip on temp dirs (portable `Path`).

## Smoke A — CMS without InstallDir/JRE (SC-001)

1. Install CMS (or use existing tree) with install-root **`java.properties`**:
   ```properties
   JAVA_HOME=<absolute path to Java 21 home>
   JAVA=<absolute path to java launcher>
   ```
2. Ensure **no** `<InstallDir>/JRE` or `JRE64` directory/symlink exists (rename if needed).
3. Start CMS via product `StartJetty` (console) **or** service after service install.
4. **Expect**: Process starts; logs/echo show `JAVA_HOME` matching config (not a missing relative JRE).
5. Stop CMS via product stop path.
6. **Expect**: Clean stop.

## Smoke B — DTS without InstallDir/JRE (SC-002)

1. Same pattern on DTS install root (`java.properties`, no `JRE` folder).
2. Start Production (and Staging if present) via `TomcatStartup` / service scripts.
3. **Expect**: Start/stop succeed using resolved home.

## Smoke C — Env fallback (no config file)

1. Remove or rename `java.properties`.
2. Export `JAVA_HOME` to a valid Java 21 home.
3. No InstallDir/JRE.
4. Start CMS/DTS.
5. **Expect**: Start succeeds via process env (precedence #2).

## Smoke D — Legacy install-dir fallback (SC-008)

1. No `java.properties`; unset `JAVA_HOME` (or set invalid and confirm it is skipped only if invalid — prefer unset for pure fallback).
2. Create symlink: `<InstallDir>/JRE` → real Java 21 home (or copy).
3. Start.
4. **Expect**: Start succeeds via install-dir layout.
5. Add valid `java.properties` pointing elsewhere (also 21); restart.
6. **Expect**: Config wins over install-dir (precedence #1).

## Smoke E — Interactive multi-candidate (SC-003)

1. Host with two Java 21 homes (or fixtures).
2. Run interactive install.
3. **Expect**: Prompt lists path + version; selection written to `java.properties`; first start without manual JRE placement works.

## Smoke F — Unattended (SC-004)

1. Unattended install with valid `-Dperc.java.home=...` (or documented flag).
2. **Expect**: `java.properties` written; start works without InstallDir/JRE.
3. Repeat with invalid path or Java 11 home.
4. **Expect**: Install fails; does not write success config for a missing JRE.

## Smoke G — Failure messaging (SC-005)

1. No config, no valid env, no InstallDir/JRE, no Java 21 on PATH.
2. Start CMS.
3. **Expect**: Non-zero failure; message mentions **21** and does not hang.

## Docs check (SC-007)

- [ ] Ops README / install notes describe resolution order  
- [ ] Docs say product does **not** ship a JRE  
- [ ] Migration from “manual copy/symlink to InstallDir/JRE” documented  
- [ ] No remaining “Must be version 1.8” in updated primary scripts  

## Sign-off table

| Scenario | Pass? | Notes |
|----------|-------|-------|
| A CMS no JRE folder | | |
| B DTS no JRE folder | | |
| C Env only | | |
| D Legacy symlink fallback | | |
| E Interactive multi | | |
| F Unattended | | |
| G Fail message | | |
| Automated tests | | |

**Tester / date / build**:
