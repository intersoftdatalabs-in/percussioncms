# Data Model: CLI Installer Database Targets

**Feature**: `specs/006-installer-db-targets`  
**Date**: 2026-07-15

This feature does not introduce a new persistent application schema. It models **install-time configuration entities** that flow from integrator input into the CMS repository configuration files already used at runtime.

## Entities

### 1. DatabaseTargetPropertiesFile (integrator input)

Integrator-supplied Java properties file (issue #949 / `-Ddbprops` / `--dbprops`).

|        Field (key)         |      Type      |     Required      |                                   Description                                    |
|----------------------------|----------------|-------------------|----------------------------------------------------------------------------------|
| `DB_BACKEND`               | enum string    | Yes (non-Derby)   | `DERBY` \| `MYSQL` \| `MSSQL` \| `ORACLE` (case-insensitive)                     |
| `DB_SERVER`                | string         | Yes (non-Derby)   | Backend-specific server / JDBC server identity string as used by CMS             |
| `DB_NAME`                  | string         | Backend-dependent | Database name (MySQL/MSSQL); often empty for Oracle                              |
| `DB_SCHEMA`                | string         | Backend-dependent | Schema/user schema (e.g. `dbo`, Oracle schema, empty/default for MySQL)          |
| `DB_DRIVER_NAME`           | string         | Yes (non-Derby)   | Logical driver name (e.g. `mysql`, `sqlserver`, `oracle:thin`, `derby`)          |
| `DB_DRIVER_CLASS_NAME`     | string         | Yes (non-Derby)   | JDBC driver class                                                                |
| `UID`                      | string         | Yes (non-Derby)   | Repository user                                                                  |
| `PWD`                      | string         | Yes (non-Derby)   | Repository password (may be plain; encryption handled by existing install steps) |
| `PWD_ENCRYPTED`            | `Y`/`N`        | No                | Default `N` for new supplied files                                               |
| `DSCONFIG_NAME`            | string         | No                | Default `PercussionData`                                                         |
| `DB_SSL_ENABLED`           | boolean string | No                | Default product SSL default (`true`)                                             |
| `DB_SSL_VERIFY`            | boolean string | No                | Default `true`                                                                   |
| `DB_SSL_ALLOW_SELF_SIGNED` | boolean string | No                | Default `false`                                                                  |

**Validation rules**:

- File path must exist, be a regular file, and be readable.
- Unknown `DB_BACKEND` → fail with list of allowed values.
- For `MYSQL` / `MSSQL` / `ORACLE`: require `DB_SERVER`, `UID`, `PWD`, `DB_DRIVER_NAME`, `DB_DRIVER_CLASS_NAME`; require `DB_NAME` for MYSQL/MSSQL; schema required or defaulted per backend (MSSQL default `dbo` / `DBO` consistent with product).
- For `DERBY` (or omitting dbprops): no host credentials required.
- Passwords must never be written to console/log in clear text by validation code.

**Relationships**: Maps 1:1 into `ResolvedInstallDbConfig` then into `EffectiveRepositoryConfiguration`.

---

### 2. StructuredDbCliInput (secondary input)

Already partially implemented; retained as alternate automation surface.

|     Logical key      |         Env style          |                  Description                  |
|----------------------|----------------------------|-----------------------------------------------|
| `db.type`            | `DB_TYPE` / `PERC_DB_TYPE` | `derby` \| `mysql` \| `sqlserver` \| `oracle` |
| `db.host`            | `DB_HOST`                  | Hostname                                      |
| `db.port`            | `DB_PORT`                  | Port                                          |
| `db.name`            | `DB_NAME`                  | Database name                                 |
| `db.schema`          | `DB_SCHEMA`                | Schema                                        |
| `db.user`            | `DB_USER`                  | User                                          |
| `db.password`        | `DB_PASSWORD`              | Password                                      |
| `db.ssl.*`           | `DB_SSL_*`                 | SSL flags and optional keystore paths         |
| `db.config.env.file` | `DB_CONFIG_ENV_FILE`       | Path to KEY=VALUE env-style file              |

**Validation rules**: Non-Derby requires host, port, name, user, password (existing code). Oracle composition rules documented in contracts.

**Precedence** relative to dbprops: see research D2.

---

### 3. ResolvedInstallDbConfig (internal)

In-memory result of resolution used to launch ANT.

|       Field        |                                        Description                                         |
|--------------------|--------------------------------------------------------------------------------------------|
| `systemProperties` | Map of `perc.db.*` keys passed as JVM `-D` to the install ANT process                      |
| `source`           | `default` \| `dbprops` \| `cli` \| `env-file` \| `environment` (for diagnostics; optional) |
| `backendType`      | Normalized: `derby` \| `mysql` \| `sqlserver` \| `oracle`                                  |

**Key `perc.db.*` outputs** (consumed by ANT):

|                          Key                           |                      Purpose                       |
|--------------------------------------------------------|----------------------------------------------------|
| `perc.db.type`                                         | Branch selector in `repository_properties`         |
| `perc.db.cms.backend`                                  | → `DB_BACKEND`                                     |
| `perc.db.cms.driverName`                               | → `DB_DRIVER_NAME`                                 |
| `perc.db.cms.driverClass`                              | → `DB_DRIVER_CLASS_NAME`                           |
| `perc.db.cms.server`                                   | → `DB_SERVER`                                      |
| `perc.db.cms.name`                                     | → `DB_NAME`                                        |
| `perc.db.cms.schema`                                   | → `DB_SCHEMA`                                      |
| `perc.db.user`                                         | → `UID`                                            |
| `perc.db.password`                                     | → `PWD`                                            |
| `perc.db.ssl.enabled` / `.verify` / `.allowSelfSigned` | → `DB_SSL_*`                                       |
| `perc.db.dts.*`                                        | Optional residual; not required for CMS acceptance |

**State**: Immutable after resolution; invalid resolution throws before extract/ANT.

---

### 4. EffectiveRepositoryConfiguration (install-root artifact)

File: `{install.dir}/rxconfig/Installer/rxrepository.properties`

|                   Field                   |                      Description                      |
|-------------------------------------------|-------------------------------------------------------|
| Same keys as DatabaseTargetPropertiesFile | Written by ANT `propertyfile` on **new install** only |
| Backend-specific values                   | Must match integrator intent after successful install |

**Lifecycle**:

```text
[New install + no override]
    → ship/default Derby template (+ optional SSL stamps)
[New install + dbprops or structured input]
    → resolve → validate fields → write effective file → connect validate → schema setup
[Upgrade]
    → leave existing effective file identity intact (no backend rewrite)
```

**Relationships**: Read by `PSConfigureDatasource`, `PSMakeLasagna`, `PSExecSQLStmt`, table factory, and CMS runtime container utils.

---

### 5. InstallMode

|   Value   |                                      Detection                                      | DB target feature applies? |
|-----------|-------------------------------------------------------------------------------------|----------------------------|
| `NEW`     | `do.install=true` (no prior product version markers per existing install.xml rules) | Yes                        |
| `UPGRADE` | `do.upgrade=true`                                                                   | No (preserve config)       |

---

## State transitions (new install)

```text
            ┌──────────────┐
            │  Start CLI   │
            └──────┬───────┘
                   ▼
         ┌─────────────────────┐
         │ Resolve DB config   │
         │ (dbprops / cli /    │
         │  default)           │
         └─────────┬───────────┘
    invalid        │ valid
       ▼           ▼
┌──────────┐  ┌────────────────┐
│ Fail     │  │ Extract / ANT  │
│ (nonzero)│  └───────┬────────┘
└──────────┘          ▼
             ┌────────────────────┐
             │ Write effective    │
             │ rxrepository.props │  (do.install only)
             └─────────┬──────────┘
                       ▼
             ┌────────────────────┐
             │ Connect validate   │
             └─────────┬──────────┘
          fail         │ ok
             ▼         ▼
      ┌──────────┐  ┌─────────────────┐
      │ Fail     │  │ Repository/schema│
      │ install  │  │ setup (existing) │
      └──────────┘  └────────┬────────┘
                             ▼
                      ┌─────────────┐
                      │ Success     │
                      └─────────────┘
```

## Identity & uniqueness

- One effective repository configuration per CMS install root.
- Integrator may keep multiple sample/dbprops files outside the install root; only the path passed at install time is consumed.

## Secrets handling

- `PWD` / `db.password` / `perc.db.password` are **secret**.
- Not echoed by validation errors, install console summaries, or log statements.
- File-system protection of dbprops is integrator responsibility (assumption in spec).

