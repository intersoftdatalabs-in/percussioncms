---
id: admin-developer-locales
title: Developer Locales
description: Create, save, and delete CMS locales, set a label, description, date pattern, time pattern, date-time pattern, decimal separator, grouping separator, or currency code, edit format profiles, and edit the auto-translation set from Developer Locales chrome
version: "8.2"
order: 43
tags: [admin, developer, locales]
---

# Developer Locales

**Developer → Locales** lists CMS locale definitions (language string, label,
status, base-locale flag, and whether an exact format profile exists). Admins
can **create**, **save**, and **delete** a locale from this chrome. The
language string is required on create and **cannot be renamed** later.

**Set locale label** on an existing locale saves the label only. The
update does not send the language string, description, enabled status, or
format profile, so those stay. The language string is immutable. The new
label shows only after that save succeeds. A blank or whitespace label
does not write. **Cancel** does not write. HTTP 400, 403, and 409 are not
success and leave the previous label.

**Set locale description** on an existing locale saves the description only.
The update does not send the language string, label, enabled status, or
format profile, so those stay. The language string is immutable. The new
description shows only after that save succeeds. A blank description
clears the description. **Cancel** does not write. HTTP 400, 403, and 409
are not success and leave the previous description.

**Set locale date pattern** on an existing locale saves the date pattern
only. The update sends `format.datePattern` and does not send the language
string, label, description, enabled status, or other format fields, so
those stay. The language string is immutable. The new pattern shows only
after that save succeeds. A blank date pattern clears the date pattern.
**Cancel** does not write. HTTP 400, 403, and 409 are not success and
leave the previous pattern.

**Set locale time pattern** on an existing locale saves the time pattern
only. The update sends `format.timePattern` and does not send the language
string, label, description, enabled status, or other format fields, so
those stay. The language string is immutable. The new pattern shows only
after that save succeeds. A blank time pattern clears the time pattern.
**Cancel** does not write. HTTP 400, 403, and 409 are not success and
leave the previous pattern.

**Set locale date-time pattern** on an existing locale saves the
date-time pattern only. The update sends `format.dateTimePattern` and does
not send the language string, label, description, enabled status, or other
format fields, so those stay. The language string is immutable. The new
pattern shows only after that save succeeds. A blank date-time pattern
clears the date-time pattern. **Cancel** does not write. HTTP 400, 403, and
409 are not success and leave the previous pattern.

**Set locale decimal separator** on an existing locale saves the decimal
separator only. The update sends `format.decimalSep` and does not send the
language string, label, description, enabled status, date/time patterns,
grouping separator, or other format fields, so those stay. The language
string is immutable. The new separator shows only after that save succeeds.
A blank decimal separator clears the decimal separator. **Cancel** does not
write. HTTP 400, 403, and 409 are not success and leave the previous
separator.

**Set locale grouping separator** on an existing locale saves the grouping
separator only. The update sends `format.groupingSep` and does not send the
language string, label, description, enabled status, date/time patterns,
decimal separator, or other format fields, so those stay. The language
string is immutable. The new separator shows only after that save succeeds.
A blank grouping separator clears the grouping separator. **Cancel** does not
write. HTTP 400, 403, and 409 are not success and leave the previous
separator.

**Set locale currency code** on an existing locale saves the currency code
only. The update sends `format.currencyCode` and does not send the language
string, label, description, enabled status, date/time patterns, decimal
separator, grouping separator, or other format fields, so those stay. The
language string is immutable. The new code shows only after that save
succeeds. A blank currency code clears the currency code. **Cancel** does not
write. HTTP 400, 403, and 409 are not success and leave the previous code.

Admins can **create, update, and clear** a locale **format profile**
(`RXLOCALEFORMAT`: date/number/currency/text direction and related fields)
on the locale detail panel. Format save uses the same locale **Save** action
(no separate lock). Clearing the stored row is **uncheck Store format
profile** and **Save**.

Admins can also **view and replace** the singleton **auto-translation** set
(locale × content-type rows, plus workflow and community) from this chrome.

## Product path — set a label

1. Open an existing locale. The committed label is shown under **Set
   locale label**.
2. Click **Set locale label**, edit the text, and click **Save label**.
   The request is `PUT /services/locales/{idOrLang}` with `label` only.
   Reload shows that label. The language string, description, enabled
   status, and format profile are unchanged.
3. A blank or whitespace label is not written. The same label is not
   written.
4. Click **Cancel** before save to keep the previous label. Cancel does
   not call the server.
5. HTTP 400, 403, and 409 do not show **Locale label saved** and do not
   replace the previous label.

## Product path — set a description

1. Open an existing locale. The committed description is shown under **Set
   locale description**.
2. Click **Set locale description**, edit the text, and click **Save
   description**. The request is `PUT /services/locales/{idOrLang}` with
   `description` only. Reload shows that description. The language string,
   label, enabled status, and format profile are unchanged.
3. A blank description clears the stored description. The same description is
   not written.
4. Click **Cancel** before save to keep the previous description. Cancel does
   not call the server.
5. HTTP 400, 403, and 409 do not show **Locale description saved** and do not
   replace the previous description.

## Product path — set a date pattern

1. Open an existing locale. The committed date pattern is shown under
   **Set locale date pattern**.
2. Click **Set locale date pattern**, edit the text, and click **Save date
   pattern**. The request is `PUT /services/locales/{idOrLang}` with
   `format.datePattern` only. Reload shows that pattern. The language
   string, label, description, enabled status, and other format fields
   (time pattern, separators, currency, text direction) are unchanged.
3. A blank date pattern clears the stored date pattern. The same pattern
   is not written.
4. Click **Cancel** before save to keep the previous pattern. Cancel does
   not call the server.
5. HTTP 400, 403, and 409 do not show **Locale date pattern saved** and do
   not replace the previous pattern.

## Product path — set a time pattern

1. Open an existing locale. The committed time pattern is shown under
   **Set locale time pattern**.
2. Click **Set locale time pattern**, edit the text, and click **Save time
   pattern**. The request is `PUT /services/locales/{idOrLang}` with
   `format.timePattern` only. Reload shows that pattern. The language
   string, label, description, enabled status, and other format fields
   (date pattern, separators, currency, text direction) are unchanged.
3. A blank time pattern clears the stored time pattern. The same pattern
   is not written.
4. Click **Cancel** before save to keep the previous pattern. Cancel does
   not call the server.
5. HTTP 400, 403, and 409 do not show **Locale time pattern saved** and do
   not replace the previous pattern.

## Product path — set a date-time pattern

1. Open an existing locale. The committed date-time pattern is shown under
   **Set locale date-time pattern**.
2. Click **Set locale date-time pattern**, edit the text, and click **Save
   date-time pattern**. The request is `PUT /services/locales/{idOrLang}`
   with `format.dateTimePattern` only. Reload shows that pattern. The
   language string, label, description, enabled status, and other format
   fields (date pattern, time pattern, separators, currency, text
   direction) are unchanged.
3. A blank date-time pattern clears the stored date-time pattern. The same
   pattern is not written.
4. Click **Cancel** before save to keep the previous pattern. Cancel does
   not call the server.
5. HTTP 400, 403, and 409 do not show **Locale date-time pattern saved**
   and do not replace the previous pattern.

## Product path — set a decimal separator

1. Open an existing locale. The committed decimal separator is shown under
   **Set locale decimal separator**.
2. Click **Set locale decimal separator**, edit the text, and click **Save
   decimal separator**. The request is `PUT /services/locales/{idOrLang}`
   with `format.decimalSep` only. Reload shows that separator. The language
   string, label, description, enabled status, date/time patterns, grouping
   separator, and other format fields are unchanged.
3. A blank decimal separator clears the stored decimal separator. The same
   separator is not written.
4. Click **Cancel** before save to keep the previous separator. Cancel does
   not call the server.
5. HTTP 400, 403, and 409 do not show **Locale decimal separator saved**
   and do not replace the previous separator.

## Product path — set a grouping separator

1. Open an existing locale. The committed grouping separator is shown under
   **Set locale grouping separator**.
2. Click **Set locale grouping separator**, edit the text, and click **Save
   grouping separator**. The request is `PUT /services/locales/{idOrLang}`
   with `format.groupingSep` only. Reload shows that separator. The language
   string, label, description, enabled status, date/time patterns, decimal
   separator, and other format fields are unchanged.
3. A blank grouping separator clears the stored grouping separator. The same
   separator is not written.
4. Click **Cancel** before save to keep the previous separator. Cancel does
   not call the server.
5. HTTP 400, 403, and 409 do not show **Locale grouping separator saved**
   and do not replace the previous separator.

## Product path — set a currency code

1. Open an existing locale. The committed currency code is shown under
   **Set locale currency code**.
2. Click **Set locale currency code**, edit the text, and click **Save
   currency code**. The request is `PUT /services/locales/{idOrLang}` with
   `format.currencyCode` only. Reload shows that code. The language string,
   label, description, enabled status, date/time patterns, decimal
   separator, grouping separator, and other format fields are unchanged.
3. A blank currency code clears the stored currency code. The same code is
   not written.
4. Click **Cancel** before save to keep the previous code. Cancel does
   not call the server.
5. HTTP 400, 403, and 409 do not show **Locale currency code saved** and do
   not replace the previous code.

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
6. To store a format profile, check **Store format profile**, fill date /
   time / number / currency / text-direction fields, and **Save**. GET then
   reports `hasFormatProfile=true` and the fields round-trip. Uncheck **Store
   format profile** and **Save** to **remove** the `RXLOCALEFORMAT` row.
   An invalid date/time/currency pattern is **400**.
7. Click **Delete** and confirm in the in-app dialog (not a browser prompt).
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
- Format profiles are keyed by language string (not locale id). Runtime UI
  may still resolve date/number formats via regional → base → en-us defaults
  when no exact row is stored.
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
| Save | `PUT /services/locales/{idOrLang}` (language immutable; optional `format` / `hasFormatProfile`) |
| Set label | `PUT /services/locales/{idOrLang}` (`label` only; a blank or whitespace value is not a write; language, description, enabled status, and format are omitted and stay) |
| Set description | `PUT /services/locales/{idOrLang}` (`description` only; a blank value clears; language, label, enabled status, and format are omitted and stay) |
| Set date pattern | `PUT /services/locales/{idOrLang}` (`format.datePattern` only; a blank value clears; language, label, description, enabled status, and other format fields are omitted and stay) |
| Set time pattern | `PUT /services/locales/{idOrLang}` (`format.timePattern` only; a blank value clears; language, label, description, enabled status, and other format fields are omitted and stay) |
| Set date-time pattern | `PUT /services/locales/{idOrLang}` (`format.dateTimePattern` only; a blank value clears; language, label, description, enabled status, and other format fields are omitted and stay) |
| Set decimal separator | `PUT /services/locales/{idOrLang}` (`format.decimalSep` only; a blank value clears; language, label, description, enabled status, date/time patterns, grouping separator, and other format fields are omitted and stay) |
| Set grouping separator | `PUT /services/locales/{idOrLang}` (`format.groupingSep` only; a blank value clears; language, label, description, enabled status, date/time patterns, decimal separator, and other format fields are omitted and stay) |
| Set currency code | `PUT /services/locales/{idOrLang}` (`format.currencyCode` only; a blank value clears; language, label, description, enabled status, date/time patterns, separators, and other format fields are omitted and stay) |
| Delete | `DELETE /services/locales/{idOrLang}` (`204` on success) |
| Auto-translation list | `GET /services/locales/auto-translations` |
| Auto-translation replace | `PUT /services/locales/auto-translations` (empty `[]` clears) |

Integrator notes: [REST API — Locales](id:developer-rest).
