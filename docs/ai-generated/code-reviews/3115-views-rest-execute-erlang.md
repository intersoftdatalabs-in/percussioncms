# Erlang review: #3115 Views REST execute façade

**Branch:** `feat/issue-3115-views-rest-execute`  
**Base:** `origin/main`  
**Date:** 2026-08-12  
**Persona:** Erlang (independent of implementer)

## Summary

Adds `POST /rest/views/{idOrName}/execute` (WebUI `/services/views/...`) for **standard** CX design views. Loads designs via `IPSUiDesignWs.findViews` / `loadViews` (not the search catalog). Twin request/result DTOs keep Views vs Searches contracts separate. Custom URL views (`isCustomView`) return explicit 400 pointing at Inbox / #3118.

Companion closure matches rest/sitemanage change class: resource + `IViewAdaptor` method + `ViewAdaptor` impl + Mockito `ViewResourceTest` + Spring `TestViewAdaptor` stub + `ViewAdaptorExecuteTest` + product-docs `rest.md`.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No bugs found. Behavioral tests cover 200/400/404/503, custom-URL rejection, unsafe keys, paging, and views-catalog load (not `findSearches`). Path I/O not introduced.

## Change-class closure

| Companion | Status |
|-----------|--------|
| rest resource + OpenAPI | present (`ViewResource.executeView`) |
| Adaptor interface + wire DTOs | present (`IViewAdaptor`, `ViewExecuteRequest/Result`, `ViewResultItem`) |
| sitemanage apibridge | present (`ViewAdaptor.executeView`) |
| Mockito resource tests | present (`ViewResourceTest` 15 tests) |
| Spring test stub | present (`TestViewAdaptor.executeView`) |
| sitemanage unit tests | present (`ViewAdaptorExecuteTest` 20 tests) |
| product-docs | present (`product-docs/8.2/developer/rest.md`) |
| Playwright / WebUI | N/A (V2/V3 out of scope) |

## Cross-platform path checklist

N/A — no new filesystem path construction. Key hygiene still rejects `/`, `\`, `..`, NUL (URL path tokens, not OS joins).

## Issues

None (hard-gate).

## Memory patterns hit

- Incomplete change-class closure (rest↔sitemanage adaptor surface) — closed
- Shared Spring test stubs for new adaptor methods — `TestViewAdaptor` updated
- Behavioral tests for validation/rejection (custom URL, bad paging) — present
- Non-portable paths — not applicable

## Build evidence (pre-PR)

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 333, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1050, Failures: 0
- C2: `IViewAdaptor` gained a method; `ViewAdaptor` ctor added `IPSFolderHelper`/`IPSIdMapper`. Grep: only `TestViewAdaptor` + `ViewAdaptor` implement the interface; `new ViewAdaptor` only in new execute tests. Downstream sitemanage install green.
