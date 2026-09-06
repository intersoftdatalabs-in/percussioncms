---
id: admin-developer-database-explorer
title: Developer Database Explorer
description: Admin REST browse of allow-listed JDBC datasources and tables/views (not File Explorer)
version: "8.2"
order: 52
tags: [admin, developer, database-explorer]
---

# Developer Database Explorer

**Database Explorer** browse (classic Workbench supporting navigator) is an **Admin**
catalog of **allow-listed** JDBC datasources. Operators open it from
**Developer → Database Explorer**. Listing is **read-only**. SQL, DDL, and DBA
write tools are not available on this surface.

This is **not** Developer File Explorer (server filesystem browse).

## Configure allow-listed datasources

In `rxconfig/Server/server.properties`:

```properties
databaseExplorer.allowListedDatasources=cms=repository
```

- Each entry is `id` or `id=cmsDatasourceName`, separated by `;`.
- `id` is the catalog token used in REST (`letters`, digits, `_`, `-`; starts with a letter).
- The reserved name `repository` maps to the CMS repository datasource (H2 in QA cells).
- A bare `id` (no `=`) uses that same token as the CMS datasource configuration name.
- Entries that are not path-safe (contain `/`, `..`, `:`, or JDBC URL text) are ignored.
- If the property is missing or empty, `GET /services/databaseexplorer` returns an empty list
  — the server does not catalog JDBC metadata by default.

Restart the CMS after changing the property.

Responses never include JDBC URLs, usernames, or passwords.

## Developer SPA

1. Sign in as **Admin**.
2. Open **Developer** and select the **Database Explorer** tab
   (`/cm/app/developer/database-explorer`).
3. The table lists configured datasources: display name, catalog `id`, kind
   (Repository / Datasource), and **Available**.
4. Open a datasource to list **TABLE** and **VIEW** objects (name, type, schema).
5. If no datasources are configured, the panel is empty until
   `databaseExplorer.allowListedDatasources` is set and the CMS is restarted.

Non-Admin users receive **403** from REST; the panel shows a load error.

## REST (Admin)

| Action | Request |
|--------|---------|
| List datasources | `GET /services/databaseexplorer` |
| List tables/views | `GET /services/databaseexplorer/{datasourceId}/tables` |

Unsafe or **non-allow-listed** catalog ids are **400**. An allow-listed id whose
CMS datasource is missing is **404**. Responses never include the JDBC URL or
credentials.

Non-Admin is **403**.

Integrator notes: [REST API](id:developer-rest).
