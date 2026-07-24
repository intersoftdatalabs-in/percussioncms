# Implementation Plan: Replace Retired Default Embedded Repository

**Branch**: `548-derby-embedded-migration` | **Date**: 2026-07-23 | **Spec**: [spec.md](spec.md)  
**Input**: Feature specification from `specs/548-derby-embedded-migration/spec.md`  
**Tracked as**: [GitHub Issue #548](https://github.com/intersoftdatalabs-in/percussioncms/issues/548)  
**Feature directory**: `specs/548-derby-embedded-migration`

## Summary

Apache Derby is retired upstream while CMS and DTS still default to embedded Derby for many customers. This plan delivers a **maintained pure-Java default embedded repository** (primary: **H2**, bake-off alternate: **HSQLDB**), **automatic upgrade-time migration** from product-managed Derby with a **hard pre-migration backup gate**, **offline-only** steady-state backup/restore, **operator-controlled** retention of legacy Derby files, and **unchanged** MySQL/SQL Server external paths.

Technical approach: extend existing multi-backend plumbing (`PSJdbcUtils`, TableFactory datatype maps, driver config, Hibernate dialect registration, Jetty/DTS datasources, installer defaults) rather than inventing a parallel persistence stack.

**CMS migration**: dual-access Derby read-only → **TableFactory** schema → FK-safe identity-preserving data pump → multi-file config cutover.  
**DTS migration**: dual-access → **Hibernate/Liquibase** schema (port `dbms="derby"` changesets) → per-service data pump → service config + start-script cutover.  
**Installer**: replace networked Derby (`NetworkServerControl` / port 1527 / `Repository.zip`) per research **R11** — not a simple property rewrite.

Quality gates: [checklists/quality-gates.md](checklists/quality-gates.md) (QC-001–QC-030). Sherlock review: [checklists/sherlock-design-review.md](checklists/sherlock-design-review.md).

## Technical Context

- **Language/Version**: Java 21 on `development` (this branch base); Maven multi-module mono-repo  
- **Owning Module(s)**:
  - **CMS core**: `system/` (datasource, dialect, upgrade/migration orchestration, function defs, defaults)
  - **Schema/data**: `modules/TableFactory`
  - **Shared JDBC**: `modules/utils` (`PSJdbcUtils`, `PSSqlHelper`)
  - **CMS runtime packaging**: `modules/perc-jetty`
  - **Installer / distribution**: `modules/perc-distribution-tree` (and related install resources under `system/installResources` as needed)
  - **DTS**: `deliverytiersuite/delivery-tier-suite/*` services using Derby + `delivery-tier-distribution`
- **AGENTS Hierarchy**: root `AGENTS.md`; module AGENTS for each touched path when present  
- **Dependencies & Storage**:
  - Today: `derby.version` **10.17.1.0** (parent POM); Hibernate **7.2.6.Final**
  - Add: H2 (coordinates pinned at implement time; JDK 21 + Hibernate 7.2 compatible)
  - Retain Derby artifacts for **migration-only** through FR-021 window
  - External: MySQL / SQL Server unchanged
- **Testing**: JUnit 5, Mockito; module standalone `./mvn-env.sh clean install`; migration IT + concurrency harness (≥10 editors); failure injection  
- **Scale/Impact**: Major 8.2 non-UI lift — install defaults, upgrade path, CMS+DTS runtime, docs, packaging; multiuser floor 10 concurrent CMS editors; offline ops model

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*  
*Source: `.specify/memory/constitution.md` (v2.3.0)*

- [x] **I. Module-First Boundaries**: Owning modules listed; shared JDBC in `modules/utils`; no new org-only module for the engine.
- [x] **II. Evidence Over Invention**: Cites existing Derby wiring (`config.xml`, `rxrepository.properties`, `PSCommunityDerbyDialect`, `PSSessionFactoryBean`, `PSJdbcDataTypeMaps.xml` DERBY map, Jetty `perc-ds.*`, DTS cargo/Derby deps, TableFactory networked Derby tests).
- [x] **III. Test Discipline**: Unit + integration + migration + concurrency + external-DB non-interference planned ([quickstart.md](quickstart.md)).
- [x] **IV. Contract & Integration Integrity**: TableFactory for schema/data; repository property **key names** preserved for external DBs; no REST/SOAP contract break required; `.ppkg` formats untouched unless a package embeds Derby-only config (audit in tasks).
- [x] **V. Safe Modernization**: No Spring Boot; no full persistence rewrite; additive backend + migrator.
- [x] **VI. Security by Default**: No secret logging; backup artifacts sensitive; path handling portable.
- [x] **VII. Build & Dependency Hygiene**: Parent POM dependencyManagement for H2; JDK 21 via `mvn-env`; Spotless as existing.
- [x] **VIII. Documentation & Operability**: Migration, offline backup/restore, cleanup, release notes (FR-011/012).
- [x] **IX. PR Review Comment Resolution**: Process obligation on feature PRs.
- [x] **Complexity Budget**: Large multi-module scope is inherent to replacing default repository — not a constitution violation; staged work packages (see below). No unjustified new frameworks.

**Re-evaluation after Phase 1 design**: Gates still pass. Design reuses multi-backend patterns; contracts are upgrade/ops behavioral contracts; engine bake-off is an implementation gate with HSQL fallback path that only swaps maps/dialects/constants.

**Re-evaluation after Sherlock review (2026-07-23)**: Critical design gaps addressed in research R2 split, R11–R14 and quality-gates checklist. Constitution IV still holds for **CMS** TableFactory; DTS divergence is documented (not a new violation—existing product reality). Implementation remains **conditional** until QC-001 inventory exists and WP0 bake-off evidence is filed.

## Project Structure

### Documentation (this feature)

```text
specs/548-derby-embedded-migration/
├── plan.md                 # This file
├── research.md             # Phase 0
├── data-model.md           # Phase 1
├── quickstart.md           # Phase 1
├── contracts/
│   ├── README.md
│   ├── repository-config.md
│   ├── migration-upgrade.md
│   ├── backup-restore.md
│   └── migration-observability.md
├── checklists/
│   ├── requirements.md              # Spec quality (speckit-specify)
│   ├── quality-gates.md             # QC-001–QC-030 GA/PR gates (Sherlock)
│   └── sherlock-design-review.md    # Adversarial design review notes
├── spec.md
└── tasks.md                # Phase 2 — /speckit-tasks (not this command)
```

### Source Code (affected paths — expected)

```text
pom.xml                                    # h2.version (or hsqldb); keep derby.version for migration window
modules/utils/.../PSJdbcUtils.java         # H2_* constants, URL/backend maps
modules/utils/.../PSSqlHelper.java         # isH2 / identity/limit branches as needed
modules/TableFactory/.../PSJdbcDataTypeMaps.xml   # H2 DataTypeMap
modules/TableFactory/src/test/...          # load/create tests on H2; keep Derby migration IT source
system/config/config.xml                   # driver registry
system/config/Default/rxrepository.properties
system/config/sys_DatabaseFunctionDefs.xml # h2 function rows
system/services/.../datasource/            # dialect, PSSessionFactoryBean
system/.../install|upgrade|migrator        # NEW migration orchestration + backup gate
modules/perc-jetty/.../perc-ds.*           # defaults
modules/perc-distribution-tree/...         # install defaults, samples, docs
deliverytiersuite/delivery-tier-suite/
  {comments,forms,feeds,membership,metadata,polls,...}/  # deps + datasource props
  delivery-tier-distribution/              # package Derby→H2, cargo DS, install
docs/ (or help site sources)               # ops guides, release notes material
```

**Structure Decision**: Prefer a dedicated migrator package under CMS install/upgrade code (existing upgrade plugin patterns) callable from installer ANT/Java, plus parallel DTS upgrade hooks in distribution. Shared pure logic (detect backend, gate, pump orchestration interfaces) may live in `system` or a small shared util if both CMS and DTS need it — **avoid** a new top-level module unless plan Complexity Tracking is updated.

## Implementation approach

### Work package overview

| WP | Name | Outcome | Primary QCs |
|----|------|---------|-------------|
| **WP0** | Engine bake-off + lock entry points | Lock H2 or HSQL with written evidence (R1); product checkout/workflow lock tests not raw JDBC only | QC-006, QC-025 |
| **WP1** | Inventory + platform primitives | **Derby surface inventory** (R12/QC-001); POM deps; `PSJdbcUtils`/`isH2`; TableFactory H2 map; dialect; function defs; generalize LOB/`isDerby` APIs; `sqlH2` matrix start | QC-001, QC-002, QC-005, QC-026, QC-030 |
| **WP2** | CMS new-install default | **R11** replace NetworkServer/1527/Repository.zip; Jetty drop `derby.drda.*`; defaults + installer in-process or AUTO_SERVER | QC-013, QC-014, QC-026 |
| **WP3** | CMS migrator | Backup gate (R13); FK-safe identity-preserving pump; NEXTNUMBER; multi-file cutover; lock file; residue retention | QC-003, QC-004, QC-007–011, QC-016, QC-019, QC-021, QC-022, QC-028, QC-029 |
| **WP4** | DTS default + migrator | **Not TableFactory-only**: Hibernate/Liquibase H2 changesets; per-service pump; scripts/Procrun `derby.system.home` | QC-012, QC-020, QC-024 |
| **WP5** | Concurrency & fidelity tests | SC-002/003/005; failure injection; CLOB content; boolean probes | QC-005, QC-006, QC-008, QC-029 |
| **WP6** | Docs & release comms | Offline backup/restore; migration sizing table; DRDA loss; FR-021 window; cleanup | QC-015, QC-027 |
| **WP7** | External DB regression | SC-006; no accidental force | QC-011, QC-020 |
| **WP8** | GA hardening | Packaging audit; .ppkg; Maven evidence; residual Derby live dependency zero | QC-013, QC-018, QC-023, QC-027 |

### WP0 — Bake-off (gate)

1. Spike minimal CMS IT: Hibernate SessionFactory against H2 file DB with product dialect registration + **canonical H2 URL template** (schema/case).  
2. Product **checkout/workflow** lock tests + ≥10 concurrent editors (not JDBC-only).  
3. CLOB content open/save; identity insert smoke.  
4. If fail, repeat HSQLDB.  
5. File bake-off report on #548 (QC-025); freeze engine for WP1+.

### WP1 — Inventory + platform primitives

1. **Generate Derby surface inventory** (R12) → disposition checklist in feature dir (QC-001).  
2. Parent POM: manage H2 version; keep Derby for migration classpath.  
3. `PSJdbcUtils` / `PSSqlHelper` / `IPSSystemService` detection (`isH2` / database type — do not overload Derby forever).  
4. TableFactory `DataTypeMap for="H2"` + boolean/BIT rules (R14).  
5. Generalize LOB materialization beyond `isDerbyDatabase()`.  
6. `config.xml` driver entry; Hibernate dialect in **all** server-beans copies (QC-030).  
7. `PSSessionFactoryBean` H2-specific props (no blind Derby substitutions).  
8. `sys_DatabaseFunctionDefs.xml` H2 rows; start `sqlH2` pairs for install SQL (QC-026).  
9. Unit tests for each.

### WP2 — CMS new install (R11)

1. Replace **NetworkServerControl / 1527 / ClientDriver** install orchestration (option A preferred).  
2. Seed strategy: H2 tree or empty + TableFactory.  
3. Default `rxrepository.properties` / install templates → H2.  
4. Jetty `perc-ds.*` / `perc.mod`: drop `derby.drda.*`; set H2 system props as needed.  
5. Smoke: clean install starts, login works (Q1); no live Derby (QC-013/014).

### WP3 — CMS migration

1. Detector for product-managed Derby (embedded **or** networked ClientDriver configs).  
2. Backup gate: full-dir offline copy (R13) **or** logged external confirm (QC-007).  
3. Disk precheck (QC-021); exclusive migrator lock (QC-019).  
4. TableFactory schema + FK-safe **explicit-PK** pump; NEXTNUMBER (QC-003/028).  
5. Boolean/LOB/table-set validation (QC-004/005/028/029).  
6. Multi-file cutover with rollback (QC-009).  
7. Observability outcomes; residue retention (QC-016); secrets redaction (QC-022).  
8. Tests: happy path, gate block, fail mid-copy, already migrated, non-Derby skip, concurrent lock.

### WP4 — DTS (Hibernate/Liquibase)

1. Inventory DTS Derby deps, `DerbyDialect` hardcodes, Liquibase `dbms="derby"`.  
2. Add H2 (or HSQL) changesets / dialect; hbm2ddl target strategy documented.  
3. Per-service migrator + distribution packaging.  
4. Rewrite `TomcatStartup` / Procrun service scripts (`derby.system.home`) — QC-024.  
5. Mixed-estate tests (QC-020); smoke Q2/Q4.

### WP5 — Heavy verification

1. CMS fixture ≥1000 items (or agreed equivalent).  
2. Concurrency harness ≥10 editors.  
3. Failure injections 10/10 (SC-004).  
4. Cross-platform path tests (Windows-safe `Path` usage).

### WP6 — Documentation

1. Upgrade guide: who is affected, gate, duration guidance, FR-021 window.  
2. Offline backup/restore runbooks (Win/Linux/macOS).  
3. Post-migration cleanup.  
4. Release notes blurb.

### WP7 — External backends

1. Regression: MySQL/MSSQL install+upgrade untouched.  
2. Guard tests: migrator never rewrites non-Derby.

### WP8 — Release readiness

1. New installs: zero live Derby dependency.  
2. Packaging audit: H2 present; Derby migration-only.  
3. Close #548 only when quickstart exit criteria met.

### Story → WP mapping

| Spec story | WPs |
|------------|-----|
| US1 New install | WP0–2, WP4 |
| US2 Upgrade migrate | WP3–5 |
| US3 External unchanged | WP7 |
| US4 Multiuser/locking | WP0, WP5 |
| US5 Backup/restore | WP3 (pre), WP6 |
| US6 Comms | WP6 |

## Complexity Tracking

*(No constitution principle violations requiring exception.)*

| Item | Note |
|------|------|
| Multi-module breadth | Inherent; mitigated by WP staging and per-module standalone Maven builds |
| Dual engine during support window | Required by FR-021; Derby classpath scoped to migration/upgrade |
| Custom Hibernate dialect | Only if H2 dialect gaps appear (mirror `PSCommunityDerbyDialect` pattern) |

## Phase outputs

| Phase | Artifact | Status |
|-------|----------|--------|
| 0 | [research.md](research.md) | Complete |
| 1 | [data-model.md](data-model.md), [contracts/](contracts/), [quickstart.md](quickstart.md) | Complete |
| 2 | [tasks.md](tasks.md) | Complete (`/speckit-tasks` + analyze remediations 2026-07-24) — **T001–T105**, QC-mapped; C1–C5 fixed |

## Risks and mitigations (engineering)

| Risk | Mitigation |
|------|------------|
| H2 locking ≠ product expectations | WP0 product API bake-off; HSQL fallback; QC-006 |
| Large CMS schema fidelity | Table-set equality + SC-002 + QC-003/004/005/028 |
| Partial cutover | Multi-file cutover + rollback; QC-008/009 |
| DTS ≠ TableFactory | Explicit Hibernate/Liquibase path; QC-012 |
| Networked install forgotten | R11 redesign; QC-014 |
| Missed sqlDerby branches | Inventory QC-001 + QC-026 |
| Path non-portability | NIO `Path`/`Files`; QC-017 |
| Windows service drift | QC-024 |
| Dependency weight | Single H2 jar; Derby migration-scoped |

## Next command

```text
/speckit-tasks
```

Then implement by work package (prefer separate PRs per WP or story checkpoint per constitution Development Workflow). Each task must cite QC IDs from [checklists/quality-gates.md](checklists/quality-gates.md).

**Pre-tasks gate**: Do not freeze task estimates until (1) QC-001 inventory script/checklist exists or is first task, and (2) WP0 bake-off report template is accepted.
