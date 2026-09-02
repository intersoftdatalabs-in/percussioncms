---
id: admin-developer-locales
title: Developer Locales
description: Create, save, and delete CMS locales and edit the auto-translation set from Developer Locales chrome
version: "8.2"
order: 43
tags: [admin, developer, locales]
---

# Developer Locales

**Developer → Locales** lists CMS locale definitions (language string, label,
status, base-locale flag, and whether an exact format profile exists). Admins
can **create**, **save**, and **delete** a locale from this chrome. The
language string is required on create and **cannot be renamed** later.

Admins can also **view and replace** the singleton **auto-translation** set
(locale × content-type rows, plus workflow and community) from this chrome.
Format-profile (`RXLOCALEFORMAT`) rows remain **read-only** on the locale
detail panel.

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
6. Click **Delete** and confirm in the in-app dialog (not a browser prompt).
   The catalog no longer lists that language.
   Delete of a missing locale is **404**. A locale with remaining dependents
   is **409**.

The singleton **auto-translation** set (locale × content-type rows) is Admin
**GET/PUT** `/services/locales/auto-translations`. **GET** returns every
existing `PSX_AUTOTRANSLATION` row (not an empty list when rows exist).
**PUT** replaces the full set; empty `[]` **clears** all rows. A design lock
held by **another** user is **409** (not **500**). A leftover lock from the
same Admin after a failed save is taken over so a retry can succeed.

## Product path — auto-translation set

1. Sign in as **Admin** (auto-translation GET/PUT require the Admin role).
2. Open **Developer → Locales**, or deep-link
   `spa.jsp?entry=developer&section=locales`.
3. Click **Auto-translations**. The table lists current locale × content-type
   rows (workflow and community on each row).
4. Click **Add row** and choose a locale, content type, workflow, and
   community (or type names that exist in the CMS). Remove a row with
   **Remove**. Duplicate locale × content-type rows cannot be saved
   (**400**).
5. Click **Save auto-translations**. Save **replaces the full set**. An empty
   table (remove every row, then save) **clears** all auto-translation rows.
6. Unknown locale or content type is **400** and the editor shows the error.
   A design lock held by another user is **409**.

## Limits

- Language string is immutable after create (REST `PUT` rejects a change).
- Format-profile create/edit is not in this chrome (read of the exact row
  only).
- Auto-translation save is a full replace of the singleton set via
  GET/PUT `/services/locales/auto-translations` (not a per-row PATCH). Empty
  list clears. There is no TMX bulk translate in this chrome.

## REST

The chrome calls:

| Action | Request |
|--------|---------|
| List | `GET /services/locales` |
| Load | `GET /services/locales/{idOrLang}` |
| Create | `POST /services/locales` (`languageString` and `label` required) |
| Save | `PUT /services/locales/{idOrLang}` (language immutable) |
| Delete | `DELETE /services/locales/{idOrLang}` (`204` on success) |
| Auto-translation list | `GET /services/locales/auto-translations` |
| Auto-translation replace | `PUT /services/locales/auto-translations` (empty `[]` clears) |

Integrator notes: [REST API — Locales](id:developer-rest).
