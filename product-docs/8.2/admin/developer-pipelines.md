---
id: admin-developer-pipelines
title: Developer Pipelines
description: Browse classic XML Applications, Admin start/stop, pipe IR, HTTP datasource Test invoke, and Problems from Developer Pipelines chrome
version: "8.2"
order: 51
tags: [admin, developer, pipelines]
---

# Developer Pipelines

**Developer → Pipelines** lists classic **XML Applications** (data pipeline
packages) visible to the current security token. Open a row for application
metadata, the **data set** catalog (request pages / content editors), and a
**pipe IR** summary (resources, stage presence, backend tanks, mapper mappings)
so operators can inspect structure without the Swing E2Designer.

**Admins** can **Start** or **Stop** a non-hidden application from the detail
toolbar. Those actions peer the server console `start application` /
`stop application` commands and call Admin REST
`POST /services/pipelines/{idOrName}/start` and
`POST /services/pipelines/{idOrName}/stop`. Non-Admin sessions do not see the
lifecycle controls.

The IR section calls `GET /services/pipelines/{idOrName}/ir`. When a native IR
file exists under `ObjectStore/pipeline-ir/`, that document is shown (`source`
`NATIVE`). Otherwise the server imports the classic application into IR **in
memory** (`source` `CLASSIC_IMPORT`) until an Admin **saves an HTTP backend
tank**, which writes native IR without rewriting classic XML Applications.

**Admins** also get **HTTP datasource**, **Test invoke**, and **Problems** on the
same detail page:

- **HTTP datasource** sets `adapterType=HTTP` and a **loopback / local fixture
  URL** (default `http://127.0.0.1/pipeline-http-fixture`) on the selected
  resource via `PUT /services/pipelines/{app}/resources/{resource}/backendTank`.
  Cloud hosts, credentials in the URL, and non-http(s) schemes return **400**.
- **Test invoke** posts sample JSON (`params` / `rows`) to
  `POST /services/pipelines/{app}/resources/{resource}/execute` and shows the
  structured execute result (or a clear error). HTTP tanks return mapped JSON
  `rows` (for example `sku` / `name` from the bundled fixture) — not empty
  invented data.
- **Problems** loads Admin `GET /services/pipelines/{idOrName}/validation` when
  that endpoint is present. If validation REST is not deployed yet, the section
  shows a soft empty state instead of failing the page.

Graph editing, enable/disable, and classic ZIP import/export remain later slices
(see detail **design gaps** when present).

## Product path — browse and lifecycle

1. Sign in (Admin required for Start / Stop, Test invoke, and Problems).
2. Open **Developer → Pipelines**, or deep-link
   `spa.jsp?entry=developer&section=pipelines`.
3. Open a listed application. Detail shows type, **Enabled**, **Running**,
   hidden, version, app root, and data sets.
4. As **Admin**, use **Start** when the application is enabled, not hidden, and
   not already running. Use **Stop** when it is running. Both actions are
   idempotent on the server (already running / already stopped returns success
   with refreshed **Running** state).
5. Start stays disabled while the application is **disabled**, **hidden**, or
   already **running**. Stop stays disabled while hidden or not running.
6. Non-Admin callers that hit the REST start/stop paths receive **403**. Hidden
   or disabled lifecycle attempts are **400**. Unknown applications are **404**.

## Product path — inspect pipe IR

1. On the same application detail, scroll to **Pipe IR**. Confirm **IR source**
   (`NATIVE` or `CLASSIC_IMPORT`) and **IR version**. Expand each IR resource for:
   - Stages present (page tank, backend tank, mapper, selector, pager, updater)
   - Backend tank tables (alias / table / datasource) and join count when present
   - Mapper mappings (document field ↔ backend column/extension)
   - Selector method / where-clause count and updater allow flags when present
2. Use **Back to list** to return to the catalog.

If IR cannot be loaded (for example **404** for an unknown app or missing IR),
the catalog detail still renders and the Pipe IR section shows an error — the
chrome does not echo the raw path name in that message.

## Product path — HTTP datasource and Test invoke

1. As **Admin**, open an application detail page.
2. In **HTTP datasource**, keep **Adapter** = **HTTP** and set **URL** to a
   loopback address or the bundled fixture
   `http://127.0.0.1/pipeline-http-fixture`. Enter a **resource** name in
   **Test invoke** (data-set names are offered when present).
3. Choose **Save HTTP tank**. Success shows a saved notice. Cloud URLs such as
   `https://erp.example/api/items` or URLs with userinfo fail closed with a
   clear **400** error.
4. In **Test invoke**, edit the **Request JSON** body (default
   `{ "params": {} }`) and choose **Invoke**. On success, the structured execute
   result JSON appears under **Execute result** with non-empty `rows` (or
   document fields). Invalid JSON, a blank resource, or server
   **400**/**404**/**500** responses show a clear error under the form.
5. Execute uses the native pipeline IR runtime — it does **not** call classic
   `PSQueryHandler` / `PSUpdateHandler`. HTTP execute never leaves loopback /
   the bundled local fixture.

## Product path — Problems

1. As **Admin**, open an application detail page. The **Problems** section loads
   automatically.
2. When validation REST is available, rows show severity, code, message, and
   optional resource/path. An empty list means the application validated with no
   errors or warnings.
3. When validation REST is **not** available (**404**), the section shows a soft
   empty message that validation is deferred — the rest of the detail page still
   works (including Test invoke, Pipe IR, and Start/Stop).
4. Non-Admin sessions do not see the Problems section. Admin callers without the
   role on the REST path receive **403**.

## Limits

- Catalog and detail omit **hidden** applications from the list contract used by
  this chrome; hidden rows are not started or stopped here.
- Pipe IR has no graph editor or drag-drop tanks. Admins may persist an HTTP
  backend tank (native IR overlay) only.
- Enable/disable and classic ZIP import/export are not in this chrome.
- Surface-filtered Playwright for Start/Stop lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-start-stop.spec.js`.
- Surface-filtered Playwright for pipe IR lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-pipe-ir.spec.js`.
- Surface-filtered Playwright for Test invoke (+ Problems soft-assert) lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-test-invoke.spec.js`.
- Surface-filtered Playwright for HTTP datasource save + Test invoke lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-http-execute.spec.js`.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/pipelines` (optional `name`, `limit`, `offset`) |
| Load | `GET /services/pipelines/{idOrName}` |
| Start | `POST /services/pipelines/{idOrName}/start` (**Admin**) |
| Stop | `POST /services/pipelines/{idOrName}/stop` (**Admin**) |
| Pipe IR | `GET /services/pipelines/{idOrName}/ir` |
| HTTP tank | `PUT /services/pipelines/{app}/resources/{resource}/backendTank` (**Admin**) |
| Test invoke | `POST /services/pipelines/{app}/resources/{resource}/execute` |
| Problems | `GET /services/pipelines/{idOrName}/validation` (**Admin**; soft-empty if absent) |

Successful start/stop responses return refreshed `ApplicationDetail` including
`active` (**Running** in the UI). Execute returns `PipelineExecuteResult`.
Validation returns `ApplicationValidationResult` with `problems[]` when present.
Integrator notes: [REST API — Pipelines](id:developer-rest).
