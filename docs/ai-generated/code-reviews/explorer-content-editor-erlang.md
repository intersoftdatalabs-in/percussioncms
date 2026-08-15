# Erlang review — feat/explorer-content-editor

**Date**: 2026-08-15  
**Scope**: uncommitted vs `origin/main`  
**Reviewer**: Erlang  
**Memory patterns hit**: change-class completeness (WebUI screen + Playwright + product-docs + dual spa.jsp + Java SPA_ENTRIES + sitemanage REST + mapper unit tests); behavioral tests for field filter/stringify; no filesystem path I/O

## Summary

First Content Editor slice (995). Explorer Edit/View opens a chrome-less `/editor` window. Edit checkouts via existing itemmanagement workflow REST. Fields load/save through `GET`/`PUT /services/itemmanagement/item/fields/{id}` (scalar `PSContentItem` map; `sys_*` except `sys_title` omitted; binary omitted). Labels from content-type catalog. New Item, TinyMCE, and AA contenteditable stay later.

## Recommendation

`approve`

## Gate

May commit/push: **yes**

## Change-class closure

| Companion | Status |
|-----------|--------|
| sitemanage GET/PUT fields + mapper | Present |
| SPA entry (TS + both spa.jsp + Java filter) | Present |
| Chrome-less editor route | Present |
| Dispatcher + openInEditor (no `?view=editor`) | Present |
| Vitest (host, dispatch, URL, mapper) | Present |
| Playwright `explorer-content-editor.spec.js` | Present |
| product-docs 8.2 admin + developer rest | Present |
| Spec 995 | Present |

## Issues

None blocking.

### Suggestion

Home / TopNav still navigate to leftover `?view=editor`. Out of this Explorer slice; switch those shells when Home create/open is next.

## Cross-platform path checklist

URL/query paths only (`/cm/app/spa.jsp`, `/services/itemmanagement/...`). No filesystem joins.

## Tests run

- `cd rest && ../mvnw.cmd clean install` — SNAPSHOT restore only (unchanged sources); BUILD SUCCESS
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1258, Failures: 0, Skipped: 125
- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS; Surefire 61/0; Vitest 333 files, 2525 tests
