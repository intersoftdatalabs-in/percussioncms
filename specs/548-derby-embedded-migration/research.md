# Research: Replace Retired Default Embedded Repository (#548)

**Branch**: `548-derby-embedded-migration`  
**Date**: 2026-07-23  
**Spec**: [spec.md](spec.md)  
**Issue**: [#548](https://github.com/intersoftdatalabs-in/percussioncms/issues/548)

## R1 — Default embedded engine selection

### Decision

**Primary target: H2** (pure Java, actively maintained) — **LOCKED 2026-07-24**.  
**Bake-off alternate: HSQLDB** if H2 fails multiuser/locking soak — **not required** (H2 passed T008 smoke + T009 ≥10-editor product-shaped lock harness).  
**Rejected as default: SQLite** (native bridge; single-writer; weak multiuser locking).  
Evidence: `checklists/bake-off-report.md`; tests `PSH2DialectSmokeTest`, `PSH2MultiuserLockHarnessTest`.

### Rationale

- Spec requires multiuser access and locking comparable to Derby (FR-003, FR-004, SC-005, 10 concurrent editors).
- Derby retired upstream (2025-10-10); product pins `derby.version` 10.17.1.0 today.
- H2: pure Java JAR, embedded + optional TCP/`AUTO_SERVER`, `SELECT … FOR UPDATE`, first-class Hibernate dialect (`org.hibernate.dialect.H2Dialect` under Hibernate 7.2.x used on `development`).
- HSQLDB: strong locking modes (MVCC/LOCKS/MVLOCKS); good fallback if H2 page-store or lock-upgrade behavior fails product soak.
- SQLite: fails multiuser + installer packaging (native libs per OS) bar recorded on #548.

### Alternatives considered

| Option | Why not primary |
|--------|-----------------|
| Stay on Derby | Retired; no fixes/releases |
| HSQLDB | Viable; slightly less Java ecosystem gravity than H2 — keep as bake-off winner path |
| SQLite | Not multi-writer parity; JNI packaging |
| Force all customers to MySQL/SQL Server | Violates FR-014 / product positioning for zero-admin default |
| DuckDB / analytics engines | Wrong OLTP workload |

### Bake-off gate (must pass before engine lock)

Against CMS fixture + DTS service write paths, pool ≥ production defaults:

1. ≥10 concurrent CMS editors (SC-005): no silent lost updates, no corruption  
2. Pessimistic / `FOR UPDATE` paths used by content checkout & workflow  
3. CLOB/BLOB and identity/auto-increment round-trip  
4. Long transaction (publish / upgrade-plugin style) under concurrent readers  
5. Clean restart and offline backup file consistency  

**Pass H2 → lock H2.** Fail H2, pass HSQLDB → lock HSQLDB and re-run plan deltas for dialect/maps only.

---

## R2 — Migration architecture (CMS vs DTS split)

### Decision

**Upgrade-time dual-access migration** with hard cutover. **CMS and DTS use different schema engines today** (Sherlock Issue 1) — do not force one mechanism:

#### CMS (TableFactory-centric)

1. **Backup gate** (FR-018): product offline backup **or** affirmative operator confirmation of external backup.  
2. Detect product-managed Derby (`DB_BACKEND=DERBY` / driver derby patterns in `rxrepository.properties`).  
3. Open Derby **read-only** (Derby jars retained for migration window — FR-021). Note: runtime/install may be **networked Derby** (ClientDriver / port 1527) — see **R11**.  
4. Create empty target repository; apply product schema via **TableFactory** (constitution IV).  
5. **Data pump**: catalog product-owned tables from source metadata ∩ product schemas; **FK-safe order** (topological sort; disable FKs during load if engine allows; re-enable + validate). Insert **explicit primary keys** (preserve content ids); re-sync **NEXTNUMBER** after copy.  
6. Validate: table-set equality, row counts, boolean/LOB probes, NEXTNUMBER, critical entity probes (SC-002).  
7. **Multi-file cutover** (all-or-nothing): `rxrepository.properties` + Jetty `perc-ds.*` + any other live DS pointers; rollback props from backup on failure.  
8. Live store = new engine only (FR-013). Retain Derby files until operator cleanup (FR-019).

#### DTS (Hibernate / Liquibase-centric — not TableFactory)

1. Same backup gate and safe-fail rules per service.  
2. Detect Derby per service datasource props / beans.  
3. Create target DB; apply schema via **Hibernate schema update and/or Liquibase** with **H2 (or HSQL) changesets** — port every `dbms="derby"` changeset (e.g. metadata `changeLogIndex*.xml` SYS catalog preconditions).  
4. JDBC/data pump of service tables with FK-safe order.  
5. Cutover service config + Windows/Linux start scripts (`derby.system.home` → new home).  
6. Validate health + sample reads (SC-003).

On any failure before cutover: abort, leave Derby config + files intact (FR-008). Partial target never live-pointed.

### Rationale

- Constitution: schema/data portability via TableFactory; no ad-hoc schema DDL in app code as primary path.  
- Existing multi-backend maps: `PSJdbcDataTypeMaps.xml` already has DERBY, MYSQL, MSSQL, ORACLE, DB2 — add **H2** (or HSQL) map.  
- `PSJdbcUtils` already centralizes driver/backend labels.  
- Safe-fail and backup gate are clarified product requirements, not optional polish.

### Alternatives considered

| Option | Why rejected / deferred |
|--------|-------------------------|
| In-place Derby file conversion | Impossible; different storage engines |
| Manual customer-only export | Violates automatic migration (Story 2) |
| Dual-write forever | Operational complexity; FR-013 forbids dual-write steady-state |
| Logical SQL SCRIPT only | Fragile for large CMS schemas; TableFactory is product standard |

---

## R3 — CMS platform wiring

### Decision

Treat CMS default path as first-class backend alongside MYSQL/MSSQL:

| Concern | Existing Derby touchpoint | New engine work |
|---------|---------------------------|-----------------|
| Driver registry | `system/config/config.xml` `PSXJdbcDriverConfig` derby | Add H2 (or HSQL) driver config |
| Defaults | `system/config/Default/rxrepository.properties` | New `DB_BACKEND`, driver name/class, server URL form |
| Jetty DS | `modules/perc-jetty/.../perc-ds.properties` | Driver/class/URL defaults |
| Hibernate dialect | `PSCommunityDerbyDialect` + `PSSessionFactoryBean` Derby substitutions | Register H2 dialect; H2-specific props (boolean, isolation) — **do not** blindly copy Derby `true=T,false=F` unless required |
| SQL helpers | `PSSqlHelper.isDerby`, identity/limit quirks | `isH2` / backend branches where Derby-only |
| Function defs | `sys_DatabaseFunctionDefs.xml` driver=`derby` | Parallel `h2` (or hsqldb) rows |
| Installer | distribution `rxrepository.properties`, install XML | Default new installs → new engine; upgrade invokes migrator |
| Utils | `PSJdbcUtils.DERBY_*` | Add `H2_*` (or HSQL) constants; URL map |

### Rationale

Evidence-based extension of multi-backend patterns rather than a parallel stack.

---

## R4 — DTS platform wiring

### Decision

Migrate **each product-managed DTS service** that defaults to embedded Derby (comments, forms, feeds, membership, metadata, polls, integrations as applicable) to the same engine family and migration contract:

- Module `pom.xml` Derby deps → new engine + Derby **test/migration-scoped** where needed  
- `perc-datasources.properties` / beans / cargo DS URLs  
- `delivery-tier-distribution` packaging (`derby.system.home`, cargo datasource, Procrun/install trees)  
- Per-service upgrade detection using that service’s config (mixed estate: CMS Derby + DTS MySQL allowed — Edge Cases)

### Rationale

Spec Stories 1–3 and FR-002/006 require DTS parity with CMS for default embedded.

---

## R5 — Backup gate & offline backup (ops)

### Decision

- **Pre-migration (FR-018)**: Hard gate in upgrade orchestration. Product backup path MUST stop services / use offline-consistent copy of Derby files (aligned with FR-020 spirit). External confirmation is affirmative, logged, non-default.  
- **Steady-state (FR-020)**: Document **stop instance → copy repository files (and documented companion config) → start**. No online full backup claim.  
- **Restore**: Stop → replace files from backup → start → smoke verify.

### Rationale

Clarified Session 2026-07-23; embedded file stores are consistently backed up offline.

---

## R6 — Derby artifact lifecycle

### Decision

| Phase | Derby role |
|-------|------------|
| Pre-GA feature work | Full dependency as today for tests + migration development |
| GA release (new default) | **Not** live default; jars retained for **read-only migration** from prior installs |
| Next product line after GA | Still ship migration path (FR-021) |
| After window | Remove Derby deps and migration code with deprecation notice in prior line |

Retained **data files** after successful migrate: until operator cleanup (FR-019), independent of jar lifecycle.

### Rationale

Supports late upgraders without forever-shipping a retired engine as a live default.

---

## R7 — Testing strategy

### Decision

| Layer | Coverage |
|-------|----------|
| Unit | Dialect, `PSJdbcUtils` backend mapping, migrator state machine, backup gate logic, datatype map selection |
| Module integration | TableFactory H2 map + create/load; CMS datasource start against H2 file DB |
| Migration integration | Derby fixture → migrate → probe entities (SC-002 scale ≥1000 content items or agreed fixture) |
| Failure injection | Disk full, mid-copy abort, gate not satisfied (SC-004, SC-010) |
| Concurrency | ≥10 concurrent CMS editors (SC-005) |
| External DB regression | MySQL/SQL Server upgrade paths do not invoke migrator (SC-006) |
| DTS | Per-service default install smoke + one multi-service migration fixture (SC-001, SC-003) |
| Docs dry-run | Offline backup/restore walkthrough (SC-007) |

Maven: standalone `./mvn-env.sh clean install` per changed module (AGENTS pre-PR gate).

### Rationale

Constitution III + spec FR-016 / success criteria.

---

## R8 — Security & packaging

### Decision

- No passwords in migration logs (constitution VI).  
- Paths via `Path` / `Files` APIs; no Unix-only path assumptions (AGENTS cross-platform).  
- Ship H2 (or HSQL) JAR via existing JDBC/lib packaging patterns (`perc-jetty`, DTS distribution) similar to Derby placement.  
- Pre-migration backup artifacts treated as sensitive (same class as repository files).

### Alternatives considered

Embedding engine only in app classpath vs shared `lib/jdbc` — prefer **consistent with how Derby is packaged today** per distribution module to minimize install drift.

---

## R9 — Hibernate / locking notes (H2)

### Decision

- Use Hibernate **7.2.x** `org.hibernate.dialect.H2Dialect` (or project-standard dialect registration path used for other drivers).  
- Validate `FOR UPDATE` / lock modes against product checkout paths early in bake-off.  
- If community dialect gaps appear, prefer **minimal custom dialect subclass** (pattern: `PSCommunityDerbyDialect`) over broad SQL rewrites.  
- Revisit `PSSessionFactoryBean.configureDatabaseSpecificProperties` — Derby isolation and query substitutions must not apply blindly to H2.

### Rationale

Product already required a Derby dialect patch for Hibernate 7 locking SQL correctness; expect similar diligence for H2.

---

## R10 — Out of scope confirmation (design)

- PostgreSQL as new external backend  
- Multi-node shared embedded clustering  
- Online hot full backup  
- Indefinite Derby migration beyond FR-021 window  
- Guaranteeing non-product objects in Derby  

---

## R11 — Installer multi-process access (replacing Derby Network Server)

### Decision

CMS install today is **not pure single-JVM embedded**:

- `installRepository.xml` / distribution install: unzip seed `Derby/Repository.zip`, start **`NetworkServerControl`**, SQL via **ClientDriver** on **localhost:1527**  
- Jetty `perc.mod`: `-Dderby.system.home=...`, `-Dderby.drda.startNetworkServer=true`  

New default engine **must** replace this deliberately (Sherlock Issue 2). Preferred order of evaluation in bake-off / WP0–WP2:

| Option | Description | When to choose |
|--------|-------------|----------------|
| **A** | Pure embedded + all install SQL **in-process Java** (no second JVM) | Preferred if ANT multi-JVM SQL can be retired |
| **B** | H2 **`AUTO_SERVER`** so installer process + server process share file DB | If multi-process install SQL must remain |
| **C** | H2 **TCP server** analogous to DRDA | Last resort; document port and lifecycle |

**Seed strategy (T024 locked 2026-07-24)**:

| Option | Description |
|--------|-------------|
| **A (LOCKED)** | Empty H2 file DB + TableFactory / product load for schema+seed data |
| ~~B~~ | Prebuilt H2 seed tree replacing `Derby/Repository.zip` — deferred; more packaging risk without first bake-off evidence |

**Rationale**: Matches constitution IV (TableFactory), avoids shipping binary seed DB trees, and aligns with automatic schema create already used for multi-backend installs. Drop `derby.drda.*` from Jetty module when H2 is live default.

**Product decision on remote network access** (Sherlock Issue 11): default embedded **does not** promise DRDA-compatible remote access; document break in release notes. Optional H2 TCP for support-only is a plan detail, not a customer cluster feature.

### Rationale

Without R11, WP2 “change rxrepository defaults” cannot produce a working installer.

---

## R12 — Derby surface inventory (mandatory)

### Decision

WP1 **hard deliverable**: repo-wide inventory (scripted) of:

`derby`, `DERBY`, `sqlDerby`, `NetworkServer`, `isDerby`, `derby.system`, `dbms="derby"`, `DerbyDialect`, `ClientDriver`/`EmbeddedDriver` (Derby), `1527`, package `psx_archiveInfo` driver stamps, `IPSSystemService.isDerby`, LOB `isDerbyDatabase`, Windows `derby.system.home` scripts.

Each hit: **port / add sqlH2 / generalize / migration-only / docs-only**. QC-001 freezes inventory at GA with zero unknowns.

---

## R13 — Product pre-migration backup implementation

### Decision

FR-018a has **no existing full-repo snapshot API** (`PSUpgradeBackupTable` is per-table). Implement:

1. Ensure Network Server / CMS stopped (or install-controlled offline state).  
2. Copy resolved repository data directory + documented companion config via NIO `Path`/`Files`.  
3. Log size/checksum; no secrets in logs.  
4. Gate proceeds only on success.  

FR-018b **primary UX (frozen)**: property `perc.migration.externalBackupConfirmed=true` set only by explicit installer checkbox or `-Dperc.migration.externalBackupConfirmed=true`. Affirmative, non-default, logged without secrets.

Concurrent migration: exclusive lock via `FileChannel.tryLock` on well-known path under install root; stale-lock recovery documented (QC-019).

---

## R14 — Type / LOB / identity fidelity

### Decision

| Concern | Rule |
|---------|------|
| Boolean/BIT | Do **not** copy Derby `true=T,false=F` substitutions blindly. Define H2 map + pump conversion; probes on flag columns (QC-004). |
| CLOB/BLOB | Generalize `PSContentRepository.isDerbyDatabase()` / type config LOB materialization to embedded file-store predicate covering H2 (QC-005). |
| Identity | Pump inserts **explicit PKs**; post-copy NEXTNUMBER validation (QC-003). |
| H2 URL | Freeze canonical URL template (path, MODE, case folding, schema/user) so `UpperCaseNamingStrategy` + TableFactory + security tables resolve (Issue 15). |

---

## Open items deferred to implementation tasks (not blocking plan after design revision)

- Exact H2 Maven coordinates/version pin in parent POM (resolve at implement time from current stable compatible with Hibernate 7.2 / JDK 21).  
- Exact on-disk directory names for new CMS/DTS repositories (follow product conventions; document in ops guide).  
- Whether optional product cleanup step is installer UI, CLI, or documented manual `rm`/delete — product may ship optional step (FR-019 allows).  
- Fixture generation approach for ≥1000 items (existing test content loaders vs synthetic generator).

## Resolved NEEDS CLARIFICATION

None remain for **product** planning. Spec Session 2026-07-23 closed product decisions (backup gate, retention, offline backup, support window, 10 editors). Engine bake-off is an **implementation gate**.

**Sherlock revision (2026-07-23)**: Design gaps on DTS schema path, networked install, and Derby surface breadth are addressed in R2 (split), R11–R14. Quality gates QC-001–QC-030 live in [checklists/quality-gates.md](checklists/quality-gates.md).
