# Data Model: Default Embedded Repository Migration (#548)

**Branch**: `548-derby-embedded-migration`  
**Date**: 2026-07-23  
**Spec**: [spec.md](spec.md)

This feature does **not** introduce a new domain content model. It introduces **configuration, migration control, and operational artifacts** around the product-managed repository. CMS/DTS business tables remain product-owned schemas managed by TableFactory.

## 1. Product-managed repository (logical)

|    Attribute     |                                                 Description                                                 |
|------------------|-------------------------------------------------------------------------------------------------------------|
| **Identity**     | One primary repository per CMS instance; one or more product-managed datasources per DTS service deployment |
| **Backend kind** | `DERBY` (legacy) \| `H2` (target default*) \| `MYSQL` \| `MSSQL` \| (others already supported)              |
| **Connection**   | Driver name, driver class, server/URL fragment, schema, credentials, DS config name                         |
| **Live store**   | Exactly one backend is live after successful start (FR-013)                                                 |

\*If bake-off selects HSQLDB, substitute HSQL backend kind throughout.

### CMS configuration entity (`rxrepository.properties` and equivalents)

Canonical keys (existing product contract — preserve key names where possible):

|         Field          |          Legacy Derby example          |          Target default (illustrative)           |
|------------------------|----------------------------------------|--------------------------------------------------|
| `DB_BACKEND`           | `DERBY`                                | `H2`                                             |
| `DB_DRIVER_NAME`       | `derby`                                | `h2`                                             |
| `DB_DRIVER_CLASS_NAME` | `org.apache.derby.jdbc.EmbeddedDriver` | `org.h2.Driver`                                  |
| `DB_SERVER`            | path/name + `;create=true`             | file path form for H2                            |
| `DB_SCHEMA`            | schema name                            | schema/user as required by H2 mapping            |
| `UID` / `PWD`          | credentials                            | credentials (may be empty for embedded defaults) |
| `DSCONFIG_NAME`        | Jetty/DS name                          | unchanged role                                   |

**Validation**: Migrator rewrites these only after successful data pump + validation; external MYSQL/MSSQL configs are never rewritten by embedded migration (FR-009).

### DTS configuration entity

Per-service datasource properties / Spring beans (existing files such as `perc-datasources.properties`). Same conceptual fields: driver, URL, credentials. Migration is **per service config**, enabling mixed estates.

## 2. Migration run (control entity)

Logical record of an upgrade migration attempt (may be file-based marker, log structured lines, and/or properties — implementation choice; contract is observability FR-017).

|           Field            |    Type    |                                            Rules                                            |
|----------------------------|------------|---------------------------------------------------------------------------------------------|
| `component`                | enum       | `CMS` \| `DTS_<service>`                                                                    |
| `sourceBackend`            | string     | Must be Derby for migration path                                                            |
| `targetBackend`            | string     | New default engine                                                                          |
| `startedAt` / `finishedAt` | timestamp  | Required on complete                                                                        |
| `outcome`                  | enum       | `SUCCESS` \| `FAILED` \| `SKIPPED_NON_DERBY` \| `BLOCKED_BACKUP_GATE` \| `ALREADY_MIGRATED` |
| `backupGate`               | enum       | `PRODUCT_BACKUP` \| `EXTERNAL_CONFIRM` \| `NOT_SATISFIED`                                   |
| `failureReason`            | string     | Required if `FAILED` / `BLOCKED_*`; no secrets                                              |
| `sourcePath`               | path       | Derby data location retained                                                                |
| `targetPath`               | path       | New repository location                                                                     |
| `validationSummary`        | string/map | Row counts / probe results                                                                  |
| `reportPath`               | path       | Durable migration report file under install tree (FR-017; required)                         |

### State transitions

```text
DETECTED_DERBY
    → BLOCKED_BACKUP_GATE (terminal until re-run with gate)
    → BACKUP_GATE_OK
         → MIGRATING
              → FAILED (source intact; no live cutover)
              → VALIDATING
                   → FAILED
                   → CUTOVER
                        → SUCCESS (live = target; source retained)
DETECTED_NON_DERBY → SKIPPED_NON_DERBY
ALREADY_MIGRATED → SKIPPED / idempotent no-op
```

**Invariants**

- No transition to SUCCESS without VALIDATING pass.
- FAILED never points live config at partial target.
- SUCCESS never dual-writes to Derby.

## 3. Backup artifact set

|       Field       |                                 Rules                                  |
|-------------------|------------------------------------------------------------------------|
| `kind`            | `PRE_MIGRATION` \| `STEADY_STATE`                                      |
| `mode`            | `OFFLINE` only for supported full backup (FR-020)                      |
| `includes`        | Repository data files + documented companion config needed for restore |
| `createdAt`       | Required                                                               |
| `instanceStopped` | Must be true for supported procedure                                   |

**Pre-migration product backup** (FR-018 path a): produces a `PRE_MIGRATION` artifact before pump starts.

## 4. Derby legacy residue

|    Field    |                         Rules                          |
|-------------|--------------------------------------------------------|
| `dataFiles` | Retained after SUCCESS until operator cleanup (FR-019) |
| `role`      | Not live store; support/forensics only                 |
| `cleanup`   | Operator-initiated only; optional product step allowed |

## 5. TableFactory datatype map (schema portability)

Logical backend map entry (extends `PSJdbcDataTypeMaps.xml` pattern):

|      Concept      |                    Derby (existing)                     |                                        H2 (new)                                         |
|-------------------|---------------------------------------------------------|-----------------------------------------------------------------------------------------|
| Map `for`         | `DERBY`                                                 | `H2`                                                                                    |
| `driver`          | `derby`                                                 | `h2`                                                                                    |
| JDBC→native types | BIT→CHAR FOR BIT DATA, CLOB, BLOB, DATE→TIMESTAMP, etc. | H2-native equivalents preserving product semantics (BOOLEAN/BIT, CLOB, BLOB, TIMESTAMP) |

**Rule**: Product schema definitions remain backend-agnostic at authoring layer; TableFactory emits correct DDL per map.

## 6. Relationships

```text
ProductInstance 1──1 live ProductManagedRepository
ProductInstance 0──* MigrationRun (history)
ProductManagedRepository 0──1 DerbyLegacyResidue (after SUCCESS from Derby)
ProductManagedRepository 0──* BackupArtifactSet
MigrationRun ──requires── BackupGateSatisfaction (for Derby source)
```

## 7. Validation rules (product data fidelity)

Post-migration probes (minimum for CMS SC-002):

- **Table-set equality**: product user tables present on target (no silent missing tables)
- Auth principals / roles usable for login
- Folder tree reachable
- Content items count match fixture; **content ids preserved**
- **NEXTNUMBER** (or equivalent) ≥ max(id)+1 for key sequences
- Relationships load for sample items
- Workflow state present for sample items
- Boolean/flag columns match source semantics
- Large CLOB body open/save
- Site/publish config present if in fixture

DTS: per-service record counts, sample read APIs, and expected indexes after Liquibase/Hibernate on new engine (SC-003).

## 8. Scale assumptions

|         Item          |                            Spec / plan                            |
|-----------------------|-------------------------------------------------------------------|
| CMS multiuser floor   | ≥10 concurrent editors                                            |
| CMS migration fixture | ≥1,000 content items or equivalent agreed fixture                 |
| Positioning           | Small-to-mid default install; large multi-server → external RDBMS |

