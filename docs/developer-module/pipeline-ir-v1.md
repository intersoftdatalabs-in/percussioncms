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
| `PSBackEndDataTank` | `stages.backendTank` (tables + join count) |
| `PSDataMapper` | `stages.mapper` (field inventory) |
| `PSDataSelector` | `stages.selector` (method + clause counts) |
| `PSResultPager` | `stages.pager` |
| `PSDataSynchronizer` | `stages.updater` |

Not imported in v1 (deferred): full where-clause trees, join graphs, exits/hooks, result pages/XSL, ACLs, CE field maps.

## Service API

`IPSPipelineIrService` / `PSPipelineIrService` / `PSPipelineIrServiceLocator`:

- `importClassicXml` / `importClassicApplication`
- `toJson` / `fromJson`
- `save` / `load` / `exists`

No REST surface in this slice (catalog list remains `GET /services/pipelines`).

## Tests

- IR JSON round-trip + file load/save
- Golden classic fixture `sys_adminCataloger` → IR stage inventory (backend tank, mapper, selector, page tank)

## Evolution

- **1.x** additive fields preferred; bump `irVersion` only for breaking shape changes.
- Slice A2 (#2248): SQL execute + JSON I/O + hooks consume this IR.
- Slice B: designer writes native IR (`source=NATIVE`).
