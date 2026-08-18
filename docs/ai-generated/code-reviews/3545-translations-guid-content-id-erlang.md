# Erlang review: Translations GUID content id for variants + create-variant (#3545)

**Branch:** `fix/issue-3545-translations-guid-content-id`  
**Base:** `origin/main`  
**Date:** 2026-08-17  
**Persona:** Erlang (independent of implementer)

Parent QA #2649 (Failed of #2430): Explorer Translations used `Number(itemId)`
only. List rows are Percussion GUIDs (`1-101-708`); create-variant then emitted
`Selected item does not have a numeric content id`.

## Summary

`TranslationsPanel` now resolves Explorer row ids with `parseExplorerContentId`
(GUID last segment) for variants GET (`708`) and create-variant POST
`itemIds: [708]`. Folders/sites with no content id still return null and keep
the existing no-id create message. Shell already gated the panel with
`parseExplorerContentId`; a GUID-row shell test plus Vitest GUID create body
and Playwright intercept cover the user-visible path. Product-docs note the
GUID last-segment behavior.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral tests exercise GUID parse → GET key and POST body.
Playwright companion updated. No public Java API shape change. No filesystem
path I/O.

## Change-class closure

Change class: **WebUI Explorer product screen (Translations panel GUID id)**.

| Companion | Status |
|-----------|--------|
| `TranslationsPanel` parse via `parseExplorerContentId` | present |
| Vitest GUID GET + create body + no-id folder path | present |
| Shell GUID row mounts panel | present |
| Playwright `explorer-translations.spec.js` | present (create intercept) |
| product-docs `8.2/admin/content-explorer.md` | present |
| i18n / a11y | existing keys + axe gates; no new chrome strings |

## Cross-platform path checklist

No filesystem path construction. REST URLs and GUID tokens use `/` and `-`
as protocol forms, not OS paths. **Outcome: clean.**

Memory patterns hit: missing behavioral tests (covered); WebUI Playwright
companion (updated); change-class product-docs (updated).

## Issues

None blocking.

### nit

`resolveTranslationsContentId` is a one-line alias of `parseExplorerContentId`.
Acceptable as a named call-site comment; not required to inline.

C5 live Playwright against H2 QA is a process gate after this review, not a
code defect.
