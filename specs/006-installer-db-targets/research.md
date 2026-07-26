# Research: CLI Installer Database Targets for New Installs

**Feature**: `specs/006-installer-db-targets`  
**Branch**: `984-installer-db-targets`  
**Date**: 2026-07-15  
**Source issue**: [#949](https://github.com/intersoftdatalabs-in/percussioncms/issues/949)

## Current-state findings

### Existing partial implementation (evidence)

The distribution already contains a **partial** non-Derby new-install path that is incomplete relative to issue #949:

|         Layer         |                                       Location                                       |                                                                                                            Behavior today                                                                                                            |
|-----------------------|--------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Preinstall entry      | `modules/perc-distribution-tree/src/main/java/com/percussion/preinstall/Main.java`   | `resolveDbConfig()` accepts CLI `--db.*` / env file / env vars; maps **mysql** and **sqlserver** to `perc.db.*` system properties; defaults `db.type=derby`. **No Oracle.** **No `dbprops` / `rxrepository.properties` file input.** |
| Property pass-through | `Main.execJar(...)`                                                                  | Forwards `ResolvedDbConfig.systemProperties` as `-Dperc.db.*=...` on the ANT JVM command line.                                                                                                                                       |
| Fresh-install write   | `.../rxconfig/Installer/installRepository.xml` target `repository_properties`        | Guarded by `${do.install}` (true only for **new** installs). Writes `rxrepository.properties` for `mysql` and `sqlserver`; Derby only updates SSL keys. **No Oracle branch.**                                                        |
| Default ship file     | `.../rxconfig/Installer/rxrepository.properties`                                     | `DB_BACKEND=DERBY` with embedded driver settings.                                                                                                                                                                                    |
| Upgrade safety        | `install.xml` `do.install` / `do.upgrade`                                            | Upgrade path does **not** enter the fresh-install property rewrite for backend selection — aligns with FR-006.                                                                                                                       |
| Downstream consumers  | `modules/perc-ant` (`PSConfigureDatasource`, `PSMakeLasagna`, `PSExecSQLStmt`, etc.) | Read **install-root** `rxconfig/Installer/rxrepository.properties` after it is written.                                                                                                                                              |
| Backend constants     | `modules/utils/.../PSJdbcUtils.java`                                                 | Canonical backend labels: `DERBY`, `MYSQL`, `MSSQL`, `ORACLE`.                                                                                                                                                                       |
| JDBC packaging        | `specs/001-fix-jdbc-drivers`                                                         | Distribution ships MariaDB, Derby, MSSQL, jTDS, Oracle drivers under `jetty/base/lib/jdbc/`.                                                                                                                                         |
| Tests                 | `MainExtractExecutableTest` + JDBC assembly tests                                    | **No unit tests** for `resolveDbConfig` / `parseArgs` / property-file load.                                                                                                                                                          |
| DTS                   | `perc.db.dts.*` set in `Main` for mysql/sqlserver                                    | **No ANT consumer found** under installer resources that writes DTS datasources from these properties. Spec keeps full DTS contract out of scope.                                                                                    |

### Gap vs issue #949

Issue asks for: property file shaped like `rxrepository.properties` + `-Ddbprops=<path>` for **new** CLI installs targeting MySQL, SQL Server, **Oracle**.

Gaps:

1. **Input contract mismatch**: Code uses `db.type` / `db.host` / …; issue wants **`dbprops` file** in repository-properties format.
2. **Oracle missing** from both Java mapping and ANT write branches.
3. **Connectivity preflight** (FR-008) not present before repository schema steps.
4. **Documentation / samples** for integrators not shipped for the new contract.
5. **Test coverage** for resolution, validation, and upgrade non-touch of repository props.
6. **Error handling**: `main` catches `Exception` and prints a message but may still exit in ways that obscure fail-fast for bad DB config; needs explicit non-zero exit on resolve/validation failure.
7. **Driver class drift**: existing mysql path uses `com.mysql.cj.jdbc.Driver` while the product default packaged driver is **MariaDB** (`org.mariadb.jdbc.Driver` per JDBC packaging work). Research decision required (below).

### ANT property semantics (important)

In ANT, `<property name="perc.db.type" value="derby" />` sets the property **only if unset**. JVM `-Dperc.db.type=mysql` therefore wins over the XML default. This is the correct mechanism for preinstall → ANT handoff; do not replace with `ant -D` only or unconditional `propertyfile` defaults that clobber upgrades.

---

## Decisions

### D1 — Primary integrator contract: `dbprops` file

**Decision**: Support a filesystem path to a Java properties file in **`rxrepository.properties` key format** as the issue-mandated new-install input:

- System property: `-Ddbprops=<path>` (issue wording)
- CLI equivalent: `--dbprops=<path>` (parity with existing `--db.*` style in `parseArgs`)

**Rationale**: Matches issue #949 and existing table-factory / upgrade tooling mental model (`-dbprops` already used by `RxJdbcTableFactory` / import scripts). Integrators can reuse the same property names they already know.

**Alternatives considered**:

|                      Alternative                      |                                              Why not primary                                               |
|-------------------------------------------------------|------------------------------------------------------------------------------------------------------------|
| Only keep `--db.host` / `DB_TYPE` env style           | Does not satisfy issue #949 wording; harder for ops teams that already maintain repository property files. |
| Require interactive GUI                               | Out of scope; CLI/unattended is the pain point.                                                            |
| Post-install manual edit of `rxrepository.properties` | Status quo; fails SC-001.                                                                                  |

### D2 — Keep existing `db.*` CLI/env as secondary input

**Decision**: Retain and document the already-coded precedence for structured flags:

1. **`dbprops` file** (if path present) — highest for repository identity fields
2. CLI `--db.*` / env-style keys in CLI
3. Env file (`--db.config.env.file` / `DB_CONFIG_ENV_FILE` / `PERC_DB_CONFIG_ENV_FILE`)
4. Process environment
5. Defaults (Derby + SSL defaults)

When `dbprops` is present, map its keys into the same internal `ResolvedDbConfig` used today so ANT remains a single write path. Do **not** invent a second ANT write path that copies the file blindly without validation.

**Rationale**: Avoids throwing away working mysql/sqlserver mapping code; dual input is low cost once resolution is extracted and tested.

### D3 — Mapping from `rxrepository.properties` keys → internal model

**Decision**: Load with `java.util.Properties` and map:

|           File key           |                                                          Internal / `perc.db.*` role                                                          |
|------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------|
| `DB_BACKEND`                 | Determines `perc.db.type`: `DERBY`→`derby`, `MYSQL`→`mysql`, `MSSQL`→`sqlserver`, `ORACLE`→`oracle` (case-insensitive). Reject unknown.       |
| `DB_SERVER`                  | `perc.db.cms.server` (pass-through; **do not re-compose** host/port when using dbprops — file already holds the product’s server string form) |
| `DB_NAME`                    | `perc.db.cms.name`                                                                                                                            |
| `DB_SCHEMA`                  | `perc.db.cms.schema`                                                                                                                          |
| `DB_DRIVER_NAME`             | `perc.db.cms.driverName`                                                                                                                      |
| `DB_DRIVER_CLASS_NAME`       | `perc.db.cms.driverClass`                                                                                                                     |
| `UID`                        | `perc.db.user`                                                                                                                                |
| `PWD`                        | `perc.db.password`                                                                                                                            |
| `DSCONFIG_NAME`              | optional; default `PercussionData` if absent                                                                                                  |
| `DB_SSL_*` / `PWD_ENCRYPTED` | optional; SSL keys map to existing `perc.db.ssl.*`; encrypted password handling must follow existing `PSMakeLasagna` / encryptor rules        |

When using **structured** `--db.*` (not dbprops), continue **composing** `DB_SERVER` / JDBC URL from host+port+name+ssl as today.

**Rationale**: `DB_SERVER` format is backend-specific (`//host:port/db?...`, `//host:port;databaseName=...`, `@host:port:sid` / service forms for Oracle). Re-parsing free-form server strings is error-prone; dbprops is already in final product shape.

### D4 — Oracle support

**Decision**: Add `oracle` as a first-class `perc.db.type` alongside `derby` / `mysql` / `sqlserver`:

- Backend label: `ORACLE` (`PSJdbcUtils.ORACLE_DB_BACKEND`)
- Default driver name: `oracle:thin` (or value from file)
- Default driver class: `oracle.jdbc.OracleDriver` (or value from file; match bundled `ojdbc17`)
- ANT: mirror mysql/sqlserver `propertyfile` write block for `oracle`
- Structured `--db.*` composition for Oracle: build CMS `DB_SERVER` in the product’s historical thin form (e.g. `@host:port:serviceOrSid` or documented service-name form); ship sample properties for the recommended form

**Rationale**: Explicitly required by issue #949 and already a supported upgrade/runtime backend.

### D5 — MySQL vs MariaDB driver class for composed (non-dbprops) path

**Decision**: For structured `db.type=mysql` defaults, prefer **MariaDB** driver class/name consistent with shipped JDBC packaging (`org.mariadb.jdbc.Driver` / MariaDB connector), while still accepting `DB_BACKEND=MYSQL` in repository properties. Document that MySQL-compatible servers are the target and that integrators may override class/name via **dbprops**.

**Rationale**: Distribution ships MariaDB client (001-fix-jdbc-drivers). Hard-coding `com.mysql.cj.jdbc.Driver` causes FR-012-style failures when only MariaDB JAR is present. Preserve ability to set classic MySQL class via dbprops for environments that drop in Oracle MySQL drivers.

**Alternatives**: Keep `com.mysql.cj.jdbc.Driver` only — rejected unless both JARs ship; today MariaDB is the default packaged connector.

### D6 — Connectivity validation placement

**Decision**: Perform connectivity validation **after** repository properties are applied to the install root and JDBC driver directory is available, as an ANT step (or small `perc-ant` action) in the **new-install** chain only — not solely inside preinstall `Main` before extract (preinstall classpath does not reliably include distribution JDBC drivers).

Flow:

1. Preinstall resolves config + fails fast on missing/unreadable dbprops / incomplete fields (no network required).
2. ANT `repository_properties` writes effective `rxrepository.properties`.
3. New task e.g. `validateRepositoryConnection` loads those properties, loads driver from `jetty/base/lib/jdbc` (and/or system classpath), attempts `DriverManager.getConnection`, fails install with actionable message **without logging password**.
4. Existing table/schema install continues only if connect succeeds.

**Rationale**: Satisfies FR-007 (early static validation) + FR-008 (connectivity) with correct classloader/driver availability. Reuse patterns from `PSJdbcDbmsDef` / existing install SQL actions where practical without pulling large upgrade-only plugins into preinstall.

**Alternatives**:

|             Alternative              |               Why secondary                |
|--------------------------------------|--------------------------------------------|
| Validate only in Main before extract | Drivers often not on preinstall classpath. |
| Skip connect; only write props       | Fails FR-008; half-configured installs.    |

### D7 — Upgrade non-regression

**Decision**: No code path may rewrite `DB_BACKEND` / connection identity on upgrade. Keep all fresh-install writes inside `${do.install}` guards. Unit/integration checks assert upgrade fixtures keep existing backend.

**Rationale**: Issue note + FR-006 / SC-005.

### D8 — DTS scope

**Decision**: **CMS repository only** for acceptance of this feature. Continue emitting `perc.db.dts.*` only if already present for mysql/sqlserver **or** extend Oracle symmetrically if trivial; do **not** block this feature on a full DTS datasource write-through. Track residual DTS write as follow-on if product install always co-installs DTS against the same RDBMS.

**Rationale**: Spec Out of Scope; no ANT consumer found today.

### D9 — Extraction for testability

**Decision**: Extract pure functions from `Main` into a focused helper (e.g. `com.percussion.preinstall.DbInstallConfigResolver` or package-visible methods) so JUnit 5 can cover:

- parseArgs
- load/map dbprops
- backend normalization
- required-field validation
- precedence
- secret redaction helpers for log messages

Avoid large refactor of extract/zip/upgrade helpers in the same change set.

**Rationale**: Constitution III; Main is ~1000 lines and hard to unit-test as a monolith.

### D10 — Documentation artifacts

**Decision**:

- Sample files under distribution installer config, e.g.  
  `rxconfig/Installer/samples/rxrepository.mysql.properties`,  
  `...sqlserver.properties`, `...oracle.properties`  
  (or `docs/` + copy into distribution via assembly — prefer **shipping with installer tree** so offline installs work).
- Document `-Ddbprops` / `--dbprops` and secondary `--db.*` in `modules/perc-distribution-tree/README.md` (and install help text if one exists).
- Note upgrade vs new-install behavior.

---

## Best practices applied

- **Single write path** to effective repository config (ANT `propertyfile` on install-root file) — avoids dual sources of truth.
- **Fail-fast validation** before expensive schema work.
- **No secret logging** (Constitution VI).
- **JDK 21** / `./mvn-env.sh` for tests (Constitution VII).
- **Reuse** `PSJdbcUtils` backend constants and existing property key names (Constitution II).

## Open risks (accepted, mitigated in plan)

|                                   Risk                                    |                                                                                                                                                                          Mitigation                                                                                                                                                                          |
|---------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Password special characters in `-Dperc.db.password=...` on ProcessBuilder | Prefer writing a temp properties sidecar or env for secrets if quoting proves fragile; at minimum document and test characters that break shells. Research favors keeping `-D` for parity with current code but adding a test matrix for special chars; if failures found, switch password pass-through to ANT property file only (dbprops already has PWD). |
| MariaDB vs MySQL URL differences                                          | Document; allow full override via dbprops.                                                                                                                                                                                                                                                                                                                   |
| Oracle service name vs SID                                                | Document one recommended sample; accept full `DB_SERVER` from file.                                                                                                                                                                                                                                                                                          |
| Connect timeout hangs install                                             | Set short login timeout on validation connection.                                                                                                                                                                                                                                                                                                            |

## NEEDS CLARIFICATION resolution

All Technical Context unknowns resolved via codebase evidence and decisions D1–D10. No remaining blockers for Phase 1 design.
