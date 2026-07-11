# Feature Specification: Systemd Linux Service Scripts (Replace init.d)

**Feature Branch**: `006-systemd-linux-services`
**Created**: 2026-07-11
**Status**: Draft  
**Input**: User description: "Please take a look at issue https://github.com/intersoftdatalabs-in/percussioncms/issues/962 and build a feature spec for this. We will need to handle upgrade scenarios and remove the legacy init.d scripts after conversion to systemd."
**Source issue**: intersoftdatalabs-in/percussioncms#962 (migrated from percussion/percussioncms#426)

## Clarifications

### Session 2026-07-11

- Q: Supported-platform matrix / init.d fallback strategy (drop in same release vs. one-release fallback vs. keep indefinitely) → A: Drop init.d support in the same release that introduces systemd (cleanest cutover; existing hosts must upgrade through this release's installer to stay supported).
- Q: Restart policy on unexpected process exit → A: `Restart=on-failure` with `RestartSec=30s` and a bounded `StartLimitBurst=` / `StartLimitIntervalSec=` cap (CMS comes back after a JVM crash but cannot spin in a tight restart loop).
- Q: Whether to physically delete the legacy init.d scripts and installer references from the source tree, or keep them deprecated for one release → A: Physically delete in this release (no in-tree dead code; matches issue #962's wording).
- Q: Multi-instance support on one host (in scope via template unit vs. out of scope for v1) → A: In scope — ship `percussioncms@.service` template unit with per-instance parameterisation via `EnvironmentFile=/etc/percussion/cms-%i.env`.

## Module Scope *(mandatory for this mono-repo)*

- **Primary module(s)**: `modules/perc-distribution-tree` (installer / distribution tree that ships Linux service scripts), `system/release/installer/Linux` (legacy init.d sources that ship with the CMS installer)
- **Secondary / integration modules**: `modules/perc-jetty` (owns the `rxjetty.sh` service wrapper and `install-jetty-service.sh` installer), `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution` (ships `DTSStagingService.sh` / `DTSProductionService.sh` for the Delivery Tier Suite)
- **AGENTS files to apply**: root `AGENTS.md`, `system/AGENTS.md` (only as a sanity check — no new Java services are added), `modules/perc-distribution-tree/AGENTS.md` if present
- **User roles affected**: integrator / system administrator who installs, upgrades, and operates the CMS and/or DTS on Linux servers; build/release engineer who assembles the distribution tree
- **Install / upgrade impact**: package `.ppkg` content change (distribution tree replaces init.d templates with systemd units); install/upgrade scripts under `modules/perc-ant` and `modules/perc-distribution-tree/scripts` may need to detect existing init.d installations and migrate them

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Fresh Install on a systemd Linux Host (Priority: P1)

An integrator downloads the Percussion CMS Linux installer and runs it on a modern Linux distribution (Ubuntu 22.04+, RHEL 9+, Debian 12+, or any other systemd-based distribution). After installation, the CMS processes are managed by systemd: starting the host, running `systemctl start percussioncms` brings the service up, `systemctl status percussioncms` reports health, and `journalctl -u percussioncms` shows the logs.

**Why this priority**: This is the primary path for new customers and is the core value of issue #962. Without a clean systemd story on fresh installs, the feature does not exist.

**Independent Test**: Perform a clean CMS install on a fresh systemd Linux VM; verify that systemd units are installed, enabled, started, and that the CMS responds on its documented ports without invoking any legacy init.d scripts.

**Acceptance Scenarios**:

1. **Given** a fresh Linux host with systemd as PID 1 and the CMS installer, **When** the installer completes successfully, **Then** the appropriate systemd unit file(s) for the CMS are present under `/etc/systemd/system/` or `/usr/lib/systemd/system/`, are enabled to start at boot, and are running.
2. **Given** the CMS is managed by systemd, **When** the administrator runs `systemctl restart percussioncms`, **Then** the CMS process is stopped and started cleanly and the service returns to a healthy state within a documented timeout.
3. **Given** the CMS is managed by systemd, **When** the administrator runs `journalctl -u percussioncms -n 200`, **Then** the recent CMS server log entries are visible through the systemd journal.
4. **Given** the CMS is managed by systemd, **When** the host reboots, **Then** the CMS service starts automatically as part of the normal boot sequence.

---

### User Story 2 - Existing Installation Upgrade From init.d to systemd (Priority: P2)

An integrator previously installed the CMS using the legacy init.d-based installer. They upgrade to a new release that ships systemd units. The upgrade must detect the existing init.d service, stop it cleanly, install the new systemd units, transfer any custom configuration, and start the CMS under systemd — without requiring a fresh install or losing custom paths / ports.

**Why this priority**: Existing customers are the largest install base. A new release that breaks upgrade in place will block adoption and force reinstalls. Issue #962 explicitly calls out upgrade scenarios.

**Independent Test**: Take a CMS installed via the legacy init.d installer (symlinks under `/etc/init.d/` or `/etc/rc.d/`), run the upgrade installer, and verify that the legacy init.d scripts are removed/disabled, the systemd units are installed and active, and the CMS comes back up with the same effective configuration (ports, install path, JVM options).

**Acceptance Scenarios**:

1. **Given** a CMS previously installed via the legacy init.d scripts (symlinks and LSB-style scripts present), **When** the operator runs the upgrade installer, **Then** the installer detects the existing init.d installation, stops the init.d-managed process, removes the legacy init.d symlinks/scripts, installs the systemd unit(s), and starts the CMS under systemd.
2. **Given** an existing init.d installation with custom ports or non-default install paths, **When** the upgrade completes, **Then** the new systemd unit is configured with those same custom values (no re-prompting for values the installer already knows).
3. **Given** an upgrade that fails partway through, **When** the operator re-runs the upgrade, **Then** the installer is idempotent and does not leave the system in a half-converted state (either fully on systemd or fully on init.d).
4. **Given** an upgrade from init.d, **When** the CMS is running under systemd, **Then** `systemctl list-unit-files | grep percussion` shows the new unit(s) installed and enabled, and `ls /etc/init.d/ | grep percussion` shows no remaining percussion init.d scripts.

---

### User Story 3 - Operator Lifecycle and Diagnostics (Priority: P3)

A system administrator who is comfortable with systemd uses standard Linux tooling to operate the CMS service: start, stop, restart, check status, view logs, and enable/disable autostart. They do not need to learn any Percussion-proprietary commands.

**Why this priority**: This is the operator-experience payoff of moving to systemd. It is lower priority than the install and upgrade paths because it is "free" once the units exist, but the spec must still cover it because the choice of unit type (`simple`, `forking`, `notify`, `oneshot`) and the unit metadata determine whether this story works.

**Independent Test**: From a clean systemd-managed CMS, exercise `systemctl start|stop|restart|status|enable|disable` and `journalctl` and confirm each behaves correctly without falling back to any init.d script.

**Acceptance Scenarios**:

1. **Given** the CMS is running under systemd, **When** the operator runs `systemctl stop percussioncms`, **Then** the CMS process shuts down gracefully (in-flight requests complete or are timed out per documented behavior) and `systemctl is-active percussioncms` reports `inactive` within a documented timeout.
2. **Given** the CMS is stopped, **When** the operator runs `systemctl start percussioncms`, **Then** the CMS becomes active and the documented CMS ports begin accepting traffic within a documented timeout.
3. **Given** the CMS process dies unexpectedly, **When** systemd detects the failure, **Then** it is logged to the journal with the unit name, the unit enters `failed` state (visible via `systemctl status percussioncms`), and after `RestartSec=30s` systemd attempts one restart; after `StartLimitBurst` failures within `StartLimitIntervalSec` the unit stops auto-restarting and remains in `failed` until the operator intervenes.

---

### User Story 4 - Delivery Tier Suite (DTS) Services Move to systemd (Priority: P3)

The DTS has its own init.d-style scripts (`DTSStagingService.sh`, `DTSProductionService.sh`) shipped via `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution`. Operators who install both CMS and DTS on the same host should get a consistent systemd experience for both.

**Why this priority**: DTS is a separate but co-installed product. It would be confusing to ship systemd for CMS and init.d for DTS on the same host. Lower priority because DTS upgrades follow their own cadence and the CMS install path does not strictly depend on this.

**Independent Test**: On a fresh systemd host, install the DTS distribution; verify that the staging and production services are installed as systemd units and behave the same as the CMS units under `systemctl`/`journalctl`.

**Acceptance Scenarios**:

1. **Given** a fresh systemd Linux host and the DTS distribution, **When** the DTS installer completes, **Then** both the staging and production DTS services are installed as systemd units, enabled, and running.
2. **Given** an existing DTS installation using init.d scripts, **When** the operator runs the DTS upgrade, **Then** the legacy scripts are migrated out and the new systemd units take over with the same effective configuration.

### Edge Cases

- **Distribution without systemd** (legacy RHEL 7, older Ubuntu LTS). **Resolved**: out of support in this release (per Q1 / FR-005); the installer refuses non-systemd hosts with exit code 2 and the message "systemd is not PID 1; this installer does not support non-systemd hosts" (see `contracts/installer-script.md`).
- **Container hosts** where PID 1 is not systemd (Docker without `--privileged` and a systemd image). Unit files still install but `systemctl` does not work; the installer must not fail destructively.
- **Non-root install paths** (`/opt/percussion`, `/srv/percussion`, custom user). The unit must use `EnvironmentFile=` or unit-time substitution so paths are not hard-coded into the shipped unit template.
- **Multiple CMS instances on one host** (per FR-004a, supported via `percussioncms@<instance>.service`). The installer MUST enumerate any existing init.d-prefixed CMS instances it finds and convert each one to its own template-instantiated unit, preserving per-instance install paths and ports.
- **Upgrades where the init.d service was disabled or already stopped.** The migration must not fail just because the legacy service is not currently running.
- **Hostname change / IP change** between install and first systemd start. Service should start cleanly regardless of network identity.
- **SELinux / AppArmor denying journal writes or process exec.** Unit must declare the minimum needed capabilities; failure mode should be a clear journal error, not a silent crash.
- **Upgrade interrupted by power loss / SIGKILL mid-install.** Re-running the installer must converge to a consistent state.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The CMS Linux distribution MUST include one or more systemd unit files that start, stop, and supervise the CMS service(s) on systemd-based hosts. *(Replaces the legacy `BEGIN INIT INFO`-style init.d scripts in `system/release/installer/Linux` and `system/release/installer/unix`.)*
- **FR-002**: The CMS Linux installer MUST install the systemd unit file(s) under the standard systemd directory (`/etc/systemd/system/` or `/usr/lib/systemd/system/`), run `systemctl daemon-reload`, `systemctl enable`, and `systemctl start` as appropriate, and report clear success/failure to the operator.
- **FR-003**: The CMS Linux installer MUST support an upgrade-in-place path that detects a pre-existing init.d-based CMS installation, stops the legacy service, removes the init.d symlinks/scripts, and brings the CMS up under systemd without requiring a fresh install.
- **FR-004**: The systemd unit file(s) MUST be parameterised so that custom install paths, ports, JVM options, and user/group are NOT hard-coded into the shipped file. An `EnvironmentFile=` (e.g. `/etc/percussion/cms.env`) or template-time substitution MUST be used so the same unit file ships to every customer.
- **FR-004a**: The CMS unit MUST be shipped as a systemd template unit `percussioncms@.service` (per the resolved clarification), with per-instance configuration in `/etc/percussion/cms-<instance>.env`, so multiple CMS instances can coexist on one host as `percussioncms@<instance>.service`. Single-instance installs use the instance name `default`. The DTS units follow the same template pattern (`percussiondts-staging@.service`, `percussiondts-production@.service`).
- **FR-005**: The project MUST publish a supported-platform matrix in `modules/perc-distribution-tree` documentation stating which Linux distributions / init systems are supported in this release. Per the resolved clarification, init.d-based hosts are NOT supported in this release: the installer is the only supported migration path, and hosts that do not run the upgrade installer are out of support.
- **FR-006**: After a successful conversion, the legacy init.d scripts MUST NOT be required by the running CMS. The unit file MUST start the CMS via the same Jetty / service-wrapper entry points already used today (so the runtime is unchanged — only the supervision mechanism changes).
- **FR-006a**: The CMS unit MUST declare `Restart=on-failure` with `RestartSec=30s` and a bounded `StartLimitBurst=` / `StartLimitIntervalSec=` cap (per the resolved clarification), so that a JVM crash brings the CMS back automatically but a failing service cannot spin in a tight restart loop. The DTS units follow the same policy.
- **FR-007**: The legacy init.d scripts (`percussion-service.sh`, `InstallPublisherDaemon.sh`, `InstallDaemon.sh`, `InstallFTSDaemon.sh`, and their `unix/` counterparts) and their installer-manifest references MUST be physically deleted from the source tree in this release (per the resolved clarification). Init.d support is dropped entirely this release (no one-release overlap); the installer must convert any existing init.d installation to systemd as part of upgrade (FR-003).
- **FR-008**: The DTS distribution (`deliverytiersuite/delivery-tier-suite/delivery-tier-distribution`) MUST include equivalent systemd units for the staging and production services, with an upgrade path from the existing `DTSStagingService.sh` / `DTSProductionService.sh` init.d scripts.
- **FR-009**: The new systemd units MUST direct all CMS / DTS log output to the systemd journal (`StandardOutput=journal`, `StandardError=journal`) in addition to any existing log files, so operators can use `journalctl -u <unit>` without additional configuration.
- **FR-010**: The installer MUST be idempotent: running it twice on the same host (whether mid-upgrade or to repair state) MUST converge to the same end state and MUST NOT produce duplicate or conflicting systemd units or init.d symlinks.
- **FR-011**: The installer MUST detect a partially-completed prior conversion (init.d scripts removed but no systemd unit installed) and recover to a working systemd-managed state on the next run, rather than leaving the host unmanaged.
- **FR-012**: All behavior changes MUST be accompanied by automated tests: unit tests for the installer's detection / migration logic and an integration test that exercises `systemctl` against the generated units in a systemd-enabled container or VM. *(Per constitution principle III.)*
- **FR-013**: All changes MUST update module documentation (`modules/perc-distribution-tree` README, `system/release/installer/Linux` README, DTS distribution docs, and the appropriate `src/site/markdown/` pages) describing the new install / upgrade procedure and the supported-platform matrix.

### Key Entities *(include if feature involves data)*

- **Systemd Unit File**: A `.service` template file shipped in the distribution tree that describes how to start, stop, and supervise one CMS (or DTS) service. Identified by template name (e.g. `percussioncms@.service`) and instantiated as `percussioncms@<instance>.service`. Has `EnvironmentFile=`, `ExecStart=`, `ExecStop=`, `Restart=on-failure`, `RestartSec=30s`, `StartLimitBurst=`, `StartLimitIntervalSec=`, `User=`, `Group=`, `WantedBy=`, and LSB-equivalent metadata (`Description=`, `Documentation=`).
- **Environment File**: A small shell-sourced file (e.g. `/etc/percussion/cms-<instance>.env`) holding install-path, port, JVM options, and user/group overrides for one instance. Referenced from the unit via `EnvironmentFile=`; the `%i` specifier expands to the instance name.
- **Legacy Init.d Script**: An existing LSB-style init script with `BEGIN INIT INFO` headers, symlinked under `/etc/init.d/` or `/etc/rc.d/`. Identified by file presence and by `chkconfig`/`update-rc.d` registration. Tracked by the installer so it can be cleanly removed during upgrade.
- **Installer Manifest**: The existing installer configuration (under `modules/perc-ant` and `modules/perc-distribution-tree`) that decides which files to copy and which install/upgrade actions to run. Must be updated to install the unit file, remove init.d artifacts, and run `systemctl` commands.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A clean install of the CMS on a supported systemd Linux distribution completes end-to-end (installer invoked → `systemctl is-active percussioncms@default` returns `active` and the CMS login port responds) within 90 seconds on the reference VM (Ubuntu 22.04, 4 vCPU, 8 GB RAM, 20 GB disk, no other CMS instances) — measured by the integration test harness `docker/systemd-test/`.
- **SC-002**: An upgrade from a legacy init.d-based CMS install to the systemd-based release completes end-to-end without operator interaction beyond launching the installer (no prompts, no manual `systemctl` invocations, no `journalctl` monitoring required); afterwards `systemctl is-active percussioncms@default` returns `active`, `ls /etc/init.d/ | grep -i percussion` returns zero matches, and the CMS login port responds on the same port as before the upgrade.
- **SC-003**: On a supported systemd host, 100% of the standard operator lifecycle commands (`systemctl start|stop|restart|status|enable|disable` and `journalctl -u percussioncms`) succeed without falling back to init.d or wrapper scripts.
- **SC-004**: The distribution artifacts that previously included init.d scripts no longer contain them in this release (verifiable by `find modules/perc-distribution-tree -name '*.sh' | grep -i init.d` returning zero matches, and likewise for DTS distribution).
- **SC-005**: Automated test coverage for the installer's detection / migration logic reaches at least 85% line coverage on the new code paths, and the systemd integration test passes on a clean Ubuntu 22.04 (or equivalent) container.
- **SC-006**: Zero new high or critical security findings are introduced by the new units and install/upgrade path (no setuid wrappers, no world-writable env files, no log-injection vectors via `EnvironmentFile=`).

## Assumptions

- The underlying CMS runtime (Jetty-based) does not need to change. The change is purely in the *supervision* layer (how the OS starts/stops/monitors the CMS process), not in the CMS process itself.
- The systemd unit will reuse the existing Jetty/service-wrapper entry points (`StartJetty.sh` / `StopJetty.sh` / `rxjetty.sh`) so that JVM arguments, ports, and logging paths behave exactly as they do today.
- The repo currently builds under JDK 21 on the `development` branch (per `AGENTS.md`); the installer shell scripts and unit files are not JDK-sensitive, but the build wiring must still go through `mvn-env.sh`.
- Linux distributions without systemd (RHEL 7 / CentOS 7 / older Ubuntu LTS) are a minority of the installed base. The exact handling of these hosts is captured as clarification Q1.
- "Upgrade scenarios" means in-place upgrade of the CMS install — not a downgrade path from systemd back to init.d.
- The DTS staging/production services follow the same architectural pattern as CMS services, so a single design decision applies to both. If they diverge in implementation, that is a concern for `/speckit.plan`, not for this spec.
- An `EnvironmentFile=` per service is the accepted mechanism for parameterisation; we are not introducing a new templating system.
- The project does not need to ship a `systemd` *generator*; we ship static unit files that are parameterised via the env file at install time.

## Resolved Questions

All open questions raised during `/speckit.specify` were resolved during the `/speckit.clarify` pass (see the `## Clarifications` section at the top of this file). Resolutions, with rationale and downstream impact, for the record:

### Q1 — Supported-platform matrix: init.d fallback strategy

- **Question**: Do we (a) drop init.d support in the same release that introduces systemd, (b) keep init.d as a one-release fallback, or (c) keep init.d indefinitely as a documented alternative?
- **Answer**: **(a) Drop init.d support in the same release that introduces systemd.** No one-release overlap; no in-tree deprecated init.d fallback.
- **Rationale**: Cleanest cutover. Issue #962's wording ("remove the legacy init.d scripts after conversion to systemd") supports a single cutover. A one-release overlap would perpetuate two supervision paths and complicate the upgrade installer, which already has to detect-and-migrate legacy installs (FR-003).
- **Downstream impact**:
  - `FR-005` (supported-platform matrix): init.d-based hosts are NOT supported in this release; the upgrade installer is the only supported migration path.
  - `FR-007` (init.d removal): legacy scripts and installer references are physically deleted this release (no in-tree deprecated copy).
  - `SC-004`: `find … init.d` returns zero matches in the distribution tree.
  - The "Distribution without systemd" edge case (FR-005) is closed: non-systemd hosts are out of support; the installer refuses with exit code 2 and a clear message (covered by US1 negative-path test T013-b).
  - The "Open Questions" section is now collapsed to this resolved record; no live ambiguity.

### Q2 — Init.d removal scope: delete from source tree or keep deprecated?

- **Question**: In this release (the one that introduces systemd), should the legacy init.d scripts and their installer-manifest references be physically deleted, or kept in-tree marked `@Deprecated`/commented out?
- **Answer**: **Physically delete in this release.** No in-tree deprecated copy, no commented-out installer entries.
- **Rationale**: Matches issue #962's "remove the legacy init.d scripts" wording. Keeping a deprecated in-tree copy would create dead code that future readers mistake for live behavior, and would invite a future re-enable in the wrong place. Constitution V (Safe Modernization) favors incremental removal over preserving unused code when the cutover is intentional.
- **Downstream impact**:
  - `FR-007`: physical deletion of `system/release/installer/Linux/*.sh` and `system/release/installer/unix/*.sh` and the DTS `rootFiles/DTS*Service.sh`.
  - US2 implementation tasks T030 (CMS deletions) and T031 (DTS deletions) plus the manifest-cleanup task T032 (greps every reference before deletion).
  - The legacy-detection logic in the upgrade installer (US2 / T024 / T025) handles the *runtime* artifacts on customer hosts (`/etc/init.d/*`, `/etc/rc?.d/*`, `chkconfig`/`update-rc.d` registrations) — those are on customer machines, not in our source tree, and are removed by the installer on upgrade, not by FR-007.

### Q3 — Multi-instance support: in scope via template unit?

- **Question**: Is preserving the historical "prefix symlinks allow multiple CMS instances on one host" pattern in scope for the systemd unit, or do we standardise on one instance per host for v1?
- **Answer**: **In scope.** Ship `percussioncms@.service` as a systemd template unit with per-instance configuration at `/etc/percussion/cms-<instance>.env`. Single-instance installs use the instance name `default`. The DTS units follow the same template pattern (`percussiondts-staging@.service`, `percussiondts-production@.service`).
- **Rationale**: Multi-instance is rare but historically supported (the legacy `install-jetty-service.sh` already gates on `/etc/init.d/${SERVICE_NAME}` and can be re-run with different names). A systemd template unit is the standard idiom for "N instances of the same service on one host" — it costs almost nothing in unit complexity and avoids designing around an edge case that hasn't been validated against current installer behavior. The DTS staging and production services have always been two instances on the same host, so a single design decision covers CMS and DTS.
- **Downstream impact**:
  - `FR-004a`: template unit + per-instance env file is mandatory, not optional.
  - `FR-004` (parameterisation): the same unit file ships to every customer; per-instance values come from the env file.
  - The unit template is named `percussioncms@.service` (no hyphens — see note in `## Clarifications` / inline renames); the env file is `cms-<instance>.env`; the service name (after `@`) is the instance identifier (`default` for single, `instance2`, `instance3`, … for multi).
  - US2 task T028 (per-instance detection) and US1 task T016 (the template itself) reflect this.
  - The `EnvironmentFile=` line in the template uses the `%i` specifier, which systemd expands to the instance name at `systemctl start percussioncms@default` time.

### Out-of-scope clarifications deferred to planning

The following items were intentionally NOT promoted to `[NEEDS CLARIFICATION]` markers because they are tuning knobs best resolved in `/speckit.tasks` or during implementation, not in the spec:

- Exact `StartLimitBurst` / `StartLimitIntervalSec` numeric values (per Q3 the *policy* is fixed; the numbers are tuned against observed CMS startup behavior — currently 5 / 600s as a placeholder).
- Exact `TimeoutStartSec` / `TimeoutStopSec` values (per Q3 the *policy* is fixed; the numbers depend on Jetty startup time on the reference VM — currently 300s / 120s as a placeholder).
- Exact `EnvironmentFile=` directory mode/owner (per SC-006 the security property is fixed; the directory is `/etc/percussion/` with mode `0755 root:root` and per-file mode `0640 root:<group>` — see `contracts/env-file.md`).
- `systemd-nspawn` vs. privileged-Docker for the integration test harness (per Decision 8 in `research.md`; the docker harness is the default, nspawn is an option on systemd hosts).

These are recorded in `research.md` (Decisions 1, 3, 5, 8) and will be pinned as concrete values in `contracts/unit-template.md` once the integration test harness (US1 / T005) runs end-to-end on the reference VM.