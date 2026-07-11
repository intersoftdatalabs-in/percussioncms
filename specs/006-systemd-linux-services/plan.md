# Implementation Plan: Systemd Linux Service Scripts (Replace init.d)

**Branch**: `006-systemd-linux-services` | **Date**: 2026-07-11 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/006-systemd-linux-services/spec.md`
**Source issue**: intersoftdatalabs-in/percussioncms#962 (migrated from percussion/percussioncms#426)

## Summary

Replace the legacy init.d-based service supervision of the Percussion CMS (and the Delivery Tier Suite staging/production services) with native systemd units. The change is a pure supervision-layer swap: the CMS runtime (Jetty 12 + Jakarta Servlet 6.1) is untouched; systemd just owns `start`, `stop`, `restart`, journal logging, and failure-restart semantics. The first release that ships systemd **drops init.d entirely** (per resolved clarification): existing init.d installs MUST be converted in-place by the upgrade installer (FR-003); legacy scripts and their installer-manifest references are physically deleted (FR-007). Multi-instance support on a single host is in scope via a systemd template unit `percussioncms@.service` (FR-004a). The plan covers the installer migration logic, the new unit files, an integration-test harness using a systemd-enabled container, and documentation/site updates.

## Technical Context

**Language/Version**: Java 21 on `development` (build/test via `./mvn-env.sh`). The actual implementation work is shell + systemd unit syntax + a small Java/Ant test harness — no new Java services are added. Jetty 12.1.7 / Jakarta Servlet 6.1 (per `modules/perc-jetty/AGENTS.md`) — unchanged.
**Primary Dependencies**: Maven multi-module (existing); the installer (`modules/perc-ant`) and distribution tree (`modules/perc-distribution-tree`) wire the new units into the CMS installer. No new Maven dependencies needed.
**Storage**: N/A (no schema change; no persistence).
**Testing**: JUnit 5 + Mockito for installer-logic unit tests; a new shell-level integration test using `systemd-nspawn` or a privileged Docker container running `ubuntu:22.04` (or `debian:12`) with `/sbin/init` as PID 1 to exercise `systemctl` against the generated units. Jetty runtime is unchanged, so the existing Jetty test suites remain authoritative for runtime behavior.
**Target Platform**: Linux distributions with systemd as PID 1 — explicitly: Ubuntu 22.04+, Debian 12+, RHEL 9+, Rocky/AlmaLinux 9+, Amazon Linux 2023+. The supported-platform matrix (FR-005) is published in `modules/perc-distribution-tree` docs. Hosts without systemd are out of support in this release (per resolved clarification Q1).
**Project Type**: Multi-module CMS mono-repo. The work touches the installer and distribution tree (no source-code behavior changes in `system/` runtime).
**Performance Goals**: Install/upgrade completion within +10% of the legacy init.d path (SC-001). No runtime-performance regression since the Jetty entry points are unchanged.
**Constraints**:
- Branch JDK 21 via `./mvn-env.sh`; not a Spring Boot app.
- Backward compatibility: the *runtime* contract (ports, JVM options, log locations) is preserved. The *installer* contract changes (no init.d install path). Document the breaking change in the release notes and `BREAKING_CHANGES.md` if one exists.
- Module AGENTS hierarchy: `AGENTS.md` (root), `modules/perc-jetty/AGENTS.md`, `modules/perc-distribution-tree/AGENTS.md` (if present).
**Scale/Scope**:
- Modules touched: `modules/perc-distribution-tree` (primary), `system/release/installer/Linux` (delete), `system/release/installer/unix` (delete), `modules/perc-jetty` (extend `install-jetty-service.sh` or add new sibling script that writes systemd unit + env file), `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution` (delete init.d rootFiles, add systemd units).
- User roles: integrator / system administrator / build & release engineer.
- Install/upgrade impact: distribution tree content change; installer behavior change for upgrade path. `.ppkg` content changes when installer ships new unit files.
**Owning module(s)**:
- Primary: `modules/perc-distribution-tree`, `modules/perc-jetty`
- Deletion-only: `system/release/installer/Linux`, `system/release/installer/unix`, `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/rootFiles/DTSStagingService.sh`, `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/src/main/rootFiles/DTSProductionService.sh`
**AGENTS hierarchy applied**: `AGENTS.md` (root), `modules/perc-jetty/AGENTS.md`, `system/AGENTS.md` (sanity check — no Java service changes)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*
*Source: `.specify/memory/constitution.md` (v2.1.0)*

- [x] **I. Module-First Boundaries**: Owning modules identified above; Rule Discovery Protocol applied (read root + `modules/perc-jetty/AGENTS.md` + `system/AGENTS.md`); no orphan shared code — installer logic stays in `modules/perc-jetty`/`perc-distribution-tree`, not in `system/`.
- [x] **II. Evidence Over Invention**: Approach cites existing paths: `modules/perc-jetty/src/main/jetty/service/install-jetty-service.sh` (today's init.d installer; evolved), `modules/perc-jetty/src/main/jetty/StartJetty.sh` & `StopJetty.sh` (runtime entry points we reuse as `ExecStart=`/`ExecStop=`), `system/release/installer/Linux/percussion-service.sh` (deleted in this release per FR-007), `deliverytiersuite/.../DTSStagingService.sh` & `DTSProductionService.sh` (deleted). No new third-party libraries needed; systemd is part of the OS.
- [x] **III. Test Discipline**: Unit tests planned for installer detection/migration logic (JUnit 5 + Mockito, mirroring existing `modules/perc-jetty` test patterns); integration test against a real systemd-enabled container; tests added as first-class tasks in `/speckit.tasks`.
- [x] **IV. Contract & Integration Integrity**: Public REST/SOAP/package contracts unchanged. Installer behavior is changing — documented as a release-note breaking change. Per-instance `cms-<instance>.env` is the new operator contract for configuration.
- [x] **V. Safe Modernization**: No Spring Boot; no new frameworks; scope limited to the supervision-layer change. The deletion of legacy init.d scripts is a scope-clean cutover, not drive-by cleanup.
- [x] **VI. Security by Default**: Unit file's `EnvironmentFile=` MUST be `0640 root:root` (no world-readable secret leakage); no setuid wrappers; `User=`/`Group=` set on the unit; `NoNewPrivileges=true`, `ProtectSystem=strict`, `ProtectHome=true` declared where compatible with the install path. Threat notes in this plan, abuse-case tests (env-file injection, world-writable path) added.
- [x] **VII. Build, Platform & Dependency Hygiene**: JDK 21 via `./mvn-env.sh`; no new Maven/npm deps; Spotless not relevant for shell files. ShellCheck on new shell scripts (added to CI if not already present).
- [x] **VIII. Documentation & Operability**: README updates in `modules/perc-distribution-tree` (supported-platform matrix), `modules/perc-jetty/README.md` (new install procedure), `deliverytiersuite/.../delivery-tier-distribution/README.md` (DTS systemd procedure). Worklog entry under `modules/perc-jetty/src/site/markdown/worklog/`. Failures diagnosable via `journalctl -u percussioncms@<instance>.service`.
- [x] **IX. PR Review Comment Resolution**: Plan owner will follow root `AGENTS.md` "PR Review Comment Resolution" — inline reply + `resolveReviewThread` mutation — for every review thread on the resulting PR (none yet; this is a forward-looking commitment).
- [x] **Complexity Budget**: No principle violations. Template unit (`percussioncms@.service`) is justified by FR-004a (multi-instance) and is the standard systemd idiom — not complexity bloat.
- [x] **Governance**: Constitution list will be re-checked after Phase 1 design.

## Project Structure

### Documentation (this feature)

```text
specs/006-systemd-linux-services/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (unit-file schemas, env-file schema)
└── tasks.md             # Phase 2 output (NOT created by /speckit.plan)
```

### Source Code (repository modules)

```text
# Primary — installer & distribution wiring
modules/perc-distribution-tree/
├── src/main/resources/
│   ├── systemd/
│   │   ├── percussioncms@.service.template      # CMS template unit
│   │   ├── percussiondts-staging@.service.template
│   │   └── percussiondts-production@.service.template
│   └── env/
│       └── percussioncms.env.template           # Per-instance env file template
├── scripts/
│   ├── install-systemd-units.sh                  # NEW: install/upgrade/migrate
│   ├── uninstall-systemd-units.sh                # NEW: clean removal
│   └── README.md                                  # UPDATE: document new scripts
└── src/site/markdown/systemd-linux-services.md   # NEW: operator guide

# Primary — Jetty-runtime glue
modules/perc-jetty/
├── src/main/jetty/service/
│   ├── install-jetty-service.sh                  # UPDATE: deprecated, dispatch to new path
│   └── install-systemd-jetty-service.sh          # NEW: writes the systemd unit + env file
├── README.md                                      # UPDATE
└── src/site/markdown/worklog/
    └── systemd-linux-services.md                 # NEW: worklog entry

# Deletion-only
system/release/installer/Linux/                   # DELETE entire directory
system/release/installer/unix/                    # DELETE entire directory
deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/
└── src/main/rootFiles/
    ├── DTSStagingService.sh                      # DELETE
    └── DTSProductionService.sh                   # DELETE

# Tests
modules/perc-distribution-tree/src/test/
└── java/.../systemd/
    ├── InstallSystemdUnitsScriptTest.java        # Unit test: detection logic
    ├── EnvFileSchemaTest.java                    # Unit test: env-file validation
    └── MultiInstanceDetectionTest.java           # Unit test: per-instance migration

modules/perc-jetty/src/test/
└── shell/
    └── install-systemd-jetty-service.bats        # Shell-level test (bats-core)

# Integration test harness (new)
docker/systemd-test/
├── Dockerfile                                     # ubuntu:22.04 + systemd-nspawn
├── run-tests.sh
└── README.md
```

**Structure Decision**: The work is split between `modules/perc-jetty` (which already owns the Jetty-runtime install script `install-jetty-service.sh` and the `StartJetty.sh`/`StopJetty.sh` entry points the unit will call) and `modules/perc-distribution-tree` (which owns the install tree / distribution artifacts). This avoids creating a new module and respects existing ownership — constitution I.

## Complexity Tracking

No principle violations. No entries.

## Phase 0: Outline & Research

Research artifacts captured in [`research.md`](./research.md). Key resolutions:

- **NEEDS CLARIFICATION → Resolved**: Unit `Type=` chosen as `simple` (Jetty doesn't `fork()`; the wrapper script blocks). `ExecStart=` invokes `StartJetty.sh`; `ExecStop=` invokes `StopJetty.sh`. `PIDFile=` not used (Jetty writes its own pid under `${JETTY_RUN}/rxjetty.pid`; unit reads it for `ExecStop` via a small wrapper or `KillMode=mixed`).
- **NEEDS CLARIFICATION → Resolved**: Multi-instance detection enumerates `S??percussion-*` symlinks under `/etc/rc?.d/` and `/etc/init.d/percussion-*` files; each maps to one `percussioncms@<instance>.service` instantiation with its own env file.
- **NEEDS CLARIFICATION → Resolved**: Env-file permissions `0640 root:root` with the unit's `User=`/`Group=` set to the runtime user (so the runtime can read but unprivileged users cannot exfiltrate).
- **NEEDS CLARIFICATION → Resolved**: Idempotent installer uses "filesystem + systemd" state probes (does `/etc/systemd/system/percussioncms@<instance>.service` exist? is it `enabled`? is it `active`?) rather than relying on installer manifests.

## Phase 1: Design & Contracts

Artifacts produced:

- [`data-model.md`](./data-model.md) — entities: Unit File, Environment File, Legacy Init.d Script, Installer Manifest, Instance Record.
- [`contracts/`](./contracts/) — schemas for the unit template file, env file, and detection/migration contract.
- [`quickstart.md`](./quickstart.md) — runnable validation scenarios.

### Re-evaluate Constitution Check (post-Phase 1)

- [x] I. Module-First Boundaries — confirmed; no new modules created.
- [x] II. Evidence Over Invention — confirmed; all paths cited exist in tree.
- [x] III. Test Discipline — confirmed; unit + shell + integration test tasks defined.
- [x] IV. Contract & Integration Integrity — confirmed; runtime contract unchanged, installer behavior change documented.
- [x] V. Safe Modernization — confirmed; no Spring Boot, no framework churn.
- [x] VI. Security by Default — confirmed; env-file perms, `ProtectSystem=`, `NoNewPrivileges=` in unit template; abuse-case tests planned.
- [x] VII. Build, Platform & Dependency Hygiene — confirmed; no new deps; ShellCheck in CI.
- [x] VIII. Documentation & Operability — confirmed; README + worklog + site doc + operator quickstart planned.
- [x] IX. PR Review Comment Resolution — confirmed; owner commits to root `AGENTS.md` procedure.
- [x] Complexity Budget — no violations.
- [x] Governance — done.

## Mandatory Post-Execution Hooks

No hooks registered (`.specify/extensions.yml` absent) — skipped.

## Completion Report

- **Branch**: `006-systemd-linux-services`
- **Plan path**: `specs/006-systemd-linux-services/plan.md`
- **Generated artifacts**:
  - `specs/006-systemd-linux-services/research.md`
  - `specs/006-systemd-linux-services/data-model.md`
  - `specs/006-systemd-linux-services/contracts/` (unit template schema, env file schema, installer contract)
  - `specs/006-systemd-linux-services/quickstart.md`
- **Next command**: `/speckit.tasks`
