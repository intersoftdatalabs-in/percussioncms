# Erlang review — #4111 SPA UI-08 view field-selection

**Date:** 2026-09-01  
**Branch:** `feat/issue-4111-view-field-selection`  
**Base:** `origin/main` (`a88447e5e8`)  
**Recommendation:** approve  
**Gate:** May commit/push: **yes**  
**Cross-platform path review:** no new filesystem path joins; Playwright uses URL paths only.

## Summary

Admin add/remove/reorder of field criteria on user/standard CX views from Developer Views. `ViewAdaptor.applyFields` persists `ViewDef.fields` on PUT; omitted `fields` leaves existing criteria (`ViewDef.fields` default null). Inbox-family / custom-URL / system views remain 409. SPA `ViewDetailPanel` shares the CX field catalog with display-format columns. Updates return the in-memory saved object (H2 `findAllViews` XML cache can lag field rows) and resolve PUT by GUID when the catalog misses the name.

## Scope

- `projects/sitemanage` `ViewAdaptor` + `ViewAdaptorFieldsTest`
- `rest` `ViewDef` / `ViewResource` OpenAPI
- `WebUI` ViewDetailPanel, `viewFieldCriteria.ts`, viewsApi DESIGN_GAPS
- `modules/perc-qa-automation` `developer-view-field-selection.spec.js`
- `product-docs/8.2/admin/developer-views.md`, `developer/rest.md`

Prior report: none for this slice. Related: issue-4085 view create/delete.

Memory patterns: H2 catalog lag after saveViews (UI-07 GET 404); do not return stale `findAllViews` after field PUT.

## Issues

None at `bug` severity.

### suggestion

- H2 `GET /services/views/{name}` can still 404 immediately after field PUT when XML cache lags; SPA PUT 200 + GUID reload is the persist path. Playwright treats GET 200 as preferred and asserts SPA rows when GET is not 200.

### nit

- Playwright GET-optional branch documents H2 catalog lag; keep until findAllViews cache invalidation is a dedicated slice.

## Tests

- `ViewAdaptorFieldsTest` 12, `ViewAdaptorWriteTest` 25
- Vitest ViewDetailPanel PUT body / 400 / 403 / Inbox readonly
- Playwright H2: 2 passed (`developer-view-field-selection.spec.js`)
