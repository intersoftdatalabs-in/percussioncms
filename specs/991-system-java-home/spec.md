# Feature Specification: System / Configurable Java Home (No Required installDir/JRE)

**Feature Branch**: `991-system-java-home`  
**Created**: 2026-07-19  
**Status**: Draft  
**Input**: GitHub issue #1340 — Use system `JAVA_HOME` (or configurable Java home) instead of requiring operators to place a JRE under `<InstallDir>/JRE`. Prefer system or install-selected Java home; interactive multi-candidate prompt; consistent resolution across start/stop/service scripts on Linux, Windows, and macOS. Product requires **JDK/JRE 21** on the `development` / 8.2 line.

**Related issue**: https://github.com/intersoftdatalabs-in/percussioncms/issues/1340

## Problem statement (current behavior)

The product **does not ship or bundle a JRE** in the distribution (and has not for a long time). Post-install, start/stop/service scripts still expect Java at:

- `<InstallDir>/JRE`, and/or legacy `<InstallDir>/JRE64`

Today, operators must **manually copy** a JRE into that folder **or create a symlink** from `<InstallDir>/JRE` to a real Java home before CMS/DTS will run. That manual step is error-prone, poorly aligned with system-managed Java, and breaks installs that only set `JAVA_HOME` without the copy/symlink.

**This feature replaces that requirement** with install-time selection and/or system/`JAVA_HOME` resolution, so a post-install manual copy/symlink is no longer required for a working system.

## Module Scope

- **Primary module(s)**: `modules/perc-jetty/` (CMS Jetty start/stop/service); `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution/` (DTS Tomcat start/stop/service scripts); `modules/perc-distribution-tree/` (preinstall, install tree, JRE-related install assumptions); `system/release/installer/` (run/shutdown/service helpers)
- **Secondary / integration modules**: service packaging shared with Linux systemd feature (`988-linux-systemd-services`) where units/env files carry `JAVA_HOME`; any install-root util scripts that still invoke a relative `<InstallDir>/JRE` (update or explicitly document out of scope)
- **AGENTS files to apply**: root `AGENTS.md`; module AGENTS under `perc-jetty`, distribution-tree, delivery-tier-distribution if present
- **User roles affected**: ops / system administrators installing, upgrading, and running Percussion CMS and DTS
- **Install / upgrade impact**: installer prompts/config and start/stop/service scripts — **no** CMS schema, `.ppkg`, or application API contract changes; **no** change to “we ship a JRE in the archive” (we already do not)

## User Scenarios & Testing

Each story must be independently testable.

### User Story 1 - Run CMS without a manual InstallDir/JRE copy or symlink (Priority: P1)

An operator has a valid system JDK/JRE 21 (via environment or install-time selection). After install they start and stop CMS **without** copying a JRE into `<InstallDir>/JRE` and **without** creating a symlink there. Scripts and services use the configured or resolved Java home.

**Why P1**: Core value of #1340 — eliminate the mandatory post-install manual Java placement step.

**Acceptance Scenarios**:
1. **Given** a completed CMS install with **no** `<InstallDir>/JRE` directory or symlink and a valid Java 21 available via install-persisted config or process `JAVA_HOME`, **When** the operator starts CMS with the product start script or service, **Then** the product starts using that Java home.
2. **Given** CMS is running under that configuration, **When** the operator stops CMS via the product stop script or service, **Then** the process stops cleanly without requiring `<InstallDir>/JRE`.
3. **Given** the same host, **When** the operator inspects the effective Java home used at start (logs or documented diagnostic), **Then** it matches the configured/resolved home and is not a missing relative `JRE` path.

### User Story 2 - Run DTS under the same Java resolution rules (Priority: P1)

Delivery Tier Service (Production and Staging) start/stop/service paths follow the **same** resolution precedence and version requirement as CMS, so operators do not maintain two different mental models or two manual JRE folders.

**Why P1**: DTS is a first-class runtime surface on #1340; inconsistent Java resolution is an ops defect.

**Acceptance Scenarios**:
1. **Given** a DTS install without `<InstallDir>/JRE` and a valid Java 21 via config or environment, **When** the operator starts DTS Production (or Staging) via product scripts/services, **Then** DTS starts using the resolved Java home.
2. **Given** DTS is running, **When** the operator stops it via product scripts/services, **Then** stop succeeds without requiring a manual JRE folder under the install root.
3. **Given** CMS and DTS on the same host, **When** both resolve Java, **Then** both honor the same documented precedence order and major-version requirement (21).

### User Story 3 - Interactive install chooses among multiple eligible Java installs (Priority: P1)

During interactive install, when more than one eligible Java 21 installation is detected, the installer shows path and version for each candidate, lets the operator pick one, validates the choice, and **persists** it so post-install start/stop/service scripts do not require a manual copy/symlink into `<InstallDir>/JRE`.

**Why P1**: Explicit #1340 requirement; multi-JDK corporate images are common.

**Acceptance Scenarios**:
1. **Given** an interactive install and two or more eligible Java 21 candidates, **When** the installer reaches Java selection, **Then** the operator sees each candidate’s path and version and can select one.
2. **Given** the operator selects a candidate, **When** install completes, **Then** the chosen home is persisted in the durable product configuration used by CMS and DTS runtime scripts/services.
3. **Given** only one eligible Java 21 candidate, **When** install runs interactively, **Then** that candidate is selected automatically with a clear log/message (no unnecessary multi-choice prompt).
4. **Given** no eligible Java 21 candidate, **When** install runs, **Then** install fails with clear guidance (does not instruct “success” while leaving the operator to discover that a manual JRE copy is still required).

### User Story 4 - Unattended install supplies Java home explicitly (Priority: P1)

Silent/unattended install accepts an explicit Java home parameter (property, response file, or environment — product-documented) and validates major version 21 before writing config.

**Why P1**: Automation and enterprise imaging cannot use interactive prompts or ad-hoc post-install copy scripts.

**Acceptance Scenarios**:
1. **Given** unattended install with a valid explicit Java 21 home, **When** install completes, **Then** runtime config points at that home and start scripts work without a manual `<InstallDir>/JRE` copy or symlink.
2. **Given** unattended install with a missing or incompatible Java home, **When** install validates, **Then** install fails with a clear error naming the required major version (21).

### User Story 5 - Operator re-points Java after install (Priority: P2)

After install, an operator can change which Java home the product uses **without** a full reinstall and **without** redoing a manual JRE copy into the install tree, via a documented configuration location and/or environment override, then restart services.

**Why P2**: Ops need lifecycle flexibility (OS Java upgrades, path moves); slightly secondary to first-boot success.

**Acceptance Scenarios**:
1. **Given** a running install with persisted Java home A, **When** the operator updates the documented config (or higher-precedence env override) to Java home B (valid 21) and restarts CMS/DTS, **Then** the product uses home B.
2. **Given** an invalid re-point (wrong version or missing path), **When** the operator starts the product, **Then** start fails with a clear error listing what was tried and that version 21 is required.

### User Story 6 - Compatibility with existing manual InstallDir/JRE layouts (Priority: P2)

Some existing installs already have a **manually** copied tree or **symlink** at `<InstallDir>/JRE` (or legacy `JRE64`). During transition, runtime **may** still honor that path as a **lower-priority fallback** after install-persisted config and process `JAVA_HOME`, so upgraded hosts that have not yet adopted config/`JAVA_HOME` keep working. New installs MUST NOT depend on that layout as the only path to success.

**Why P2**: Reduces upgrade risk for fleets that already completed the manual copy/symlink step.

**Acceptance Scenarios**:
1. **Given** no usable install-persisted config and no usable process `JAVA_HOME`, but a valid Java 21 exists at `<InstallDir>/JRE` (copy or symlink), **When** the operator starts CMS/DTS, **Then** start may succeed using that path as a lower-priority fallback.
2. **Given** valid install-persisted config or `JAVA_HOME` **and** a present `<InstallDir>/JRE`, **When** the product starts, **Then** config/`JAVA_HOME` wins over the install-directory path.
3. **Given** upgrade documentation, **When** operators migrate from “manual JRE under install dir” to system/config Java, **Then** they have clear steps and no requirement to keep the copy/symlink once config is set.

### Edge Cases

- No compatible Java found on the host (all candidates wrong major version or broken installs).
- `JAVA_HOME` set but points to a non-executable or incomplete runtime (missing `bin/java` / Windows binaries).
- Multiple candidates with the same version string but different vendors/paths.
- Path with spaces or non-ASCII characters (Windows and Unix).
- Windows service vs console start: service must receive an absolute resolved Java home, not a relative `JRE` that breaks under service account context.
- Unix service units / env files (`/etc/default/…` or equivalent) must not keep stale wrong `JAVA_HOME` after re-point without documented update steps.
- Operator has only JRE 21 (no full JDK) — product accepts a compatible JRE 21 where a full JDK is not required for runtime.
- Broken or half-finished manual copy under `<InstallDir>/JRE` (directory exists but is not a valid Java home).
- Symlink at `<InstallDir>/JRE` pointing at a removed or upgraded path.
- macOS host with system Java layout differing from Linux common paths.
- Upgrade from a prior install whose service wrappers hard-coded `<InstallDir>/JRE`.
- Concurrent CMS and DTS installs sharing one machine-level Java vs per-product config.

## Requirements

### Functional Requirements

- **FR-001**: Product runtime for CMS MUST resolve a Java home without requiring the operator to copy or symlink a JRE into `<InstallDir>/JRE` (or `JRE64`) when a valid Java 21 is available via install-persisted configuration or process environment.
- **FR-002**: Product runtime for DTS (Production and Staging) MUST resolve Java under the same precedence and version rules as CMS.
- **FR-003**: Product MUST implement and document a single cross-platform resolution order for runtime Java home, including at least: (1) install-persisted / explicit product configuration, (2) process environment `JAVA_HOME` when valid and version-compatible, (3) optional existing `<InstallDir>/JRE` (or legacy `JRE64`) if present as a valid home—**manual copy or symlink only; not product-shipped**, (4) `java` discoverable on `PATH` when it yields a resolvable home, (5) fail clearly if none succeed.
- **FR-004**: All primary production start, stop, and service install/start paths for CMS and DTS on **Windows and Unix-like** platforms MUST share that precedence (behaviorally equivalent; platform-native script form allowed).
- **FR-005**: Resolved Java MUST be major version **21** for the 8.2 / `development` line; incompatible versions MUST be rejected with an error that states the required major version.
- **FR-006**: Interactive install MUST detect eligible Java 21 candidates and, when more than one exists, prompt the operator to choose among them showing path and version for each.
- **FR-007**: Interactive install MUST auto-select when exactly one eligible candidate exists, and MUST fail with actionable guidance when zero eligible candidates exist (MUST NOT complete “successfully” while still requiring a secret post-install manual JRE copy for first start).
- **FR-008**: Unattended/silent install MUST accept an explicit Java home input (documented property, response-file field, and/or environment variable) and apply the same version validation as interactive install.
- **FR-009**: Install MUST persist the chosen or supplied Java home into a durable configuration location consumed by CMS and DTS start/stop/service mechanisms after install.
- **FR-010**: Operators MUST be able to change the effective Java home post-install via documented configuration and/or environment override without a full product reinstall and without redoing a manual install-dir JRE copy, subject to version validation at next start.
- **FR-011**: Windows services and Unix service registrations MUST receive a usable absolute Java home (or equivalent service-native Java setting) consistent with the resolution result — not a broken relative `<InstallDir>/JRE` path when system/config Java is intended.
- **FR-012**: Install and ops documentation MUST describe resolution order, interactive vs unattended selection, post-install re-point, migration from the **current** “manual copy or symlink into `<InstallDir>/JRE`” practice, and clear failure messages. Docs MUST NOT claim the product ships a JRE.
- **FR-013**: Automated tests MUST cover resolution precedence and version rejection (unit/script tests with fixtures or mocks where live multi-JDK hosts are impractical), runnable in CI via project-supported build wrappers.
- **FR-014**: New path handling MUST be cross-platform (Windows, Linux, macOS); no Unix-only absolute path assumptions in product logic or tests for this feature.
- **FR-015**: Build-machine / developer toolchain selection (`mvn-env` / developer `JAVA_HOME_21`) remains out of scope and MUST NOT be broken by this feature’s runtime changes.
- **FR-016**: Product MUST NOT introduce a new requirement to ship or re-bundle a full JRE inside the distribution archive; the goal is to stop depending on a **manually operator-provided** install-dir JRE layout, not to start distributing Java again.

### Key Entities

- **Java home**: Absolute filesystem location of a JRE/JDK used to launch CMS or DTS.
- **Eligible candidate**: Detected Java installation that passes major-version 21 validation and basic executability checks.
- **Persisted Java configuration**: Install-written durable setting (product-owned) that start/stop/service scripts read with highest precedence after any documented env override rules.
- **Resolution order**: Ordered list of sources tried until a valid Java home is found or failure is reported.
- **Install-directory JRE layout (legacy ops practice)**: Operator-created `<InstallDir>/JRE` (or `JRE64`) as a **copy or symlink** to a real Java home — **not** content shipped by the product. Optional lower-priority fallback during transition.
- **Runtime surface**: CMS Jetty start/stop/service and DTS Production/Staging start/stop/service on supported OSes.

## Success Criteria

### Measurable Outcomes

- **SC-001**: On a clean Linux and Windows smoke install **without** any `<InstallDir>/JRE` copy or symlink, CMS starts and stops successfully when a valid system/config Java 21 is provided (checklist or automated smoke).
- **SC-002**: Under the same conditions, DTS Production and Staging start and stop successfully.
- **SC-003**: Interactive install with two mock/fixture Java 21 candidates records the operator’s selection and subsequent start uses that selection (100% of scripted UAT runs) **without** a post-install manual JRE placement step.
- **SC-004**: Unattended install with an explicit valid Java home succeeds; with an invalid home, fails before writing a “successful” broken config (zero silent-wrong-path successes in test matrix).
- **SC-005**: When no compatible Java is available, start and install surfaces present an error that mentions required major version **21** and does not hang or claim success while still requiring a manual JRE copy.
- **SC-006**: Automated resolution-order tests pass in CI on the project’s standard test path.
- **SC-007**: Documented migration notes exist for operators moving from “manual copy/symlink under `<InstallDir>/JRE`” to system/config Java; peer review confirms ops can follow them without tribal knowledge; docs correctly state that the product does not ship a JRE.
- **SC-008**: Existing installs that already have a valid operator-provided `<InstallDir>/JRE` (copy or symlink) and no higher-precedence config continue to start during the transition period (no forced break without documentation).

## Assumptions

- Target product line is **8.2 / `development`** requiring **Java 21** only (not multi-version support).
- **CMS and DTS** are both in scope for runtime scripts/services; other legacy tools under install root are either updated or explicitly listed out of scope in planning.
- The product **does not and will not** reintroduce shipping a full JRE in the distribution as part of this feature; the pain being removed is the **mandatory operator copy/symlink** into `<InstallDir>/JRE`.
- A **transition fallback** to an existing operator-provided `<InstallDir>/JRE` remains allowed when higher-precedence sources are absent.
- Interactive multi-candidate discovery covers practical host locations (environment, common OS install locations, `PATH`); exhaustive detection of every possible custom path is not required if unattended explicit home covers those cases.
- Operators performing service install have sufficient privileges (root / Administrator) as today.
- “Eligible” means major version 21 and a working launcher binary; micro-version and vendor (Eclipse Temurin, Oracle, Microsoft, etc.) are not restricted beyond that unless security policy is added later.
- Related systemd work (#962 / `988-linux-systemd-services`) may already place `JAVA_HOME` in service env files; this feature owns **how** that value is chosen, validated, and kept consistent with console scripts.
- macOS is supported for the same resolution rules where the product already supports running CMS/DTS on macOS; if a surface is Windows/Linux-only today, this feature does not invent new macOS product support beyond fixing shared scripts used there.
- No change to application business logic, public REST APIs, or database schema.

