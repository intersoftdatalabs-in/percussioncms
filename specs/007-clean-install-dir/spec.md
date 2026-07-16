# Feature Specification: Clean Obsolete Install Directories on Upgrade

**Feature Branch**: `985-clean-install-dir`  
**Created**: 2026-07-16  
**Status**: Draft  
**Input**: GitHub issue [#1157](https://github.com/intersoftdatalabs-in/percussioncms/issues/1157) — prompt users to delete obsolete installation folders during upgrade; flag `--clean-install-dir` (default false). Milestone 8.2; also relevant to 8.1.8 field observations.

## Clarifications

### Session 2026-07-16

- Q: Which obsolete folders are in scope for the first ship (MVP)? → A: `PreInstall` plus other already-referenced legacy stubs (e.g. `_Percussion_Installation`, `JBossServerXML_BAK`) if present
- Q: If cleanup fails for one or more candidate folders, what should the upgrade do? → A: Warn and continue (report failed paths; do not abort the upgrade)
- Q: When during the upgrade should cleanup of obsolete folders run? → A: Early, before main upgrade work (after upgrade detected / install root known)
- Q: If interactive upgrade AND `--clean-install-dir=true`, what happens? → A: Flag wins — no prompt; delete candidates

## Module Scope

- **Primary module(s)**: CMS command-line installer / upgrade path under `modules/perc-distribution-tree` (preinstall entry and install scripts that operate on an existing install root)
- **Secondary / integration modules**: related obsolete-folder cleanup already referenced by installer scripts (e.g. legacy `_Percussion_Installation` removal); DTS installer only if it shares the same install-root layout and obsolete paths (otherwise CMS upgrade path only for MVP)
- **AGENTS files to apply**: root `AGENTS.md`, `modules/perc-distribution-tree/AGENTS.md`
- **User roles affected**: integrator / operator performing **upgrades** of existing CMS installs (interactive and automated)
- **Install / upgrade impact**: **upgrade path only** — optional deletion of obsolete directories under the install root; new installs are unaffected (paths typically absent)

## User Scenarios & Testing

Each story must be independently testable.

### User Story 1 - Interactive upgrade offers cleanup of obsolete folders (Priority: P1)

An operator upgrading a long-lived production CMS finds that legacy folders (especially `PreInstall`, historically used for old-installer backups and observed at tens of gigabytes) still sit under the install root and waste disk. During an **interactive** upgrade, **early** (after upgrade is detected and the install root is known, **before** the main upgrade copy/unpack work), the installer detects which obsolete candidate folders exist, shows their names and the approximate disk space that would be freed, and asks whether to remove them. If the operator agrees, those folders are deleted immediately; if they decline, the upgrade continues without deleting them.

**Why this priority**: Production disk waste is the customer-visible pain in #1157; interactive consent prevents accidental data loss while making cleanup discoverable.

**Independent Test**: Point an interactive upgrade at an install root fixture that contains a large `PreInstall` tree (and optionally other candidates). Confirm the prompt lists folders and space; accept → folders gone and space reclaimed; decline → folders remain and upgrade still proceeds.

**Acceptance Scenarios**:

1. **Given** an existing install root with `PreInstall` present and interactive upgrade mode, **When** the operator starts the upgrade, **Then** they are shown a list of obsolete folders that exist on disk and an estimate of total space that would be freed.
2. **Given** that prompt, **When** the operator confirms cleanup, **Then** the listed folders are removed from the install root and the upgrade continues successfully.
3. **Given** that prompt, **When** the operator declines cleanup, **Then** no obsolete candidate folders are deleted and the upgrade continues successfully.
4. **Given** an install root with **none** of the candidate obsolete folders present, **When** interactive upgrade runs, **Then** the operator is not forced through a meaningless cleanup decision (no prompt, or a clear “nothing to clean” path that does not block the upgrade).

---

### User Story 2 - Automated upgrade cleans only when explicitly requested (Priority: P1)

Automation and scripts run upgrades non-interactively. They must not delete large directories by default. A flag `--clean-install-dir` defaults to **false**. When set to true, the upgrade deletes the same curated set of obsolete folders without an interactive prompt (the flag is the explicit consent for automation).

**Why this priority**: Issue #1157 requires safe defaults for automated installs while still enabling bulk cleanup in ops scripts.

**Independent Test**: Run non-interactive upgrade with flag false/absent (folders remain) and with flag true (folders removed) against a fixture install root.

**Acceptance Scenarios**:

1. **Given** an upgrade install root containing `PreInstall` (and/or other candidates), **When** upgrade runs without `--clean-install-dir` or with the flag false, **Then** those folders are **not** deleted.
2. **Given** the same root, **When** upgrade runs with `--clean-install-dir` true (or equivalent documented true form), **Then** existing candidate obsolete folders are deleted without requiring interactive input.
3. **Given** the flag true but no candidate folders exist, **When** upgrade runs, **Then** the upgrade succeeds and reports that nothing was cleaned (or equivalent non-error outcome).
4. **Given** an interactive (TTY) upgrade **and** `--clean-install-dir` true, **When** candidates exist, **Then** the installer does **not** prompt and still deletes candidates (flag overrides interactive confirm).

---

### User Story 3 - Curated MVP list includes PreInstall and known legacy stubs (Priority: P1)

Operators benefit from cleaning more than just `PreInstall` when other **already-referenced** obsolete install-root directories remain from pre-8.x / Jetty migration eras. For the first ship, the documented candidate list is:

1. `PreInstall` (primary; unused 8.x preinstall/backup tree)
2. `_Percussion_Installation` (or product-equivalent legacy install-metadata folder already targeted by existing cleanup scripts)
3. `JBossServerXML_BAK` (JBoss-era server.xml backup stub left by upgrades)

Only paths on this list that **exist** under the install root are considered. Broader leftover trees are out of MVP unless later approved.

**Why this priority**: Clarified for MVP — covers the production `PreInstall` incident plus stubs the product already treats as disposable, without inventing a wide folder set.

**Independent Test**: Fixture install root with each of the three candidates present (and live product dirs); cleanup removes only those three when present; `jetty/`, `rxconfig/`, repository data remain.

**Acceptance Scenarios**:

1. **Given** the documented obsolete-folder list is the MVP set above, **When** cleanup runs, **Then** only those relative paths that exist under the install root are deleted.
2. **Given** `PreInstall` and `JBossServerXML_BAK` exist and `_Percussion_Installation` does not, **When** cleanup is confirmed, **Then** only the two existing candidates are removed.
3. **Given** a normal live install tree (`jetty/`, `rxconfig/`, repository data, etc.), **When** cleanup runs, **Then** no required runtime or configuration directories are deleted.

---

### User Story 4 - Operators can see what was cleaned (Priority: P2)

After cleanup (or when cleanup is skipped), the operator can tell from install output whether folders were removed and how much space was reported as freeable / freed, so support and capacity planning can verify the action.

**Why this priority**: Transparency builds trust for a destructive optional step; aids remote support of the 36.9 GB class of incidents.

**Independent Test**: Capture installer console/log for accept, decline, flag-true, and nothing-to-do paths; assert presence/absence of folder names and space figures without silent deletion.

**Acceptance Scenarios**:

1. **Given** cleanup is performed, **When** the operator inspects install output, **Then** they see which folders were deleted and the approximate space associated with the cleanup decision.
2. **Given** cleanup is declined or the flag is false, **When** the operator inspects output, **Then** it is clear that obsolete folders were left in place (if any were found).

---

### Edge Cases

- What if a candidate folder is not empty or is very large (tens of GB)? Cleanup must still complete or fail with a clear error; partial failure must not leave the install root in an undocumented mixed state without reporting which paths failed.
- What if a candidate path is a file rather than a directory, or a symlink? Only treat as removable if it matches the documented obsolete name/role; do not follow symlinks outside the install root.
- What if permissions prevent deletion? Fail that path with a clear message; **do not abort the upgrade** — report cleanup failures and continue (MVP policy: warn-and-continue).
- What if the operator runs **new install** (empty target) with `--clean-install-dir`? No-op success; no destructive action outside the install root.
- What if interactive mode cannot prompt (no TTY)? Treat like non-interactive: do **not** delete unless `--clean-install-dir` is true.
- What if cleanup runs mid-upgrade after the product has already written into a path that shares a name? The list must only include paths that remain obsolete for 8.x; planning must verify against current upgrade copy/delete logic.

## Requirements

### Functional Requirements

- **FR-001**: During a CMS **upgrade**, the installer MUST detect the MVP **obsolete directory** set under the install root when present: `PreInstall`, `_Percussion_Installation` (legacy install-metadata name as used by existing cleanup scripts), and `JBossServerXML_BAK`. Planning MUST confirm exact on-disk names against current installer scripts before implementation freezes the list.
- **FR-002**: The installer MUST support a command-line flag `--clean-install-dir` that defaults to **false**. When true on upgrade, the installer MUST delete existing candidate obsolete directories without requiring interactive confirmation.
- **FR-014**: Obsolete-folder detection, interactive prompt (if any), and deletion MUST run **early** in the upgrade flow: after the installer has determined that the operation is an upgrade of the given install root, and **before** the main upgrade file copy / unpack / schema steps.
- **FR-003**: In **interactive** upgrade mode, when at least one candidate obsolete folder exists **and** `--clean-install-dir` is **not** true, the installer MUST present: (a) the list of candidate folders that exist, (b) an estimate of total disk space that would be freed, and (c) a clear yes/no decision to proceed with deletion. When `--clean-install-dir` is true, the installer MUST skip the prompt and perform cleanup (flag is explicit consent).
- **FR-004**: If the operator declines the interactive cleanup prompt, the installer MUST NOT delete those folders and MUST continue the upgrade.
- **FR-005**: If the operator accepts the interactive cleanup prompt, the installer MUST delete the listed candidate folders that exist under the install root.
- **FR-006**: When no candidate obsolete folders exist, the installer MUST NOT block the upgrade and MUST NOT require a cleanup decision.
- **FR-007**: The obsolete-folder list MUST be documented for integrators (which relative paths are candidates and that 8.x does not need them for runtime).
- **FR-008**: Cleanup MUST NOT delete required live product directories (for example current Jetty base, `rxconfig` configuration, repository data, and other paths required for normal 8.x operation).
- **FR-009**: Non-interactive upgrades without `--clean-install-dir=true` MUST leave candidate folders in place (safe default for automation).
- **FR-010**: Cleanup actions and skip decisions MUST be visible in installer output (folders considered, approximate space, deleted or retained).
- **FR-011**: Space estimates MUST be approximate but directionally correct (within a reasonable tolerance of actual reclaimed space for large trees; exact byte match not required).
- **FR-012**: New installs MUST remain correct when the flag is present or absent (no harmful deletion outside the target install root; typically nothing to clean).
- **FR-013**: If deletion of one or more candidate folders fails (permissions, locks, I/O errors), the installer MUST report each failed path clearly, MUST leave remaining candidates best-effort cleaned, and MUST **continue the upgrade** (cleanup failure MUST NOT by itself fail the upgrade process).

### Key Entities

- **Install root**: Directory of the existing CMS installation being upgraded.
- **Obsolete folder candidate**: A relative path under the install root on the MVP list: `PreInstall`, `_Percussion_Installation` (or documented equivalent), `JBossServerXML_BAK` — known not required by current 8.x runtime after migration eras.
- **Cleanup decision**: Interactive yes/no or automated flag true/false controlling whether candidates are deleted.
- **Space estimate**: Reported approximate size of candidates present before deletion.

## Success Criteria

### Measurable Outcomes

- **SC-001**: On a fixture or production-like install root containing a multi-gigabyte `PreInstall` tree, an operator who accepts interactive cleanup (or sets `--clean-install-dir` true) ends with `PreInstall` absent and measurable free space increase on the volume hosting the install root.
- **SC-002**: Automated upgrade with default flags never deletes obsolete folders (100% of test runs with flag false/absent leave candidates intact).
- **SC-003**: Interactive operators who decline cleanup retain 100% of candidate folders while still completing upgrade.
- **SC-004**: When candidates exist, interactive prompt always shows both folder names and a non-zero space estimate before deletion consent.
- **SC-005**: Documented obsolete list names the MVP set (`PreInstall`, `_Percussion_Installation` / equivalent, `JBossServerXML_BAK`); support can verify from docs without reading source.
- **SC-006**: No cleanup path removes required live product directories in regression tests against a minimal valid 8.x install layout.
- **SC-007**: When a candidate folder is made undeletable in a test fixture and cleanup is requested, the upgrade still completes successfully and output names the path that could not be removed.

## Assumptions

- Scope is **CMS upgrade** of an existing install root; new installs are out of the primary UX (no-op if candidates missing).
- Interactive mode means a real console capable of prompting; headless/CI without TTY does not invent a default “yes” — only the explicit flag enables deletion. If both interactive TTY and `--clean-install-dir=true` apply, the flag wins (no second confirmation prompt).
- `PreInstall` (including historical backup content under it) is obsolete for all 8.x lines and safe to remove from a capacity standpoint; operators who still need those backups must copy them elsewhere before accepting cleanup (call out in prompt text).
- MVP candidate list is fixed as `PreInstall`, `_Percussion_Installation` (or the exact name already used by remove/cleanup scripts), and `JBossServerXML_BAK`. Planning confirms none are required mid-upgrade after cleanup timing is chosen; broader leftovers are out of MVP.
- Approximate space calculation (e.g. recursive size before delete) is sufficient; OS free-space polling after delete is optional.
- Deletion is best-effort: cleanup errors are reported per path; upgrade always continues after cleanup attempts (warn-and-continue policy).
- DTS-specific install roots may share `PreInstall` naming; full DTS parity can follow the same list if the path is under the same root, otherwise CMS-first.

## Out of Scope

- Changing default upgrade behavior to always delete without consent or flag.
- Cleaning arbitrary user folders not on the curated obsolete list.
- Archiving/exporting `PreInstall` content to external storage (operator responsibility before accept).
- GUI-only installer UX redesign beyond the upgrade CLI/interactive path described in #1157.
- Compressing or relocating live Jetty/AppServer data as a substitute for deletion.

## Dependencies

- Ability to distinguish **upgrade** vs **new install** in the existing installer (already present for other upgrade behaviors).
- Accurate curated list of obsolete relative paths validated against current 8.x upgrade scripts; cleanup runs early so remaining upgrade steps must not require those paths after deletion.
- Issue tracking: [#1157](https://github.com/intersoftdatalabs-in/percussioncms/issues/1157).
