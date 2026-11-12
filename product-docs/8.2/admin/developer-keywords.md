---
id: admin-developer-keywords
title: Developer Keywords
description: Browse CMS keyword definitions, view one keyword with its choices, create a keyword, update one label, description, or sequence, manage choices, and delete one keyword
version: "8.2"
order: 44
tags: [admin, developer, keywords]
---

# Developer Keywords

**Developer → Keywords** is the content-design catalog of keyword definitions
(value lookups used by keyword fields). It mirrors the classic Workbench
**Keywords** editor: each row shows the keyword **label**, **value**,
**choice count**, and **description**. Selecting a row opens that keyword with
its stored label, description, sequence, and saved choices.

An Admin can **create** one keyword (label required and unique, with optional
description, sequence, and choices), **open** one keyword to see its stored
choices, **edit the label**, **description**, or **sequence** of one existing
keyword, **add**, **rename**, **re-value**, **describe**, **resequence**,
**or remove one choice** at a time, and **delete** one existing keyword from
this catalog. Packaged keywords such as Body_Markup are not created by this
form. If the server rejects a save, the catalog keeps the previous row and
the detail keeps the previous values. A failed choice read does not invent
choices.

## Product path — browse

1. Sign in as **Admin** (the browse catalog requires the Admin role).
2. Open **Developer → Keywords**, or deep-link
   `spa.jsp?entry=developer&section=keywords`.
3. The panel loads the full catalog and shows one table row per keyword:
   label (select to open), value, choice count, and description.
4. Select a row to open that keyword's stored label, description, sequence,
   and saved choices.

An empty catalog is a valid **200** with an empty state, not an error.

## Readback

The catalog table is a thin read: it lists label, value, choice count, and
description from `GET /services/keywords?includeChoices=false` and carries
no per-keyword choice payload. Opening a row loads the detail from
`GET /services/keywords/{idOrValue}` (by uuid, numeric id, value, or label)
and shows the stored label, the stored description, the stored sequence,
and the saved choices. Packaged keywords such as Body_Markup read back with
zero writes: the row renders from the thin list and the detail renders
without creating, renaming, or deleting anything. A keyword created through
the shell (`POST /services/keywords`) reads back in the detail (label,
description, sequence) and as a new catalog row; removing it is
`DELETE /services/keywords/{id}` (`200`/`204` on success, by uuid), after
which the catalog no longer lists that label.

## Product path — create a keyword

1. Select **New keyword**.
2. Type the **label** (required, must be unique), an optional
   **description**, a **sequence**, and optional **choices** (one
   `label|value|sequence` entry per line).
3. Save. The catalog returns and the new row renders with its description.

Create always uses the keyword create service. It does not update an
existing keyword. A blank or duplicate label is **400**. A caller who is
not Admin is **403**.

## Product path — edit label, description, or sequence

1. Select a keyword row to open it.
2. Edit the **label**, **description**, or **sequence** and save that field.

Each field saves only itself: a label save does not send the description,
sequence, or choices, so those stay. A description save omits choices, so
stored choices stay, and a blank description clears the keyword description.
A sequence save sends only the sequence. The new value shows only after
that save succeeds. **Cancel** does not write. HTTP **400**, **403**, and
**409** leave the previous value.

## Product path — manage one choice

1. Select a keyword row to open it.
2. **Add** one choice (label, value, optional description and sequence),
   or change one saved choice's **label**, **value**, **description**, or
   **sequence**, or **remove** / **clear the description of** one saved
   choice after confirming.

Choice writes keep the stored keyword label, description, and sequence and
send the full choice list with the one change applied. Removing the last
choice clears the list and does not delete the keyword. A blank choice
label or value does not write. HTTP **400**, **403**, and **409** leave the
previous choices.

## Product path — delete a keyword

1. Select a keyword row to open it.
2. Delete the keyword and confirm.

Delete removes the keyword and its choices (**204** on success). A missing
keyword is **404**. A caller who is not Admin is **403**.

Integrator notes: [REST API](id:developer-rest) (Keywords). Related
chrome: [Developer Content Types](id:admin-developer-content-types).
