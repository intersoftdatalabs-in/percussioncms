---
id: admin-developer-pipelines
title: Developer Pipelines
description: Browse classic XML Applications, Admin start/stop, Admin enable/disable, Admin delete, pipe IR, OpenAPI from resources, HTTP datasource, JDBC datasource backend, nested filter groups, field mappings, webhook hooks, binary resource retrieve, HTML result pages or raw JSON/XML, request tracing, Test invoke, and Problems from Developer Pipelines chrome
version: "8.2"
order: 51
tags: [admin, developer, pipelines]
---

# Developer Pipelines

**Developer → Pipelines** lists classic **XML Applications** (data pipeline
packages) visible to the current security token. Open a row for application
metadata, the **data set** catalog (request pages / content editors), and a
**pipe IR** summary (resources, stage presence, backend tanks, mapper mappings,
imported **result pages**) so operators can inspect structure without the Swing
E2Designer.

**Admins** can **Start** or **Stop** a non-hidden application from the detail
toolbar, **Enable** or **Disable** it via the same toolbar, and **Delete** it
(with confirm) from the same toolbar. Those
actions peer the server console `start application` /
`stop application` commands, the classic save-with-`setEnabled` behavior, and
the classic `delete application` behavior,
calling Admin REST `POST /services/pipelines/{idOrName}/start`,
`POST /services/pipelines/{idOrName}/stop`,
`PUT /services/pipelines/{idOrName}/enabled`, and
`DELETE /services/pipelines/{idOrName}`. Disabling a running application
stops it on save; enabling starts it on save. Non-Admin sessions do not see the
lifecycle controls.

The IR section calls `GET /services/pipelines/{idOrName}/ir`. When a native IR
file exists under `ObjectStore/pipeline-ir/`, that document is shown (`source`
`NATIVE`). Otherwise the server imports the classic application into IR **in
memory** (`source` `CLASSIC_IMPORT`) until an Admin saves a stage binding — an
HTTP backend tank, field mappings, or any sibling native PUT — which writes
native IR without rewriting classic XML Applications.
Classic **result pages** (`PSResultPage` / `PSResultPageSet`) import onto
`resources[].resultPages[]` (request extension, MIME type, stylesheet URI).
Relative application sheets such as `file:login.xsl` become `login.xsl`. Path
traversal (`..`), absolute customer paths, and cloud stylesheet URLs are
**skipped** (not imported). Developer chrome lists those imported pages as
read-only inspect; saving a native HTML result page is a separate Admin PUT
and still does not rewrite classic XML.

The **OpenAPI** section calls `GET /services/pipelines/{idOrName}/openapi`
(default YAML; JSON via `format=json`) and lets operators **view** or
**download** an OpenAPI 3 document generated from the application's IR
resources. It does not publish to an external registry.

**Admins** also get **HTTP datasource**, **JDBC datasource backend**, **nested filter groups**, **field mappings**, **HTTP webhook hooks**,
**binary resource**, **result page (HTML)**, **request tracing**, **Test invoke**, and **Problems** on the same detail page:

- **HTTP datasource** sets `adapterType=HTTP` and a **loopback / local fixture
  URL** (default `http://127.0.0.1/pipeline-http-fixture`) on the selected
  resource via `PUT /services/pipelines/{app}/resources/{resource}/backendTank`.
  Cloud hosts, credentials in the URL, and non-http(s) schemes return **400**.
- **JDBC datasource backend** binds a **named allow-listed datasource id**
  (for example `cms`; never a JDBC URL) on the selected resource via
  `PUT /services/pipelines/{app}/resources/{resource}/datasourceBackend`.
  The server connection-tests before persist: a failed test returns **502**
  and nothing is saved. `POST …/datasourceBackend/test` tests without
  persisting; `DELETE …/datasourceBackend` removes the binding (**404** when
  none). Resources with an HTTP tank return **409**.
- **HTTP webhook hooks** attach **pre-execute** and **post-execute** HTTP POST
  URLs on the same native resource via
  `PUT /services/pipelines/{app}/resources/{resource}/webhookHooks`. Only
  loopback / local fixture URLs are accepted (default
  `http://127.0.0.1/pipeline-webhook-fixture`). Cloud hosts, credentials, and
  non-http(s) schemes return **400**. A **blank URL skips** that hook (no
  invented delivery).
- **Binary resource** attaches a **portable-safe local fixture path** and
  **content type** via
  `PUT /services/pipelines/{app}/resources/{resource}/binaryResource`. The
  bundled token is `pipeline-binary-fixture` (classpath bytes, no internet).
  Cloud URLs, credentials in the path, and path traversal return **400**.
  **Retrieve bytes** calls `GET …/binary` and shows the fixture bytes
  (`PIPE-BIN-FIXTURE`) with the IR content type. A missing or empty fixture is
  **404** — the server does **not** invent content. Retrieve is capped at 1 MB
  by default; operators may raise the cap with JVM system property
  `perc.pipeline.binary.maxBodyBytes` (positive integer, bytes).
- **Result page (HTML)** attaches a **request extension** (`.html`) and a
  **local stylesheet URI** via
  `PUT /services/pipelines/{app}/resources/{resource}/resultPage`. The bundled
  token is `pipeline-xsl-result-fixture` (classpath XSL, no internet). Cloud
  URLs, credentials in the path, and path traversal return **400**. **Test HTML**
  posts `requestExtension=.html` to execute and shows the merged HTML marker
  `PIPE-XSL-HTML` (plus fixture row fields such as `SKU-1`) — not raw JSON.
  Missing stylesheets fail closed; HTML is never invented. **Use raw structured
  output** sends `presentation=none` so HTML Test invoke is untransformed.
  **Test JSON** (`.json` / `Accept: application/json`) **never** applies XSL
  even when a result page remains. **Test XML** returns untransformed row XML.
  **Legacy stylesheets:** classic definitions carry app-local sheets as
  `file:pages/result.xsl`. The save endpoint accepts that form and stores the
  portable relative URI (`pages/result.xsl`). Operators stage app-owned sheets
  as files under the pipeline IR store app directory
  (`<pipeline-store>/<appName>/pages/result.xsl`); at execute time the runtime
  resolves the bound URI under that directory and merges it the same way as
  the bundled fixture (byte-identical output for the same stylesheet and row
  document). Absolute paths, traversal, and cloud URLs still return **400**.
- **Nested filter groups** save an AND/OR tree of selector predicates on the
  selected native resource via
  `PUT /services/pipelines/{app}/resources/{resource}/filterGroup`. The default
  editor is `(sku = SKU-1) AND (qty = 3 OR qty = 99)` so Test invoke against the
  bundled HTTP fixture returns **SKU-1** only. Empty columns and other malformed
  groups fail closed (**400**). If the resource already has a non-local HTTP
  backend or leftover credentials in the URL, save is **400**. Classic XML
  Applications are not rewritten.
- **Field mappings** save ordered **document field → backend field** rows on the
  selected native resource via
  `PUT /services/pipelines/{app}/resources/{resource}/mapper`. Each row needs a
  document field and a backend field; the kind is `COLUMN`, `EXTENSION`, or
  `OTHER` (blank normalizes to `OTHER`). An empty row list **clears** the stage.
  Saving rows rewrites the resource's mapper stage without touching classic XML
  Applications, and Test invoke projects HTTP fixture rows through the saved
  mappings (for example `itemSku` / `itemName` keys instead of the raw `sku` /
  `name` fixture keys).
- **Request tracing** turns last-trace capture on or off for the application
  via `PUT /services/pipelines/{idOrName}/tracing`. After **Test invoke**,
  **Last trace** loads `GET …/lastTrace` and shows stages with timings.
  Passwords, tokens, and `Authorization` values are **redacted** (`[REDACTED]`);
  result rows are never shown on the trace. Disabling tracing clears last-trace.
- **Test invoke** posts sample JSON (`params` / `rows`) to
  `POST /services/pipelines/{app}/resources/{resource}/execute` and shows the
  structured execute result (or a clear error). HTTP tanks return mapped JSON
  `rows` (for example `sku` / `name` from the bundled fixture) — not empty
  invented data. **Test HTML** asks for `.html` so a bound result page merges
  those rows through XSL unless presentation is `none`. **Test JSON** / **Test
  XML** skip XSL. When webhook hooks are saved, the result includes real
  `preWebhookStatus` / `postWebhookStatus` and body snippets (for example
  `hook-ok`) from the fixture — not a fake success.
- **Problems** loads Admin `GET /services/pipelines/{idOrName}/validation` when
  that endpoint is present. If validation REST is not deployed yet, the section
  shows a soft empty state instead of failing the page.

Graph editing and auxiliary-file transfer remain later slices
(see detail **design gaps** when present).

## Product path — browse and lifecycle

1. Sign in (Admin required for Start / Stop, Enable / Disable, Test invoke,
   and Problems).
2. Open **Developer → Pipelines**, or deep-link
   `spa.jsp?entry=developer&section=pipelines`.
3. Open a listed application. Detail shows type, **Enabled**, **Running**,
   hidden, version, app root, and data sets.
4. As **Admin**, use **Start** when the application is enabled, not hidden, and
   not already running. Use **Stop** when it is running. Both actions are
   idempotent on the server (already running / already stopped returns success
   with refreshed **Running** state).
5. Use **Disable** when the application is enabled and not hidden; use
   **Enable** when it is disabled and not hidden. The flag persists through the
   classic object store, so the server stops a running application on disable
   and starts it on enable. A no-change save is a no-op that still returns the
   refreshed detail.
6. Start stays disabled while the application is **disabled**, **hidden**, or
   already **running**. Stop stays disabled while hidden or not running.
   Enable stays disabled while **hidden** or already **enabled**; Disable stays
   disabled while **hidden** or already **disabled**.
7. Non-Admin callers that hit the REST start/stop/enabled paths receive **403**.
   Hidden lifecycle attempts are **400**. Unknown applications are **404**.

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

## Product path — view and download OpenAPI

1. On the same application detail, scroll to **OpenAPI**. The section is always
   on the detail page (it is not limited to the first catalog row). Apps such as
   `sys_ActionPage` may have data sets but **no IR resources**, so the document
   can omit execute paths — pick a content-editor app (`sys_cmp…`) or another
   application that lists IR resources when you need a documented
   `POST …/execute` path.
2. Confirm the document starts with OpenAPI 3 and includes at least one
   `POST /pipelines/{app}/resources/{resource}/execute` path when the
   application has IR resources.
3. Optionally switch **YAML** / **JSON**, choose **View OpenAPI** to refresh, or
   **Download** to save `{app}.openapi.yaml` (or `.json`).
4. Hidden applications cannot be documented via this API (**400**). Unknown
   applications are **404**. Error chrome does not echo the raw path name.

Integrator notes: [REST API — Pipelines](id:developer-rest).

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

## Product path — JDBC datasource backend

1. As **Admin**, open an application detail page.
2. In **JDBC datasource backend**, enter a **Datasource id** — a named
   allow-listed catalog token such as `cms` (configured in
   `server.properties` as `databaseExplorer.allowListedDatasources`). This is
   never a JDBC URL: raw URLs, credentials, and non-allow-listed ids fail
   closed with a clear **400** error.
3. Choose **Test connection** to open a JDBC connection without persisting
   anything. Success shows the named test notice; a failed test names the
   datasource and persists **nothing** (never recorded as a success).
4. Choose **Save datasource backend** to create or update the binding. The
   server connection-tests before persist and fails closed — a failed test
   returns **502** and nothing is saved. A resource already backed by an HTTP
   tank returns **409** (the HTTP tank is never clobbered).
5. Choose **Remove datasource backend** to clear the binding. Removing a
   resource with no datasource backend is **404**.

## Product path — nested filter groups

1. As **Admin**, open an application detail page and enter a **resource** name
   in **Test invoke**.
2. In **Nested filter groups**, set the **root** AND/OR operator, a leaf
   predicate (column / operator / value), and a nested group of two predicates.
   The default tree is `(sku = SKU-1) AND (qty = 3 OR qty = 99)`.
3. Choose **Save filter groups**. Success shows a saved notice. A blank column
   or other malformed group fails closed in the chrome (or **400** from REST).
   A leftover credentialed URL or cloud HTTP backend on the same resource is
   **400**.
4. Save an **HTTP datasource** tank (bundled fixture) if needed, then
   **Invoke**. The execute result includes mapped fixture rows that match the
   nested predicate (`SKU-1`) and does **not** invent empty rows.

## Product path — field mappings

1. As **Admin**, open an application detail page and enter a **resource** name
   in **Test invoke** (data-set names are offered when present).
2. In **Field mappings**, set each row's **Document field** (the projected
   response property) and **Backend field** (the source column), with **Kind**
   `COLUMN`, `EXTENSION`, or `OTHER`. Use **Add mapping row** for more rows;
   the arrow buttons reorder rows and ✕ removes one. Removing every row and
   saving **clears** the stage.
3. Choose **Save field mappings**. Success shows a saved notice and the Pipe IR
   inspect table lists the saved rows. A row with a blank document or backend
   field fails closed in the chrome (or **400** from REST); an unknown kind is
   **400**. Classic XML Applications are not rewritten.
4. Save an **HTTP datasource** tank (bundled fixture) if needed, then
   **Invoke**. The execute result JSON shows rows projected through the saved
   mappings (for example `itemSku` / `itemName` keys) instead of the raw
   fixture keys.

## Product path — HTTP webhook hooks

1. As **Admin**, open an application detail page.
2. Enter a **resource** name in **Test invoke** (data-set names are offered when
   present).
3. In **HTTP webhook hooks**, set **Pre-execute URL** and/or **Post-execute URL**
   to a loopback address or the bundled fixture
   `http://127.0.0.1/pipeline-webhook-fixture`.
4. Choose **Save webhook hooks**. Success shows a saved notice. Cloud URLs such
   as `https://hooks.example/catch` or URLs with userinfo fail closed with a
   clear **400** error. If both URLs are blank, the chrome asks for at least
   one URL. On the server, a blank URL **skips** that hook (no delivery is
   invented).
5. Save an **HTTP datasource** tank (bundled HTTP fixture) so Test invoke can
   execute the native resource, then choose **Invoke**. The execute result JSON
   includes webhook `status` / body evidence (`hook-ok`, `pipeline-webhook`)
   from the local fixture.

## Product path — request tracing

1. As **Admin**, open an application detail page.
2. In **Request tracing**, check **Trace requests** and choose **Save tracing**.
   Success shows a saved notice.
3. Enter a **resource** name in **Test invoke** and save an **HTTP datasource**
   tank (bundled `http://127.0.0.1/pipeline-http-fixture`) so execute has rows.
4. Optionally put `password`, `token`, or `Authorization` in the **Request JSON**
   `params` object to confirm fail-closed redaction.
5. Choose **Invoke**. **Last trace** lists stages (`adapter`, hooks) with
   durations and a JSON snapshot. Secret values appear as `[REDACTED]`, never
   the original password/token/Authorization strings.

## Product path — binary resource retrieve

1. As **Admin**, open an application detail page.
2. Enter a **resource** name in **Test invoke** (use a dedicated name such as
   `binaryFixture` so you do not convert an existing query resource).
3. In **Binary resource**, keep **Fixture path** as `pipeline-binary-fixture`
   (or another portable-safe relative path) and set **Content type** (default
   `text/plain`).
4. Choose **Save binary resource**. Success shows a saved notice. Cloud URLs
   such as `https://cdn.example/blob.bin`, credentialed URLs, and `..`
   traversal fail closed with a clear **400** error.
5. Choose **Retrieve bytes**. The preview shows the local fixture marker
   `PIPE-BIN-FIXTURE` (not invented content). Missing or empty files are
   **404**. Optionally **Download** the retrieved bytes.

## Product path — HTML result page

1. As **Admin**, open an application detail page.
2. Enter a **resource** name in **Test invoke** (use a dedicated name such as
   `xslHtmlFixture`).
3. In **HTTP datasource**, keep the bundled loopback URL
   `http://127.0.0.1/pipeline-http-fixture` and choose **Save HTTP tank** so
   Test HTML has fixture rows to merge.
4. In **Result page (HTML)**, keep **Stylesheet URI** as
   `pipeline-xsl-result-fixture` (or another portable-safe relative path). A
   legacy classic value such as `file:pages/result.xsl` is accepted and
   normalized to `pages/result.xsl` before save; app-owned sheets must exist as
   files under the pipeline store app directory
   (`<pipeline-store>/<appName>/pages/result.xsl`) for **Test HTML** to merge
   them.
5. Choose **Save result page**. Success shows a saved notice. Cloud URLs such as
   `https://cdn.example/result.xsl`, credentialed URLs, and `..` traversal fail
   closed with a clear **400** error.
6. Choose **Test HTML**. The HTML result shows the local fixture marker
   `PIPE-XSL-HTML` and fixture fields such as `SKU-1` (stylesheet merge, not
   raw JSON). Missing stylesheets fail closed.
7. Choose **Test JSON**. The structured result shows fixture rows (for example
   `SKU-1`) and **does not** wrap them in `PIPE-XSL-HTML`, even while the
   result page is still bound.
8. Choose **Use raw structured output**. Then **Test HTML** is untransformed
   (no HTML wrapper). **Test XML** shows a `<rows>` document without XSL.

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

## Product path — classic XML import/export

1. As **Admin**, open **Developer → Pipelines**. The catalog shows an
   **Import application XML** section (non-Admin sessions do not see it).
2. To back up or move one classic application, open its detail page and use
   **Export XML**. The download is the `PSXApplication` design XML
   (`GET /services/pipelines/{idOrName}/export`, `Content-Disposition`
   attachment named from the application). Export is read-only: it does not
   acquire or steal a design lock. Hidden applications cannot be exported
   (**400**); unknown names are **404**.
3. To import, choose a `PSXApplication` XML file in the catalog import
   section and optionally set **Import as** to a unique name (blank keeps the
   XML name), then **Import application**. Import is **create-only**: a name
   that already exists is rejected with **409** and nothing is overwritten.
   Imported names must be single-segment and portable across the
   Windows/Linux/macOS install base (no path separators or `"` `:` `*` `?`
   `<` `>` `|`). Malformed XML is **400**.
   An import whose design XML references auxiliary files that are not present
   on the server (for example a `sys_*` export whose DTD/app files were never
   copied over) fails closed with a named **400** and keeps no catalog row: a
   late server-init failure rolls the partial create back, so a retry is not
   a spurious **409**. Copy the referenced files into the application
   directory first — auxiliary-file transfer is not part of this API.
4. Non-Admin callers on either REST path receive **403**.

## Product path — delete application

1. As **Admin**, open the application detail from **Developer → Pipelines**.
2. Use **Delete** in the lifecycle toolbar (disabled while the application is
   **hidden**), then confirm. **Cancel** writes nothing and keeps the detail
   open.
3. On confirm the chrome calls `DELETE /services/pipelines/{idOrName}`.
   Success is **204**: the catalog row drops without a refetch and the
   application, its files, and its virtual directory are gone.
4. A **running** application is rejected with **409** — **Stop** it first,
   then delete. A design lock held by another session is also **409** (the
   delete never steals a lock). Hidden applications are **400**, unknown
   applications **404**, non-Admin callers **403**.

## Limits

- Catalog and detail omit **hidden** applications from the list contract used by
  this chrome; hidden rows are not started or stopped here.
- Pipe IR has no graph editor or drag-drop tanks. Admins may persist an HTTP
  backend tank, a JDBC datasource backend, nested selector filter groups, field
  mappings, HTTP webhook hooks, a binary
  resource, and an HTML result-page binding (native IR overlay) only.
- Enable/disable ships in this chrome (Admin toolbar toggle + REST below).
  Classic application transfer is XML export/import (above); auxiliary-file
  transfer is not supported.
- Surface-filtered Playwright for Start/Stop lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-start-stop.spec.js`.
- Surface-filtered Playwright for Enable/Disable lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-enabled.spec.js`.
- Surface-filtered Playwright for pipe IR lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-pipe-ir.spec.js`.
- Surface-filtered Playwright for Test invoke (+ Problems soft-assert) lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-test-invoke.spec.js`.
- Surface-filtered Playwright for HTTP datasource save + Test invoke lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-http-execute.spec.js`.
- Surface-filtered Playwright for webhook hooks save + Test invoke lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-webhook-hooks.spec.js`.
- Surface-filtered Playwright for JDBC datasource backend write lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-datasource-backend.spec.js`.
- Surface-filtered Playwright for nested filter groups save + Test invoke lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-nested-filter-groups.spec.js`.
- Surface-filtered Playwright for field mappings save + Test invoke projection lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-mapper-persist.spec.js`.
- Surface-filtered Playwright for binary resource save + retrieve lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-binary-resource.spec.js`.
- Surface-filtered Playwright for request tracing + last-trace lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-request-tracing.spec.js`.
- Surface-filtered Playwright for HTML result-page save + Test HTML lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-result-page-html.spec.js`.
  Classic import inspect lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-result-page-import.spec.js`.
  Raw JSON/XML (clear presentation) lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-result-page-raw.spec.js`.
- Surface-filtered Playwright for OpenAPI view/download lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-openapi.spec.js`
  (prefers `sys_cmp*` IR/execute apps, or `PIPELINE_APP_NAME`; does not require
  OpenAPI execute paths on empty-IR first-catalog rows).
- Surface-filtered Playwright for classic XML export + duplicate-import 409 lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-import-export.spec.js`.
- Surface-filtered Playwright for application delete (import copy, cancel keeps
  row, confirm drops row + GET 404) lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-delete.spec.js`.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/pipelines` (optional `name`, `limit`, `offset`) |
| Load | `GET /services/pipelines/{idOrName}` |
| Start | `POST /services/pipelines/{idOrName}/start` (**Admin**) |
| Stop | `POST /services/pipelines/{idOrName}/stop` (**Admin**) |
| Enable / Disable | `PUT /services/pipelines/{idOrName}/enabled` (**Admin**; `{enabled:boolean}`; disabling a running app stops it, enabling starts it) |
| Delete | `DELETE /services/pipelines/{idOrName}` (**Admin**; **204**; running is **409**, stop first) |
| Pipe IR | `GET /services/pipelines/{idOrName}/ir` |
| OpenAPI | `GET /services/pipelines/{idOrName}/openapi` (`format=yaml` default, or `json`) |
| HTTP tank | `PUT /services/pipelines/{app}/resources/{resource}/backendTank` (**Admin**) |
| Webhook hooks | `PUT /services/pipelines/{app}/resources/{resource}/webhookHooks` (**Admin**) |
| Filter groups | `PUT /services/pipelines/{app}/resources/{resource}/filterGroup` (**Admin**) |
| Field mappings | `PUT /services/pipelines/{app}/resources/{resource}/mapper` (**Admin**) |
| Binary resource | `PUT /services/pipelines/{app}/resources/{resource}/binaryResource` (**Admin**) |
| Retrieve binary | `GET /services/pipelines/{app}/resources/{resource}/binary` |
| Result page | `PUT /services/pipelines/{app}/resources/{resource}/resultPage` (**Admin**) |
| Request tracing | `PUT` / `GET /services/pipelines/{idOrName}/tracing` (**Admin**) |
| Last trace | `GET /services/pipelines/{idOrName}/lastTrace` (**Admin**) |
| Test invoke | `POST /services/pipelines/{app}/resources/{resource}/execute` |
| Problems | `GET /services/pipelines/{idOrName}/validation` (**Admin**; soft-empty if absent) |
| Export XML | `GET /services/pipelines/{idOrName}/export` (**Admin**; XML attachment) |
| Import XML | `POST /services/pipelines/import` (**Admin**; create-only, duplicate is **409**) |

Successful start/stop/enable/disable responses return refreshed
`ApplicationDetail` including `active` (**Running** in the UI) and the persisted
`enabled` flag. Execute returns `PipelineExecuteResult`.
Validation returns `ApplicationValidationResult` with `problems[]` when present.
Integrator notes: [REST API — Pipelines](id:developer-rest).
