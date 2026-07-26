# Contract: Product-managed repository configuration

## Purpose

Define how CMS and DTS declare the live product-managed repository so install, upgrade, migration, and support can detect backend and rewrite safely.

## CMS — `rxrepository.properties` (and install-root equivalents)

|          Key           |        Required        |                                             Semantics                                             |
|------------------------|------------------------|---------------------------------------------------------------------------------------------------|
| `DB_BACKEND`           | Yes                    | Backend label: `DERBY` (legacy), `H2` (new default*), `MYSQL`, `MSSQL`, …                         |
| `DB_DRIVER_NAME`       | Yes                    | Short driver token used by product utilities (e.g. `derby`, `h2`, `mysql`)                        |
| `DB_DRIVER_CLASS_NAME` | Yes                    | JDBC driver class                                                                                 |
| `DB_SERVER`            | Yes                    | Server/path fragment used to build JDBC URL (product-specific encoding; may include create flags) |
| `DB_SCHEMA`            | As required by backend | Schema / default schema                                                                           |
| `DB_NAME`              | As required            | Database name when applicable                                                                     |
| `UID` / `PWD`          | As required            | Credentials; empty allowed for some embedded defaults                                             |
| `DSCONFIG_NAME`        | As used by runtime     | Datasource config name for container wiring                                                       |

\*Or HSQL label if bake-off selects HSQLDB.

### Detection rules

|                              Condition                               |                     Interpretation                      |
|----------------------------------------------------------------------|---------------------------------------------------------|
| `DB_BACKEND=DERBY` **or** driver name/class is Derby embedded/client | Product-managed **Derby** → migration candidate         |
| `DB_BACKEND` in {MYSQL, MSSQL, …} with non-Derby driver              | **External** → migration **skipped** (FR-009)           |
| `DB_BACKEND` = new default                                           | **Already migrated / new install** → no Derby migration |

### Stability

- **Do not rename** existing keys for MYSQL/MSSQL customers.
- New default installs MUST write new-default backend values consistently across installer templates, Jetty `perc-ds.*`, and defaults under `system/config`.
- Migration MAY only rewrite keys for instances detected as product-managed Derby.

## DTS — per-service datasource properties

Each DTS service keeps its existing property file / bean contract. Migration MUST:

1. Detect Derby URL/driver patterns for that service
2. Rewrite only that service’s product-managed default datasource
3. Leave external JDBC URLs untouched

## Jetty CMS datasource (`perc-ds.properties`)

Must stay consistent with `rxrepository.properties` after install/upgrade (driver name, class, connection test query appropriate to engine).

## Canonical new-default JDBC URL (freeze in WP0/WP1)

Document the exact H2 (or HSQL) URL template used for CMS defaults after bake-off, including:

- File path form (portable; no Unix-only roots)
- Schema / user alignment with product `DB_SCHEMA` and `UpperCaseNamingStrategy`
- Any required MODE / case-folding / `AUTO_SERVER` parameters for install multi-process (research R11)

**Draft candidate (subject to bake-off T008–T011):**

|                       Piece                       |                                                                                        Value                                                                                         |
|---------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Driver name                                       | `h2`                                                                                                                                                                                 |
| Driver class                                      | `org.h2.Driver`                                                                                                                                                                      |
| `DB_BACKEND`                                      | `H2`                                                                                                                                                                                 |
| JDBC URL form                                     | `jdbc:h2:file:<absolute-or-install-relative-path>/CMDB;DB_CLOSE_ON_EXIT=FALSE;AUTO_SERVER=FALSE`                                                                                     |
| Server fragment for `rxrepository` / `getJdbcUrl` | `file:<path>/CMDB` (product may prepend `jdbc:h2:`)                                                                                                                                  |
| Notes                                             | Do **not** use Derby `;create=true`. H2 creates file DBs by default. `AUTO_SERVER` only if R11 multi-process install requires it. Uppercase physical naming remains product default. |

Installer MUST NOT require Derby DRDA port **1527** after cutover unless H2 TCP is an explicit designed substitute.

## Seed strategy (new installs)

Exactly one strategy is active (chosen in tasks T024 / research R11):

- **A**: empty H2 + TableFactory/product load, **or**
- **B**: prebuilt H2 seed tree (replacement for `Derby/Repository.zip`)

Document the chosen option here when locked:

- **Locked choice**: **A** — empty H2 + TableFactory/product load (2026-07-24; research R11)

## Non-goals

- Public REST API for reconfiguring backend at runtime
- Multi-repository write fan-out
- Promising Derby Network Server / DRDA remote access on the new default (document as intentional break in release notes)

