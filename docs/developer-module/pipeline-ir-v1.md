# Pipeline IR v1.0

| Field | Value |
|-------|-------|
| **IR version** | `1.0` (`PipelineIrDocument.CURRENT_IR_VERSION`) |
| **Issue** | [#2247](https://github.com/intersoftdatalabs-in/percussioncms/issues/2247) (parent [#1690](https://github.com/intersoftdatalabs-in/percussioncms/issues/1690)) |
| **Code** | `com.percussion.services.pipeline` (`system/services`) |
| **Companions** | [data-pipeline-engine-inventory.md](./data-pipeline-engine-inventory.md) §16 Slice A; [data-pipeline-server-runtime-map.md](./data-pipeline-server-runtime-map.md) |

## Purpose

Versioned, JSON-friendly **intermediate representation** for classic XML Applications / data pipelines:

```
application (meta)
  └── resources[]  (classic PSDataSet)
        └── stages (pageTank | backendTank | mapper | selector | pager | updater)
```

Slice A part 1 delivers **IR model + load/save + classic import**. Execution, SQL runtime, JSON request I/O, hooks, and the graph editor are later slices.

## Document shape (normative sketch)

```json
{
  "irVersion": "1.0",
  "source": "CLASSIC_IMPORT | NATIVE",
  "app": {
    "id": 390,
    "name": "sys_adminCataloger",
    "description": "",
    "requestRoot": "sys_adminCataloger",
    "enabled": true,
    "hidden": false,
    "appType": null,
    "version": "2.0"
  },
  "resources": [
    {
      "name": "Dataset34",
      "description": "",
      "kind": "QUERY | UPDATE | CONTENT_EDITOR | UNKNOWN",
      "requestPage": "sys_rxlookup",
      "transactionMode": "none | row | all",
      "pipeName": "QueryPipe",
      "stages": {
        "pageTank": {
          "present": true,
          "schemaSource": "file:Properties.dtd",
          "actionTypeXmlField": null
        },
        "backendTank": {
          "present": true,
          "tables": [
            { "alias": "PSX_ADMINLOOKUP", "table": "PSX_ADMINLOOKUP", "datasource": "" }
          ],
          "joins": [],
          "joinCount": 0
        },
        "mapper": {
          "present": true,
          "allowEmptyDocReturn": false,
          "mappings": [
            {
              "documentField": "Properties/@Type",
              "backend": "PSX_ADMINLOOKUP.TYPE",
              "backendKind": "COLUMN | EXTENSION | OTHER"
            }
          ]
        },
        "selector": {
          "present": true,
          "unique": false,
          "method": "whereClause | nativeStatement | unknown",
          "whereClauseCount": 1,
          "whereClauses": [
            {
              "leftKind": "COLUMN",
              "left": "PSX_ADMINLOOKUP.TYPE",
              "operator": "=",
              "rightKind": "PARAM",
              "right": "sys_key",
              "booleanOp": "AND",
              "omitWhenNull": false
            }
          ],
          "sortedColumnCount": 0,
          "nativeStatement": null
        },
        "pager": { "present": false, "maxRowsPerPage": 0, "maxPages": 0, "maxPageLinks": 0 },
        "updater": {
          "present": false,
          "allowInsert": false,
          "allowUpdate": false,
          "allowDelete": false,
          "updateColumnCount": 0
        }
      }
    }
  ]
}
```

## Storage

| Mode | Location | Notes |
|------|----------|-------|
| Classic objectstore XML | Existing `PSServerXmlObjectStore` / application directories | Source for **import** only in this slice |
| Native IR JSON | `<rxRoot>/ObjectStore/pipeline-ir/<appName>.pipeline.json` | File store via `PSPipelineIrFileStore`; injectable base dir for tests |

App names are single path components only (no `/`, `\`, `..`) to prevent path injection — same rule as the Pipelines catalog adaptor.

## Import subset (v1)

From classic `PSApplication` / `PSDataSet` / pipes:

| Classic | IR |
|---------|-----|
| App name/id/enabled/hidden/requestRoot/version | `app.*` |
| `PSQueryPipe` | resource `kind=QUERY` + selector |
| `PSUpdatePipe` | resource `kind=UPDATE` + updater |
| `PSContentEditor` | resource `kind=CONTENT_EDITOR` (no deep CE pipe expand) |
| `PSPageDataTank` | `stages.pageTank` |
| `PSBackEndDataTank` | `stages.backendTank` (tables + `joins[]` edges + join count) |
| `PSDataMapper` | `stages.mapper` (field inventory) |
| `PSDataSelector` | `stages.selector` (method + whereClauses IR + counts) |
| `PSWhereClause` / `PSConditional` | `selector.whereClauses[]` (COLUMN/PARAM/LITERAL/OTHER) |
| `PSResultPager` | `stages.pager` |
| `PSDataSynchronizer` | `stages.updater` |

Not imported in v1 (deferred): join **translators** (edges still import with `translatorPresent=true`; generated planner rejects those edges), exits/hooks, result pages/XSL, ACLs, CE field maps. Unsupported where right-hand kinds stay as `OTHER` (planner requires native SQL for those).

## Service API

### IR (Slice A part 1 — #2247)

`IPSPipelineIrService` / `PSPipelineIrService` / `PSPipelineIrServiceLocator`:

- `importClassicXml` / `importClassicApplication`
- `toJson` / `fromJson`
- `save` / `load` / `exists`

### Runtime (Slice A part 2 — #2248; richer UPDATE/DELETE — #2340)

`IPSPipelineRuntimeService` / `PSPipelineRuntimeService` / `PSPipelineRuntimeServiceLocator`:

- `execute(appName, resourceName, request)` — load native IR, run resource
- `execute(document, resource, request)` — in-memory IR (tests / callers)
- SQL via `IPSPipelineSqlAdapter` / `PSJdbcPipelineSqlAdapter` (parameterized JDBC only)
- Planner: `PSPipelineSqlPlanner` (generated single-table or **multi-table JOIN** SELECT; single-table INSERT/**UPDATE**/**DELETE**; or native SELECT with `:param`)
- Generated SELECT **WHERE** prefers `selector.whereClauses` IR when present (COLUMN left; operators `=`, `<>`, `!=`, `<`, `<=`, `>`, `>=`, `LIKE`, `NOT LIKE`, `IS NULL`, `IS NOT NULL`; right PARAM/LITERAL/COLUMN; AND/OR; `omitWhenNull`). Otherwise falls back to request-param equality on mapped columns.
- Generated multi-table SELECT uses `backendTank.joins[]` (`joinType` = `INNER`|`LEFT`|`RIGHT`|`FULL`; `left`/`right` = `alias.column`). ANSI `INNER` / `LEFT OUTER` / `RIGHT OUTER` / `FULL OUTER` JOIN … ON. Join edges with classic translators are rejected (native SELECT escape hatch). Mutations remain single-table.
- Pre/post hooks: `IPSPipelinePreExecuteHook` / `IPSPipelinePostExecuteHook`
- JSON I/O: `PipelineExecuteRequest` / `PipelineExecuteResult` + `PSPipelineExecuteJsonCodec`
- Thin REST: `POST /services/pipelines/{app}/resources/{resource}/execute` (see #2269 / PR #2341)

**Not** calling classic `PSQueryHandler` / `PSUpdateHandler` as the public path.

Catalog list remains `GET /services/pipelines`.

### UPDATE resource mutations (`updater.*` flags)

| Request | Planner | Flag gate |
|---------|---------|-----------|
| `operation=insert` (or only insert allowed) | `planInserts` | `allowInsert` |
| `operation=update` (or only update allowed) | `planUpdates` | `allowUpdate` |
| `operation=delete` (or only delete allowed) | `planDeletes` | `allowDelete` |

- When more than one of insert/update/delete is allowed, **`request.operation` is required**.
- When a flag is false, the runtime rejects with a clear API error naming the flag (e.g. `updater.allowUpdate=false`).
- **UPDATE**: `SET` non-key mapped columns from each row; **WHERE** from `request.keyColumns` values on the row, or from mapped keys in `request.params` (shared WHERE).
- **DELETE**: WHERE from `keyColumns` / mapped `params` / mapped columns present on each row. Empty WHERE (unrestricted delete) is rejected.
- Unrestricted UPDATE/DELETE without keys is rejected.

Example update body:

```json
{
  "operation": "update",
  "keyColumns": ["TYPE", "NAME"],
  "rows": [
    { "TYPE": "workflow", "NAME": "wf1", "LOOKUPVALUE": "99" }
  ]
}
```

### Multi-row transaction modes

IR field `resource.transactionMode` (`none` | `row` | `all`) is honored for multi-plan mutations via `IPSPipelineSqlAdapter#updateAll`:

| Mode | Behavior |
|------|----------|
| `none` (default) | Each plan uses its own connection; auto-commit per plan |
| `row` | Each plan runs in its own explicit transaction (commit per plan); prior plans stay committed if a later plan fails |
| `all` | All plans share one connection/transaction; **any failure rolls back the entire batch** |

Documented + covered by H2 tests in `PSPipelineRuntimeServiceTest` (`transactionMode=all` commit + rollback; `row` keeps prior commits).

### Runtime security

| Default | Behavior |
|---------|----------|
| Parameterized SQL | Request values bound as JDBC `?` only |
| Generated identifiers | Table/column must match `[A-Za-z_][A-Za-z0-9_]*` |
| Native SQL escape hatch | Single `SELECT`/`WITH` only; named `:param`; rejects `;`, DML/DDL keywords |
| Joins / multi-table QUERY | **Supported** when `backendTank.joins` has edges: ANSI JOIN SELECT with qualified identifiers + parameterized filters. **Rejected** when multi-table / `joinCount > 0` without edges (`JOIN_PRODUCT_LIMIT_MESSAGE`), disconnected graphs, self-joins, or `translatorPresent` edges (`JOIN_TRANSLATOR_LIMIT_MESSAGE`). Use `nativeStatement` for translator joins or exotic SQL. |
| Joins / multi-table mutations | INSERT/UPDATE/DELETE remain single-table only |
| Unrestricted DELETE/UPDATE | Rejected (WHERE keys required) |

Manual raw multi-statement SQL is **not** a product surface.

## Tests

- IR JSON round-trip + file load/save
- Golden classic fixture `sys_adminCataloger` → IR stage inventory (backend tank, mapper, selector whereClauses, page tank)
- H2 runtime: generated query + JSON params, **where-clause IR** (PARAM/LIKE), native SELECT, pre/post hooks, INSERT, UPDATE/DELETE flag gates, multi-row `transactionMode` all/row, **multi-table INNER + LEFT OUTER JOIN** generation/execution, joinCount-without-edges rejection (`PSPipelineRuntimeServiceTest`)

## Evolution

- **1.x** additive fields preferred; bump `irVersion` only for breaking shape changes.
- Slice A2 (#2248): SQL execute + JSON I/O + hooks consume this IR (landed).
- Residual #2269: thin REST invoke (landed / PR #2341).
- Residual #2340: richer UPDATE/DELETE + multi-row txn modes (landed / PR #2360).
- Residual #2359: where-clause IR executable path + join product limit documentation (landed).
- Residual #2391: **join-graph SQL generation** (`backendTank.joins` + multi-table SELECT planner; this work). Refs #1690.
- Slice B: designer writes native IR (`source=NATIVE`).
