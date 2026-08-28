# Erlang review — issue #3926 REST CD-01 create content type (docs + reserved)

**Date:** 2026-08-27  
**Branch:** `feat/issue-3926-rest-cd01-create-content-type`  
**Scope:** uncommitted vs `HEAD` (based on `origin/main`)  
**Memory patterns hit:** Change-class closure (product-docs companion after cluster absorb); typed 409 vs message-substring; exact implementors of `IContentTypesAdaptor`

## Summary

CD-01 `POST /services/contenttypes` already shipped on main via #3912 / PR #3918 (`IPSContentDesignWs.createContentTypes` then `saveContentTypes`). Cluster absorb #3923 dropped the Create row and create status codes from `product-docs/8.2/developer/rest.md`. This change restores that integrator documentation, notes reserved system types such as Folder as **409** collisions (SOAP `createContentType("Folder")` peer), and adds resource + adaptor tests for Folder plus create-then-GET. Does **not** remap collision to 400 (would regress the shipped 409 contract). No SPA chrome, rename, or DELETE.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No bugs, missing behavioral tests, or non-portable path/file I/O.

## Change-class closure

| Companion | Status |
|-----------|--------|
| rest resource + OpenAPI | yes (description/409 text only; POST already on main) |
| `IContentTypesAdaptor.createContentType` | javadoc only (no signature change) |
| Mockito `ContentTypesResourceDetailTest` | reserved Folder 409 |
| Spring stubs `TestContentTypeAdaptor` + `ContentTypesTestAdaptor` | already implement `createContentType` |
| sitemanage `ContentTypeAdaptor` | already on main; tests added |
| sitemanage adaptor tests | `ContentTypeAdaptorCreateTest` Folder 409 + create-then-GET |
| product-docs 8.2 | `developer/rest.md` Create row restored; admin Content Types 400/403/409 |
| rest → sitemanage Maven edge | none |
| C2 implementors | grep `implements IContentTypesAdaptor` — 3 types; no signature change; no anonymous subclasses |

## Cross-platform path checklist

N/A — no filesystem path construction, temp files, or path assertions.

## Issues

None (blocking).

## Nits (non-blocking)

- Issue #3926 triage asked for collision **400**. Shipped #3918 uses **409** (REST Conflict vs invalid name 400). This PR keeps 409 and documents Folder as catalog collision.

## Builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 669, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS, Tests run: 1667, Failures: 0, Skipped: 125
- `scripts\ci-smoke-product-docs.bat` — OK
