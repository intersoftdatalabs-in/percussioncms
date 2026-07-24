# Implementation Plan: System / Configurable Java Home

**Branch**: `991-system-java-home` | **Date**: 2026-07-19 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/991-system-java-home/spec.md`  
**Issue**: https://github.com/intersoftdatalabs-in/percussioncms/issues/1340

## Summary

Stop requiring operators to **manually copy or symlink** a JRE into `<InstallDir>/JRE` after install. Persist a chosen system Java home (interactive multi-candidate or unattended `-Dperc.java.home`) into install-root **`java.properties`**, and make CMS Jetty + DTS start/stop/service scripts resolve Java via a **shared precedence** (config → env `JAVA_HOME` → legacy install-dir JRE layout → PATH → clear fail for major version **21**). Product does **not** ship a JRE and must not reintroduce bundling.

## Technical Context
- **Language/Version**: Bash and Windows `.bat` for runtime scripts; Java 21 for preinstall/installer helpers and unit tests (`development` line).
- **Owning Module(s)**: `modules/perc-jetty/` (CMS Jetty scripts/services); `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/` (DTS scripts/services + DTS preinstall); `modules/perc-distribution-tree/` (CMS preinstall, install.xml gates); `system/release/installer/` (legacy helpers messaging/paths).
- **AGENTS Hierarchy**: root `AGENTS.md`; `modules/perc-jetty/AGENTS.md`; `modules/perc-distribution-tree/AGENTS.md` if present; delivery-tier-distribution README.
- **Dependencies & Storage**: No new Maven libraries required. Durable file: install-root `java.properties` (`JAVA_HOME`, `JAVA`). Linux services continue to use `/etc/default/<service>` populated from the same resolution result. Optional legacy `<InstallDir>/JRE` operator layout.
- **Testing**: JUnit 5 (+ Mockito as needed) for resolution/version helpers and property I/O (portable `java.nio.file.Path`); structural script tests under `perc-jetty` / DTS modules (pattern from `InstallJettyServiceScriptTest`); smoke steps in [quickstart.md](./quickstart.md).
- **Scale/Impact**: Ops/install only; no CMS app schema, REST, or `.ppkg` format changes. Cross-platform (Windows, Linux, macOS script paths). Coordinates with `988-linux-systemd-services` for `JAVA_HOME` in service env files.

## Constitution Check
- [x] **I. Module-First Boundaries** — Jetty / DTS distribution / distribution-tree / installer helpers; no new top-level module
- [x] **II. Evidence Over Invention** — extends existing `java.properties`, `perc.java.home`, StartJetty/DTS scripts, install-jetty-service (see research.md)
- [x] **III. Test Discipline** — unit + structural tests planned for resolver and install write path
- [x] **IV. Contract & Integration Integrity** — no public REST/schema/package API changes; ops contract only
- [x] **V. Safe Modernization** — scripts + preinstall; no Spring Boot
- [x] **VI. Security by Default** — no secrets in java.properties; only filesystem paths; validate path existence before exec
- [x] **VII. Build & Dependency Hygiene** — no new deps; JDK 21 via `./mvn-env.sh` for tests; build toolchain out of scope
- [x] **VIII. Documentation & Operability** — README/installer messages; migration off manual JRE copy; update stale “1.8” text
- [x] **IX. PR Review Comment Resolution** — apply on PR reviews
- [x] **Complexity Budget** — no constitution exceptions (dual sh/bat implementations justified by platform, not a new framework)

### Post-design Constitution Check
- [x] Re-validated after Phase 1 artifacts: contracts define ops-facing resolution; tests portable; no app API breakage; legacy install-dir fallback preserves upgrades.

## Project Structure
### Documentation (this feature)
```text
specs/991-system-java-home/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── java-home-resolution.md
│   └── java-properties-contract.md
├── checklists/requirements.md
├── tasks.md                 # next: /speckit-tasks
└── spec.md
```

### Source Code (affected paths — planned)
```text
# Shared resolution (names illustrative; finalize in tasks)
modules/perc-jetty/src/main/jetty/
  resolve-java-home.sh          # sourced by Start/Stop/service
  resolve-java-home.bat
  StartJetty.sh / StartJetty.bat / StopJetty.bat
  service/install-jetty-service.sh / .bat
modules/perc-jetty/src/test/java/com/percussion/jetty/java/
  JavaHomeResolutionTest.java  # or structural script tests

modules/perc-distribution-tree/
  preinstall/Main.java          # discover/select/write java.properties
  install.xml                   # soft-gate JRE backup/ext when missing
  (+ unit tests for discovery/write)

delivery-tier-distribution/src/main/rootFiles/
  resolve-java-home.sh / .bat   # DTS copy or shared packaging of same logic
  TomcatStartup.* TomcatShutdown.*
  DTSProductionService.* DTSStagingService.*
  (+ MainDTSPreInstall if DTS install writes its own java.properties)

system/release/installer/**     # replace hard-coded ./JRE; update 1.8 messages
```

## Complexity Tracking
*(None — dual shell/bat is existing product pattern, not a constitution violation.)*

## Implementation Decisions (from research.md)

| Topic | Decision |
|-------|----------|
| Problem | Replace **manual** InstallDir/JRE copy/symlink requirement |
| Durable config | Install-root `java.properties` (`JAVA_HOME`, `JAVA`) |
| Precedence | config → env → install-dir JRE/JRE64 → PATH → fail (major 21) |
| Helpers | Shared resolve scripts per product root + Java-tested pure logic |
| Install UI | Preinstall multi-candidate prompt; unattended `perc.java.home` |
| Legacy | Keep install-dir JRE as **fallback only** |
| Packaging | Do **not** ship/re-bundle a JRE |
| systemd (988) | Populate `JAVA_HOME` in `/etc/default` from same resolution |

## Story → PR Strategy (constitution workflow)
1. **US1 + US5 foundation PR**: resolver helpers + CMS Jetty Start/Stop + `java.properties` read + re-point docs + tests  
2. **US2 PR**: DTS Tomcat/service scripts same resolver + tests  
3. **US3 + US4 PR**: interactive/unattended install discovery + write `java.properties` + fail paths + tests  
4. **US6 + docs/XML PR**: install.xml soft-gates, legacy fallback verified, migration docs, installer helper cleanup  

(Stories may be combined if PR size stays reviewable; keep CMS console path first for earliest value.)

## Phase mapping
- **Phase 0**: [research.md](./research.md) — done  
- **Phase 1**: [data-model.md](./data-model.md), [contracts/](./contracts/), [quickstart.md](./quickstart.md) — done with this plan cycle  
- **Next**: `/speckit-tasks` → implement per story PRs  

## Risks & mitigations

| Risk | Mitigation |
|------|------------|
| Script drift sh vs bat | Single contract doc + dual structural tests |
| Service still points at old JRE after re-point | Document re-run service install or update Procrun/`/etc/default`; optional service-install re-read of java.properties |
| install.xml fails without JRE dir | Soft-gate filesets (R10) |
| Operators trust only PATH without install write | Runtime PATH step still works; prefer install-persisted for services |
| Stale “Must be 1.8” messages | Explicit doc/script text update tasks |

## Out of scope (confirmed)
- Shipping a JRE inside the product archive  
- Build machine JDK selection (`mvn-env` / `JAVA_HOME_21`)  
- Supporting Java majors other than 21 on 8.2  
