# Quickstart Validation: CLI Installer Database Targets

**Feature**: `specs/006-installer-db-targets`  
**Date**: 2026-07-15  
**Contracts**: [contracts/](contracts/) · **Model**: [data-model.md](data-model.md)

This guide is a **validation runbook** for implementers and reviewers. It is not the full task list.

## Prerequisites

- Branch: `984-installer-db-targets` (or equivalent with this feature)
- JDK 21 via `./mvnw`
- Built modules: at minimum `modules/perc-distribution-tree` (and test dependencies)
- For live non-Derby scenarios: a reachable empty database and credentials
- For unit-only scenarios: no external DB required

## Scenario 1 — Unit: resolve default Derby

**Goal**: SC-002 / FR-005

```bash
./mvnw -pl modules/perc-distribution-tree -am test \
  -Dtest=DbInstallConfigResolverTest,Main*Test
```

**Expect**:

- No dbprops / no `--db.type` → `perc.db.type=derby` (or equivalent default)
- Required non-Derby fields not forced

## Scenario 2 — Unit: load dbprops MySQL and map keys

**Goal**: FR-001, FR-002, FR-003, FR-004

Provide a temp properties file in test resources with `DB_BACKEND=MYSQL` and required keys (see [contracts/installer-db-input.md](contracts/installer-db-input.md)).

**Expect**:

- Resolver returns `perc.db.type=mysql` and CMS fields matching file
- Missing file → clear exception
- Missing `UID`/`PWD`/`DB_SERVER` → validation error listing keys, not secret values

## Scenario 3 — Unit: Oracle and SQL Server backends

**Goal**: FR-004

**Expect**:

- `DB_BACKEND=ORACLE` → `oracle` / `ORACLE` mapping
- `DB_BACKEND=MSSQL` → `sqlserver` / `MSSQL` mapping
- Unknown backend → failure with allowed list

## Scenario 4 — Unit: precedence dbprops over CLI

**Goal**: research D2

**Expect**: When both `--dbprops` (MySQL file) and `--db.type=sqlserver` are present, repository identity follows **dbprops**.

## Scenario 5 — Unit / action: connect validation failure

**Goal**: FR-008, SC-003

Mock or use invalid host with short timeout.

**Expect**: Validation reports failure without printing password; install chain would abort (assert task/action result).

## Scenario 6 — New install Derby (integration / manual)

**Goal**: SC-002 regression

```bash
# After building distribution artifact per modules/perc-distribution-tree/README.md
java -jar <distribution-installer.jar> /tmp/perc-install-derby
# Inspect:
grep -E '^(DB_BACKEND|DB_DRIVER)' /tmp/perc-install-derby/rxconfig/Installer/rxrepository.properties
```

**Expect**: `DB_BACKEND=DERBY` (or product default labels); install completes.

## Scenario 7 — New install with dbprops (manual / CI with MySQL)

**Goal**: SC-001

1. Create `mysql.properties` from shipped sample; set host/user/password for a pre-created empty DB.
2. Run:

```bash
java -Ddbprops=/path/to/mysql.properties -jar <distribution-installer.jar> /tmp/perc-install-mysql
```

3. Inspect effective properties and confirm install success.
4. Confirm no password appears in console capture.

**Expect**: Effective file shows `MYSQL` (or equivalent) and supplied server/user; no manual post-edit needed.

## Scenario 8 — Upgrade non-regression (fixture)

**Goal**: SC-005 / FR-006

1. Seed an install root fixture with `DB_BACKEND=MSSQL` (or MYSQL) in `rxconfig/Installer/rxrepository.properties` and version markers that force **upgrade** mode.
2. Run installer **without** dbprops against that root.
3. Re-read `DB_BACKEND`.

**Expect**: Backend unchanged; not rewritten to Derby.

## Scenario 9 — Documentation check

**Goal**: SC-006 / FR-010

```bash
# Samples exist in distribution tree sources:
ls modules/perc-distribution-tree/src/main/resources/distribution/rxconfig/Installer/samples/
# README documents -Ddbprops and backends:
rg -n "dbprops|DB_BACKEND|Oracle|SQL Server" modules/perc-distribution-tree/README.md
```

**Expect**: Samples for MySQL/MariaDB, SQL Server, Oracle; README describes new vs upgrade.

## Suggested automated gate for CI

Prefer unit tests (Scenarios 1–5) on every PR. Scenarios 6–8 may be nightly or Docker-based; at least one non-Derby **config write** path should be asserted without requiring a live DB (file content after ANT `repository_properties` with injected `-Dperc.db.*`).

## Related verification

JDBC drivers must be present in the distribution (`scripts/verify-jdbc-drivers.sh` from `001-fix-jdbc-drivers`). A non-Derby install will fail FR-012 cleanly if the needed driver JAR is absent — confirm message quality when testing Oracle/MySQL/MSSQL.
