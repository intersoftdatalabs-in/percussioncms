# Quality Gates Checklist: #548 Default Embedded Repository Migration

**Purpose**: Hard/soft verification gates before GA and per implementation PR. Sourced from Sherlock design review (`sherlock-design-review.md`) plus spec success criteria.  
**Created**: 2026-07-23  
**Feature**: [spec.md](../spec.md) | [plan.md](../plan.md) | [quickstart.md](../quickstart.md)  
**Review**: [sherlock-design-review.md](sherlock-design-review.md)

**Legend**: `- [ ]` open · `- [x]` done · Gate **hard** blocks GA (or blocks stated WP) · **soft** should not slip past release without explicit waiver in PR body.

---

## Platform inventory & install surface

- [ ] **QC-001** Full Derby surface inventory freeze (hard)
  - Maps to: FR-010, Sherlock Issue 3
  - Verify: Scripted repo search (`derby` / `DERBY` / `sqlDerby` / `NetworkServer` / `isDerby` / `derby.system` / `dbms="derby"`) → dispositioned checklist checked into feature dir; **zero unknown** rows at GA
- [ ] **QC-014** Installer path without Derby NetworkServerControl (or approved H2 multi-process) (hard)
  - Maps to: FR-001, Sherlock Issue 2
  - Verify: Clean install Windows + Linux completes schema/SQL steps; no dependency on listen port **1527** unless H2 TCP is an explicit designed substitute
- [ ] **QC-026** Every install `sqlDerby` that still runs post-cutover has `sqlH2` (or generic) (hard)
  - Maps to: FR-010, Issue 3
  - Verify: Install/upgrade SQL path on H2 executes views/DDL successfully
- [ ] **QC-030** Hibernate dialect registration for `h2` in all server-beans / dialect maps (hard)
  - Maps to: FR-001
  - Verify: Grep dialect maps; runtime start without dialect exception
- [ ] **QC-013** New install without live Derby dependency (hard)
  - Maps to: SC-009, FR-001/002
  - Verify: Packaging audit — H2 on runtime path; Derby migration-scoped only

---

## Schema, types, fidelity

- [ ] **QC-002** H2 TableFactory map parity suite (hard) — **CMS**
  - Maps to: FR-005/007, constitution IV
  - Verify: Create/load/alter sample product tables on H2 including CLOB/BLOB/BIT/DATE semantics; round-trip vs Derby
- [ ] **QC-012** DTS per-service migrate + Liquibase H2 indexes (hard) — **DTS**
  - Maps to: SC-003, FR-002/006, Issues 1 & 9
  - Verify: Each default service Derby→migrate→health + sample reads; indexes exist where product expects; no reliance on TableFactory-only path for DTS
- [ ] **QC-003** Identity-preserving pump + NEXTNUMBER probe (hard)
  - Maps to: SC-002, FR-007, Issue 16
  - Verify: Known content ids preserved; NEXTNUMBER ≥ max(id)+1 for key sequences
- [ ] **QC-004** Boolean/BIT fidelity probes (hard)
  - Maps to: FR-007, Issue 5
  - Verify: Known flag columns; no T/F vs BOOLEAN corruption; login/ACL
- [ ] **QC-005** Content-type CLOB open/save on H2 (hard)
  - Maps to: SC-002, Issue 4
  - Verify: Large body content edit/save after migrate and on new install; `isDerbyDatabase()` LOB branch generalized
- [ ] **QC-028** Table-set completeness (hard)
  - Maps to: FR-007, Issue 7
  - Verify: User table name set source vs target; zero missing product tables
- [ ] **QC-029** Large fixture scale ≥1000 content items (hard)
  - Maps to: SC-002
  - Verify: Automated fixture; wall-clock recorded

---

## Multiuser, locks, concurrency

- [ ] **QC-006** Pessimistic lock / checkout parity at ≥10 editors (hard)
  - Maps to: SC-005, FR-003/004, US4
  - Verify: Harness against **product** checkout/workflow APIs (not raw JDBC only); 0 lost updates, 0 corruption; **plus** DTS concurrent write smoke at default levels (tasks T071)
- [ ] **QC-019** Concurrent migration lock (hard)
  - Maps to: Edge concurrent upgrades, Issue 10
  - Verify: Two migrator processes; one proceeds; other blocked with clear message
- [ ] **QC-025** Bake-off evidence artifact before engine lock (hard)
  - Maps to: research R1, Q12
  - Verify: Written report on #548 (engine, versions, OS, editor count, lock test classes, pass/fail); WP1+ blocked until locked

---

## Migration safety & cutover

- [ ] **QC-007** Backup gate matrix (hard)
  - Maps to: SC-010, FR-018
  - Verify: Neither → block; product backup → migrate; external confirm → migrate
- [ ] **QC-008** Failure injection 10/10 with source startability (hard)
  - Maps to: SC-004, FR-008
  - Verify: Disk full, kill mid-copy, corrupt source, validation fail; config remains Derby; source opens
- [ ] **QC-009** Cutover multi-file consistency (hard)
  - Maps to: FR-013, Issue 17
  - Verify: After SUCCESS, `rxrepository` + Jetty DS + dialect resolution all new engine; mid-cutover crash leaves non-live partial target
- [ ] **QC-010** Idempotent re-upgrade / already migrated (hard)
  - Maps to: Edge already migrated
  - Verify: Second upgrade skips; no data churn
- [ ] **QC-016** Post-success Derby residue retention + cleanup (hard)
  - Maps to: SC-011, FR-019
  - Verify: Files remain after SUCCESS; cleanup removes residue only; live store untouched
- [ ] **QC-021** Disk precheck before pump (hard)
  - Maps to: Edge disk full, FR-008
  - Verify: Simulated low space fails before source mutation/cutover

---

## External backends & mixed estate

- [ ] **QC-011** External MySQL/MSSQL non-interference (hard)
  - Maps to: SC-006, FR-009, US3
  - Verify: Migrator skip; connection keys byte-stable
- [ ] **QC-020** Mixed estate CMS Derby / DTS MySQL and reverse (hard)
  - Maps to: Edge mixed estate, FR-017
  - Verify: Only Derby component migrates; logs show skip on external

---

## Ops, packaging, platform hygiene

- [ ] **QC-015** Offline backup/restore dry-run ≤60 minutes (hard)
  - Maps to: SC-007, FR-020/011, US5
  - Verify: Support timed walkthrough from docs alone; restore verifies login
- [ ] **QC-017** Cross-platform path I/O (hard)
  - Maps to: AGENTS Cross-Platform; FR-011
  - Verify: Path/Files only in new migrator code; no hardcoded filesystem `/`; Windows install/script validation
- [ ] **QC-018** Pre-PR Maven standalone clean install per touched module (hard per PR)
  - Maps to: AGENTS pre-PR gate
  - Verify: PR body lists `cd <module> && …/mvn-env.sh clean install`, BUILD SUCCESS, test counts, no new warnings
- [ ] **QC-022** Secrets redaction in migration logs (hard)
  - Maps to: constitution VI
  - Verify: Unit tests — PWD / JDBC userinfo never in formatted log lines
- [ ] **QC-023** Package install on H2 default (soft → hard if driver-validated)
  - Maps to: `.ppkg` audit, Issue 12
  - Verify: Representative packages install; no Derby driver requirement failures
- [ ] **QC-024** Windows service JVM options after upgrade (hard for DTS GA)
  - Maps to: FR-011, Issue 18
  - Verify: Service start uses new embedded home; not only `derby.system.home`
- [ ] **QC-027** Migration support window checklist FR-021 (soft until near window end; hard before Derby removal)
  - Maps to: SC-012, FR-021
  - Verify: Tracking items for GA line, next line, deprecation notice

---

## Sherlock design issues (must be closed in design/plan before task freeze)

| Issue |  Severity   |                          Theme                          |                  Status                  |
|-------|-------------|---------------------------------------------------------|------------------------------------------|
| 1     | critical    | DTS ≠ TableFactory (Hibernate/Liquibase)                | open → address in plan/research revision |
| 2     | critical    | Networked Derby installer (1527 / NetworkServerControl) | open → address in plan/research revision |
| 3     | critical    | sqlDerby / isDerby surface inventory                    | open → QC-001                            |
| 4     | major       | CLOB/BLOB product branch                                | open → QC-005                            |
| 5     | major       | Boolean/BIT conversion                                  | open → QC-004                            |
| 6     | major       | Product full-repo backup design                         | open → contracts + WP3                   |
| 7     | major       | FK order / table inventory                              | open → QC-028                            |
| 8     | major       | CMS+DTS sequencing / mixed estate                       | open → ops contract                      |
| 9     | major       | Liquibase dbms=derby                                    | open → QC-012                            |
| 10    | major       | Concurrent migration lock                               | open → QC-019                            |
| 11    | major       | Loss of DRDA network access                             | open → release notes decision            |
| 12    | minor/major | .ppkg derby stamps                                      | open → QC-023                            |
| 13    | major       | isDerby() API / sanitization                            | open → inventory                         |
| 15    | major       | H2 URL/schema/case folding                              | open → repository-config freeze          |
| 16    | major       | Identity / NEXTNUMBER                                   | open → QC-003                            |
| 17    | major       | Multi-file cutover                                      | open → QC-009                            |
| 18    | major       | Windows service scripts                                 | open → QC-024                            |

Track closure by updating Status in [sherlock-design-review.md](sherlock-design-review.md) when design text lands.

---

## Notes

- Do **not** mark #548 complete until all **hard** gates above are checked or explicitly waived with product owner sign-off in the issue.
- Implementers: map each WP in `tasks.md` (when created) to the QC IDs it must satisfy.

