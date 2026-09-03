# Erlang review — #4214 SPA UI-01 user control update/delete

**Branch:** `feat/issue-4214-spa-user-control-update-delete`  
**Scope:** WebUI Developer CE Controls PUT/DELETE chrome + Vitest + product-docs 8.2  
**Date:** 2026-09-03  
**Stacked on:** #4213 / PR #4216

## Summary

Admin **save** (`PUT /services/cecontrols/{name}`) and **delete** (`DELETE` 204,
following GET 404, catalog omits the row) for user CE controls. In-app
`CatalogConfirmDialog` (not `window.confirm`). System controls stay read-only
(409 / no save-delete chrome). 403/404/409 surface in the detail error region.
Omitted `xslSource` on PUT follows the REST default-stylesheet rule.

Playwright live H2 is sibling #4215 (not in this slice).

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None (bugs / missing behavioral tests / non-portable paths).

## Companions

| Kind | Status |
|------|--------|
| `controlsApi` PUT/DELETE + wrap + `isSystemControl` / `isControlSaveReady` | yes |
| `ControlDetailPanel` save/delete + `CatalogConfirmDialog` | yes |
| `ControlsPanel` reload after save/delete | yes |
| Vitest (API, detail 403/404/409, omit xslSource, confirm, catalog omit) | yes |
| Playwright | deferred #4215 (issue out of scope; do not steal) |
| product-docs 8.2 admin + REST + developer index | yes |
| Dual-ship `WebUI/war` | N/A (SPA is `src/main/ts` → generated `cm/modern`) |

## Cross-platform path checklist

N/A — no filesystem path construction. REST URLs use `/` + `encodeURIComponent`.

## Tests

Focused Vitest: controlsApi 12, ControlDetailPanel 13, ControlsPanel 10,
ControlCreatePanel 9, DeveloperShell 38 — all passed.

## Memory patterns hit

- Wrap JAXB/Jackson root on PUT (same as POST)
- System/packaged objects stay non-mutable in SPA
- In-app confirm, not `window.confirm`
- 403/404/409 surfaced via `panelErrMsg` + typed fallback
- Omitted optional write field follows REST default (blank `xslSource`)
