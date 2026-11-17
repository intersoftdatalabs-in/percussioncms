---
id: admin-developer-api-keys
title: Developer API Keys
description: Register Developer API keys and edit their scope sets with least-privilege presets from Developer API Keys chrome
version: "8.2"
order: 47
tags: [admin, developer, api-keys, security]
---

# Developer API Keys

**Developer → API Keys** registers named API keys and edits the scope set
each key holds. Scopes are product-defined (content, assets, design,
publishing, sites, admin); the registry stores only which scopes each named
key holds. There is no Workbench twin for API keys — this editor is the
system of record for key scopes.

Only callers with the **Admin** role can view or edit key scopes. Non-Admin
callers receive **403** on every endpoint.

## Least-privilege presets

The editor offers presets from least to most privilege. A preset only
selects scopes; it is never stored as a separate grant. When the draft scope
set exactly matches a preset, the editor labels the key with that preset —
any other combination is labelled **Custom**.

| Preset | Intended use |
|--------|--------------|
| **Read-only** | Dashboards and status checks; no writes |
| **Content author** | Create and update content and assets; no publish |
| **Publisher** | Author plus edition publish |
| **Site builder** | Publisher plus template and site design writes |
| **Integrator** | Content sync plus user directory reads; no publish, no design writes |
| **Administrator** | Every scope including deletes and system config |

Prefer the smallest preset that does the job. `system:config` appears only
in **Administrator**: it permits server configuration changes.

## Product path — register a key and save its scopes

1. Open **Developer → API Keys**. The catalog lists every registered key
   with its scope count and preset label.
2. Choose **New API key**, enter a key **name** (a letter, then letters,
   digits, dash, underscore, or dot) and an optional **label**, pick a
   preset, then **Save scopes**. The key appears only after the server
   accepts the save (`PUT /services/developer/apikeys/keys/{name}`).
3. Open the key to edit: toggle individual scopes grouped by area, or apply
   a preset to replace the draft set. **Save scopes** persists the draft;
   **Reset** restores the stored set without calling the server.
4. Reloading the page reads the stored assignment back from
   `GET /services/developer/apikeys/keys/{name}` — a saved key keeps its
   scopes and preset label across sessions.

A blank or unsafe key name is rejected before any request. A body name that
does not match the path name, and any scope outside the catalog
(`GET /services/developer/apikeys/scopes`), is **400** and nothing is
stored. A key that was never registered reads back **404**.

## REST

- `GET /services/developer/apikeys/scopes` — assignable scope catalog
- `GET /services/developer/apikeys/presets` — least-privilege presets
- `GET /services/developer/apikeys/keys` — all assignments, name-sorted
- `GET /services/developer/apikeys/keys/{name}` — one assignment (**404** when unregistered)
- `PUT /services/developer/apikeys/keys/{name}` — Admin create-or-replace of one key's scope set (**400** unknown scope, **403** non-Admin)

PUT bodies use the Jackson root wrapper (`{"ApiKeyAssignment": {...}}`).
Assignments persist in the server registry file
(`rxconfig/ApiKeyScopes/apikey-scopes.json` under the deploy dir), written
atomically; a corrupted registry reads back empty rather than failing the
catalog.
