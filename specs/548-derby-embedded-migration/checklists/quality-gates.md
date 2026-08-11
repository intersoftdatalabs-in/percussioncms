# Quality Gates Checklist: #548 Default Embedded Repository Migration

**Purpose**: Hard/soft verification gates before GA and per implementation PR. Sourced from Sherlock design review (`sherlock-design-review.md`) plus spec success criteria.  
**Created**: 2026-07-23  
**Updated**: 2026-08-11 (main reality: stack #1494–#1499 + US6 #1504; residual #3065 QC freeze; human QA #2332/#2333)  
**Feature**: [spec.md](../spec.md) | [plan.md](../plan.md) | [quickstart.md](../quickstart.md)  
**Review**: [sherlock-design-review.md](sherlock-design-review.md)

**Legend**: `- [ ]` open · `- [x]` done · Gate **hard** blocks GA (or blocks stated WP) · **soft** should not slip past release without explicit waiver in PR body.

**Evidence PRs**: #1494 foundation · #1495 US1 · #1496 US2 · #1497 US3 · #1498 US5 · #1499 US4 · #1504 US6/Phase 9 docs · residual QC freeze #3065.

---

## Platform inventory & install surface

- [x] **QC-001** Full Derby surface inventory freeze (hard)
  - Maps to: FR-010, Sherlock Issue 3
  - Verify: Scripted repo search → dispositioned checklist; **zero unknown** rows
  - **Evidence**: `derby-surface-inventory.md` regenerated **2026-08-11** on `main` — **0 unknown** (`python scripts/derby-surface-inventory.py --fail-on-unknown`); residual re-freeze under #3065
- [x] **QC-014** Installer path without Derby NetworkServerControl (or approved H2 multi-process) (hard)
  - Maps to: FR-001, Sherlock Issue 2
  - Verify: Default install path does not require listen port **1527**
  - **Evidence**: US1 install redesign + packaging unit tests (`DefaultEmbeddedH2PackagingTest`); full OS install/login smoke still **T038 open** → human QA **#2332**
- [x] **QC-026** Every install `sqlDerby` that still runs post-cutover has `sqlH2` (or generic) (hard)
  - Maps to: FR-010, Issue 3
  - **Evidence**: US1 sqlH2 / install resource work (#1495)
- [x] **QC-030** Hibernate dialect registration for `h2` in all server-beans / dialect maps (hard)
  - Maps to: FR-001
  - **Evidence**: Foundation T017 + `PSH2DialectSmokeTest` (#1494)
- [x] **QC-013** New install without live Derby dependency (hard)
  - Maps to: SC-009, FR-001/002
  - **Evidence**: [packaging-audit.md](./packaging-audit.md) — H2 defaults; Derby migration-scoped

---

## Schema, types, fidelity

- [x] **QC-002** H2 TableFactory map parity suite (hard) — **CMS**
  - **Evidence**: TableFactory H2 map + create/load tests (#1494)
- [x] **QC-012** DTS per-service migrate + Liquibase H2 indexes (hard) — **DTS**
  - **Evidence**: DTS migrator + Liquibase h2 changesets + packaging tests (#1495/#1496)
- [x] **QC-003** Identity-preserving pump + NEXTNUMBER probe (hard)
  - **Evidence**: `PSTableFactoryMigrationTransferTest`, migration IT (#1496)
- [x] **QC-004** Boolean/BIT fidelity probes (hard)
  - **Evidence**: Transfer IT + CHAR flag fixtures (#1496)
- [x] **QC-005** Content-type CLOB open/save on H2 (hard)
  - **Evidence**: LOB generalization + harness CLOB concurrent edit (#1494/#1499)
- [x] **QC-028** Table-set completeness (hard)
  - **Evidence**: `PSMigrationValidator` table-set checks (#1496)
- [x] **QC-029** Large fixture scale ≥1000 content items (hard)
  - **Evidence**: `PSMigrationScaleFixtureTest` + [migration-timing.md](./migration-timing.md) (#1496/#1498)

---

## Multiuser, locks, concurrency

- [x] **QC-006** Pessimistic lock / checkout parity at ≥10 editors (hard)
  - **Evidence**: `PSH2MultiuserLockHarnessTest` + `PSH2DtsConcurrentWriteSmokeTest` (#1499); bake-off report
- [x] **QC-019** Concurrent migration lock (hard)
  - **Evidence**: `PSMigratorLockTest` (#1496)
- [x] **QC-025** Bake-off evidence artifact before engine lock (hard)
  - **Evidence**: [bake-off-report.md](./bake-off-report.md) **CLOSED** — H2 locked

---

## Migration safety & cutover

- [x] **QC-007** Backup gate matrix (hard)
  - **Evidence**: `PSRepositoryBackupGateTest` (#1496)
- [x] **QC-008** Failure injection 10/10 with source startability (hard)
  - **Evidence**: `PSMigrationFailureInjectionTest` (#1496)
- [x] **QC-009** Cutover multi-file consistency (hard)
  - **Evidence**: `PSConfigCutoverTest` (#1496)
- [x] **QC-010** Idempotent re-upgrade / already migrated (hard)
  - **Evidence**: `PSEmbeddedRepositoryMigratorTest` outcomes (#1496)
- [x] **QC-016** Post-success Derby residue retention + cleanup (hard)
  - **Evidence**: SUCCESS never deletes residue; operator docs FR-019 (#1496/#1498)
- [x] **QC-021** Disk precheck before pump (hard)
  - **Evidence**: `PSRepositoryOfflineBackup.hasSufficientDiskSpace` + failure injection (#1496)

---

## External backends & mixed estate

- [x] **QC-011** External MySQL/MSSQL non-interference (hard)
  - **Evidence**: `PSEmbeddedRepositoryExternalDbTest` (#1497)
- [x] **QC-020** Mixed estate CMS Derby / DTS MySQL and reverse (hard)
  - **Evidence**: External + DTS isolation tests (#1497)

---

## Ops, packaging, platform hygiene

- [x] **QC-015** Offline backup/restore dry-run ≤60 minutes (hard)
  - **Evidence**: [backup-restore-dry-run.md](./backup-restore-dry-run.md) + operator docs (#1498)
- [x] **QC-017** Cross-platform path I/O (hard)
  - **Evidence**: Migrator/backup use `Path`/`Files`; unit tests include separator scenarios (#1496/#1498)
- [x] **QC-018** Pre-PR Maven standalone clean install per touched module (hard per PR)
  - **Evidence**: Satisfied per implementation PR body for #1494–#1499; **docs-only** PRs N/A
- [x] **QC-022** Secrets redaction in migration logs (hard)
  - **Evidence**: [security-pass.md](./security-pass.md); `PSMigrationSecretsRedactor` tests
- [x] **QC-023** Package install on H2 default (soft → hard if driver-validated)
  - **Evidence**: Soft disposition in [packaging-audit.md](./packaging-audit.md) — archive stamps. **Hard** representative package install on H2: human QA **#2333** **CLOSED** 2026-08-11 (`QA: Passed` by @vijaya-boddipudi).
- [x] **QC-024** Windows service JVM options after upgrade (hard for DTS GA)
  - **Evidence**: [windows-service-h2-notes.md](./windows-service-h2-notes.md) + DTS/Jetty H2 defaults (#1495)
- [x] **QC-027** Migration support window checklist FR-021 (soft until near window end; hard before Derby removal)
  - **Evidence**: [fr-021-migration-window.md](../../../docs/ai-generated/tasks/548-derby-embedded-migration/fr-021-migration-window.md) + #548 FR-021 comment (GA line name still open until release locks)

---

## Still open for product GA (not QC text freeze)

|                Item                |      Task      |                                 Notes / tracker                                 |
|------------------------------------|----------------|---------------------------------------------------------------------------------|
| Full OS install login/health smoke | T038 / SC-001  | Packaging unit PASS; full multi-OS install/login → human QA **#2332** (open)    |
| QC-023 hard package install IT     | T096 follow-up | **Closed** — #2333 passed 2026-08-11                                            |
| FR-021 GA line name                | T093           | Checklist open until GA version locks (policy + deprecation template ready)     |
| Issue #548 close                   | T104           | Leave open until T038 (#2332) done or product-owner waiver on #548              |

Agent-safe US6 residual inventory/QC freeze: **#3065** (docs only; does not replace #2332).

---

## Sherlock design issues (status after implementation)

| Issue |  Severity   |                          Theme                          |                   Status                   |
|-------|-------------|---------------------------------------------------------|--------------------------------------------|
| 1     | critical    | DTS ≠ TableFactory (Hibernate/Liquibase)                | **addressed** — DTS path + Liquibase H2    |
| 2     | critical    | Networked Derby installer (1527 / NetworkServerControl) | **addressed** — default path in-process H2 |
| 3     | critical    | sqlDerby / isDerby surface inventory                    | **addressed** — QC-001 freeze              |
| 4     | major       | CLOB/BLOB product branch                                | **addressed**                              |
| 5     | major       | Boolean/BIT conversion                                  | **addressed**                              |
| 6     | major       | Product full-repo backup design                         | **addressed**                              |
| 7     | major       | FK order / table inventory                              | **addressed**                              |
| 8     | major       | CMS+DTS sequencing / mixed estate                       | **addressed**                              |
| 9     | major       | Liquibase dbms=derby                                    | **addressed**                              |
| 10    | major       | Concurrent migration lock                               | **addressed**                              |
| 11    | major       | Loss of DRDA network access                             | **addressed** — release notes (US6)        |
| 12    | minor/major | .ppkg derby stamps                                      | **soft closed** — packaging-audit          |
| 13    | major       | isDerby() API / sanitization                            | **addressed**                              |
| 15    | major       | H2 URL/schema/case folding                              | **addressed** — repository-config          |
| 16    | major       | Identity / NEXTNUMBER                                   | **addressed**                              |
| 17    | major       | Multi-file cutover                                      | **addressed**                              |
| 18    | major       | Windows service scripts                                 | **addressed** — notes + defaults           |

Track residual GA items in the table above and GitHub #548.

---

## Notes

- Do **not** mark #548 complete until all **hard** gates above are checked or explicitly waived with product owner sign-off in the issue.
- Engineering stack + US6 docs are on **main**. Remaining product-GA blocker tracked as human QA: **T038 full OS install smoke (#2332)**. QC-023 hard is closed (#2333).

