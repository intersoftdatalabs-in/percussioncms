# Feature Specification: CLI Installer Database Targets for New Installs

**Feature Branch**: `984-installer-db-targets`  
**Created**: 2026-07-15  
**Status**: Draft  
**Input**: GitHub issue [#949](https://github.com/intersoftdatalabs-in/percussioncms/issues/949) — "Add support for different database targets to the command line installer for new installs." Migrated from percussion/percussioncms#633. Milestone 8.2.

## Module Scope

- **Primary module(s)**: `modules/perc-distribution-tree` (distribution install scripts and preinstall entry point), `modules/perc-ant` (install-time repository/datasource configuration actions)
- **Secondary / integration modules**: `modules/TableFactory` (repository property / JDBC setup patterns already used with `-dbprops`), default and sample repository property resources under distribution `rxconfig/Installer/`; related JDBC packaging already covered by `specs/001-fix-jdbc-drivers`
- **AGENTS files to apply**: root `AGENTS.md`, `modules/perc-distribution-tree/AGENTS.md` (and any local overrides under `modules/perc-ant` if present)
- **User roles affected**: integrator / operator performing unattended or scripted **new** CMS installs; automation (CI/Docker) that currently depends on Derby-only fresh installs
- **Install / upgrade impact**: **new install path only** — effective repository configuration written for the selected RDBMS; upgrade path behavior must remain unchanged (issue states upgrades already handle non-Derby backends)

## User Scenarios & Testing

Each story must be independently testable.

### User Story 1 - Fresh install to an enterprise RDBMS via property file (Priority: P1)

An integrator is deploying Percussion CMS for the first time in an environment that uses MySQL (or MariaDB-compatible), SQL Server, or Oracle—not the embedded Derby database. They prepare a repository properties file in the same format as `rxconfig/Installer/rxrepository.properties` (connection host/server, schema/database, credentials, backend, driver identifiers as applicable) and pass its path to the command-line installer (for example, via a system property such as `-Ddbprops=<path>`). After a successful install, the CMS is configured to use that database for the repository without hand-editing installed config files.

**Why this priority**: Today, new CLI installs default to embedded Derby. Customers and automation that need enterprise databases cannot complete a correct first install without unsupported post-install rework. This is the core gap described in issue #949.

**Independent Test**: On a clean target host (or disposable install root), run a new install with a valid properties file targeting a pre-provisioned supported non-Derby database. Assert that install completes successfully and that the effective repository configuration in the install root matches the intended backend and connection settings (without requiring manual edits after install).

**Acceptance Scenarios**:

1. **Given** a clean install root and a valid repository properties file for MySQL/MariaDB, **When** the integrator runs a new command-line install supplying that file as the database target input, **Then** the install completes successfully and the effective CMS repository configuration identifies MySQL/MariaDB (or the product’s equivalent backend label) with the supplied connection and credential settings.
2. **Given** the same setup for SQL Server, **When** a new CLI install is run with a valid SQL Server properties file, **Then** the install completes successfully and the effective repository configuration matches SQL Server and the supplied settings.
3. **Given** the same setup for Oracle, **When** a new CLI install is run with a valid Oracle properties file, **Then** the install completes successfully and the effective repository configuration matches Oracle and the supplied settings.
4. **Given** a successful non-Derby new install, **When** the integrator inspects the install root’s repository configuration (for example under `rxconfig/Installer/rxrepository.properties` or the documented effective location), **Then** they do **not** need to manually rewrite backend, server, schema, driver, or credential fields to match the intended database.

---

### User Story 2 - Default remains Derby when no alternate target is supplied (Priority: P1)

An integrator (or automation) runs a new command-line install **without** supplying an alternate database properties path. Behavior remains the familiar embedded Derby default so existing docs, demos, and scripts do not break.

**Why this priority**: Backward-compatible default is required so the feature is additive; issue #949 frames enterprise targets as an **option**, not a forced change of default.

**Independent Test**: Run a new CLI install with no database-target override; assert repository configuration remains Derby-oriented as today and install succeeds on a clean root.

**Acceptance Scenarios**:

1. **Given** a clean install root and no database-target property file argument, **When** the integrator runs a new command-line install, **Then** the install uses the embedded Derby default and completes successfully as it does today.
2. **Given** such a default install, **When** the effective repository configuration is inspected, **Then** the backend identifies Derby (or the product’s equivalent default labels) consistent with current shipped defaults.

---

### User Story 3 - Invalid or incomplete database target fails fast with clear guidance (Priority: P2)

An integrator supplies a properties file that is missing, unreadable, incomplete for the chosen backend, or points at a database that cannot be reached with the given credentials. The installer stops during the new-install path with a clear, actionable message rather than leaving a half-configured install that looks successful.

**Why this priority**: Silent misconfiguration forces long debug cycles and unsafe “green” installs. Fail-fast is essential for unattended automation.

**Independent Test**: Run new installs with deliberately bad inputs (missing file, missing required keys, wrong password / unreachable host) and assert non-zero failure and message content that identifies the problem class without printing secrets.

**Acceptance Scenarios**:

1. **Given** a new install invocation that references a database properties path that does not exist or is unreadable, **When** install starts, **Then** it fails early with a message that names the path problem.
2. **Given** a properties file missing required fields for a non-Derby backend (for example server identity or credentials), **When** install runs, **Then** it fails with a message listing what is required for that backend.
3. **Given** a well-formed properties file whose connection cannot be established (wrong host, credentials, or database not provisioned), **When** install runs, **Then** it fails with a user-readable connectivity/auth failure and does **not** report overall install success.
4. **Given** any of the failure cases above, **When** install output and logs are inspected, **Then** passwords and other secret values from the properties file are not printed in clear text.

---

### User Story 4 - Documented property-file contract and samples for integrators (Priority: P2)

An integrator needs to know which keys to set, which backends are supported for new CLI installs, how to pass the file path on the install command line, and what a minimal working example looks like for each supported enterprise backend.

**Why this priority**: Without a documented contract, the feature is unusable outside the team that implemented it; issue #949 explicitly points at the existing `rxrepository.properties` shape as the simplicity model.

**Independent Test**: Review installer documentation (distribution README, install help text, and/or sample property files shipped with the distribution) and verify each supported backend has an example and that the CLI flag/system property name is documented.

**Acceptance Scenarios**:

1. **Given** the product documentation for the command-line installer, **When** an integrator looks up database targeting for new installs, **Then** they find the input mechanism (path to properties file / system property name), supported backends, and required keys.
2. **Given** sample property files (or equivalent documented examples) for MySQL/MariaDB, SQL Server, and Oracle, **When** an integrator copies and fills credentials for their environment, **Then** they can complete User Story 1 without reverse-engineering installed defaults.
3. **Given** the documentation, **When** an integrator reads the upgrade section (or notes), **Then** it is clear that this feature applies to **new installs** and that upgrades continue to use existing repository configuration as today.

---

### User Story 5 - Upgrade path unchanged (Priority: P1)

An operator upgrading an existing CMS installation that already uses MySQL, SQL Server, Oracle, or Derby does not need a new database-target flag and does not have their repository configuration overwritten by Derby defaults.

**Why this priority**: Issue #949 states upgrades already handle different databases; regressing upgrades would be a high-severity release blocker.

**Independent Test**: Run an upgrade against an install root whose repository configuration already targets a non-Derby backend (fixture or representative config); assert backend remains the pre-upgrade value and upgrade completes without requiring `-Ddbprops`.

**Acceptance Scenarios**:

1. **Given** an existing install whose repository is configured for a non-Derby backend, **When** the operator runs the standard upgrade path without a new-install database-target override, **Then** the repository backend and connection settings remain that non-Derby configuration.
2. **Given** an existing Derby-based install, **When** upgrade runs without database-target override, **Then** Derby configuration is preserved as today.

---

### Edge Cases

- What happens when the properties file path is relative vs absolute? The installer must resolve it predictably (documented rule: relative paths relative to the process working directory unless docs specify otherwise) and fail clearly if resolution fails.
- What happens when the file uses the same key names as `rxrepository.properties` but with unexpected casing or whitespace? Behavior should match existing repository-properties parsing conventions; invalid/unknown backend labels fail with a clear list of accepted values.
- What happens when the target database exists but the schema/user lacks permission to create objects required by repository setup? Install fails with a permission/setup error, not a silent partial schema.
- What happens when the integrator selects a backend whose JDBC driver is not present in the distribution’s driver location? Install fails with guidance to place the appropriate driver where the product documents (consistent with JDBC driver packaging work), rather than succeeding with a broken runtime.
- What happens if both a database-target properties path and conflicting interactive or legacy defaults would apply? For new installs, the explicit properties-file input takes precedence over the shipped Derby defaults; precedence is documented.
- What happens on a second “new install” into a non-empty install root? Existing product rules for re-install vs refuse apply; this feature must not bypass them solely to force a different database.
- What happens if the properties file contains only a subset of keys? Missing optional keys may fall back to documented defaults where safe; missing required keys for the selected backend fail validation (User Story 3).

## Requirements

### Functional Requirements

- **FR-001**: For a **new** command-line install, the installer MUST accept an explicit database-target input that is a filesystem path to a properties file whose key set is compatible with the existing `rxconfig/Installer/rxrepository.properties` format (including at least backend identity, server/connection identity, schema/database name as applicable, driver name/class as applicable, and credentials).
- **FR-002**: The documented command-line mechanism for supplying that path MUST be a system property or equivalent install flag of the form `-Ddbprops=<path>` (or the product’s chosen equivalent name if an existing install property already serves this role—see Assumptions); the name and usage MUST be documented for integrators.
- **FR-003**: When the database-target input is supplied on a new install, the installer MUST configure the CMS repository for the selected backend such that post-install effective configuration matches the intended backend without manual file editing.
- **FR-004**: Supported backends for this new-install option MUST include at least: embedded Derby (default when no override), MySQL (including MariaDB-compatible configurations as already supported by the product), Microsoft SQL Server, and Oracle.
- **FR-005**: When no database-target input is supplied on a new install, the installer MUST retain the current embedded Derby default behavior.
- **FR-006**: The **upgrade** path MUST continue to honor the existing installation’s repository configuration and MUST NOT require the new database-target input to preserve non-Derby backends.
- **FR-007**: On new install with a database-target input, the installer MUST validate that the properties file is readable and that required fields for the selected backend are present before performing irreversible repository setup steps that depend on that configuration.
- **FR-008**: On new install with a database-target input, the installer MUST attempt to verify database connectivity (or equivalent pre-flight) using the supplied settings and MUST fail the install if connectivity/authentication fails, with a user-readable error that does not print passwords.
- **FR-009**: Installer and related logs MUST NOT emit clear-text passwords or other secret property values from the database properties file.
- **FR-010**: The distribution or installer documentation MUST include: how to pass the database-target file, which backends are supported for new CLI installs, required property keys per backend (or a single shared key list with backend-specific notes), and sample property files or copy-paste examples for MySQL/MariaDB, SQL Server, and Oracle.
- **FR-011**: After a successful new install with a database-target input, repository schema/setup steps that the installer already performs for Derby MUST run against the selected backend using the product’s existing multi-database install/setup capabilities (the same class of capability upgrades already rely on), without requiring the integrator to run separate undocumented tools for basic repository creation.
- **FR-012**: If the selected backend requires a JDBC driver that is not available in the install’s documented driver location, the installer MUST fail with an actionable message rather than reporting success.

### Key Entities

- **Database target properties file**: Integrator-supplied file describing the CMS repository RDBMS target for a new install; shape aligns with `rxrepository.properties` (backend, server, schema/name, driver identifiers, credentials, datasource name as applicable).
- **Effective repository configuration**: The post-install configuration under the install root that the CMS uses to connect to its repository (including `rxconfig/Installer/rxrepository.properties` and any immediately derived runtime datasource settings the install already writes today).
- **New install vs upgrade**: Install modes distinguished by product rules; this feature’s alternate database targeting applies to **new** installs; upgrades retain existing repository configuration.
- **Supported backend**: One of the RDBMS platforms the product supports for the CMS repository on new CLI install: Derby, MySQL/MariaDB-compatible, SQL Server, Oracle.

## Success Criteria

### Measurable Outcomes

- **SC-001**: An integrator can complete a new CLI install against MySQL/MariaDB, SQL Server, or Oracle using only the documented properties-file input and a pre-provisioned empty (or product-allowed) database, with zero manual post-install edits to repository connection settings, in a single install invocation.
- **SC-002**: A new CLI install with no database-target input continues to succeed with Derby defaults on a clean install root (no regression vs current default path).
- **SC-003**: 100% of scripted negative tests for missing file, incomplete required keys, and failed connectivity result in install failure (non-success exit) and operator-visible error text that identifies the failure class.
- **SC-004**: Spot-check of install console output and primary install log for successful and failed runs shows no clear-text password values from the properties file.
- **SC-005**: Upgrade of a non-Derby existing install completes without supplying the new database-target input and retains the pre-upgrade backend identity in effective repository configuration.
- **SC-006**: A new integrator following only product documentation (no source code) can produce a valid properties file for at least one non-Derby backend and complete SC-001 within one working session (documentation completeness).
- **SC-007**: Automated verification (unit and/or install-level tests) covers at least: default Derby new install, one non-Derby new-install configuration path (properties applied to effective config), validation failure for missing/invalid input, and upgrade non-regression for existing repository config.

## Assumptions

- Scope is the **CMS command-line installer new-install path** described in issue #949; interactive GUI installer changes are out of scope unless they share the same underlying new-install configuration path and pick up the behavior automatically.
- Delivery Tier Service (DTS) datasource configuration is **out of scope** for this specification unless the CMS new-install flow already necessarily writes DTS datasource files in the same pass—in that case, only parity needed to avoid leaving DTS on Derby while CMS is non-Derby is in scope; a full DTS-specific input contract is otherwise a follow-on.
- Property key names and semantics match existing `rxrepository.properties` / table-factory conventions already used by upgrades and tools (`DB_BACKEND`, `DB_SERVER`, `DB_SCHEMA`, `DB_NAME`, `UID`, `PWD`, driver fields, etc.), rather than inventing a parallel schema.
- The issue’s suggested `-Ddbprops=<path>` is the preferred integrator-facing contract; if the preinstall entry point already reserves a different property name for the same purpose, planning may adopt the existing name provided documentation is unambiguous and equivalent.
- “MySQL” in the issue includes MariaDB-compatible deployments already used as the product’s common non-Derby repository (consistent with shipped JDBC packaging).
- Target empty database / schema is pre-created by the customer’s DBA when the backend requires it; the installer creates product objects inside that database, not the RDBMS server instance itself.
- Supported backend list for new install matches platforms the product already supports for upgrades/runtime; this feature does not add a new RDBMS engine.
- Security: properties files on disk remain the integrator’s responsibility to protect (file permissions); the installer’s obligation is no secret leakage in logs/console and fail-fast validation.
- Related work: JDBC drivers must be present in the distribution for bundled backends (see `specs/001-fix-jdbc-drivers`); this feature depends on drivers being available or on clear failure when they are not.
- Prior internal notes under `docs/ai-generated/tasks/PR#-installer-db-target-enhancement/` inform context but do **not** expand this spec to env-var precedence matrices, SSL flag defaults, or multi-input CLI surfaces beyond the properties-file contract unless required for FR compliance.

## Out of Scope

- Changing the default new-install database away from Derby.
- Migrating data from Derby (or any backend) to another RDBMS after install.
- Redesigning upgrade database handling (already considered adequate per issue #949).
- Adding new RDBMS platforms beyond those the product already supports.
- Full DTS installer redesign, SSL policy product changes, or a multi-source env/CLI precedence system beyond the properties-file input (unless discovered as already required for a correct CMS-only new install).
- GUI/wizard UX redesign for database selection (except incidental shared-code benefits).

## Dependencies

- Supported JDBC drivers available in the install tree for backends that ship drivers; integrator-supplied drivers for cases the product documents as bring-your-own.
- Pre-provisioned database/schema and network access from the install host to the RDBMS for non-Derby targets.
- Existing multi-database repository setup/table-factory behavior used on upgrades must remain usable from the new-install path once configuration is written correctly.
- Issue tracking: [GitHub #949](https://github.com/intersoftdatalabs-in/percussioncms/issues/949); milestone 8.2.
