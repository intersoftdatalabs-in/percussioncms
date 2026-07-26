# Quickstart validation: #548 Default Embedded Repository Migration

**Purpose**: Runnable validation scenarios that prove the feature against [spec.md](spec.md) success criteria.  
**Not**: full implementation code (see `/speckit-tasks` later).

## Prerequisites

- Branch: `548-derby-embedded-migration` (or implementation PR branch)
- JDK 21 via repo `./mvn-env.sh` / `mvn-env.bat`
- Ability to run standalone module `clean install` for touched modules
- Optional: local CMS/DTS install trees for end-to-end upgrade dry-runs

## Scenario matrix

|  #  |                      Scenario                      |          Spec refs          |                                               Expected                                                |
|-----|----------------------------------------------------|-----------------------------|-------------------------------------------------------------------------------------------------------|
| Q1  | New CMS default install                            | US1, SC-001, FR-001, FR-014 | Starts on new engine; login works; **not** Derby live; **Windows + Linux + macOS** matrix (or waived) |
| Q2  | New DTS default install                            | US1, SC-001, FR-002         | Services healthy on new engine; same OS matrix as applicable                                          |
| Q3  | CMS Derby → migrate happy path                     | US2, SC-002, FR-005/007/018 | Gate → migrate → probes pass; live = new engine                                                       |
| Q4  | DTS Derby → migrate happy path                     | US2, SC-003, FR-006         | Service data preserved                                                                                |
| Q5  | Backup gate blocked                                | FR-018, SC-010              | No migration; clear block; Derby intact                                                               |
| Q6  | Migration failure injection                        | FR-008, SC-004              | No cutover; Derby startable/restorable                                                                |
| Q7  | External MySQL/MSSQL upgrade                       | US3, SC-006, FR-009         | No migrator rewrite                                                                                   |
| Q8  | ≥10 concurrent CMS editors + DTS concurrent writes | US4, SC-005, FR-003/004     | 0 lost updates, 0 corruption (CMS harness + DTS multi-writer smoke)                                   |
| Q9  | Offline backup/restore                             | US5, SC-007, FR-020         | Stop → backup → restore → verify                                                                      |
| Q10 | Idempotent re-upgrade                              | Edge: already migrated      | Skip; no corruption                                                                                   |
| Q11 | Post-success Derby files retained                  | FR-019, SC-011              | Files remain until cleanup action                                                                     |
| Q12 | Bake-off lock                                      | research R1                 | H2 or HSQL locked with evidence                                                                       |

## Module build verification (pre-PR gate)

For each changed Maven module (standalone):

```bash
cd <module>
# depth-adjusted path to repo root:
../mvn-env.sh clean install          # or ../../mvn-env.sh etc.
```

Record BUILD SUCCESS and test counts in PR body (AGENTS hard gate).

Suggested early modules (order depends on producer/consumer):

1. `modules/utils` (JDBC constants)
2. `modules/TableFactory` (datatype map + load tests)
3. `system` (dialect, session factory, migrator, function defs)
4. `modules/perc-jetty` (DS defaults packaging)
5. DTS leaf modules + `delivery-tier-distribution`
6. Installer / `perc-distribution-tree` as applicable

## Suggested automated test packages (implementation targets)

|      Area       |                                           Example test focus                                           |
|-----------------|--------------------------------------------------------------------------------------------------------|
| Backend mapping | `PSJdbcUtils` new engine constants / URL map                                                           |
| Dialect         | Lock string / FOR UPDATE smoke for new dialect                                                         |
| TableFactory    | Create schema on new engine; load sample tables                                                        |
| Migrator unit   | State machine: gate, fail, success, skip                                                               |
| Migrator IT     | Mini Derby source → target; count asserts                                                              |
| Gate            | Missing confirmation blocks                                                                            |
| Concurrency     | Editor simulation harness ≥10 threads                                                                  |
| Packaging       | Distribution contains new engine JAR; Derby present only as migration dependency during support window |

## Manual dry-run (support doc validation)

1. Install prior-release-like tree with Derby fixture (or migrate from current Derby default install).
2. Run upgrade with product backup path.
3. Confirm logs per [contracts/migration-observability.md](contracts/migration-observability.md).
4. Execute offline backup/restore per [contracts/backup-restore.md](contracts/backup-restore.md) within 60 minutes for small instance (SC-007).
5. Confirm Derby residue still on disk; run documented cleanup; confirm residue gone.

## Bake-off procedure (before engine lock)

1. Run scenarios Q3, Q8, and locking-focused unit/IT on **H2**.
2. If fail: repeat on **HSQLDB**.
3. Record winner in PR/issue comment; update constants/maps only if HSQL wins.
4. Do not ship dual “pick your embedded engine” product surface for 8.2 without a separate spec.

## Quality gates (Sherlock)

All hard gates in [checklists/quality-gates.md](checklists/quality-gates.md) (QC-001–QC-030) must pass or be explicitly waived before #548 close. Map scenarios:

|          Scenario          |                Primary QCs                 |
|----------------------------|--------------------------------------------|
| Q1–Q2 new install          | QC-013, QC-014, QC-026, QC-030             |
| Q3–Q4 migrate              | QC-002–005, QC-007–009, QC-012, QC-028–029 |
| Q5–Q6 gate/fail            | QC-007, QC-008, QC-021                     |
| Q7 external                | QC-011, QC-020                             |
| Q8 multiuser               | QC-006, QC-025                             |
| Q9 backup/restore          | QC-015, QC-022                             |
| Q10–Q11 idempotent/residue | QC-010, QC-016                             |
| Q12 bake-off               | QC-025                                     |

## Exit criteria for “feature done” (#548)

- All Q1–Q11 pass (Q12 recorded).
- All **hard** QC-001–QC-030 checked (or product-owner waiver on issue).
- Docs published for migration + offline backup/restore + cleanup + DRDA/network-access change.
- Release notes cover affected/unaffected audiences and FR-021 window.
- Pre-PR Maven clean install green on all changed modules (QC-018).
- Issue #548 checkboxes complete; issue closed only then.

