# Erlang review — #4214 SPA UI-01 user control update/delete

**Branch:** `feat/issue-4214-spa-user-control-update-delete-2`  
**Stacked on:** PR #4231 / `feat/issue-4213-spa-user-control-create-2`  
**Scope:** WebUI Developer CE Controls PUT/DELETE chrome + Vitest + product-docs 8.2 + Playwright spec  
**Date:** 2026-09-03

## Summary

Admin update and delete of **user** CE controls from **Developer → CE Controls**
via existing `PUT` / `DELETE /services/cecontrols/{name}`. Metadata save omits
blank `xslSource` (server default stylesheet). Delete uses in-app
`CatalogConfirmDialog` (not `window.confirm`). System controls stay read-only
(no save/delete chrome; 409 surfaced if the API returns conflict). 403/404/409
in the detail error region. Does not duplicate create chrome from #4213.

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None (bugs / missing behavioral tests / non-portable paths).

## Companions

| Kind | Status |
|------|--------|
| `controlsApi` PUT/DELETE + wrap + `isControlSaveReady` / `isSystemControl` | yes |
| `ControlDetailPanel` save/delete + `ControlsPanel` catalog omit after delete | yes |
| Vitest (API, detail PUT/DELETE/409, catalog omit, GET 404) | yes |
| Playwright `developer-control-update-delete.spec.js` | yes |
| product-docs 8.2 admin + REST | yes |
| Dual-ship `WebUI/war` | N/A (SPA is `src/main/ts` → generated `cm/modern`) |

## Cross-platform path checklist

N/A — no filesystem path construction. REST URLs use `/`. Playwright `URLSearchParams` is portable.

## Tests

WebUI standalone `mvnw clean install`: BUILD SUCCESS, Tests 3798 passed.

## Memory patterns hit

- Wrap JAXB/Jackson root on PUT (flat body fails UNWRAP_ROOT_VALUE)
- In-app `CatalogConfirmDialog` instead of `window.confirm`
- System/packaged objects stay non-mutable in SPA
- 403/404/409 surfaced via `panelErrMsg` + typed fallback
- Omitted optional XSL follows REST default-stylesheet rule
