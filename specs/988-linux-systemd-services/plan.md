# Implementation Plan: Linux systemd Service Management

**Branch**: `988-linux-systemd-services` | **Date**: 2026-07-17 | **Spec**: [spec.md](./spec.md)  
**Input**: Feature specification from `/specs/988-linux-systemd-services/spec.md`  
**Issue**: https://github.com/intersoftdatalabs-in/percussioncms/issues/962

## Summary

Ship a **native systemd unit** for Percussion CMS (Jetty) and teach `install-jetty-service.sh` to install/enable/uninstall it on systemd hosts, while retaining **init.d as a fallback**. Fix the support failure mode where LSB-generated units **timeout** on slow post-upgrade starts and leave **journalctl** nearly empty and state inconsistent. Approach: `Type=forking` + product PID file + `EnvironmentFile` + elevated `TimeoutStartSec` + journal stdout/stderr, structural tests, and ops docs.

## Technical Context

- **Language/Version**: Shell (bash) for Linux installers; unit files (systemd); existing Jetty distribution packaging (Maven). JDK 21 on `development` for any Java tests.
- **Owning Module(s)**: `modules/perc-jetty/` (primary); packaging via `modules/perc-distribution-tree/` if service tree copy needs a new file path.
- **AGENTS Hierarchy**: root `AGENTS.md`, `modules/perc-jetty/AGENTS.md`, `modules/perc-distribution-tree/AGENTS.md` if distribution packaging changes.
- **Dependencies & Storage**: systemd (runtime on target Linux); existing `/etc/default/<service>`, `/var/run/rxjetty/`, `rxjetty.sh` / Jetty start scripts. No new Maven libraries.
- **Testing**: JUnit 5 structural tests (unit file content) under `modules/perc-jetty/src/test/java` (packaging=pom — bind surefire like GH-939); optional bash dry-run tests; no root systemd required in CI.
- **Scale/Impact**: Ops-only; CMS app code unchanged; install/upgrade docs and distribution service folder.

## Constitution Check

- [x] **I. Module-First Boundaries** — `perc-jetty` owns service scripts; no new top-level module
- [x] **II. Evidence Over Invention** — extends `install-jetty-service.sh`, `rxjetty.sh`, Jetty upstream `jetty.service` as reference only
- [x] **III. Test Discipline** — unit/structural tests planned for unit template + installer selection
- [x] **IV. Contract & Integration Integrity** — no REST/schema/package format changes
- [x] **V. Safe Modernization** — scripts/units only; no Spring Boot
- [x] **VI. Security by Default** — unit runs as configured non-root user when set; no secrets in journal templates; root still required for install
- [x] **VII. Build & Dependency Hygiene** — no new deps; ship files via existing assembly
- [x] **VIII. Documentation & Operability** — README + installer usage messages; FR-007 logging/journal
- [x] **IX. PR Review Comment Resolution** — apply on PR reviews
- [x] **Complexity Budget** — no constitution exceptions

## Project Structure

### Documentation (this feature)

```text
specs/988-linux-systemd-services/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   └── systemd-unit-contract.md
├── checklists/requirements.md
├── tasks.md
└── spec.md
```

### Source Code (affected paths)

```text
modules/perc-jetty/src/main/jetty/service/
  install-jetty-service.sh          # detect systemd; install/uninstall unit
  PercussionCMS.service.in          # unit template (name/paths substituted) OR
  templates/percussion-cms.service  # static template with EnvironmentFile
modules/perc-jetty/src/main/jetty/defaults/bin/
  rxjetty.sh                        # minor: ensure forking/PID behavior documented for Type=forking
modules/perc-jetty/README.md
modules/perc-jetty/AGENTS.md
modules/perc-jetty/src/test/java/com/percussion/jetty/service/
  SystemdUnitTemplateTest.java      # structural FR checks
  InstallJettyServiceScriptTest.java  # string/contract checks or dry-run helpers
```

Windows `install-jetty-service.bat` — **no behavioral change** (regression-only awareness).

## Complexity Tracking

*(None — no constitution violations.)*

## Implementation Decisions (from research.md)

|    Topic     |                                         Decision                                          |
|--------------|-------------------------------------------------------------------------------------------|
| Unit type    | `Type=forking` + `PIDFile=` matching existing `JETTY_PID`                                 |
| Timeout      | `TimeoutStartSec=1800` (30 min) default; document override via drop-in                    |
| Logging      | `StandardOutput=journal`, `StandardError=journal`; keep start.log path in EnvironmentFile |
| Installer    | Prefer systemd when `systemctl` + `/run/systemd/system` exist; else init.d                |
| Dual install | On systemd path, **do not** also register chkconfig/update-rc.d                           |
| Fallback     | Keep init.d install when not systemd or `--initd` flag                                    |
| DTS          | Out of scope for this feature                                                             |

## Story → PR Strategy (constitution workflow)

1. **US1 PR**: unit template + systemd install path + tests + docs
2. **US2 PR**: timeout/journal/PID consistency hardening + tests
3. **US3 PR**: uninstall/migration + init.d coexistence + docs

## Phase mapping

- **Phase 0**: research.md (done with this plan cycle)
- **Phase 1**: data-model.md, contracts/, quickstart.md
- **Next**: tasks.md → implement per story

