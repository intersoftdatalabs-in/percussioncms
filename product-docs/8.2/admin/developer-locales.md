---
id: admin-developer-locales
title: Developer Locales
description: Create, save, and delete CMS locales from Developer Locales chrome
version: "8.2"
order: 43
tags: [admin, developer, locales]
---

# Developer Locales

**Developer → Locales** lists CMS locale definitions (language string, label,
status, base-locale flag, and whether an exact format profile exists). Admins
can **create**, **save**, and **delete** a locale from this chrome. The
language string is required on create and **cannot be renamed** later.

This is **not** the Workbench auto-translation set editor. Format-profile
(`RXLOCALEFORMAT`) rows remain **read-only** on the detail panel.

## Product path — create, save, delete

1. Sign in as **Admin** (write calls require the Admin role).
2. Open **Developer → Locales**, or deep-link
   `spa.jsp?entry=developer&section=locales`.
3. Click **New locale**. Enter a **language** string (for example `fr-ca`) and
   a **label**. Save stays disabled until both are present and the language
   string is valid. Optional: description, status (`active` / `inactive`), and
   **Base locale**.
4. Click **Save**. A duplicate language string is **409** and the editor shows
   that the locale already exists. After a successful create, the language
   field is read-only.
5. Change the label (or description / status / base flag) and **Save** again.
6. Click **Delete** and confirm. The catalog no longer lists that language.
   Delete of a missing locale is **404**. A locale with remaining dependents
   is **409**.

## Limits

- Language string is immutable after create (REST `PUT` rejects a change).
- Format-profile create/edit is not in this chrome (read of the exact row
  only).
- Auto-translation configuration is a separate surface and is not edited here.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/locales` |
| Load | `GET /services/locales/{idOrLang}` |
| Create | `POST /services/locales` (`languageString` and `label` required) |
| Save | `PUT /services/locales/{idOrLang}` (language immutable) |
| Delete | `DELETE /services/locales/{idOrLang}` (`204` on success) |

Integrator notes: [REST API — Locales](id:developer-rest).
