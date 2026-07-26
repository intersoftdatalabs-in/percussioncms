# Feature Specification: Replace Retired Default Embedded Repository

**Feature Branch**: `548-derby-embedded-migration`  
**Created**: 2026-07-23  
**Status**: Clarified  
**Input**: "Replace retired Apache Derby as the default embedded repository for CMS and DTS. New installs and upgrades must land on a maintained embedded engine with multiuser access and locking comparable to Derby. Automatic, safe migration of existing Derby customer data on upgrade. Document backup/restore. MySQL/SQL Server external configs remain supported and unchanged. Tracked as GH Issue #548; major 8.2 non-UI lift."  
**Tracked as**: [GitHub Issue #548](https://github.com/intersoftdatalabs-in/percussioncms/issues/548)

## Clarifications

### Session 2026-07-23

- Q: Pre-upgrade backup policy before Derby migration → A: Hard gate — migration starts only after product backup succeeds **or** operator confirms a verified external backup exists (Option B)
- Q: Post-migration Derby file retention after successful cutover → A: Retain indefinitely until operator runs documented cleanup; product may offer optional cleanup step; never auto-delete without operator action (Option B)
- Q: Steady-state backup mode for the new default embedded repository → A: Offline only — product must be stopped for supported consistent full backup and restore (Option A)
- Q: How long must upgrades still migrate from Derby after GA → A: GA release that introduces the new default **plus one** subsequent product line; then remove Derby migration with prior deprecation notice (Option B)
- Q: Minimum concurrent-user target for multiuser acceptance on default CMS → A: **10 concurrent editors** as the hard acceptance floor (Option B)

## Module Scope

- **Primary module(s)**: CMS core repository configuration and data access (`system/`), schema/data tooling (`modules/TableFactory`), CMS runtime packaging (`modules/perc-jetty`), installer / distribution tree pieces that select or wire the default repository
- **Secondary / integration modules**: Delivery Tier Suite services and DTS distribution (default embedded repository for metadata, forms, comments, feeds, membership, polls, and related services); upgrade plugins and install resources; shared JDBC utilities and repository property templates; product documentation for install, upgrade, backup, and restore
- **AGENTS files to apply**: root `AGENTS.md`; module-local `AGENTS.md` / `AGENTS.local.md` for each touched module (especially `system/`, TableFactory, DTS modules, installer/distribution)
- **User roles affected**: CMS administrators and operators (install/upgrade/backup/restore); CMS editors and publishers (day-to-day multiuser content work on default installs); DTS operators; support engineers; external-database customers (must remain unaffected)
- **Install / upgrade impact**: **schema + config + distribution tree** — new default repository engine for zero-admin installs; automatic data migration for existing default-repository (Derby) sites; installer and upgrade paths updated; backup/restore procedures updated. External MySQL / SQL Server configurations remain supported and unchanged.

## User Scenarios & Testing

Each story must be independently testable.

### User Story 1 - New install uses maintained default repository (Priority: P1)

As a CMS or DTS administrator installing a new 8.2 instance without providing an external database, I get a supported, maintained default repository that does not depend on the retired Derby project, and the product is immediately usable for multiuser editorial and delivery workloads.

**Why this priority**: New installs must not ship on an abandoned engine; this is the forward default for the majority of zero-admin deployments.

**Acceptance Scenarios**:

1. **Given** a clean host with no external database, **When** the administrator completes a standard CMS install accepting defaults, **Then** the CMS starts successfully against the new default embedded repository and basic admin login and content operations succeed.
2. **Given** a clean host with no external database, **When** the administrator completes a standard DTS install accepting defaults, **Then** DTS services start successfully against the new default embedded repository and core delivery service health checks succeed.
3. **Given** a new default install, **When** multiple concurrent editorial or service sessions perform normal reads and writes, **Then** the system remains consistent (no data corruption) and contention is handled without unexplained permanent failure (timeouts or retries may occur under heavy contention; silent loss of updates must not).

---

### User Story 2 - Upgrade migrates Derby data automatically and safely (Priority: P1)

As an administrator upgrading an existing CMS and/or DTS installation that still uses the default Derby-backed repository, the upgrade automatically moves all customer repository data to the new default engine without requiring a separate manual export/import project, and I can resume production use with content, security, workflow, and configuration intact.

**Why this priority**: A large share of the installed base remains on Derby; migration fidelity is the release-risk center of this feature.

**Acceptance Scenarios**:

1. **Given** a CMS installation on the prior release using default Derby with representative production-like content (items, folders, relationships, workflow state, users/roles/ACLs, site and publish configuration as applicable), **When** the administrator runs the supported 8.2 upgrade path, **Then** the upgrade completes with the repository on the new default engine and a post-upgrade verification checklist (login, browse, open, edit/save, workflow transition, search/list as applicable) succeeds without missing or corrupted core entities.
2. **Given** a DTS installation on the prior release using default Derby with representative service data (e.g. metadata indexes, forms, comments, membership, feeds, polls as deployed), **When** the administrator runs the supported 8.2 DTS upgrade path, **Then** services start on the new default engine and prior service data remains available and consistent with pre-upgrade behavior for supported features.
3. **Given** an upgrade that detects existing Derby data, **When** migration is about to start, **Then** the upgrade enforces a hard gate: either a product-produced pre-migration backup has completed successfully, or the operator has explicitly confirmed that a verified external backup already exists; migration does not begin until one of those conditions is met.
4. **Given** an upgrade that detects existing Derby data and has passed the backup gate, **When** migration runs, **Then** the process is automatic (no separate customer-built migration toolkit required) and progress/outcome is visible in upgrade logs with a clear success or failure result.
5. **Given** migration fails partway (simulated I/O or validation failure), **When** the upgrade aborts, **Then** the pre-upgrade Derby repository remains intact and usable for recovery (no silent half-migrated production cutover), and the failure message tells the operator what failed and that the prior repository was not discarded.

---

### User Story 3 - External database customers are unaffected (Priority: P1)

As an administrator whose CMS or DTS already uses MySQL or SQL Server, upgrading to 8.2 does not force a repository engine change, does not rewrite my external connection configuration, and does not require embedded-repository migration steps.

**Why this priority**: External RDBMS customers must not absorb risk or downtime from a change aimed at the embedded default.

**Acceptance Scenarios**:

1. **Given** a CMS on MySQL (or SQL Server) with valid repository configuration, **When** the administrator upgrades to 8.2, **Then** the product continues to use that external database with the same logical configuration (host, database, credentials, schema as already defined) and no Derby/default-embedded migration is invoked.
2. **Given** a DTS on MySQL (or SQL Server), **When** upgraded to 8.2, **Then** external datasource configuration remains in effect and services start without requiring the new embedded default.

---

### User Story 4 - Multiuser access and locking parity for default installs (Priority: P1)

As editors and background services on a default embedded install, we can work concurrently with locking behavior that is at least as safe as the prior Derby default for content edit, workflow, and related contended operations—without requiring an external database solely to get safe multiuser behavior.

**Why this priority**: Derby’s multiuser and locking characteristics are why many sites never moved to external DBs; regressing that forces unexpected production incidents or emergency DB migrations.

**Acceptance Scenarios**:

1. **Given** a default embedded CMS with a realistic connection pool and **at least 10 concurrent interactive editor sessions**, **When** they edit different items and the same item under normal product locking rules, **Then** outcomes match product rules (successful exclusive edit, clear lock/conflict behavior, no lost updates).
2. **Given** concurrent CMS and/or DTS write activity on default embedded repositories at or below documented supported levels (CMS multiuser acceptance floor: **10 concurrent editors**), **When** the load test runs, **Then** the system does not exhibit silent corruption and recovers cleanly from lock wait timeouts.

---

### User Story 5 - Backup and restore are documented and operable (Priority: P2)

As an administrator or support engineer, I can back up and restore a default embedded repository using product-documented **offline** procedures that work on Windows, Linux, and macOS deployments, and I can confirm restore by bringing the instance back to a working state.

**Why this priority**: Operability and supportability are required for a default data store change; without docs and a verified procedure, migration success still leaves customers unsafe.

**Acceptance Scenarios**:

1. **Given** a default-embedded CMS (or DTS) install that has been **stopped** per documentation, **When** the operator follows the documented offline backup procedure, **Then** a complete backup artifact set is produced; documentation states that supported consistent full backup requires the instance to be stopped (no supported online/hot full backup).
2. **Given** a valid offline backup and a target install of the same product major version with the instance stopped, **When** the operator follows the documented offline restore procedure and then starts the instance, **Then** the instance starts and representative verification checks pass (login and sample data access).
3. **Given** the product documentation set for 8.2, **When** an operator searches for default repository backup/restore and for Derby upgrade migration, **Then** both topics are findable and describe supported offline steps, prerequisites (including stop/start), and failure recovery at a level support can follow without source code.

---

### User Story 6 - Support and release communications (Priority: P3)

As a customer or partner, I understand that Derby is retired upstream, what happens on upgrade, that external MySQL/SQL Server paths are unchanged, how long Derby migration remains available, and where to find migration and backup guidance—before or during the 8.2 upgrade window.

**Acceptance Scenarios**:

1. **Given** 8.2 release notes / upgrade guide material, **When** a reader reviews default repository changes, **Then** they see: Derby retirement context, automatic migration for default Derby installs, external DB unchanged, the Derby migration support window (GA + one subsequent product line), and links to backup/restore and troubleshooting.

---

### Edge Cases

- **Empty or minimal Derby repository**: Upgrade still succeeds and leaves a valid new default repository.
- **Large repositories**: Migration completes successfully within a documented time expectation class (see Success Criteria); operator sees progress; failure mid-run does not destroy the source Derby data.
- **Insufficient disk space for migration copy**: Upgrade fails with a clear disk-space (or capacity) message before destroying source data.
- **Backup gate not satisfied**: If neither a successful product pre-migration backup nor an explicit operator confirmation of a verified external backup is present, migration MUST NOT start; the upgrade reports a clear blocking error.
- **Already on new default engine**: Re-running upgrade or repair does not double-migrate or corrupt data (idempotent / “already migrated” safe behavior).
- **Mixed estate**: CMS on Derby default and DTS on external MySQL (or the reverse)—each component follows its own configuration; only Derby-default components migrate.
- **Custom or non-product Derby usage outside product datasources**: Out of scope unless product-owned paths; product documents only product-managed repositories.
- **Concurrent upgrade attempts**: Second upgrade while first is migrating is rejected or serialized with a clear message.
- **Online backup attempts**: Operators attempting backup while the instance is running are outside the supported path; documentation MUST state stop-first; product tools that perform product-managed pre-migration backup (FR-018) MUST use a safe offline or upgrade-controlled procedure consistent with consistency requirements.
- **Post-migration Derby files**: After successful cutover, source Derby data files are retained until the operator runs a documented cleanup (or an optional product cleanup step). The product MUST NOT auto-delete retained Derby files without operator action.
- **Unsupported multi-node shared embedded repository**: Product does not claim multi-server shared-file embedded clustering (same class as prior Derby default: one primary process / deployment unit).

## Requirements

### Functional Requirements

- **FR-001**: New CMS installs that accept the default repository option MUST provision and use a maintained embedded default repository that is not Apache Derby.
- **FR-002**: New DTS installs that accept the default repository option MUST provision and use a maintained embedded default repository that is not Apache Derby.
- **FR-003**: The default embedded repository MUST support multiuser concurrent access appropriate for default-install positioning (multiple connections from the product process; safe concurrent editorial and service workloads). For CMS acceptance, this MUST include at least **10 concurrent interactive editor sessions** without silent lost updates or repository corruption.
- **FR-004**: The default embedded repository MUST support locking behavior sufficient for existing product rules for contended content and workflow operations (no silent lost updates under supported concurrency, including the 10-editor CMS acceptance floor).
- **FR-005**: CMS upgrades from a prior release that still use product-managed Derby as the repository MUST automatically migrate all product repository data required for normal operation onto the new default embedded repository as part of the supported upgrade path.
- **FR-006**: DTS upgrades from a prior release that still use product-managed Derby as the repository MUST automatically migrate all product-managed service data required for normal operation onto the new default embedded repository as part of the supported upgrade path.
- **FR-007**: Automatic migration MUST be fidelity-preserving for product-managed data: content and structure required for login, authorization, editorial, workflow, site/publishing configuration (CMS), and deployed DTS service data remain available and correct after upgrade.
- **FR-008**: If migration cannot complete successfully, the upgrade MUST fail safely: pre-upgrade Derby data remains intact, the product MUST NOT cut over to a partial new repository as the live store, and operators receive a diagnosable failure outcome in upgrade logs.
- **FR-018**: Before starting automatic Derby-to-new-default migration, the upgrade path MUST enforce a hard backup gate: either (a) a product-produced pre-migration backup of the product-managed Derby repository completes successfully, or (b) the operator explicitly confirms that a verified external backup already exists. Migration MUST NOT start until the gate is satisfied; the confirmation path MUST be an affirmative operator action (not a silent default).
- **FR-019**: After a successful migration and cutover, the product MUST retain pre-migration Derby data files until the operator performs a documented cleanup action (which MAY be an optional product-provided cleanup step). The product MUST NOT automatically delete those retained files without operator action. Documentation MUST state location, purpose (support/forensics only; not the live store), disk impact, and cleanup steps.
- **FR-009**: Upgrades of installations already configured for MySQL or SQL Server MUST leave those configurations in effect and MUST NOT run embedded Derby migration against those datasources.
- **FR-010**: Installer, upgrade, and runtime configuration surfaces that previously implied Derby as the zero-admin default MUST present the new default consistently (no contradictory “Derby default” guidance for new 8.2 installs).
- **FR-011**: Product documentation MUST describe: (a) default repository choice for new installs, (b) automatic Derby migration on upgrade including the pre-migration backup gate, (c) offline backup procedure for the new default repository, (d) offline restore procedure, (e) failure recovery when migration fails, (f) retention and operator cleanup of pre-migration Derby files after successful cutover, on Windows, Linux, and macOS where the product is supported.
- **FR-020**: Supported consistent full backup and restore of the new default embedded repository MUST be **offline only**: the product instance MUST be stopped for backup and for restore. The product MUST NOT document or claim a supported online/hot full backup path for the default embedded repository in this release.
- **FR-021**: The product MUST support automatic migration from product-managed Derby for: (1) the GA release that introduces the new default embedded repository, and (2) **one subsequent product line** after that GA. Before removal, release notes MUST deprecate Derby migration with at least one product-line notice. After the support window ends, new product lines are not required to migrate from Derby (customers remaining on Derby must upgrade while migration is still supported).
- **FR-012**: Release notes / upgrade guide MUST communicate Derby upstream retirement impact, who is affected (default Derby installs), who is not (MySQL/SQL Server), and the Derby migration support window (GA + one subsequent product line, then removal with prior deprecation).
- **FR-013**: After a successful migration, subsequent product starts MUST use only the new default repository as the live store for that instance (no dual-write requirement to Derby for steady-state operation).
- **FR-014**: Migration and backup/restore procedures MUST be operable without requiring customers to purchase or stand up an external database server solely to leave Derby.
- **FR-015**: The solution MUST remain compatible with the product’s existing external MySQL and SQL Server support (connection configuration, upgrade of those estates, and documented operations continue to work).
- **FR-016**: Automated verification MUST cover: new default install smoke (CMS and DTS), Derby-to-new-default migration with representative data, external DB upgrade non-interference, and concurrent multiuser safety tests including CMS at **≥10 concurrent editors**.
- **FR-017**: Operators MUST be able to determine from logs whether migration ran, succeeded, was skipped (non-Derby), or failed—and which repository is active after upgrade.

### Key Entities

- **Product-managed repository**: The primary CMS or DTS datastore configured by the product for that instance (default embedded or external).
- **Default embedded repository**: Zero-admin, product-bundled repository used when the customer does not supply MySQL/SQL Server (historically Derby; target is the maintained replacement).
- **Derby legacy repository**: Pre-8.2 product-managed Derby data directories and configuration that exist as a migration source during upgrade and, after successful cutover, as retained files until operator cleanup (not the live store).
- **Migration outcome**: Recorded result of upgrade migration (success, skipped, failed) used by operators and support.
- **Backup artifact set**: Files and/or export artifacts produced by the documented **offline** backup procedure for the default embedded repository.
- **External repository configuration**: Customer-supplied MySQL or SQL Server connection settings that must pass through upgrades unchanged in intent.

## Success Criteria

### Measurable Outcomes

- **SC-001**: 100% of scripted new-install default CMS and default DTS smoke suites on supported OS families (Windows, Linux, macOS as applicable to each component) pass without requiring an external database.
- **SC-002**: For a representative CMS Derby fixture (minimum: accounts/roles, nested folders, ≥1,000 content items or equivalent fixture scale agreed in plan, relationships, workflow states, at least one site/publish-related config if present in fixture), automated post-upgrade verification passes with **zero** missing core entities and **zero** login/authorization regressions attributable to migration.
- **SC-003**: For a representative DTS Derby fixture covering each product-managed default service store in scope, post-upgrade service data checks pass with **zero** unexplained data loss for migrated records.
- **SC-004**: In **10/10** controlled migration failure injections (disk full, forced mid-copy abort, invalid source), source Derby data remains startable or restorable per recovery docs and no live cutover to a partial target store occurs.
- **SC-010**: In **100%** of upgrade test runs that detect Derby and have not satisfied the backup gate, migration does not start and a blocking, operator-visible message is produced; runs that satisfy product backup **or** explicit external-backup confirmation proceed to migration.
- **SC-011**: After successful migration in test, pre-migration Derby files remain on disk until an explicit cleanup action; no automatic deletion occurs in the default upgrade path.
- **SC-005**: Concurrent multiuser tests with **≥10 concurrent interactive CMS editor sessions** (and DTS write activity at documented default levels) complete with **0** silent lost updates and **0** repository corruptions across the release test matrix.
- **SC-006**: MySQL and SQL Server upgrade regression suites show **0** forced migrations and **0** unintended datasource rewrites for external configurations.
- **SC-007**: Support can execute **offline** backup and restore from documentation alone in a dry-run trial (timed walkthrough) in under **60 minutes** for a small default instance, ending in a verified working restore after restart.
- **SC-008**: Customer-facing upgrade materials for 8.2 explicitly cover this change before GA; support FAQs answer “Am I affected?” with a yes/no path (Derby default vs external) without engineering escalation for the common case.
- **SC-009**: After GA of the release that includes this feature, **new** default installs do not depend on Apache Derby as the live repository engine.
- **SC-012**: Derby→new-default migration remains available and tested for the GA release and the next product line; deprecation notice appears before removal; a checklist item exists to drop migration after that window.

## Assumptions

- **Preferred technical direction (non-binding on this spec’s outcomes):** planning research currently favors a pure-Java maintained embedded engine with multiuser and locking characteristics comparable to Derby (working recommendation recorded on issue #548: primary candidate H2, alternate HSQLDB if soak tests fail, SQLite excluded as default). Final engine selection is a planning/bake-off outcome, not a user-facing requirement of this specification.
- **Scope includes both CMS and DTS** default embedded repositories in the same major release train (8.2), even if implementation is phased by work package, as long as GA criteria below are met for both before calling #548 done.
- **Migration is automatic** on the supported upgrade path after the pre-migration backup gate is satisfied (FR-018); no separate paid professional-services-only tool is required for standard product-managed Derby repositories.
- **Safe failure** means abort without live cutover and preservation of pre-upgrade Derby data; it does not require automatic retry without operator action.
- **No supported downgrade** from a successfully migrated instance back to Derby as the live engine; recovery from failed migration uses preserved pre-upgrade data / backups.
- **Post-success retention:** pre-migration Derby files are retained until operator-initiated documented cleanup (FR-019); never auto-deleted by the product without operator action.
- **Multi-node shared embedded storage** is out of scope (same product class as Derby defaults).
- **Custom customer SQL or non-product schemas** inside Derby are best-effort only if present in product-managed databases; product guarantees cover product-owned schemas and data.
- **TableFactory and existing multi-backend repository abstractions** remain the intended product mechanisms for schema/data portability (constitution: database schema changes use TableFactory)—implementation detail for plan, constraint for design.
- **Cross-platform** install, upgrade, backup, and restore procedures are required (Windows, Linux, macOS as product-supported).
- **Steady-state backup/restore** of the default embedded repository is offline-only (FR-020); online/hot full backup is out of supported scope for this release.
- **Performance:** default embedded remains positioned for small-to-mid deployments; customers with heavy multi-server or high-throughput needs continue to be directed to MySQL/SQL Server. Migration should complete in a maintenance window proportional to data size; exact SLA numbers are refined in plan with fixture sizes.
- **CMS multiuser acceptance floor:** **10 concurrent interactive editors** on default embedded CMS (FR-003, SC-005). Higher concurrency may be documented as guidance but is not required to pass acceptance.
- **Issue #548** remains the tracking issue until all acceptance stories and success criteria for GA are met (including documentation and migration safety).
- **Derby migration support window:** GA that introduces the new default **plus one subsequent product line**, then remove with prior deprecation notice (FR-021).

## Out of Scope

- Forcing MySQL or SQL Server customers onto the new embedded engine.
- Adding new external RDBMS brands beyond currently supported MySQL and SQL Server (e.g. PostgreSQL) unless separately specified.
- Multi-server shared-disk clustering of the embedded default repository.
- Guaranteeing migration of arbitrary non-product objects customers may have created inside Derby outside product schemas.
- Indefinite Derby migration support beyond the GA + one subsequent product line window (FR-021).
- UI redesign work unrelated to repository selection/messaging (this is a non-UI major lift; only installer/docs copy required for clarity).
- Re-platforming the entire persistence stack (e.g. removing Hibernate or rewriting all SQL applications) beyond what is required for a correct default engine swap and migration.
- Supported online/hot full backup of the default embedded repository (offline only per FR-020).

## Dependencies

- GitHub issue **#548** and its findings/recommendations comment (candidate analysis, bake-off gate, migration shape).
- Existing multi-backend support patterns (external MySQL/SQL Server) as the compatibility baseline for “external unchanged.”
- Supported upgrade/installer mechanisms for CMS and DTS on 8.2.
- Constitution constraints: TableFactory for schema/data movement; tests via project Maven wrappers; cross-platform operability; no invented APIs.

## Risks (product-level)

- **Data fidelity risk** on complex CMS schemas (highest impact)—mitigate with representative fixtures and mandatory automated post-upgrade verification.
- **Concurrency/locking regression** vs Derby defaults—mitigate with bake-off and multiuser acceptance tests before locking engine choice.
- **Upgrade window duration** for large Derby repositories—mitigate with progress logging, disk prechecks, and sizing guidance in docs.
- **Communication gap** (customers surprised by maintenance window)—mitigate with release notes and upgrade guide lead time.
- **Partial estate complexity** (CMS vs DTS different backends)—mitigate with per-component detection and clear logs (FR-017).

