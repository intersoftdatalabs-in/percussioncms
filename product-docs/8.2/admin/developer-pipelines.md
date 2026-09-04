---
id: admin-developer-pipelines
title: Developer Pipelines
description: Browse classic XML Applications and Admin start/stop from Developer Pipelines chrome
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

Enable/disable, pipe IR / mapper tanks, and design import/export remain later
slices (see detail **design gaps** when present).

## Product path — browse and lifecycle

1. Sign in (Admin required for Start / Stop).
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

## Limits

- Catalog and detail omit **hidden** applications from the list contract used by
  this chrome; hidden rows are not started or stopped here.
- Pipe IR execute, mapper/tank editing, and enable/disable are not in this chrome.
- Playwright H2 proof for Start/Stop is a separate QA slice.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/pipelines` |
| Load | `GET /services/pipelines/{idOrName}` |
| Start | `POST /services/pipelines/{idOrName}/start` (**Admin**) |
| Stop | `POST /services/pipelines/{idOrName}/stop` (**Admin**) |

Successful start/stop responses return refreshed `ApplicationDetail` including
`active` (**Running** in the UI). Integrator notes: [REST API](id:developer-rest).
