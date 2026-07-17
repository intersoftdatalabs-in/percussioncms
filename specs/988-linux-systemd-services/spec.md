# Feature Specification: Linux systemd Service Management

**Feature Branch**: `988-linux-systemd-services`  
**Created**: 2026-07-17  
**Status**: Draft  
**Input**: GitHub issue #962 — Update Linux service scripts to use systemd services instead of init.d (migrated from percussion/percussioncms#426). Ops need native systemd units so `systemctl start/stop/status` and `journalctl` work correctly, especially when post-upgrade CMS startup is slow and LSB/init.d-generated units time out with little journal detail.

**Related issue**: https://github.com/intersoftdatalabs-in/percussioncms/issues/962

## Module Scope
- **Primary module(s)**: `modules/perc-jetty/` (Linux service install scripts, Jetty defaults, unit file templates)
- **Secondary / integration modules**: `modules/perc-distribution-tree/` (ship service assets in the distribution); optional DTS Linux service packaging only if the same install pattern is reused — **CMS Jetty is in scope for P1**
- **AGENTS files to apply**: root `AGENTS.md`, `modules/perc-jetty/AGENTS.md`, `modules/perc-distribution-tree/AGENTS.md` (if touched)
- **User roles affected**: ops / system administrators installing or upgrading Percussion CMS on Linux
- **Install / upgrade impact**: distribution tree and install/ops scripts only — **no** schema, package `.ppkg`, or application API changes

## User Scenarios & Testing
Each story must be independently testable.

### User Story 1 - Install CMS as a native systemd service (Priority: P1)

An operator installs Percussion CMS on a modern Linux distribution (systemd by default). They run the product-provided Linux service installer and get a **native systemd unit** (not only an LSB init.d script that systemd wraps poorly). They can enable and start the service with standard `systemctl` commands.

**Why P1**: This is the core ask of #962 and unblocks correct lifecycle management on current Linux.

**Acceptance Scenarios**:
1. **Given** a supported Linux host with systemd and a completed CMS install path, **When** the operator runs the documented service install for systemd, **Then** a unit file is installed under the standard systemd unit path and `systemctl status <service>` shows the unit as loaded.
2. **Given** the unit is installed and enabled, **When** the operator runs `systemctl start <service>`, **Then** the CMS process starts and `systemctl is-active <service>` reports active (or activating→active) without requiring manual `kill` workarounds.
3. **Given** the service is running, **When** the operator runs `systemctl stop <service>`, **Then** the CMS process stops cleanly and the unit reports inactive.

### User Story 2 - Survive slow post-upgrade startup without false failure (Priority: P1)

After an upgrade, CMS startup can take a long time (database updates, package upgrades). With today’s init.d/LSB path, `systemctl start PercussionCMS` times out, journalctl shows almost no useful progress, systemd marks the unit failed while the JVM may still be starting, and stop/status become unreliable until processes are killed by hand.

**Why P1**: Documented customer/support failure mode on #962; data loss risk is low but ops confidence and automated restart are broken.

**Acceptance Scenarios**:
1. **Given** a unit configured for product-supported long startup (upgrade scenario), **When** start exceeds a short default timeout but completes within the product-documented max, **Then** systemd does **not** mark the unit failed solely due to an inadequate short timeout.
2. **Given** a start in progress or completed, **When** the operator runs `journalctl -u <service>` (or equivalent product-documented log path), **Then** they can see start progress and/or clear pointers to CMS/Jetty log files sufficient to diagnose hangs.
3. **Given** a failed or timed-out start that left a process running under the old broken behavior, **When** using the new unit and install instructions, **Then** start/stop/status remain consistent without requiring manual process kill for normal recovery (documented exception: hard JVM hang).

### User Story 3 - Uninstall and migrate from init.d (Priority: P2)

Operators who previously installed via init.d/chkconfig/update-rc.d can uninstall the old style service and install the systemd unit without leaving conflicting boot hooks, and can optionally keep init.d only where systemd is absent.

**Why P2**: Needed for upgrades of existing Linux installs; slightly lower than P1 because greenfield systemd is the main path.

**Acceptance Scenarios**:
1. **Given** an existing init.d-based PercussionCMS service, **When** the operator follows the documented migration/uninstall path, **Then** init.d links and scripts for that service are removed (or disabled) and no dual-start conflict remains.
2. **Given** a host **without** systemd (or where policy requires SysV), **When** the operator chooses the documented init.d install path, **Then** install still works as a supported fallback.
3. **Given** the service name is customized (not only the default PercussionCMS), **When** install/uninstall run with that name, **Then** unit and config paths use the same service name consistently.

### Edge Cases
- What happens when `JAVA_HOME`, install directory, or run-as user are wrong or missing at install time?
- How does the system handle a missing or stale PID file after a crash?
- What happens when the operator is not root / lacks systemd privileges?
- How does upgrade interact with a customized unit drop-in under `/etc/systemd/system/`?
- What if both init.d and systemd units would be active for the same instance?
- Service name collisions with a pre-existing unit of the same name.
- Windows service install remains out of scope for behavioral change (existing `.bat` path).

## Requirements
### Functional Requirements
- **FR-001**: Product MUST ship a documented **native systemd unit template** for the CMS Jetty service (installable by the Linux service installer).
- **FR-002**: Linux service install MUST support installing the CMS service via **systemd** on hosts where systemd is the init system.
- **FR-003**: Linux service install MUST retain an **init.d/SysV fallback** path for hosts without systemd (or when explicitly requested), without forcing dual registration on systemd hosts.
- **FR-004**: The systemd unit MUST use a service type and start/stop configuration appropriate for Jetty’s process model (forking or equivalent product-chosen model) with a correct **PID file** (or notify protocol if product adopts it).
- **FR-005**: The unit MUST allow **extended startup time** suitable for post-upgrade CMS work (configurable or set high enough for supported upgrade scenarios), so slow starts do not false-fail under systemd.
- **FR-006**: Start/stop operations MUST leave the unit and process state **consistent** under normal success and failure paths so `systemctl status/stop` work without manual process cleanup in non-hang cases.
- **FR-007**: Operators MUST be able to obtain **diagnosable startup output** via journalctl and/or clearly documented CMS/Jetty log locations referenced in unit/docs.
- **FR-008**: Uninstall (systemd and init.d paths) MUST remove or disable the service from boot and remove product-owned unit/init scripts for that service name.
- **FR-009**: Service install MUST continue to capture **run-as user**, `JAVA_HOME`, and install/base paths in product config (as today via `/etc/default/<service>` or equivalent) and wire them into the unit.
- **FR-010**: Product documentation (module README and/or help/site notes) MUST describe install, start/stop, journal/logs, timeout behavior, and migration from init.d.
- **FR-011**: Automated tests MUST verify unit template content and installer behavior contracts (paths, unit keys, no dual-register on systemd path) without requiring a live root systemd on CI where impractical—use structural/script tests with mocks or dry-run modes as needed.
- **FR-012**: Windows service install scripts MUST remain functional; this feature MUST NOT regress Windows install.

### Key Entities
- **Service unit**: Named systemd unit representing one CMS Jetty instance (default name PercussionCMS).
- **Service environment config**: Environment/paths file (e.g. `/etc/default/<name>`) with JAVA_HOME, JETTY_HOME, JETTY_BASE, user, PID path.
- **Service installer**: Linux install/uninstall script that detects init system and registers the correct artifacts.
- **Init.d script**: Existing SysV/LSB script retained as fallback.

## Success Criteria
### Measurable Outcomes
- **SC-001**: On a systemd host, a clean install + service install results in successful `systemctl start` and `systemctl is-active` active within the product-documented timeout, without manual process kill, in the standard smoke scenario.
- **SC-002**: In a simulated slow-start scenario (startup work longer than 90 seconds but within the product max), the unit does not enter failed solely due to a short default start timeout.
- **SC-003**: After a successful start, `journalctl -u <service>` (or documented log path) contains start-related messages or an explicit pointer to Jetty/CMS logs usable for diagnosis.
- **SC-004**: Uninstall removes boot registration; a subsequent reboot does not auto-start the removed service (verified by checklist or automated dry-run assertions).
- **SC-005**: Structural/automated tests for unit template and installer selection logic pass in CI via `./mvn-env.sh` / module test or script harness.
- **SC-006**: Existing init.d-only install path still documented and selectable; Windows service install path unchanged in behavior.

## Assumptions
- Primary target is **Linux CMS Jetty** service (PercussionCMS / custom name), not Windows.
- **systemd is the default** on supported modern distros; SysV/init.d remains a **fallback**, not removed in this feature.
- Scope is **ops packaging and scripts**, not changing CMS upgrade business logic itself (DB/package upgrades may still be slow; the unit must tolerate that).
- Default service name remains **PercussionCMS** unless the operator supplies another name (existing installer behavior).
- DTS systemd units are **out of scope for P1** unless trivial reuse; can be a follow-up story.
- Operators run install/uninstall with **root** privileges (existing requirement).
- Product-supported max start time will be chosen in planning (e.g. 15–30+ minutes for upgrades) and documented; exact value is an implementation decision constrained by FR-005.
- Jetty upstream sample `jetty.service` is a reference only; Percussion must ship a unit wired to product paths and environment files.

## Out of Scope
- Rewriting Jetty application logging (see #939).
- Changing Windows `prunsrv` / `.bat` service install behavior.
- Container orchestration (Kubernetes/Docker compose) as the primary service model.
- Auto-migrating every existing customer unit drop-in without operator action.
- Fixing all possible JVM hard hangs (only false timeout / inconsistent systemd state for normal long starts).
