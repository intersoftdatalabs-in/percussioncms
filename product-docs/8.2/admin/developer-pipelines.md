---
id: admin-developer-pipelines
title: Developer Pipelines
description: Browse classic XML Applications, Admin start/stop, Test invoke, and Problems from Developer Pipelines chrome
version: "8.2"
order: 51
tags: [admin, developer, pipelines]
---

# Developer Pipelines

**Developer → Pipelines** lists classic **XML Applications** (data pipeline
packages) visible to the current security token. Open a row for application
metadata and the **data set** catalog (request pages / content editors).

**Admins** can **Start** or **Stop** a non-hidden application from the detail
toolbar. Those actions peer the server console `start application` /
`stop application` commands and call Admin REST
`POST /services/pipelines/{idOrName}/start` and
`POST /services/pipelines/{idOrName}/stop`. Non-Admin sessions do not see the
lifecycle controls.

**Admins** also get **Test invoke** and **Problems** on the same detail page:

- **Test invoke** posts sample JSON (`params` / `rows`) to
  `POST /services/pipelines/{app}/resources/{resource}/execute` and shows the
  structured execute result (or a clear error). Use a data-set name as the
  resource when the application has native pipe IR.
- **Problems** loads Admin `GET /services/pipelines/{idOrName}/validation` when
  that endpoint is present. If validation REST is not deployed yet, the section
  shows a soft empty state instead of failing the page.

Enable/disable, pipe IR / mapper tanks editors, and design import/export remain
later slices (see detail **design gaps** when present).

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

## Product path — Test invoke

1. As **Admin**, open an application detail page.
2. In **Test invoke**, enter a **resource** name (data-set names are offered
   when present) and edit the **Request JSON** body (default
   `{ "params": {} }`).
3. Choose **Invoke**. On success, the structured execute result JSON appears
   under **Execute result**. Invalid JSON, a blank resource, or server
   **400**/**404**/**500** responses show a clear error under the form.
4. Execute uses the native pipeline IR runtime — it does **not** call classic
   `PSQueryHandler` / `PSUpdateHandler`. Applications without native IR for the
   named resource return an error from the server.

## Product path — Problems

1. As **Admin**, open an application detail page. The **Problems** section loads
   automatically.
2. When validation REST is available, rows show severity, code, message, and
   optional resource/path. An empty list means the application validated with no
   errors or warnings.
3. When validation REST is **not** available (**404**), the section shows a soft
   empty message that validation is deferred — the rest of the detail page still
   works (including Test invoke and Start/Stop).
4. Non-Admin sessions do not see the Problems section. Admin callers without the
   role on the REST path receive **403**.

## Limits

- Catalog and detail omit **hidden** applications from the list contract used by
  this chrome; hidden rows are not started or stopped here.
- Mapper/tank editing, enable/disable, and IR write are not in this chrome.
- Surface-filtered Playwright for Start/Stop lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-start-stop.spec.js`.
  Test-invoke (+ Problems soft-assert) lives under
  `modules/perc-qa-automation/frontend/tests/developer-pipelines-test-invoke.spec.js`.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/pipelines` |
| Load | `GET /services/pipelines/{idOrName}` |
| Start | `POST /services/pipelines/{idOrName}/start` (**Admin**) |
| Stop | `POST /services/pipelines/{idOrName}/stop` (**Admin**) |
| Test invoke | `POST /services/pipelines/{app}/resources/{resource}/execute` |
| Problems | `GET /services/pipelines/{idOrName}/validation` (**Admin**; soft-empty if absent) |

Successful start/stop responses return refreshed `ApplicationDetail` including
`active` (**Running** in the UI). Execute returns `PipelineExecuteResult`.
Validation returns `ApplicationValidationResult` with `problems[]` when present.
Integrator notes: [REST API](id:developer-rest).
