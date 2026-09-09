---
id: admin-developer-pipelines
title: Developer Pipelines
description: Browse classic XML Applications, Admin start/stop, pipe IR, OpenAPI from resources, HTTP datasource, nested filter groups, webhook hooks, binary resource retrieve, request tracing, Test invoke, and Problems from Developer Pipelines chrome
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

The **OpenAPI** section calls `GET /services/pipelines/{idOrName}/openapi`
(default YAML; JSON via `format=json`) and lets operators **view** or
**download** an OpenAPI 3 document generated from the application's IR
resources. It does not publish to an external registry.

**Admins** also get **HTTP datasource**, **nested filter groups**, **HTTP webhook hooks**,
**binary resource**, **request tracing**, **Test invoke**, and **Problems** on the same detail page:

- **HTTP datasource** sets `adapterType=HTTP` and a **loopback / local fixture
  URL** (default `http://127.0.0.1/pipeline-http-fixture`) on the selected
  resource via `PUT /services/pipelines/{app}/resources/{resource}/backendTank`.
  Cloud hosts, credentials in the URL, and non-http(s) schemes return **400**.
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
- **Nested filter groups** save an AND/OR tree of selector predicates on the
  selected native resource via
  `PUT /services/pipelines/{app}/resources/{resource}/filterGroup`. The default
  editor is `(sku = SKU-1) AND (qty = 3 OR qty = 99)` so Test invoke against the
  bundled HTTP fixture returns **SKU-1** only. Empty columns and other malformed
  groups fail closed (**400**). If the resource already has a non-local HTTP
  backend or leftover credentials in the URL, save is **400**. Classic XML
  Applications are not rewritten.
- **Request tracing** turns last-trace capture on or off for the application
  via `PUT /services/pipelines/{idOrName}/tracing`. After **Test invoke**,
  **Last trace** loads `GET …/lastTrace` and shows stages with timings.
  Passwords, tokens, and `Authorization` values are **redacted** (`[REDACTED]`);
  result rows are never shown on the trace. Disabling tracing clears last-trace.
- **Test invoke** posts sample JSON (`params` / `rows`) to
  `POST /services/pipelines/{app}/resources/{resource}/execute` and shows the
  structured execute result (or a clear error). HTTP tanks return mapped JSON
  `rows` (for example `sku` / `name` from the bundled fixture) — not empty
  invented data. When webhook hooks are saved, the result includes real
  `preWebhookStatus` / `postWebhookStatus` and body snippets (for example
  `hook-ok`) from the fixture — not a fake success.
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
  backend tank, nested selector filter groups, HTTP webhook hooks, and a binary
  resource (native IR overlay) only.
- Enable/disable and classic ZIP import/export are not in this chrome.
- Surface-filtered Playwright for Start/Stop lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-start-stop.spec.js`.
- Surface-filtered Playwright for pipe IR lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-pipe-ir.spec.js`.
- Surface-filtered Playwright for Test invoke (+ Problems soft-assert) lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-test-invoke.spec.js`.
- Surface-filtered Playwright for HTTP datasource save + Test invoke lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-http-execute.spec.js`.
- Surface-filtered Playwright for webhook hooks save + Test invoke lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-webhook-hooks.spec.js`.
- Surface-filtered Playwright for nested filter groups save + Test invoke lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-nested-filter-groups.spec.js`.
- Surface-filtered Playwright for binary resource save + retrieve lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-binary-resource.spec.js`.
- Surface-filtered Playwright for request tracing + last-trace lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-request-tracing.spec.js`.
- Surface-filtered Playwright for OpenAPI view/download lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-openapi.spec.js`
  (prefers `sys_cmp*` IR/execute apps, or `PIPELINE_APP_NAME`; does not require
  OpenAPI execute paths on empty-IR first-catalog rows).

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/pipelines` (optional `name`, `limit`, `offset`) |
| Load | `GET /services/pipelines/{idOrName}` |
| Start | `POST /services/pipelines/{idOrName}/start` (**Admin**) |
| Stop | `POST /services/pipelines/{idOrName}/stop` (**Admin**) |
| Pipe IR | `GET /services/pipelines/{idOrName}/ir` |
| OpenAPI | `GET /services/pipelines/{idOrName}/openapi` (`format=yaml` default, or `json`) |
| HTTP tank | `PUT /services/pipelines/{app}/resources/{resource}/backendTank` (**Admin**) |
| Webhook hooks | `PUT /services/pipelines/{app}/resources/{resource}/webhookHooks` (**Admin**) |
| Filter groups | `PUT /services/pipelines/{app}/resources/{resource}/filterGroup` (**Admin**) |
| Binary resource | `PUT /services/pipelines/{app}/resources/{resource}/binaryResource` (**Admin**) |
| Retrieve binary | `GET /services/pipelines/{app}/resources/{resource}/binary` |
| Request tracing | `PUT` / `GET /services/pipelines/{idOrName}/tracing` (**Admin**) |
| Last trace | `GET /services/pipelines/{idOrName}/lastTrace` (**Admin**) |
| Test invoke | `POST /services/pipelines/{app}/resources/{resource}/execute` |
| Problems | `GET /services/pipelines/{idOrName}/validation` (**Admin**; soft-empty if absent) |

Successful start/stop responses return refreshed `ApplicationDetail` including
`active` (**Running** in the UI). Execute returns `PipelineExecuteResult`.
Validation returns `ApplicationValidationResult` with `problems[]` when present.
Integrator notes: [REST API — Pipelines](id:developer-rest).
