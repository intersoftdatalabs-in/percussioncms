---
id: admin-xsl-legacy-assembler
title: XSL and legacyAssembler support
description: 8.2 support statement for Legacy/XSL assembly and a short cookbook to migrate templates to HTML-first, Markdown, or Velocity
version: "8.2"
order: 46
tags: [admin, design, templates, assembler, xsl, migration]
---

# XSL and legacyAssembler support

Percussion CMS **8.2** still **runs** existing **Legacy / XSL** assembly
(`legacyAssembler` — XML application + stylesheet). New templates and product
packaging should **not** choose that path. Prefer **HTML-first**, **Markdown**,
or **Velocity** on [Design templates](id:admin-design-templates).

This page is the operator **support statement** and a **short migration
cookbook**. It does **not** remove XSL from the product, and it does **not**
force conversion during upgrade.

## 8.2 support statement

| Posture | What it means in 8.2 |
|---------|----------------------|
| **Supported and recommended (new work)** | **HTML-first** (`htmlAssembler`), **Markdown** (`markdownAssembler`), and **Velocity** (`velocityAssembler`), with ordered **JEXL** bindings |
| **Supported legacy (existing installs)** | Templates already on **Legacy / XSL** (`legacyAssembler`) continue to assemble. Classic XML applications and stylesheets that already work keep running. |
| **No new investment** | Do not start new Design templates, product packages, or help examples on XSL / `legacyAssembler`. |
| **Not removed in 8.2** | The server still registers Legacy / XSL. Do not delete or disable it to “clean up.” |

**Honest product messaging:**

1. Existing XSL / `legacyAssembler` content **continues to run** in 8.2.
2. **New** templates should use HTML-first (the Design create default), Markdown, or Velocity.
3. There is **no hard “must convert XSL on upgrade” cliff** in 8.2. Conversion is
   operator-driven and incremental. See [Upgrade Overview](id:upgrade-overview).
4. Long-term direction: XSL is **compatibility only**.

Specialized non-text assemblers (**Binary**, **Dispatch**, **Database**, **Page (CM1)**)
are unchanged by this cookbook — they are purpose assemblers, not XSL languages.
Bindings stay **JEXL**. Do **not** treat JavaScript extensions as an assembler
replacement.

Assembler picker details and placeholder rules: [Design templates](id:admin-design-templates).

## When to leave a template on Legacy / XSL

Leave it on **Legacy / XSL** when:

- Preview and publish already produce the HTML (or other MIME) you need.
- The assembly URL still points at a working XML application / stylesheet.
- You have not yet rebuilt a modern template and signed off golden output.

Change it **only** as part of a planned cutover (below). Switching the assembler
on Design does **not** rewrite XSL into HTML or Velocity automatically.

## How legacy assembly differs from modern templates

| Topic | Legacy / XSL | HTML-first, Markdown, Velocity |
|-------|----------------|--------------------------------|
| Assembler | `legacyAssembler` | `htmlAssembler`, `markdownAssembler`, `velocityAssembler` (or **Page (CM1)** for shipped pages) |
| Render path | Internal request to the template **assembly URL** (XML application resource) | JEXL bindings, then template **source** |
| Bindings | **Skipped** — logic lives in the XML app, stylesheet, and request parameters | **Run** in order before the assembler |
| Source language | Stylesheet / XML app, not the modern source editor languages | HTML placeholders, CommonMark, or Velocity |

Inventory remaining legacy usage **before** you plan cutover. Typical surfaces:

| Surface | What to look for |
|---------|------------------|
| **Design / Developer Templates** | Assembler **Legacy / XSL** (`legacyAssembler`) |
| **Assembly URL** | Paths into classic XML applications |
| **Publish / site variants** | Publish templates still on the legacy assembler |
| **Slots / related content** | Snippets that still assemble through a legacy variant |
| **Custom packages** | Customer or partner packages that still ship XSL bodies |

Product out-of-box page and widget templates are already on modern text
assemblers (Velocity / page context). **Customer and historic custom** templates
are the usual migration debt.

## Choose a target assembler

| Target | Use when | Assembler |
|--------|----------|-----------|
| **HTML-first** | Mostly static HTML; a few bound values; no Velocity macros | `htmlAssembler` (Design default) |
| **Markdown** | Prose that should become HTML | `markdownAssembler` |
| **Velocity** | Macros, loops, `#parse`, Active Assembly slot macros | `velocityAssembler` |
| **Page (CM1)** | Shipped page templates with `$perc` / regions | Keep `pageAssembler` on those pages |

HTML-first and Markdown fill only dollar-brace dotted paths from JEXL results
(for example <code>&#36;{title}</code>). Bare `$title`, Mustache, and Velocity
directives are **not** HTML-first/Markdown syntax. See
[Design templates](id:admin-design-templates).

## Classify complexity (per template)

Work **one template** (or one content type’s variants) at a time. Golden HTML
diffs beat a big-bang rewrite.

| Class | Symptoms | Recommended target |
|-------|----------|--------------------|
| **A — Static shell** | Stylesheet mostly emits fixed markup plus a few field values | HTML-first + JEXL + <code>&#36;{path}</code> |
| **B — Light transform** | Field maps, simple conditionals, string formatting | HTML-first or Markdown; put conditionals in **JEXL bindings** |
| **C — Structural / iterative** | Loops, grouping, slot-like expansion in XSL | Velocity, or restructure as modern **slots** + snippet templates |
| **D — Full XML app pipeline** | Multi-resource apps, queries, non-HTML MIME, deep request-parameter contracts | Keep on **Legacy / XSL** until you redesign the **pipeline**. Do not paste XSL into Velocity. |

## Migration cookbook (simple variant)

### 1. Inventory and freeze

1. List templates whose assembler is **Legacy / XSL**.
2. For each, record content types, sites/publish contexts, assembly URL, MIME type,
   and shared XML-app resources (shared stylesheets complicate per-template cutover).
3. Capture **golden** preview and publish output for representative items.

### 2. Rebuild a modern template (keep the old one)

1. On [Design templates](id:admin-design-templates), **Create template** with
   HTML-first, Markdown, or Velocity as classified. Do **not** overwrite the
   legacy template until parity is signed off.
2. Port data access to **ordered JEXL bindings** (item fields, `$sys`, `$rx`
   tools). Modern assemblers **run** bindings; legacy **skipped** them.
3. Port markup:
   - Class A/B → HTML-first (or Markdown) body with <code>&#36;{bindingKey}</code> only.
   - Class C → Velocity body; prefer small macros over one giant stylesheet.
     Insert built-in Velocity macros from
     [Developer Templates](id:admin-developer-templates) (**Insert snippet**).
4. Match MIME / charset / Active Assembly type / sites unless you intend a change.

### 3. Prove parity, then switch

1. Point a **non-production** content type variant or site publish template at the
   new template first.
2. Diff golden HTML (normalize whitespace and line endings).
3. Exercise edit vs publish, empty and multi-valued fields, and related content
   in slots.
4. Only then switch default variants / publish templates in production.

### 4. Retire the legacy path on **this site**

1. Unlink content types and publish configs from the legacy template.
2. Archive or document the old assembly URL and stylesheet for rollback.
3. Leave **Legacy / XSL** registered on the server. Site-level retirement does
   **not** require product runtime removal.

If the template ships in a package, follow the modern component package model —
see [Extensions & packages](id:developer-extensions) and
[Product page packages](id:developer-page-packages). Do **not** add new XSL
application packaging for greenfield work.

## Mapping cheat sheet

| Legacy XSL / XML-app idea | Modern approach |
|---------------------------|-----------------|
| Field copy (`value-of`) | JEXL binding → <code>&#36;{field}</code> (HTML-first) or `$field` (Velocity) |
| `xsl:if` / choose | JEXL binding for a flag or fragment; or Velocity `#if` |
| `xsl:for-each` | Velocity `#foreach`; or a **slot** + related items + snippet templates |
| Call another XML resource | Snippet template / slot expansion — avoid a second XSL app when you can |
| Request parameters as the control surface | Prefer bindings + template config |
| “No bindings” mental model | Invert: presentation in the body; data shaping in **ordered JEXL** |
| Binary or non-HTML pipeline | Specialized assembler, or keep legacy until redesigned |

## What this page does not do

- Remove `legacyAssembler` or unregister **Legacy / XSL**
- Provide mass automated conversion of customer XSL
- Force conversion during 8.2 upgrade
- Replace JEXL bindings with JavaScript
- Cover pipeline **result-page** XSL (Developer Pipelines) — that is a different
  surface; see [Developer Pipelines](id:admin-developer-pipelines)

## Related

- [Design templates](id:admin-design-templates) — create/edit assembler picker
- [Developer Templates](id:admin-developer-templates) — export/import XML and Velocity snippets
- [Upgrade Overview](id:upgrade-overview)
- [Getting Started](id:getting-started)
- [Glossary](id:reference-glossary)
- [Administration](id:admin)
