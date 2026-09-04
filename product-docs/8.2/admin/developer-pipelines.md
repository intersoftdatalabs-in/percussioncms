---
id: admin-developer-pipelines
title: Developer Pipelines
description: Browse classic XML Applications and inspect read-only pipe IR structure from Developer Pipelines chrome
version: "8.2"
order: 51
tags: [admin, developer, pipelines]
---

# Developer Pipelines

**Developer → Pipelines** lists classic **XML Applications** (data pipeline
packages) visible to the current user. Open a row for **read-only** application
detail: catalog metadata, data sets, and a **pipe IR** summary (resources, stage
presence, backend tanks, mapper mappings) so operators can inspect structure
without the Swing E2Designer.

The IR section calls `GET /services/pipelines/{idOrName}/ir`. When a native IR
file exists under `ObjectStore/pipeline-ir/`, that document is shown (`source`
`NATIVE`). Otherwise the server imports the classic application into IR **in
memory** (`source` `CLASSIC_IMPORT`) and does **not** save it. Graph editing,
IR write / native save, start/stop chrome, and classic ZIP import/export remain
later slices (see `designGaps` on application detail).

## Product path — inspect pipe IR

1. Sign in with a role that can open **Developer**.
2. Open **Developer → Pipelines**, or deep-link
   `spa.jsp?entry=developer&section=pipelines`.
3. Open a listed application. The detail panel shows metadata and **Data sets**.
4. Scroll to **Pipe IR**. Confirm **IR source** (`NATIVE` or `CLASSIC_IMPORT`)
   and **IR version**. Expand each IR resource for:
   - Stages present (page tank, backend tank, mapper, selector, pager, updater)
   - Backend tank tables (alias / table / datasource) and join count when present
   - Mapper mappings (document field ↔ backend column/extension)
   - Selector method / where-clause count and updater allow flags when present
5. Use **Back to list** to return to the catalog.

If IR cannot be loaded (for example **404** for an unknown app or missing IR),
the catalog detail still renders and the Pipe IR section shows an error — the
chrome does not echo the raw path name in that message.

## Limits

- Read-only: no graph editor, drag-drop tanks, or IR save from this chrome.
- Start/stop / enable application chrome is a separate Pipelines slice.
- Classic ZIP import/export is not exposed here.
- Thin IR execute (`POST …/execute`) is available on REST for smoke tests; this
  SPA detail panel does not invoke execute.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/pipelines` (optional `name`, `limit`, `offset`) |
| Detail | `GET /services/pipelines/{idOrName}` |
| Pipe IR | `GET /services/pipelines/{idOrName}/ir` |

Integrator notes: [REST API — Pipelines](id:developer-rest).
