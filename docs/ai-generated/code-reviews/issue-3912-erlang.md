# Erlang review — issue #3912 REST CD-01 POST create content types

**Date:** 2026-08-27  
**Branch:** `feat/issue-3912-rest-cd01-post-content-types`  
**Scope:** uncommitted vs `HEAD` (based on `origin/main`)  
**Memory patterns hit:** Change-class closure (rest resource + adaptor interface + DTO + Mockito + Spring stubs + sitemanage impl/tests + product-docs); exact implementors of `IContentTypesAdaptor`; typed 409 vs message-substring 409

## Summary

Adds Admin `POST /services/contenttypes` as thin design-WS glue: `IPSContentDesignWs.createContentTypes` then `saveContentTypes(..., release=true)` (Workbench Finish). Name required, no whitespace, case-insensitive uniqueness via catalog scan → HTTP 409. Spring test stubs on both rest implementors; Mockito resource tests; sitemanage adaptor tests. `designGaps` `CT_CREATE_DELETE` no longer claims create is impossible. Product-docs 8.2 REST + Developer Content Types updated. No WebUI chrome (C5 N/A).

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No bugs, missing behavioral tests, or non-portable path/file I/O.

## Change-class closure

| Companion | Status |
|-----------|--------|
| rest resource + OpenAPI | yes (`POST /`) |
| `IContentTypesAdaptor.createContentType` | yes |
| Wire DTO | reuse `ContentTypeDetail` |
| Mockito `ContentTypesResourceDetailTest` | yes (success, 400 name/spaces, 409 duplicate, 403) |
| Spring stubs `TestContentTypeAdaptor` + `ContentTypesTestAdaptor` | yes |
| sitemanage `ContentTypeAdaptor` | yes |
| sitemanage adaptor tests | `ContentTypeAdaptorCreateTest` (8) |
| product-docs 8.2 | `developer/rest.md`, `admin/developer-content-types.md` |
| rest → sitemanage Maven edge | none |
| C2 implementors | grep `implements IContentTypesAdaptor` — 3 types, all updated; no anonymous subclasses; sitemanage standalone install |

## Cross-platform path checklist

N/A — no filesystem path construction, temp files, or path assertions.

## Issues

None (blocking).

## Nits (non-blocking)

- `findContentTypes("*")` for uniqueness is Admin-only and matches list catalog size; SOAP `validateUniqueName` remains a second line of defense on persist.
- `CT_CREATE_DELETE` code kept (message now documents POST create; delete/rename still open) to avoid SPA code churn.

## Builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 660, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 1644, Failures: 0, Skipped: 125
