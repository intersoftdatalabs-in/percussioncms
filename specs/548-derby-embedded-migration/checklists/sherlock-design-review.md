## Design Document Review: Replace Retired Default Embedded Repository (#548)

**Reviewer**: Sherlock (QA / adversarial design review)  
**Date**: 2026-07-23  
**Artifacts reviewed**: `spec.md`, `plan.md`, `research.md`, `data-model.md`, `quickstart.md`, `contracts/*`, `checklists/requirements.md`  
**Codebase verification**: `PSJdbcUtils`, `PSSqlHelper`, `PSSessionFactoryBean`, `PSCommunityDerbyDialect`, `PSJdbcDataTypeMaps.xml`, `rxrepository.properties`, `config.xml`, Jetty `perc.mod` / `perc-ds.*`, DTS beans/Liquibase, `installRepository.xml`, `PSExecSQLStmt`, upgrade plugins under `system/.../install`, perc-packages archive metadata, DTS Windows service scripts

### Summary

**Needs revision (not ready to freeze for implementation).** The product spec is strong—stories, FR/SC, clarifications, and offline-backup posture are clear and testable. The plan correctly identifies the multi-backend extension points and the H2 bake-off gate. However, several **architecture claims do not match the codebase**: DTS is not TableFactory-driven; CMS install is **networked Derby** (not pure embedded dual-access); product-wide `sqlDerby` / `isDerby*` / CLOB materialization / boolean substitutions / Liquibase `dbms="derby"` surfaces are under-scoped. Those gaps will produce partial migrations and false “engine locked” confidence. Address critical/major issues before `/speckit-tasks` freezes work packages.

---

### Issue 1: DTS schema path is not TableFactory — plan over-applies CMS model

- **Severity**: critical
- **Section**: `plan.md` Summary / WP3–WP4; `research.md` R2, R4; `contracts/migration-upgrade.md` step 3
- **Description**: Design states schema is applied via **TableFactory** for migration generally. Code shows DTS services use **Hibernate `hbm2ddl.auto=update`** and (metadata) **Liquibase** with **Derby-only** changesets (`dbms="derby"` querying `SYS.SYSCONGLOMERATES`), not TableFactory. Examples:
  - `delivery-tier-suite/comments/.../perc-datasources.xml` — `hibernate.hbm2ddl.auto=update`
  - `delivery-tier-suite/metadata/.../beans.xml` — Liquibase `SpringLiquibase` + `masterChangeLog.xml`
  - `changeLogIndex1–6.xml` — `dbms="derby"` index creates against Derby system catalogs  
    Applying “TableFactory schema create then row copy” to DTS is either wrong or needs an explicit alternate design. Constitution IV (TableFactory) applies to **CMS** schema management; DTS already diverges and the plan does not reconcile that.
- **Suggestion**: Split migration architecture: **CMS = TableFactory inventory + pump**; **DTS = per-service strategy** (e.g. Hibernate schema on target + JDBC copy, or Liquibase with H2 changesets + data pump). Inventory every `dbms="derby"` changeset and every `org.hibernate.community.dialect.DerbyDialect` hardcode. Add an explicit DTS data-model/contract appendix.
- **Status**: open

---

### Issue 2: CMS install is networked Derby tooling — not covered by “embedded URL rewrite”

- **Severity**: critical
- **Section**: `research.md` R3; `plan.md` WP2; contracts repository-config
- **Description**: Verified install path starts **Derby Network Server**, unpacks a seed DB, and runs SQL via **ClientDriver** on port **1527**:
  - `system/installResources/installRepository.xml` — `setupEmbeddedDB` unzips `Derby/Repository.zip`, `NetworkServerControl start`, `jdbc:derby://localhost:1527/...`
  - Jetty `modules/perc-jetty/.../perc.mod` — `-Dderby.system.home=../../Repository`, `-Dderby.drda.startNetworkServer=true`  
    New-install design talks about `rxrepository.properties` / Jetty DS defaults but does not specify the **replacement for NetworkServerControl install orchestration**, seed `Repository.zip`, client vs embedded driver for installer ANT SQL, or multi-process access during install. H2 file mode is single-process unless `AUTO_SERVER` (or TCP server) is designed in. Without this, WP2 cannot produce a working installer.
- **Suggestion**: Add an **Installer dual-process access** decision (R11): (a) H2 `AUTO_SERVER` for install-time multi-process, (b) H2 TCP mode analogous to DRDA, or (c) pure embedded + all install SQL in-process Java (no ANT multi-JVM). Document seed strategy (prebuilt H2 file tree vs empty + TableFactory load). Update Jetty module flags (drop `derby.drda.*`).
- **Status**: open

---

### Issue 3: Massive `sqlDerby` / driver-branch surface omitted from owning-module map

- **Severity**: critical
- **Section**: `plan.md` Source Code paths; `research.md` R3
- **Description**: Research lists high-level touchpoints but misses the **installer SQL dialect matrix**. `PSExecSQLStmt` selects `sqlDerby` when driver is Derby; distribution has many `sqlDerby=` view/DDL statements (`modules/perc-distribution-tree/.../installRepository.xml`, `system/installResources/install.xml`). Similar branches: `PSSqlHelper.qualifyTableName` / backing-index Derby-only SYS queries, `PSJdbcStatementFactory` Derby column-recreate path, `InstallUtil.isDerbyRunning` / `shutDownDerby`, perc-ant post-SQL Derby shutdown. Without a **grep-driven inventory** as a required WP, implementers will leave Derby-only SQL that silently no-ops or fails on H2.
- **Suggestion**: Add WP1 deliverable: **repo-wide Derby surface inventory** (generated checklist) with disposition per hit (port / add `sqlH2` / generalize / leave migration-only). Gate WP2 on inventory completeness for install + runtime paths.
- **Status**: open

---

### Issue 4: Content-type CLOB/BLOB “Derby special case” must apply to new engine (or be re-validated)

- **Severity**: major
- **Section**: `research.md` R9 bake-off; plan WP1 dialect; data-model fidelity
- **Description**: `PSContentRepository.isDerbyDatabase()` and `PSTypeConfiguration` **only for Derby** materialize `java.sql.Clob`→`String` and `Blob`→`byte[]`. If H2 needs the same Hibernate mapping behavior (likely for product content LOBs), leaving `isDerbyDatabase()` as driver==`"derby"` will regress content types on new default. Spec SC-002 will fail on real content with large body fields. Design mentions CLOB/BLOB in bake-off bullets but not this **product code branch**.
- **Suggestion**: Design explicitly: generalize to `isEmbeddedFileStore()` / `needsClobAsString()` covering H2 (and HSQL), with unit tests. Bake-off item must include **content type with CLOB field** open/save, not only dialect smoke.
- **Status**: open

---

### Issue 5: Boolean / BIT dialect risk under-specified for data pump

- **Severity**: major
- **Section**: `research.md` R9; `data-model.md` §5 datatype map; `PSSessionFactoryBean`
- **Description**: Code applies Derby-only `hibernate.query.substitutions=true=T,false=F,yes=Y,no=N` and isolation READ_UNCOMMITTED for Derby (`PSSessionFactoryBean.configureDatabaseSpecificProperties`). TableFactory DERBY map uses `BIT → CHAR … FOR BIT DATA`. DTS metadata/polls also set `true 'T', false 'F'`. H2 prefers real `BOOLEAN`. Migration that copies `T`/`F` char columns into BOOLEAN (or vice versa) without conversion will corrupt flags/ACLs/workflow bits. Plan says “do not inherit Derby boolean substitutions blindly” but does not define **source→target type conversion rules** or validation probes for boolean columns.
- **Suggestion**: Data-model must specify: (1) H2 TableFactory map for BIT/BOOLEAN, (2) whether product keeps char T/F for compatibility or moves to BOOLEAN, (3) pump conversion, (4) probes on known boolean-like columns. Bake-off must fail if substitutions are copied blindly.
- **Status**: open

---

### Issue 6: Product pre-migration backup (FR-018a) has no existing full-repo mechanism

- **Severity**: major
- **Section**: `contracts/backup-restore.md`; `spec.md` FR-018; `plan.md` WP3
- **Description**: Design assumes a “product-produced pre-migration offline backup.” Existing upgrade code has `PSUpgradeBackupTable` (per-table copy), not a full embedded-repository snapshot. No durable design for: artifact location, atomicity, verification, Windows file locks, companion config set, failure if files still open, interaction with Network Server still running. External-confirm path is clearer than product backup path.
- **Suggestion**: Specify backup implementation contract: stop/ensure no Derby network server → copy `Repository/` (or resolved data dir) + listed config files via NIO `Files`/`Path` → checksum/size log → gate proceeds. Identify **where** confirmation is captured (installer UI checkbox, properties flag, CLI) for path (b). Add test for gate without either path.
- **Status**: open

---

### Issue 7: FK-safe table order and CMS table inventory are hand-waved

- **Severity**: major
- **Section**: `research.md` R2 step 5; `contracts/migration-upgrade.md`; `data-model.md` validation
- **Description**: “Ordered data copy” / “FK-safe order” is required but no algorithm or inventory source is named. CMS schema is large; TableFactory can catalog/export tables (`PSJdbcTableFactory.catalogTableData`) but product does not appear to ship a single ordered “all tables for migrate” manifest. Risk: mid-copy FK violations → FAILED with partial target (safe if no cutover) but **false confidence** on “automatic” fidelity, or implementers invent ad-hoc order that misses tables (NEXTNUMBER, ACL, relationship, publish status, etc.).
- **Suggestion**: Define inventory: catalog from source Derby metadata ∩ product-owned schemas, topological sort by FK, disable FKs during load if engine allows, re-enable + validate. Document handling of self-FKs, circular FKs, and identity/NEXTNUMBER tables. Require SC-002 fixture to assert **table set equality** (not only content item count).
- **Status**: open

---

### Issue 8: Dual CMS + DTS upgrade sequencing and mixed-estate ops

- **Severity**: major
- **Section**: `spec.md` Edge Cases (mixed estate); `plan.md` WP3/WP4; observability contract
- **Description**: Spec allows CMS Derby + DTS MySQL (and reverse). Plan does not specify **upgrade order**, shared maintenance window, or whether DTS services must be stopped before CMS migration (or vice versa) when both are Derby. DTS distribution scripts set `-Dderby.system.home=.../derbydata` in Tomcat and **Windows Procrun** service installers (`DTSProductionService.bat`, `DTSStagingService.bat`). Partial migration (CMS done, DTS not) is operable only if documented; currently not.
- **Suggestion**: Add ops contract: recommended sequence (e.g. stop all → migrate CMS → migrate each DTS service → start), independent detection per component, logging of per-component outcome, and Windows service property updates when embedded home changes from `derbydata` to H2 directory.
- **Status**: open

---

### Issue 9: Liquibase Derby system-catalog changesets will break on H2

- **Severity**: major
- **Section**: `research.md` R4; plan WP4
- **Description**: Metadata changelogs create indexes with preconditions querying Derby `SYS.SYSCONGLOMERATES` / `SYS.SYSTABLES` under `dbms="derby"`. On H2 those changesets **never run** (dbms filter), so indexes may be missing after new install or migrate-unless Hibernate/other DDL recreates them. Design never mentions Liquibase.
- **Suggestion**: For each `dbms="derby"` changeset, add `dbms="h2"` (or database-agnostic) equivalent; IT that asserts index existence on H2. Track in WP4 checklist.
- **Status**: open

---

### Issue 10: Concurrent migration serialization mechanism unspecified

- **Severity**: major
- **Section**: `spec.md` Edge Cases; `contracts/migration-upgrade.md` precondition 5
- **Description**: Contract requires “no concurrent migration already running” but design does not specify lock primitive (file lock under install root, property flag, installer mutex). Cross-platform file locking differs (Windows mandatory locks vs POSIX advisory). Risk of two upgrade processes double-writing target.
- **Suggestion**: Specify exclusive lock file via `FileChannel.tryLock` on a well-known path under install tree; document stale-lock recovery; test second process gets clear `FAILED`/`BLOCKED` message.
- **Status**: open

---

### Issue 11: Networked Derby tools and Desktop/ops assumptions

- **Severity**: major
- **Section**: Out of scope / research R10; plan risks
- **Description**: Product exposes Derby network server flags and tools (`derbynet`, `NetworkServerControl`, p13n `DatabaseStartup.bat`). Customers/support may connect with network clients. Moving to H2 without documenting **loss of DRDA network access** (or offering H2 TCP) is an operability regression for support workflows even if “multi-node shared embedded” is out of scope. Design is silent.
- **Suggestion**: Explicit product decision: no remote network engine for default embedded (document break); optional H2 TCP for support only; update/remove startup scripts that start network servers. Include in release notes FR-012 materials.
- **Status**: open

---

### Issue 12: Package (`.ppkg`) archive metadata embeds Derby

- **Severity**: minor (possibly major if install-time validation enforces driver)
- **Section**: `plan.md` Constitution Check IV (mentions audit); WP8
- **Description**: Multiple packages under `modules/perc-packages/.../psx_archiveInfo.xml` contain `<driver>derby</driver>` and `//localhost:1527/CMDB`. Plan says “audit in tasks” only. If packaging or import validates driver against live backend, packages break on H2 defaults.
- **Suggestion**: Promote package audit to **WP1/WP2 hard task** with disposition (cosmetic archive stamp vs runtime dependency). Test install of core packages on H2 default.
- **Status**: open

---

### Issue 13: `IPSSystemService.isDerby()` API and SQL sanitization branching

- **Severity**: major
- **Section**: research R3; plan WP1
- **Description**: Callers like `PSSearchService` branch on `systemService.isDerby()` for `SecureStringUtils.DatabaseType.DERBY`. H2 needs a distinct type or shared “ANSI-ish embedded” path. Leaving API as is-Derby-only will mis-sanitize queries on H2.
- **Suggestion**: Extend detection API carefully (avoid breaking binary compat if possible: add `isH2()` / `getDatabaseType()`; deprecate overloading Derby meaning). Update all call sites from inventory (Issue 3).
- **Status**: open

---

### Issue 14: Migration duration / sizing guidance still deferred

- **Severity**: minor
- **Section**: `spec.md` Assumptions (performance); success criteria large-repo edge case
- **Description**: Spec promises “documented time expectation class”; plan leaves SLA open. Operators need rough guidance (e.g. per 1k content items, disk 2× source). Without it SC edge-case “documented time expectation” is unmet at GA.
- **Suggestion**: In WP6, publish table: fixture size → wall-clock on reference HW; formula for disk precheck (source + target + temp + backup).
- **Status**: open

---

### Issue 15: H2 engine modes (MVStore, `DATABASE_TO_UPPER`, schema APP vs CMDB)

- **Severity**: major
- **Section**: `data-model.md` CMS config; research R1
- **Description**: Default `DB_SCHEMA=CMDB` and installer `set schema app` show schema naming quirks. H2 defaults (quoted identifiers, case folding, `DATABASE_TO_UPPER=TRUE`) interact with product’s uppercase physical naming (`UpperCaseNamingStrategy` in `PSSessionFactoryBean`). Unspecified JDBC URL parameters risk “table not found” after cutover despite successful pump.
- **Suggestion**: Freeze canonical H2 URL template (file path form, `MODE`, `DB_CLOSE_DELAY`, `AUTO_SERVER` if used, schema/user). Prove Hibernate + TableFactory + security provider table `USERLOGIN` resolve same identifiers. Document in repository-config contract.
- **Status**: open

---

### Issue 16: Identity / NEXTNUMBER semantics across engines

- **Severity**: major
- **Section**: bake-off R1 item 3; data-model validation
- **Description**: `PSSqlHelper.supportsIdentityColumns` treats non-Oracle/MySQL as supporting identity (includes Derby and would include H2). Product also uses **NEXTNUMBER** tables (`PSUpgradePluginNextNumberFixup`). Design bake-off mentions identity round-trip but not **preserving explicit IDs** on insert during pump (must not re-allocate content ids) nor re-sync of NEXTNUMBER after copy.
- **Suggestion**: Require pump to insert **explicit primary keys** (disable identity generation for load or use engine-specific override). Post-copy NEXTNUMBER max validation probe mandatory in SC-002.
- **Status**: open

---

### Issue 17: Atomic cutover of multiple config surfaces

- **Severity**: major
- **Section**: `contracts/migration-upgrade.md` step 6; repository-config
- **Description**: Live config is not a single file: `rxrepository.properties`, Jetty `perc-ds.properties` / modules, possibly `server-beans` dialect already fixed by driver map, installer copies. “Write temp props → replace” is necessary but incomplete if Jetty DS still points at Derby while `rxrepository` says H2 (or reverse). Design says they “must stay consistent” without cutover transaction design.
- **Suggestion**: Define cutover set + order + rollback: all or nothing; on failure restore previous props from backup artifact. Integration test asserts consistency of backend labels across files after SUCCESS.
- **Status**: open

---

### Issue 18: Windows service / Procrun and shell start scripts

- **Severity**: major
- **Section**: plan WP4 packaging; FR-011 cross-platform
- **Description**: DTS `TomcatStartup.bat/.sh`, `DTSProductionService.bat`, `DTSStagingService.bat` hardcode `-Dderby.system.home=...`. CMS Jetty `perc.mod` hardcodes Derby system properties. Design mentions packaging generically; no explicit script/service rewrite tasks or tests on Windows service reinstall.
- **Suggestion**: Enumerate all start/service scripts; replace properties; document upgrade step for existing Windows services (re-run service install or update JVM options). Cross-platform QC required.
- **Status**: open

---

### Issue 19: Alternatives fairness and H2 risk disclosure

- **Severity**: minor
- **Section**: `research.md` R1
- **Description**: HSQLDB is a fair alternate. SQLite rejection is sound. Missing: H2 historical CVEs / `INIT=` risks (mitigate by not exposing remote console), file corruption on kill -9 (offline backup posture helps), and comparison of **page lock vs MVCC** vs Derby’s behavior for the 10-editor floor. Bake-off list is good but not tied to product lock APIs (checkout) with class names.
- **Suggestion**: Name product lock entry points to test (content checkout service / workflow transition) in bake-off so WP0 is not a generic JDBC lock test.
- **Status**: open

---

### Issue 20: Constitution / AGENTS gates incomplete in plan evidence section

- **Severity**: minor
- **Section**: `plan.md` Constitution Check; quickstart
- **Description**: Plan checks constitution boxes and mentions standalone Maven and Path APIs. Good. Missing explicit: **pre-PR clean install per module** as GA gate text; **Erlang pre-commit** for code PRs; **no cargo debug** N/A; i18n for any new operator-facing installer strings (`perc-i18n`). Spec is non-UI but installer messages may need i18n.
- **Suggestion**: Add WP8 checklist rows for AGENTS pre-PR Maven evidence, cross-platform path review, and installer message i18n if any UI/text is added.
- **Status**: open

---

### Issue 21: Observability outcome enum inconsistency

- **Severity**: nit
- **Section**: `data-model.md` vs `contracts/migration-observability.md`
- **Description**: Data-model outcomes: `SUCCESS | FAILED | SKIPPED_NON_DERBY | BLOCKED_BACKUP_GATE`. Observability also lists `ALREADY_MIGRATED`. State machine has `ALREADY_MIGRATED → SKIPPED`. Slight inconsistency for implementers/grep docs.
- **Suggestion**: Unify enum in one contract file; reference from data-model.
- **Status**: open

---

### Issue 22: Secrets in default properties and logging

- **Severity**: minor (security)
- **Section**: research R8; observability; `rxrepository.properties`
- **Description**: Default `PWD=` is present (encoded) in `system/config/Default/rxrepository.properties`. Contract forbids logging passwords—good. Ensure backup artifacts of properties are access-controlled; migrator must not log full properties dumps. No threat note on world-readable backup dirs.
- **Suggestion**: Ops doc: backup dir permissions; never attach props with PWD to tickets; redaction unit test for logger helper.
- **Status**: open

---

### Issue 23: tasks.md absent (expected) but WP0 gate underspecified for “evidence”

- **Severity**: minor
- **Section**: plan Phase 2; quickstart Q12
- **Description**: Bake-off requires “written evidence” on #548 but not the evidence artifact shape (what metrics, what fixture commit hash, OS matrix).
- **Suggestion**: Template bake-off report under `docs/ai-generated/` or feature dir: engine, versions, OS, editor count, lock test class, pass/fail, HSQL rerun if needed.
- **Status**: open

---

### Missing Quality Checks (must enumerate)

These are **not** adequately specified as hard gates before GA of #548. Map to FR/SC where applicable.

- **QC-001**: Full Derby surface inventory freeze
  - **Maps to**: FR-010, plan completeness, Issue 3
  - **How to verify**: Scripted repo search for `derby`/`DERBY`/`sqlDerby`/`NetworkServer`/`isDerby` producing dispositioned checklist checked into feature dir; zero “unknown” rows at GA
  - **Gate**: hard
- **QC-002**: H2 TableFactory map parity suite
  - **Maps to**: FR-005/007, constitution IV
  - **How to verify**: Create/load/alter sample product tables on H2 including CLOB/BLOB/BIT/DATE→TIMESTAMP semantics; compare row round-trip to Derby source
  - **Gate**: hard
- **QC-003**: CMS identity-preserving pump + NEXTNUMBER probe
  - **Maps to**: SC-002, FR-007, Issue 16
  - **How to verify**: Fixture with known content ids; post-migrate ids equal; NEXTNUMBER ≥ max(id)+1 for key sequences
  - **Gate**: hard
- **QC-004**: Boolean/BIT fidelity probes
  - **Maps to**: FR-007, Issue 5
  - **How to verify**: Known flag columns pre/post; no T/F vs boolean mismatch; login/ACL checks
  - **Gate**: hard
- **QC-005**: Content-type CLOB open/save on H2 (generalized Derby LOB branch)
  - **Maps to**: SC-002, Issue 4
  - **How to verify**: Content item with large body; edit/save after migrate and on new install
  - **Gate**: hard
- **QC-006**: Pessimistic lock / checkout parity at ≥10 editors
  - **Maps to**: SC-005, FR-003/004, US4
  - **How to verify**: Automated harness against product checkout/workflow APIs (not raw JDBC only); 0 lost updates
  - **Gate**: hard
- **QC-007**: Backup gate matrix (product backup / external confirm / neither)
  - **Maps to**: SC-010, FR-018
  - **How to verify**: Three automated upgrade runs; neither blocks; both success paths migrate
  - **Gate**: hard
- **QC-008**: Failure injection 10/10 with source startability
  - **Maps to**: SC-004, FR-008
  - **How to verify**: Disk full, kill mid-copy, corrupt source, validation fail; assert config still Derby; source opens
  - **Gate**: hard
- **QC-009**: Cutover multi-file consistency
  - **Maps to**: FR-013, repository-config contract, Issue 17
  - **How to verify**: After SUCCESS, `rxrepository` + Jetty DS + dialect resolution all H2; crash mid-cutover leaves non-live partial target
  - **Gate**: hard
- **QC-010**: Idempotent re-upgrade / already migrated
  - **Maps to**: Edge case, observability `ALREADY_MIGRATED`
  - **How to verify**: Second upgrade logs skip; no data churn
  - **Gate**: hard
- **QC-011**: External MySQL/MSSQL non-interference
  - **Maps to**: SC-006, FR-009, US3
  - **How to verify**: Upgrade fixtures with external backends; migrator skip; config byte-stable for connection keys
  - **Gate**: hard
- **QC-012**: DTS per-service migrate + Liquibase H2 indexes
  - **Maps to**: SC-003, FR-002/006, Issues 1 & 9
  - **How to verify**: Each default service: Derby fixture → migrate → health + sample reads; index existence where product expects
  - **Gate**: hard
- **QC-013**: New install without live Derby dependency
  - **Maps to**: SC-009, FR-001/002
  - **How to verify**: Packaging audit: no Derby required on runtime classpath for new default path; H2 present; migration jars scoped
  - **Gate**: hard
- **QC-014**: Installer path without NetworkServerControl (or approved H2 multi-process)
  - **Maps to**: FR-001, Issue 2
  - **How to verify**: Clean install on Windows + Linux completes SQL/schema steps; no listen on 1527 required
  - **Gate**: hard
- **QC-015**: Offline backup/restore dry-run ≤60 minutes
  - **Maps to**: SC-007, FR-020/011, US5
  - **How to verify**: Support engineer timed walkthrough from docs alone; restore verifies login
  - **Gate**: hard (docs + procedure)
- **QC-016**: Post-success Derby residue retention + cleanup
  - **Maps to**: SC-011, FR-019
  - **How to verify**: Files remain after SUCCESS; cleanup action removes only residue; live H2 untouched
  - **Gate**: hard
- **QC-017**: Cross-platform path I/O
  - **Maps to**: AGENTS Cross-Platform rules; FR-011
  - **How to verify**: Unit tests for path join/normalize on Windows separators; no hardcoded filesystem `/` in new migrator code; CI or explicit Windows agent run for installer scripts
  - **Gate**: hard
- **QC-018**: Pre-PR Maven standalone clean install per touched module
  - **Maps to**: AGENTS pre-PR hard gate; constitution III/VII
  - **How to verify**: PR body lists `cd <module> && …/mvn-env.sh clean install`, BUILD SUCCESS, test counts, no new warnings
  - **Gate**: hard (per implementation PR)
- **QC-019**: Concurrent migration lock
  - **Maps to**: Edge case concurrent upgrades; Issue 10
  - **How to verify**: Two migrator processes; one proceeds, one blocked with clear message
  - **Gate**: hard
- **QC-020**: Mixed estate (CMS Derby / DTS MySQL and reverse)
  - **Maps to**: Edge case mixed estate; FR-017
  - **How to verify**: Only Derby component migrates; logs show skip on external
  - **Gate**: hard
- **QC-021**: Disk precheck before pump
  - **Maps to**: Edge case insufficient disk; FR-008
  - **How to verify**: Simulated low space fails before source mutation/cutover
  - **Gate**: hard
- **QC-022**: Secrets redaction in migration logs
  - **Maps to**: constitution VI; observability contract
  - **How to verify**: Unit tests assert PWD/JDBC userinfo never appear in formatted log lines
  - **Gate**: hard
- **QC-023**: Package install on H2 default
  - **Maps to**: constitution IV `.ppkg`; Issue 12
  - **How to verify**: Install representative packages; no Derby driver requirement failures
  - **Gate**: soft → hard if validation ties to driver
- **QC-024**: Windows service JVM options after upgrade
  - **Maps to**: FR-011, Issue 18
  - **How to verify**: Documented service update; service start uses H2 home not `derby.system.home` only
  - **Gate**: hard for DTS GA
- **QC-025**: Bake-off evidence artifact before engine lock
  - **Maps to**: research R1; quickstart Q12
  - **How to verify**: Written report on #548 with metrics; WP1+ blocked until locked
  - **Gate**: hard
- **QC-026**: `sqlH2` (or generic) for every install `sqlDerby` statement that still runs post-cutover
  - **Maps to**: FR-010, Issue 3
  - **How to verify**: Install/upgrade SQL path on H2 executes views/DDL successfully
  - **Gate**: hard
- **QC-027**: Migration support window checklist (FR-021)
  - **Maps to**: SC-012, FR-021
  - **How to verify**: Tracking issue checklist items for GA line, next line, deprecation notice
  - **Gate**: soft until near end of window; hard before removing Derby
- **QC-028**: Table-set completeness (no missing product tables)
  - **Maps to**: FR-007, Issue 7
  - **How to verify**: Diff of user table names source vs target; zero missing product tables
  - **Gate**: hard
- **QC-029**: Large fixture scale (≥1000 content items)
  - **Maps to**: SC-002
  - **How to verify**: Automated fixture generator or load; wall-clock recorded
  - **Gate**: hard
- **QC-030**: Hibernate dialect registration for `h2` in all server-beans copies
  - **Maps to**: FR-001, research R3
  - **How to verify**: Grep `sys_hibernateDialects` / test server-beans; runtime start without dialect exception
  - **Gate**: hard

---

### Strengths

- **Product clarifications are excellent**: backup gate, offline-only steady-state backup, retention until operator cleanup, GA+1 migration window, 10-editor floor—all testable and reflected in FR/SC.
- **Safe-fail philosophy is right**: no live cutover on partial migrate; source retention; observability outcomes for support.
- **Engine selection approach is sound**: H2 primary, HSQL bake-off alternate, SQLite correctly rejected for multiuser/packaging.
- **Evidence-based CMS extension points** largely match code: `PSJdbcUtils`, `PSJdbcDataTypeMaps.xml` DERBY map, `config.xml` driver registry, `PSCommunityDerbyDialect` + Hibernate 7.2.6, `rxrepository.properties`, Jetty packaging, `derby.version` 10.17.1.0.
- **External DB non-interference** is first-class (US3 / FR-009 / SC-006)—necessary for release risk control.
- **Contracts package** (config, upgrade, backup, observability) is the right abstraction for a non-HTTP feature.
- **Quickstart Q1–Q12** gives a runnable validation matrix implementers can map to tests.
- **Cross-platform and Maven standalone** called out in plan/quickstart align with AGENTS hard gates.
- **No unjustified new frameworks** / no Spring Boot—constitution V respected.

---

### Verdict for Orchestrator

|                Dimension                 |                                                              Assessment                                                              |
|------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|
| Spec (product)                           | Ready for planning refinement                                                                                                        |
| Plan / research / data-model / contracts | **Revise before task freeze**                                                                                                        |
| Recommendation                           | **Conditional no-go for implementation** until critical Issues 1–3 and major Issues 4–11, 13, 15–18 are resolved in design artifacts |

**Primary revision themes**: (1) separate CMS vs DTS migration architectures, (2) replace networked-Derby install model deliberately, (3) inventory-driven Derby surface coverage, (4) type/LOB/identity/boolean fidelity, (5) concrete backup/cutover/lock/ops sequencing, (6) adopt QC-001–QC-030 as GA gates in `tasks.md` / quickstart.

---

### Orchestrator follow-up (2026-07-23)

Design artifacts were updated after this review (not by Sherlock):

|          Theme           |                            Where addressed                            |
|--------------------------|-----------------------------------------------------------------------|
| CMS vs DTS schema paths  | `research.md` R2 split; `plan.md` WP3/WP4; migration-upgrade contract |
| Networked install / R11  | `research.md` R11; plan WP2; repository-config contract               |
| Surface inventory        | `research.md` R12; QC-001; plan WP1                                   |
| Backup / lock / cutover  | R13; backup-restore + migration-upgrade contracts; QC-007/009/019     |
| LOB / boolean / identity | R14; data-model probes; QC-003–005                                    |
| QC-001–QC-030            | [quality-gates.md](quality-gates.md); quickstart mapping              |
| Outcome enum             | data-model + observability aligned (`ALREADY_MIGRATED`)               |

**Remaining before implementation freeze**: run WP0 bake-off + produce QC-001 inventory checklist file. Issue Status rows above stay **open** until corresponding implementation/design evidence lands.

**Analyze remediations (2026-07-24)**: C1–C5 and MEDIUM items applied to `tasks.md` (T001–T105), contracts (backup confirm UX, sequencing, durable report), research R11/R13, data-model, quickstart, quality-gates.

---

*Original Sherlock review was read-only. Follow-up edits by orchestrator after review.*
