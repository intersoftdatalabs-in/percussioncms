# Inventory: Oracle user vs schema in page-database publishing

**Issue:** [#2244](https://github.com/intersoftdatalabs-in/percussioncms/issues/2244) (slice 1 of epic [#953](https://github.com/intersoftdatalabs-in/percussioncms/issues/953))  
**Related:** fix slice [#2245](https://github.com/intersoftdatalabs-in/percussioncms/issues/2245), live Oracle smoke [#2246](https://github.com/intersoftdatalabs-in/percussioncms/issues/2246)  
**Date:** 2026-08-07  
**Scope:** Code-path inventory only — no product fix in this slice.

## Symptom (from #953)

Pre-built / page-database publishing fails during table schema processing:

```text
Unable to process schema changes for table (PERC_EXPORT_PAGE): [1] 42000: ORA-01918: user 'ORAPROD' does not exist
```

Customer framing: **ORAPROD** is the **schema** name; **SYSTEM** is the **connect user**. Error text is emitted via TableFactory message `1301` (`PSJdbcTableFactoryResources.properties`).

Oracle treats a schema name as a **database user** for object ownership/qualification. `ORA-01918` means Oracle evaluated a name as a user that does not exist — typically during DDL that qualifies objects as `ORAPROD.<table>` (or other user-scoped DDL).

---

## End-to-end call path

```text
PSUpdateTablesEditionTask.perform (PREEDITION)
  └─ getConnectionInfo(edition)
       └─ IPSPubServerDao.findPubServer(edition.pubServerId)
       └─ PSDatabaseDeliveryHandler.getDbmsInfoFromPubServer(pubServer)
            ├─ new PSDatabasePubServer((PSPubServer) pubServer)
            │     userid  ← property "userid"
            │     owner   ← "owner" (MSSQL) or "schema" defaulting to userid (Oracle)
            ├─ m_origin       = dbServer.getOwner()     // schema/owner → origin
            ├─ m_driverType   = dbServer.getDriverType().getDriverName()
            ├─ m_dbName       = dbServer.getDatabase()  // unused for Oracle SID path
            └─ m_resourceName = getJndiDsName(dbServer) // "jdbc/<name>__<siteId>"
  └─ new DbmsConnection(dbmsInfo)
       └─ new PSJdbcDbmsDef(dbName, resourceName, driverType, origin, env)
            └─ init(...): m_schema = origin; connection via JNDI DataSource
  └─ PSJdbcTableFactory.processTables(conn, dbmsDef, ...)
       └─ DDL/DML uses dbmsDef.getSchema() for qualifyTableName / metadata
```

Same `getDbmsInfoFromPubServer` is used for **item delivery** (`PSDatabaseDeliveryHandler.setDbmsInfoFromPubServer` → `performAction`), not only the pre-edition table-update task.

### Fallback when no pub server

If `edition.getPubServerId()` is null or the pub server cannot be loaded, `PSUpdateTablesEditionTask` builds `DbmsInfo` from the template XML document. That path injects a hard-coded datapublisher wrapper:

|   Attribute    | Hard-coded value (template inject) |
|----------------|------------------------------------|
| `dbname`       | `cmlite_db`                        |
| `drivertype`   | `jtds:sqlserver`                   |
| `origin`       | `dbo`                              |
| `resourceName` | `jdbc/cmlite_db`                   |

This fallback is **SQL Server–shaped** and is **not** the Oracle production path when a database pub server is attached to the edition. When a pub server **is** present, `getDbmsInfoFromPubServer` fully overwrites `m_dbName` / `m_driverType` / `m_origin` / `m_resourceName`.

---

## Key classes and fields

### 1. `PSUpdateTablesEditionTask`

`projects/sitemanage/.../task/impl/PSUpdateTablesEditionTask.java`

|       Method        |                                              Role                                               |
|---------------------|-------------------------------------------------------------------------------------------------|
| `perform`           | PREEDITION: load `perc.pageDatabase` (or param) template, enable schema alter, run TableFactory |
| `getConnectionInfo` | Resolves edition → pub server → `getDbmsInfoFromPubServer`                                      |
| `updateTables`      | `DbmsConnection` + `PSJdbcTableFactory.processTables` for `PERC_EXPORT_PAGE` (+ child tables)   |
| `getTableDefs`      | Mutates template XML; injects default `origin="dbo"` only as XML scaffolding                    |

### 2. `PSDatabaseDeliveryHandler`

`system/business/.../rx/delivery/impl/PSDatabaseDeliveryHandler.java`

|         Type / method          |                                        Field mapping                                        |
|--------------------------------|---------------------------------------------------------------------------------------------|
| `DbmsInfo`                     | `m_dbName`, `m_resourceName`, `m_driverType`, **`m_origin`** (from XML attrs or pub server) |
| `DbmsInfo(Document)`           | Reads `dbname`, `resourceName`, `drivertype`, **`origin`** attributes                       |
| `DbmsConnection`               | `new PSJdbcDbmsDef(m_dbName, m_resourceName, m_driverType, **m_origin**, null)`             |
| **`getDbmsInfoFromPubServer`** | **`m_origin = dbServer.getOwner()`** — central mapping                                      |
| `setDbmsInfoFromPubServer`     | Overwrites all four DbmsInfo fields from pub server for item publish                        |

**Today:** `getOwner()` is the only source of origin for the pub-server path. There is no separate pass of connect `userid` into `PSJdbcDbmsDef` on this path.

### 3. `PSDatabasePubServer`

`system/services/.../pubserver/data/PSDatabasePubServer.java`

Constructor from `PSPubServer` properties:

|   Driver   |                                        Field `owner` source                                         |     Connect user      |
|------------|-----------------------------------------------------------------------------------------------------|-----------------------|
| **MSSQL**  | property **`owner`** (`PUBLISH_OWNER_PROPERTY`)                                                     | property **`userid`** |
| **ORACLE** | property **`schema`** (`PUBLISH_SCHEMA_PROPERTY`), **default = `userName`** if schema empty/missing | property **`userid`** |
| **MySQL**  | not set from owner/schema in this ctor branch                                                       | property **`userid`** |

```java
// Oracle branch (conceptual)
userName = pubServer.getPropertyValue("userid", "");
owner    = pubServer.getPropertyValue("schema", userName);  // default schema → connect user
```

`getOwner()` javadoc: *"owner (for MS SQL). It is the schema name for oracle."*  
So **one Java field (`owner`) holds MSSQL owner or Oracle schema**; naming is historical and confusable.

### 4. JNDI + datasource registration

`PSDatabasePubServerFilesService`

|                Step                 |                               User / schema handling                               |
|-------------------------------------|------------------------------------------------------------------------------------|
| `convertToDatasource`               | JNDI **user** = `pubServer.getUserName()`; **not** owner/schema                    |
| `createDatasourceConfig`            | `PSDatasourceConfig(..., origin=server.getOwner(), database=server.getDatabase())` |
| `testDatabasePubServer`             | Connects with `getUserName()` / password only (schema unused)                      |
| `setDatabaseProperties` (read-back) | `s.setOwner(config.getOrigin())`                                                   |

**Separation at registration time is correct:** connect credentials on JNDI DS; origin/schema on `PSDatasourceConfig`.

Publishing **runtime** rebuilds `PSDatabasePubServer` from **pub server property bag** and sets origin via `getOwner()`; it does **not** re-read `PSDatasourceConfig.getOrigin()` for `DbmsInfo.m_origin` (both should agree if save path was used).

### 5. `PSJdbcDbmsDef`

`modules/TableFactory/.../PSJdbcDbmsDef.java`

|                               Construction path                                | `m_uid` (UID)  | `m_schema` (DB_SCHEMA / origin) |
|--------------------------------------------------------------------------------|----------------|---------------------------------|
| `Properties` ctor                                                              | `UID` property | `DB_SCHEMA` property            |
| JNDI ctor `(dbName, resourceName, driverType, origin, env)` used by publishing | **left null**  | **`m_schema = origin`**         |
| Repository load helper                                                         | JNDI user id   | repository config **origin**    |

```java
// Publishing path
init(dataSource, driverType, origin) {
  m_schema = origin;   // ← only place origin is stored on def
  m_dataSource = dataSource; // connect user lives inside container DS
}
```

Oracle JNDI ctor **requires** non-empty origin when `resourceName` is specified (throws `IllegalArgumentException` otherwise).

Accessors:

|    Method     |                                 Meaning                                 |
|---------------|-------------------------------------------------------------------------|
| `getUserId()` | Connect UID from properties path; **null on pure JNDI publishing path** |
| `getSchema()` | Origin / schema / owner used for **table qualification**                |

### 6. TableFactory consumers (schema vs user)

DDL/DML builders use **`getSchema()`**, not `getUserId()`:

|          Class           |                                              Use of schema                                               |
|--------------------------|----------------------------------------------------------------------------------------------------------|
| `PSJdbcStatementFactory` | `PSSqlHelper.qualifyTableName(..., dbmsDef.getSchema(), driver)` for CREATE/ALTER/INSERT/etc.            |
| `PSJdbcPlanBuilder`      | `DatabaseMetaData.getTables(null, getSchema(), ...)`                                                     |
| `PSJdbcTableMetaData`    | Schema filter; may fall back to `getConnectionDetail().getOrigin()` (publishing def returns null detail) |
| `PSJdbcTableFactory`     | Count/existence SQL via `getSchema()`                                                                    |

**No TableFactory path was found that emits `CREATE USER` / `GRANT ... TO` using schema.**  
`ORA-01918` in this flow is almost certainly Oracle rejecting a **qualified object name** whose schema component is not an existing Oracle user/schema (e.g. `ORAPROD.PERC_EXPORT_PAGE`), or an equivalent user-scoped catalog operation.

Oracle qualification (`PSSqlHelper.qualifyTableName` for `oracle:` drivers):

```text
owner/schema set, db empty  →  ORAPROD.PERC_EXPORT_PAGE
owner/schema empty          →  PERC_EXPORT_PAGE  (or db-based forms)
```

---

## Confusable property / attribute names

|                Name                 |                               Layer                               |                             Role                             |
|-------------------------------------|-------------------------------------------------------------------|--------------------------------------------------------------|
| `userid`                            | Pub server property (`PUBLISH_USER_ID_PROPERTY`)                  | Connect user → JNDI DS user; `PSDatabasePubServer.userName`  |
| `schema`                            | Pub server property (`PUBLISH_SCHEMA_PROPERTY`)                   | **Oracle only** → stored in `PSDatabasePubServer.owner`      |
| `owner`                             | Pub server property (`PUBLISH_OWNER_PROPERTY`)                    | **MSSQL only** → `PSDatabasePubServer.owner`                 |
| `owner` (Java field / `getOwner()`) | `PSDatabasePubServer`                                             | **Dual meaning:** MSSQL owner **or** Oracle schema           |
| `origin`                            | XML datapublisher attr; `DbmsInfo.m_origin`; `PSDatasourceConfig` | Schema/owner passed into `PSJdbcDbmsDef`                     |
| `UID`                               | TableFactory / `IPSJdbcDbmsDefConstants.UID_PROPERTY`             | Connect user on **properties**-based defs                    |
| `DB_SCHEMA`                         | TableFactory / `DB_SCHEMA_PROPERTY`                               | Schema on **properties**-based defs (= origin on JNDI path)  |
| `sid`                               | Pub server property (Oracle)                                      | SID/service for JDBC URL — **not** schema                    |
| `database`                          | Pub server property                                               | Used for MySQL/MSSQL; Oracle largely unused for SID URL form |
| JNDI resource name                  | `jdbc/<name>__<siteId>`                                           | Lookup only; does not encode schema                          |

### UI surfaces (aligned with backend keys as of current tree)

- React: `WebUI/.../DatabaseDriverFields.tsx` — Oracle `schema` + `sid` + `userid`; MSSQL `owner` + `database` + `userid`
- Legacy Minuet: `propEditor.jsp` — same percName keys (`schema` for Oracle, `owner` for MSSQL)

Historical risk (see `docs/ai-generated/code-reviews/publishing-driver-key-alignment-erlang.md`): mismatched UI keys could save `schema` under the wrong property so `getOwner()` defaults to `userName` or stays empty — separate from runtime origin wiring but can produce wrong origin.

---

## MSSQL owner vs Oracle schema

|          Concern          |                MSSQL                |                                                                             Oracle                                                                              |
|---------------------------|-------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Pub-server property key   | `owner` (often `dbo`)               | `schema`                                                                                                                                                        |
| Stored in                 | `PSDatabasePubServer.owner`         | same field                                                                                                                                                      |
| Default if missing        | empty string from property default  | **`userName` (connect user)**                                                                                                                                   |
| Passed to TableFactory as | `DbmsInfo.m_origin` → `getSchema()` | same                                                                                                                                                            |
| Qualification             | `db.owner.table`                    | `schema.table` (owner param in `qualifyTableName`)                                                                                                              |
| Connect user              | JNDI / `userid`                     | JNDI / `userid`                                                                                                                                                 |
| Semantic trap             | owner `dbo` ≠ login is normal       | **schema name == Oracle user name** for object ownership; schema ≠ connect user only works if that Oracle user/schema **exists** and connect user can DDL there |

Oracle default `schema → userName` hides the bug class when schema equals login. Configs with **schema ≠ userid** (ORAPROD vs SYSTEM) are the exposing case for #953.

---

## Ranked root-cause candidates

| Rank  |                                                                                Candidate                                                                                 |             Likelihood              |                                                                                  Evidence                                                                                   |
|-------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **1** | **Origin set to Oracle `schema` (ORAPROD); TableFactory qualifies as `ORAPROD.PERC_EXPORT_PAGE`; Oracle has no user/schema ORAPROD (or name is wrong case/object type)** | High for the exact `ORA-01918` text | Path is intentional: `getOwner()` → `m_origin` → `m_schema` → `qualifyTableName`. On Oracle, missing schema-user yields `ORA-01918`.                                        |
| **2** | **Operator/UI confusion: value that is not an Oracle schema (SID, DB alias, tablespace, app name) stored in `schema`**, while connect user SYSTEM should own tables      | High operationally                  | Separate fields `sid` / `userid` / `schema` exist; wrong field fill produces Rank-1 failure. Defaulting empty schema to userid is the safe “current user schema” behavior.  |
| **3** | **Intended cross-schema publish (SYSTEM connects, objects in ORAPROD) but ORAPROD user not created / no privileges**                                                     | Medium (env)                        | Code would be “correct”; residual #2246 smoke validates. Not a code swap of UID↔schema by itself.                                                                           |
| **4** | **Pub-server property not saved under `schema` (legacy key mismatch); `owner` defaults to `userName` or empty**                                                          | Medium historical                   | Driver-key alignment review; modern React/JSP keys look correct. Would usually yield SYSTEM schema, not ORAPROD — unless wrong value landed in `schema` or `userid`.        |
| **5** | **Some path uses schema string as connect UID**                                                                                                                          | **Low on publishing path**          | JNDI uses `getUserName()`; properties `UID` separate from `DB_SCHEMA`. JNDI publishing leaves `m_uid` null; TableFactory DDL does not call `getUserId()` for qualification. |
| **6** | **Hard-coded template `origin="dbo"` leaks into Oracle**                                                                                                                 | Low when pub server present         | Overwritten by `getDbmsInfoFromPubServer`. Only matters if pub server missing.                                                                                              |
| **7** | **TableFactory “user-scoped API” treating `getSchema()` as CREATE USER target**                                                                                          | Low                                 | No CREATE USER/GRANT builders found; `ORA-01918` from qualified DDL is sufficient explanation.                                                                              |

**Interpretation for #953 wording (“capturing incorrect User name”):**  
The product stores Oracle schema in a field named **`owner`** and passes it as **`origin`/`DB_SCHEMA`**. Oracle error text labels that name a **user**. That is easy to read as “we captured the wrong user,” even when the wiring is “schema used as schema.” Slice 2 must still decide whether the **product behavior** for schema ≠ connect user is correct, incomplete (docs/validation), or needs a code change (e.g. prefer connect-user schema when Oracle schema user is absent, or validate schema exists, or stop defaulting/over-qualifying).

---

## Recommended single fix surface for #2245

### Primary (recommended)

**Module:** `system/business`  
**Class:** `com.percussion.rx.delivery.impl.PSDatabaseDeliveryHandler`  
**Method:** `getDbmsInfoFromPubServer` (and tests for `DbmsInfo` origin mapping)

**Why:** Single choke point for both pre-edition table updates and item database delivery. Today:

```text
m_origin = dbServer.getOwner();  // Oracle schema or MSSQL owner
```

Any intentional change to “what becomes origin for Oracle when schema ≠ userid” belongs here (or immediately behind a small pure helper used only here), without rewriting TableFactory.

### Companion (same PR if needed)

|         Module         |                Class                |                                                                     When to touch                                                                     |
|------------------------|-------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------|
| `system/services`      | `PSDatabasePubServer`               | If property read/default (`schema` → `owner`, default `userName`) is wrong or needs explicit Oracle accessors (`getSchema()` alias) for clarity/tests |
| `system/services`      | `PSDatabasePubServerFilesService`   | Only if JNDI origin registration must stay lockstep with delivery origin                                                                              |
| `modules/TableFactory` | `PSJdbcDbmsDef` / statement factory | **Only if** tests prove `getSchema()` is used where connect user is required — **not indicated by this inventory for ORA-01918**                      |

### Suggested unit-test fixtures (#2245, no live Oracle)

1. Pub-server property bag: `driver=ORACLE`, `userid=SYSTEM`, `schema=ORAPROD` → assert `DbmsInfo.m_origin` / `PSJdbcDbmsDef.getSchema()` == `ORAPROD` and JNDI/connect user remains `SYSTEM` (not overwritten).
2. Oracle with **empty** schema → origin defaults to `SYSTEM` (`userName`).
3. MSSQL: `owner=dbo`, `userid=sa` → origin `dbo`, user not used as origin.
4. Optional: `qualifyTableName` for oracle driver with schema ORAPROD produces `ORAPROD.PERC_EXPORT_PAGE`.

### Out of scope for #2245 (leave to #2246 / docs)

- Live Oracle matrix where schema user exists vs missing
- Installer/repository install property paths (different bug class unless proven shared)
- Monorepo-wide rename of `owner`/`origin` terminology

---

## Data-flow summary diagram

```text
UI / prop bag
  userid ─────────────────────────────► PSDatabasePubServer.userName
                                           │
                                           ├─► JNDI DataSource user (connect)
                                           └─► default for Oracle schema if blank
  schema (Oracle) ─┐
  owner  (MSSQL)  ─┴───────────────────► PSDatabasePubServer.owner
                                           │
                                           ▼
                         getDbmsInfoFromPubServer: m_origin = getOwner()
                                           │
                                           ▼
                         PSJdbcDbmsDef.m_schema = origin
                                           │
                                           ▼
                         qualifyTableName → ORAPROD.PERC_EXPORT_PAGE
                                           │
                                           ▼
                         Oracle: ORA-01918 if user/schema ORAPROD missing
```

---

## Acceptance mapping (#2244)

|                     Acceptance item                     |                                                  Status                                                  |
|---------------------------------------------------------|----------------------------------------------------------------------------------------------------------|
| Inventory of classes/methods and schema vs connect user | This document                                                                                            |
| Root-cause candidates ranked                            | Section above                                                                                            |
| Recommended single fix surface for slice 2              | `PSDatabaseDeliveryHandler.getDbmsInfoFromPubServer` (+ optional `PSDatabasePubServer` property default) |
| Links to #953                                           | Parent epic; siblings #2245, #2246                                                                       |

---

## Source anchors (absolute under repo root)

|                                                  Path                                                  |                            Notes                             |
|--------------------------------------------------------------------------------------------------------|--------------------------------------------------------------|
| `projects/sitemanage/src/main/java/com/percussion/sitemanage/task/impl/PSUpdateTablesEditionTask.java` | PREEDITION entry; `getDbmsInfoFromPubServer` call            |
| `system/business/src/com/percussion/rx/delivery/impl/PSDatabaseDeliveryHandler.java`                   | `DbmsInfo`, `DbmsConnection`, **`getDbmsInfoFromPubServer`** |
| `system/services/src/com/percussion/services/pubserver/data/PSDatabasePubServer.java`                  | Oracle `schema` / MSSQL `owner` → `owner` field              |
| `system/services/src/com/percussion/services/pubserver/IPSPubServerDao.java`                           | Property name constants                                      |
| `system/services/src/com/percussion/services/pubserver/impl/PSDatabasePubServerFilesService.java`      | JNDI user vs config origin                                   |
| `modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcDbmsDef.java`                    | `origin` → `m_schema`; UID vs DB_SCHEMA                      |
| `modules/TableFactory/src/main/java/com/percussion/tablefactory/PSJdbcStatementFactory.java`           | DDL via `getSchema()`                                        |
| `modules/utils/src/main/java/com/percussion/util/PSSqlHelper.java`                                     | Oracle `owner.table` qualification                           |
| `modules/utils/src/main/java/com/percussion/utils/jdbc/PSDatasourceConfig.java`                        | Config `origin`                                              |
| `WebUI/src/main/ts/publishing/components/drivers/DatabaseDriverFields.tsx`                             | UI property keys                                             |

> Co-Authored by Grok Build using grok-4.5 with agent main.

