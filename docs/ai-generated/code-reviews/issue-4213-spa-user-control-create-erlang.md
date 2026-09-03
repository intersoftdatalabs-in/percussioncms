# Erlang review — #4213 SPA UI-01 user control create

**Branch:** `feat/issue-4213-spa-user-control-create-2`  
**Scope:** WebUI Developer CE Controls create chrome + Vitest + product-docs 8.2 + Playwright spec  
**Date:** 2026-09-03

## Summary

Admin create of user CE controls from **Developer → CE Controls** via existing
`POST /services/cecontrols`. Catalog New chrome, name validation matching
`ControlAdaptor.requireValidName`, wrapped `ControlDef` POST body, 400/409/403
UI surfacing, name read-only after create, system controls remain read-only.
No PUT/DELETE chrome.

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None (bugs / missing behavioral tests / non-portable paths).

## Companions

| Kind | Status |
|------|--------|
| `controlsApi` POST + wrap/unwrap + name validation | yes |
| `ControlCreatePanel` + `ControlsPanel` New | yes |
| Vitest (API, create panel, catalog, detail, shell mock) | yes |
| Playwright `developer-control-create.spec.js` | yes (live C5) |
| product-docs 8.2 admin + REST | yes |
| Dual-ship `WebUI/war` | N/A (SPA is `src/main/ts` → generated `cm/modern`) |

## Cross-platform path checklist

N/A — no filesystem path construction. REST URLs use `/`. Playwright `URLSearchParams` is portable.

## Tests

WebUI standalone `mvnw clean install`: BUILD SUCCESS, Tests 3787 passed.

## Memory patterns hit

- Wrap JAXB/Jackson root on POST (flat body fails UNWRAP_ROOT_VALUE)
- Catalog New visible on empty list
- System/packaged objects stay non-mutable in SPA
- Duplicate 409 / invalid 400 surfaced via `panelErrMsg` + typed fallback
