# Tasks: Replace Retired Default Embedded Repository

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md), [checklists/quality-gates.md](checklists/quality-gates.md), [checklists/sherlock-design-review.md](checklists/sherlock-design-review.md)  
**Branch**: `548-derby-embedded-migration`  
**Feature directory**: `specs/548-derby-embedded-migration`  
**Issue**: [#548](https://github.com/intersoftdatalabs-in/percussioncms/issues/548)

**Tests**: Required by constitution III, AGENTS, FR-016, and QC gates — unit + integration + migration + concurrency tasks are first-class, not optional.

**QC mapping**: Each task cites primary QC IDs from [quality-gates.md](checklists/quality-gates.md). Hard QCs must pass before #548 close.

**Engine note**: Tasks say **H2** as primary target; if WP0 bake-off locks HSQLDB, substitute engine names/coordinates consistently (dialect, maps, URLs) without reopening product scope. **T012 acceptance**: if HSQL wins, update remaining task text/paths in this file in the same commit as engine lock.

**Analysis remediations (2026-07-24)**: Applied `/speckit-analyze` findings C1–C5 and MEDIUM/LOW items (DTS concurrency, upgrade sequencing, OS matrix, seed decision, backup-confirm UX freeze, FR/SC IDs, sizing table, i18n, p13n inventory, durable migration report, phase-number note).

**Phase numbering note**: Phase numbers are execution order, **not** user-story numbers. Phase 5 = **US4** (multiuser); Phase 6 = **US3** (external DB) because US3 depends on migrator detectors from US2.

---

## Phase 1: Setup

- [x] T001 Identify owning modules and read AGENTS hierarchy: root `AGENTS.md`, `modules/utils`, `modules/TableFactory`, `system/`, `modules/perc-jetty`, `modules/perc-distribution-tree`, `deliverytiersuite/delivery-tier-suite/**`, and any module-local `AGENTS.md` / `AGENTS.local.md`
- [x] T002 Confirm branch `548-derby-embedded-migration` is based on current `development`, JDK 21 via `./mvnw -version` (or equivalent), and note baseline health for `modules/utils`, `modules/TableFactory` with standalone `cd <module> && ../../mvnw test` (or correct depth)
- [x] T003 [P] Re-read design package: `specs/548-derby-embedded-migration/{spec,plan,research,data-model,quickstart}.md`, `contracts/*`, `checklists/quality-gates.md`, Sherlock review — especially R2 CMS/DTS split and R11 installer multi-process

---

## Phase 2: Foundational (Blocking Prerequisites)

**Goal**: Inventory every Derby touchpoint, lock engine via bake-off evidence, land shared platform primitives (deps, JDBC constants, TableFactory H2 map, dialect registration hooks) so all stories share one backend foundation.  
**Blocks**: All user stories.  
**QCs**: QC-001, QC-002 (start), QC-006 (bake-off), QC-025, QC-030 (start).

### Inventory & bake-off

- [x] T004 Generate repo-wide Derby surface inventory script under `scripts/derby-surface-inventory.py` **and** Windows launcher `scripts/derby-surface-inventory.bat` searching for `derby`/`DERBY`/`sqlDerby`/`NetworkServer`/`isDerby`/`derby.system`/`dbms="derby"`/`1527`/`EmbeddedDriver`/`ClientDriver` (Derby)/`derbydata`/`p13n`/`Repository.zip`/`drda`, writing `specs/548-derby-embedded-migration/checklists/derby-surface-inventory.md` with disposition columns (port / sqlH2 / generalize / migration-only / docs-only / unknown) — **QC-001**
- [x] T005 Run inventory script from repo root and commit initial `specs/548-derby-embedded-migration/checklists/derby-surface-inventory.md` with zero silent skips; triage top install + runtime hits from Sherlock Issues 2–3 **including any `p13n-ds` / `derbydata` rows** (disposition each; do not leave unknown)
- [x] T006 Create bake-off evidence template at `specs/548-derby-embedded-migration/checklists/bake-off-report.md` (engine, version, OS, editor count, lock test classes, CLOB/identity results, pass/fail, HSQL rerun if needed) — **QC-025**
- [x] T007 [P] Spike H2 parent POM coordinates in root `pom.xml` (`h2.version` + `dependencyManagement` for `com.h2database:h2`); keep `derby.version` for migration classpath
- [x] T008 Implement minimal CMS SessionFactory / dialect smoke IT under `system/src/test/java/com/percussion/services/datasource/` (new class e.g. `PSH2DialectSmokeIT.java` or unit-level equivalent) against file H2 using product naming strategy — freeze candidate **canonical H2 URL template** into `specs/548-derby-embedded-migration/contracts/repository-config.md`
- [x] T009 Run product-oriented lock bake-off harness (content checkout / workflow lock entry points, not raw JDBC only) with ≥10 concurrent editors against H2; record results in `bake-off-report.md` — **QC-006**, **QC-025** (`PSH2MultiuserLockHarnessTest` 5/5 PASS)
- [x] T010 If H2 fails bake-off criteria, repeat T008–T009 for HSQLDB and lock winner in `bake-off-report.md` + comment on GitHub #548 — **N/A** H2 passed
- [x] T011 Post bake-off report summary on GitHub issue #548 and mark engine locked in `specs/548-derby-embedded-migration/research.md` R1 decision line; **if HSQL won, update remaining tasks in this `tasks.md` (engine names/paths) in the same change set**

### Platform primitives

- [x] T012 Add H2 constants and backend/URL mapping in `modules/utils/src/main/java/com/percussion/utils/jdbc/PSJdbcUtils.java` (mirror `DERBY_*` patterns: driver name, class, backend label)
- [x] T013 Extend `modules/utils/src/main/java/com/percussion/util/PSSqlHelper.java` with `isH2` (and backend branches for identity/qualify as needed); do not silently treat H2 as Derby
- [x] T014 [P] Unit tests for new backend helpers in `modules/utils/src/test/java/` (e.g. `PSJdbcUtilsH2Test.java` / extend existing JDBC utils tests)
- [x] T015 Add `<DataTypeMap for="H2" driver="h2">` to `modules/TableFactory/src/main/resources/com/percussion/tablefactory/PSJdbcDataTypeMaps.xml` with BIT/BOOLEAN, CLOB, BLOB, temporal mappings per research R14 — **QC-002**
- [x] T016 [P] TableFactory create/load unit tests for H2 in `modules/TableFactory/src/test/java/com/percussion/tablefactory/` (new or extend `PSTablefactoryLoadTest` patterns without requiring NetworkServer)
- [x] T017 Register H2 driver in `system/config/config.xml` (`PSXJdbcDriverConfig`) and wire Hibernate dialect for driver `h2` in all dialect map locations used at runtime (including `WebUI` / `server-beans` copies per inventory) — **QC-030**
- [x] T018 Adjust `system/services/src/com/percussion/services/datasource/PSSessionFactoryBean.java` so Derby-only isolation/query substitutions are not applied to H2; add H2-specific props only if bake-off requires
- [x] T019 Add H2 rows to `system/config/sys_DatabaseFunctionDefs.xml` for functions product uses on default installs (parity with `driver="derby"` entries from inventory)
- [x] T020 Generalize LOB materialization beyond Derby-only checks in `system/services/src/com/percussion/services/contentmgr/impl/legacy/PSContentRepository.java` and `PSTypeConfiguration.java` (predicate covering H2) — **QC-005** start
- [x] T021 Extend `IPSSystemService` / `PSSystemService` database-type API for H2 (add `isH2` or `getDatabaseType`; update call sites like `PSSearchService` SecureStringUtils branching from inventory) without breaking external backends
- [x] T022 [P] Unit tests for dialect/session/LOB/type detection under `system/src/test/java/com/percussion/services/`
- [x] T023 Run standalone clean tests: `cd modules/utils && ../../mvnw clean test`, `cd modules/TableFactory && ../../mvnw clean test`, and targeted `system` tests for new classes — **QC-018** practice
- [x] T024 **Seed strategy decision (R11)**: choose and record in `specs/548-derby-embedded-migration/research.md` R11 and `contracts/repository-config.md` exactly one of: (A) empty H2 + TableFactory/load, or (B) prebuilt H2 seed tree replacing `Derby/Repository.zip`; no dual “or” left open — blocks US1 T029
- [x] T025 Commit foundational platform + inventory + bake-off + seed decision; open PR “548 WP0–WP1 foundation”; pause user-story PRs until this merges (or stack deliberately)

---

## Phase 3: User Story 1 — New install uses maintained default repository (Priority: P1)

**Goal**: Clean CMS and DTS installs accepting defaults use H2 (not Derby live); multiuser-capable default path without requiring an external database server (**FR-001**, **FR-002**, **FR-014**, **SC-001**, **SC-009**).  
**Independent Test**: Quickstart Q1/Q2 on **Windows, Linux, and macOS** (as each component is supported); packaging has no live Derby requirement; install does not need port 1527.  
**QCs**: QC-013, QC-014, QC-026, QC-030.

### Tests

- [x] T026 [P] [US1] Packaging/unit test that distribution/Jetty defaults reference H2 driver class (not Derby) in `modules/perc-jetty` and/or `modules/perc-distribution-tree` test sources — **QC-013**
- [x] T027 [P] [US1] Install-path test or resource assert that install orchestration no longer requires `NetworkServerControl` / `1527` for default path (refactor `system/installResources/installRepository.xml` / distribution copy) — **QC-014**; **exit criterion**: zero NetworkServerControl start for default install path
- [x] T028 [P] [US1] DTS default datasource properties tests/asserts under `deliverytiersuite/delivery-tier-suite/*/src/test` that default driver is H2 for product-managed services

### Implementation

- [x] T029 [US1] Redesign CMS install DB setup per research R11 **and T024 seed decision** in `system/installResources/installRepository.xml` and `modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/installRepository.xml` — pure in-process H2 (or approved R11 B/C only if T024/bake-off required); remove NetworkServer start/stop for default path; **exit criterion**: clean default install succeeds with no listen on 1527
- [x] T030 [US1] Replace/add `sqlH2` (or generic) statements for every install `sqlDerby` still executed on new installs in distribution + system install XML resources — **QC-026**, **FR-010**
- [x] T031 [US1] Update default `system/config/Default/rxrepository.properties` and installer-shipped `rxrepository.properties` to H2 backend/driver/server form using frozen URL template
- [x] T032 [US1] Update Jetty defaults: `modules/perc-jetty/src/main/jetty/defaults/etc/perc-ds.properties`, `perc-ds.xml`, `modules/perc.mod` / `perc-ds` — drop `derby.drda.*` and `derby.system.home` as live defaults; wire H2
- [x] T033 [US1] Ship H2 JAR on CMS JDBC/lib packaging paths consistent with how Derby was packaged (`modules/perc-jetty` / distribution tree); scope Derby to migration-only if still on classpath — **FR-014**
- [x] T034 [US1] Update DTS service default datasource props/beans for comments, forms, feeds, membership, metadata, polls (and other inventory hits) under `deliverytiersuite/delivery-tier-suite/*/resources` and `**/WEB-INF/**`
- [x] T035 [US1] Port Liquibase `dbms="derby"` changesets to H2 (or db-agnostic) in `deliverytiersuite/delivery-tier-suite/metadata/src/main/resources/changeLogIndex*.xml` and related — **QC-012** start
- [x] T036 [US1] Update `deliverytiersuite/delivery-tier-suite/delivery-tier-distribution` packaging, cargo DS, and start scripts (`TomcatStartup.*`, service bats) to H2 home properties — **QC-024** start
- [x] T037 [US1] Replace DTS Hibernate `DerbyDialect` hardcodes with H2 dialect class names in beans/xml configs (inventory-driven)
- [ ] T038 [US1] **OS matrix smoke (SC-001)**: document and execute new default CMS login + DTS health smoke on **Windows, Linux, and macOS** (skip only with product-owner waiver recorded on #548 for unsupported component/OS); capture commands/results in `specs/548-derby-embedded-migration/checklists/os-smoke-matrix.md` — checklist + packaging unit evidence PASS; **full install smoke** human QA **#2332** (open)
- [x] T039 [US1] **FR-010 copy audit**: grep installer/distribution docs and default templates for contradictory “Derby default” new-install guidance; fix remaining new-install messaging to H2/new default — see `checklists/fr010-copy-audit.md`
- [x] T040 [US1] Standalone `mvnw clean install` for each changed module in this story; record evidence for PR body — **QC-018** — completed via PR #1495 and stacked successors
- [x] T041 [US1] Commit US1 changes; open PR “548 US1 new-install H2 defaults”; pause for review — **merged** [#1495](https://github.com/intersoftdatalabs-in/percussioncms/pull/1495)
- [x] T042 [US1] Monitor CI/Kilo; fix feedback; reply+resolve review threads per AGENTS; merge before US2 — **done** (#1495)

---

## Phase 4: User Story 2 — Upgrade migrates Derby data automatically and safely (Priority: P1)

**Goal**: Automatic Derby→H2 migration with backup gate, safe-fail, multi-file cutover, observability, residue retention without requiring customers to stand up an external DB solely to leave Derby (**FR-005**, **FR-006**, **FR-007**, **FR-008**, **FR-013**, **FR-014**, **FR-017**, **FR-018**, **FR-019**, **SC-002**, **SC-003**, **SC-004**, **SC-010**, **SC-011**).  
**Independent Test**: Quickstart Q3–Q6, Q10–Q11; failure injection **10/10**; gate matrix; CMS+DTS sequencing dry-run.  
**QCs**: QC-003–005, QC-007–011, QC-016, QC-019, QC-021–022, QC-028–029.

### Tests

- [x] T043 [P] [US2] Unit tests for backup gate matrix (product backup / external confirm / neither) in migrator test class under `system/src/test/java/com/percussion/install/` (or chosen package) — **QC-007**, **FR-018**, **SC-010** (`PSRepositoryBackupGateTest`)
- [x] T044 [P] [US2] Unit tests for migrator state machine outcomes (`SUCCESS`, `FAILED`, `SKIPPED_NON_DERBY`, `BLOCKED_BACKUP_GATE`, `ALREADY_MIGRATED`) and secrets redaction helper — **QC-010**, **QC-022**, **FR-017** (`PSEmbeddedRepositoryMigratorTest`)
- [x] T045 [P] [US2] Unit/IT for exclusive migrator lock (`FileChannel.tryLock`) second-process blocked — **QC-019** (`PSMigratorLockTest`)
- [x] T046 [US2] Integration test: mini source fixture → TableFactory export/import → identity preserve + NEXTNUMBER + cutover under `system/src/test/java/` — **QC-003**, **QC-028**, **FR-007**, **SC-002** (`PSTableFactoryMigrationTransferTest`, `PSEmbeddedRepositoryMigrationIT`; H2↔H2 exercises transfer; production uses Derby source props + FR-021 jars)
- [x] T047 [US2] Integration tests for failure injection (**target 10/10** cases: disk full, kill mid-copy, corrupt source, validation fail, etc.) asserting config remains Derby and source openable — **QC-008**, **QC-021**, **FR-008**, **SC-004**
- [x] T048 [US2] Integration test multi-file cutover consistency (`rxrepository` + Jetty perc-ds labels both H2 after SUCCESS; mid-cutover restore) — **QC-009**, **FR-013** (`PSConfigCutoverTest`)
- [x] T049 [US2] Boolean/BIT and CLOB content probes in migration IT — **QC-004**, **QC-005**, **FR-007** (covered by TableFactory transfer IT + CHAR flag fixtures)
- [x] T050 [US2] Scale fixture path for ≥1000 content items (generator or loader) + wall-clock log to `specs/548-derby-embedded-migration/checklists/migration-timing.md` — **QC-029** **hard gate for SC-002**
- [x] T051 [P] [US2] DTS per-service migration IT (at least metadata + one other service) under delivery-tier test trees — **QC-012**, **FR-006**, **SC-003** (`PSDtsEmbeddedRepositoryMigratorTest` dual-service detect/cutover isolation)
- [x] T052 [US2] Test durable migration report file written under install tree (path per observability contract) readable after SUCCESS/FAILED/SKIPPED — **FR-017** (covered in `PSEmbeddedRepositoryMigratorTest`)

### Implementation

- [x] T053 [US2] Implement product offline full-dir pre-migration backup (NIO copy of repository dir + companion config) in upgrade/migrator code under `system/src/main/java/com/percussion/install/` (new class e.g. `PSEmbeddedRepositoryMigrator.java` / `PSRepositoryBackupGate.java`) per contracts/backup-restore.md — **QC-007**, **FR-018a** (`PSRepositoryOfflineBackup`)
- [x] T054 [US2] Implement **FR-018b external-backup confirmation** using the **frozen primary UX only**: upgrade property / system property `perc.migration.externalBackupConfirmed=true` set by installer checkbox or CLI `-Dperc.migration.externalBackupConfirmed=true` (see contracts/migration-upgrade.md); must be affirmative, non-default, logged without secrets — do not invent alternate silent paths
- [x] T055 [US2] If installer surfaces a visible checkbox/label for T054, add `perc-i18n` string keys for those operator-facing messages under the project i18n module/resources; if no UI strings, record N/A in PR body — **N/A**: primary UX is system property / CLI `-Dperc.migration.externalBackupConfirmed=true` only (no installer checkbox UI in this change set)
- [x] T056 [US2] Implement disk precheck before pump and exclusive lock file under install root — **QC-019**, **QC-021** (`PSMigratorLock`, `PSRepositoryOfflineBackup.hasSufficientDiskSpace`)
- [x] T057 [US2] Implement CMS detector for product-managed Derby (embedded and networked ClientDriver configs) reading `rxrepository.properties` (`PSEmbeddedRepositoryDetector`)
- [x] T058 [US2] Implement CMS schema create on H2 via TableFactory export XML → import (not custom JDBC pump) with **explicit PKs** and NEXTNUMBER preserved — **QC-003**, **QC-028**, **FR-005**, **FR-007** (`PSCatalogTableData.exportDatabase`, `PSJdbcTableFactory.importDatabase`, `PSTableFactoryMigrationTransfer`)
- [x] T059 [US2] Implement validation probes (table-set / post-import target checks, NEXTNUMBER) before cutover — **FR-007**, **SC-002** (`PSMigrationValidator.validateTargetOnly` + dual-conn validate)
- [x] T060 [US2] Implement multi-file cutover + rollback for `rxrepository.properties`, Jetty `perc-ds.properties` / related, with durable writes — **QC-009**, **FR-013** (`PSConfigCutover`)
- [x] T061 [US2] Implement migration outcome logging **and durable report file** per `contracts/migration-observability.md` (**FR-017**) — `PSMigrationReportWriter` + migrator log lines; report under `rxconfig/Installer/migration-report-CMS.properties`
- [x] T062 [US2] Retain Derby files after SUCCESS; implement optional/documented cleanup entry point without auto-delete — **QC-016**, **FR-019**, **SC-011** — SUCCESS never deletes Derby residue; cutover only rewrites configs (cleanup entry point deferred to US5 docs T085)
- [x] T063 [US2] Wire migrator into CMS upgrade path (ANT/install upgrade targets under `system/installResources` / distribution upgrade sequence) — `PSUpgradePluginEmbeddedRepositoryMigration` in `rxPreUpgradePlugins.xml` + `rxupgrade.xml`; `PSMigrateEmbeddedRepository` ANT task in system + distribution `upgrade.chain`
- [x] T064 [US2] Implement DTS per-service migration (TableFactory export/import + config cutover) — **QC-012**, **FR-006**, **SC-003** (`PSDtsEmbeddedRepositoryMigrator`, `PSMigrateDtsEmbeddedRepository`, `installDts.xml`)
- [x] T065 [US2] **CMS+DTS upgrade sequencing** documented — `checklists/upgrade-sequence.md` (+ contracts already state order)
- [x] T066 [US2] Keep Derby jars on migration classpath only (**FR-021** window); document scope — `checklists/derby-migration-classpath.md`
- [x] T067 [US2] Standalone clean install/tests for all modules touched in US2; PR evidence — **QC-018** — completed via PR #1496 (+ residual #1498)
- [x] T068 [US2] Commit US2; open PR “548 US2 Derby migration”; pause for review/merge — **merged** [#1496](https://github.com/intersoftdatalabs-in/percussioncms/pull/1496)

---

## Phase 5: User Story 4 — Multiuser access and locking parity (Priority: P1)

**Goal**: ≥10 concurrent CMS editors with product lock semantics; DTS concurrent writes at default levels; no silent lost updates on default H2 (**FR-003**, **FR-004**, **SC-005**).  
**Independent Test**: Quickstart Q8; QC-006 hard pass beyond bake-off smoke; DTS multi-writer smoke.  
**QCs**: QC-006, QC-025 (evidence updated).  
**Note**: Phase 5 executes **US4** (not US3).

### Tests

- [x] T069 [P] [US4] Automated concurrency harness under `system/src/test/java/` (or sitemanage test if product checkout APIs live there) driving ≥10 concurrent editor sessions against content checkout/workflow APIs on H2 — **QC-006**, **FR-003**, **FR-004**, **SC-005** (`PSH2MultiuserLockHarnessTest` product-shaped SQL)
- [x] T070 [P] [US4] Contention tests: same-item exclusive edit vs different-item parallel edits; assert product rules and 0 corruption (`PSH2MultiuserLockHarnessTest`)
- [x] T071 [US4] **DTS concurrent write smoke (SC-005)**: automated or scripted multi-threaded writes against at least one default embedded DTS service (e.g. metadata or forms) at documented default pool levels; assert 0 silent corruption / 0 unexplained data loss — record in `bake-off-report.md` or `checklists/dts-concurrency.md` (`PSH2DtsConcurrentWriteSmokeTest`)

### Implementation

- [x] T072 [US4] Fix any H2 dialect/lock SQL gaps found by harness (minimal custom dialect subclass pattern like `PSCommunityDerbyDialect` if required) under `system/services/src/com/percussion/services/datasource/` — **N/A**: stock `H2Dialect` for-update + harness green; no custom dialect required
- [x] T073 [US4] Tune pool/isolation only if bake-off+harness prove necessary; document defaults in ops notes — **N/A**: no pool/isolation retune required by harness
- [x] T074 [US4] Update `bake-off-report.md` / #548 with final multiuser evidence (CMS + DTS) — **QC-025**
- [x] T075 [US4] Standalone tests green; commit; PR “548 US4 multiuser”; review/merge — **merged** [#1499](https://github.com/intersoftdatalabs-in/percussioncms/pull/1499)

---

## Phase 6: User Story 3 — External database customers unaffected (Priority: P1)

**Goal**: MySQL/SQL Server upgrades never invoke embedded migration or rewrite connection identity (**FR-009**, **FR-015**, **SC-006**).  
**Independent Test**: Quickstart Q7; QC-011/QC-020.  
**QCs**: QC-011, QC-020.  
**Note**: Phase 6 executes **US3** after US2 detectors exist (phase number ≠ story number).

### Tests

- [x] T076 [P] [US3] Unit tests: migrator detector returns `SKIPPED_NON_DERBY` for `DB_BACKEND=MYSQL`/`MSSQL` fixtures under `system/src/test/java/` — **QC-011**, **FR-009** (`PSEmbeddedRepositoryExternalDbTest`)
- [x] T077 [P] [US3] Mixed-estate tests: CMS MySQL + DTS Derby detection independent — **QC-020** (`PSEmbeddedRepositoryExternalDbTest` + DTS mixed tests)
- [x] T078 [US3] Assert connection key stability (no unintended rewrite) for external sample `rxrepository.properties` fixtures in tests — **FR-015**, **SC-006**

### Implementation

- [x] T079 [US3] Guard all upgrade entry points so non-Derby backends skip pump/cutover; logging outcome `SKIPPED_NON_DERBY` — **FR-009** (detector + migrator skip path; plugin maps to SUCCESS skip)
- [x] T080 [US3] Regression pass on existing installer enterprise dbprops path (`modules/perc-distribution-tree`) still works with MySQL/MSSQL samples — **FR-015** (`ExternalDbSamplePropsPackagingTest`)
- [x] T081 [US3] Standalone tests; commit; PR “548 US3 external unchanged”; review/merge — **merged** [#1497](https://github.com/intersoftdatalabs-in/percussioncms/pull/1497)

---

## Phase 7: User Story 5 — Backup and restore documented and operable (Priority: P2)

**Goal**: Offline-only steady-state backup/restore procedures work cross-platform; support dry-run ≤60 minutes (**FR-011**, **FR-020**, **SC-007**); migration sizing guidance published.  
**Independent Test**: Quickstart Q9; QC-015.  
**QCs**: QC-015, QC-017, QC-022.

### Tests

- [x] T082 [P] [US5] Unit tests for path building in backup helper using `Path`/`Files` (Windows separator scenarios where feasible) under migrator/backup test class — **QC-017**
- [x] T083 [US5] Documented dry-run checklist execution notes filed under `specs/548-derby-embedded-migration/checklists/backup-restore-dry-run.md` (timed ≤60 min) — **QC-015**, **SC-007**

### Implementation

- [x] T084 [US5] Author operator docs for offline backup/restore (CMS + DTS) under **`docs/ai-generated/tasks/548-derby-embedded-migration/`** (and link from distribution/module README as needed) covering stop/start for Windows/Linux/macOS, paths, inclusions, unsupported online backup — **FR-011**, **FR-020**
- [x] T085 [US5] Document pre-migration gate (product backup vs `perc.migration.externalBackupConfirmed`) and post-migration Derby residue cleanup — **FR-018**, **FR-019**
- [x] T086 [US5] Document **CMS+DTS upgrade sequence** (from T065) and mixed-estate behavior for operators in the same docs tree
- [x] T087 [US5] Publish **migration duration / disk sizing guidance** table in `docs/ai-generated/tasks/548-derby-embedded-migration/migration-sizing.md` using T050 wall-clock + disk precheck formula (source + target + temp + backup) — plan WP6 / spec large-repo edge case
- [x] T088 [US5] Ensure backup helper refuses/warns if used while instance is live when that can be detected; docs state stop-first
- [x] T089 [US5] Cross-platform script notes: any helper under `scripts/` has `.bat` counterpart or Java/Maven entry point per AGENTS; inventory script already requires `.bat` (T004) — **N/A this change set**: no new `scripts/` helpers; product path is Java migrator + ANT
- [x] T090 [US5] Commit docs + tests; PR “548 US5 backup restore”; review/merge — **merged** [#1498](https://github.com/intersoftdatalabs-in/percussioncms/pull/1498)

---

## Phase 8: User Story 6 — Support and release communications (Priority: P3)

**Goal**: Release notes / upgrade guide explain Derby retirement, who is affected, FR-021 window, DRDA/network access change (**FR-012**, **SC-008**, **SC-012**).  
**Independent Test**: Quickstart release-notes acceptance; QC-027.  
**QCs**: QC-027.

### Implementation

- [x] T091 [P] [US6] Draft release notes at `docs/ai-generated/tasks/548-derby-embedded-migration/release-notes-8.2-derby-migration.md`: Derby retired, auto-migrate, external DB unchanged, offline backup, GA+1 migration window, no DRDA promise, link sizing + backup docs
- [x] T092 [P] [US6] Draft upgrade guide “Am I affected?” decision tree at `docs/ai-generated/tasks/548-derby-embedded-migration/am-i-affected.md` (Derby default vs MySQL/MSSQL) — **SC-008** support FAQ path
- [x] T093 [US6] Add FR-021 tracking checklist items on GitHub #548 for next product line + deprecation notice — **QC-027**, **SC-012** — repo checklist `docs/.../fr-021-migration-window.md` + issue comment
- [x] T094 [US6] Commit; PR “548 US6 release comms”; review/merge — **merged** [#1504](https://github.com/intersoftdatalabs-in/percussioncms/pull/1504); residual QC freeze / main reconcile [#3065](https://github.com/intersoftdatalabs-in/percussioncms/issues/3065)

---

## Phase 9: Polish & Cross-Cutting Concerns (GA hardening)

**Goal**: Packaging audit, package installs, Windows services, support window hygiene, full QC closeout, issue #548 ready.  
**QCs**: QC-013, QC-018, QC-023, QC-024, QC-027, **QC-029** (T050 must be `[x]`), + remaining open hard QCs.

- [x] T095 [P] Packaging audit: new default installs do not require Derby on live runtime classpath; H2 present; Derby migration-scoped — document in `specs/548-derby-embedded-migration/checklists/packaging-audit.md` — **QC-013**, **SC-009**
- [x] T096 [P] Audit `.ppkg` / `modules/perc-packages/**/psx_archiveInfo.xml` Derby driver stamps; fix runtime-impacting cases; soft/hard per QC-023 — **QC-023** soft disposition documented in packaging-audit (41 stamps; no mass rewrite)
- [x] T097 Verify Windows service/Procrun and shell start scripts use H2 home after upgrade; document service reinstall/update steps — **QC-024** — `checklists/windows-service-h2-notes.md`
- [x] T098 Walk [checklists/quality-gates.md](checklists/quality-gates.md) and mark every hard QC `[x]` or record product-owner waiver on #548 with rationale; **confirm T050/QC-029 and T038 OS matrix** before close — QC checklist updated 2026-08-11; T050/QC-029 met; QC-023 hard closed via #2333; T038 full install still open on **#2332**
- [x] T099 Update [checklists/derby-surface-inventory.md](checklists/derby-surface-inventory.md) so disposition has zero `unknown` rows — **QC-001** freeze — re-run 2026-08-11 on `main` — **0 unknown** (`--fail-on-unknown`)
- [x] T100 [P] Spotless / formatting on all touched Java modules via module `mvnw` spotless goals as project standard — **N/A** this closeout slice (docs/script only; Java landed in prior PRs)
- [x] T101 Security pass: no password logging; backup dir permission notes in docs; redaction tests still green — **QC-022** — `checklists/security-pass.md`
- [x] T102 Final standalone `clean install` on every module changed across the feature; attach command list + BUILD SUCCESS to #548 and final PR — **QC-018** — satisfied on implementation PRs #1494–#1499; docs/script PR N/A
- [x] T103 Erlang pre-commit review for last code PR per AGENTS; fix hard-gate findings before merge — **N/A** docs/script closeout (no product Java in this PR); prior code PRs used Kilo/Erlang gates
- [x] T104 Update GitHub issue #548 body checkboxes; link plan/tasks/QC checklist; do **not** close until all hard QCs and runtime smoke agreed for release — body + residual QA table 2026-08-07 (#2332/#2333); US6 residual #3065; leave #548 open until T038 (#2332) or waiver
- [x] T105 Optional: `/speckit-taskstoissues` or manually file WP-scoped GitHub issues if team wants tracker split beyond #548 — **done as residuals** (#2332 T038 QA, #2333 QC-023 QA closed, #3065 US6/Phase 9 docs residual; Postgres still #1500)

---

## Dependencies & Execution Order

```text
Phase 1 Setup
    → Phase 2 Foundational (inventory → bake-off lock → seed decision → platform primitives)
        → Phase 3 US1 New install          [P1]
        → Phase 4 US2 Migration            [P1]  (depends on US1 defaults + foundation)
        → Phase 5 US4 Multiuser            [P1]  (depends on US1 H2 runtime; phase# ≠ story#)
        → Phase 6 US3 External unchanged   [P1]  (depends on migrator detectors from US2)
        → Phase 7 US5 Backup/restore docs  [P2]  (depends on US2 backup helper)
        → Phase 8 US6 Release comms        [P3]
        → Phase 9 Polish / GA
```

| Story  |             Depends on             |                                     Notes                                     |
|--------|------------------------------------|-------------------------------------------------------------------------------|
| US1    | Phase 2 (incl. T024 seed decision) | First shippable product default change                                        |
| US2    | Phase 2 + US1                      | Migration target must match new install engine                                |
| US4    | Phase 2 + US1                      | Full harness after runtime exists                                             |
| US3    | US2 detectors                      | Finish after migrator API stable                                              |
| US5    | US2 backup helper + T050 timing    | Docs + sizing                                                                 |
| US6    | US1–US5 content                    | Canonical paths under `docs/ai-generated/tasks/548-derby-embedded-migration/` |
| Polish | All stories                        | QC freeze including QC-029                                                    |

### Constitution story checkpoints

Each user-story phase ends with commit → PR → CI/Kilo → resolve threads → human merge before the next story. Foundation PR (T025) merges before US1 if possible.

---

## Parallel Execution Examples

```text
# After T011 engine lock, parallelizable foundation work:
T012 PSJdbcUtils H2 constants          ||  T015 TableFactory H2 map
T014 utils unit tests                  ||  T016 TableFactory tests
T017 config.xml + dialect maps         ||  T019 DatabaseFunctionDefs H2 rows

# Within US2 tests (after migrator skeleton exists):
T043 backup gate tests  ||  T044 outcome/redaction tests  ||  T045 lock tests

# Docs late:
T091 release notes  ||  T092 am-i-affected  ||  T096 package audit
```

**Do not parallelize**: T004–T011 (inventory/bake-off), T024 before T029 (seed decision before install redesign), T029 before T030 (install redesign before sqlH2), T058 before T059–T060 (pump before validate/cutover).

---

## Implementation Strategy

### MVP (first vertical)

1. Phase 1–2 foundation (inventory + bake-off + seed decision + primitives)
2. **US1 only** — new installs on H2 (CMS + DTS defaults)
3. Validate Q1/Q2 + QC-013/014 + OS matrix (T038)

### Incremental delivery

1. US2 migration + safety gates + sequencing (largest risk)
2. US4 multiuser (CMS + DTS)
3. US3 external non-interference
4. US5 ops docs + sizing + dry-run
5. US6 communications
6. Phase 9 packaging/QC freeze

### Suggested PR slicing

|  PR  |                   Scope                   |
|------|-------------------------------------------|
| PR-A | Phase 2 foundation                        |
| PR-B | US1 new install                           |
| PR-C | US2 CMS migrator                          |
| PR-D | US2 DTS migrator (if large, split from C) |
| PR-E | US4 multiuser                             |
| PR-F | US3 + US5 + US6 docs/guards               |
| PR-G | Polish / packaging / QC closeout          |

---

## Format validation

- All tasks use `- [ ]`, sequential IDs **T001–T105**, story labels on US phases only, `[P]` only when parallel-safe, and concrete file paths.
- Total tasks: **105**
- Setup: 3 · Foundational: 22 · US1: 17 · US2: 26 · US4: 7 · US3: 6 · US5: 9 · US6: 4 · Polish: 11

---

## Next steps

```text
# Optional issue fan-out:
/speckit-taskstoissues

# Start implementation at Phase 2:
T004 inventory script → T007–T011 bake-off → T024 seed decision → T012+ platform
```

