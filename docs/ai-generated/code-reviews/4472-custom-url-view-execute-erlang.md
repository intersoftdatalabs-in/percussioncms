# Erlang review: #4472 custom URL view execute

**Branch:** `fix/issue-4472-custom-url-view-execute`  
**Base:** `origin/main`  
**Date:** 2026-09-17  
**Persona:** Erlang (independent of implementer)

## Summary

Slice 15 of parent #1690 extends `POST /services/views/{idOrName}/execute` so **user** custom URL views (non-Inbox / non-packaged `sys_cxViews` names) run through the same path-safe classic application resource as Inbox-family pages. Inbox-family catalog keys stay executable for any operator. User custom URL execute requires Admin (403). Blank/external/traversal URLs are 400. Unknown views remain 404. `ViewAdaptor.DESIGN_GAPS` and SPA `VIEW_DESIGN_GAPS` drop the “cannot be executed” gap.

SPA **Developer → Views** detail shows **Execute** for custom URL views and renders result rows or a documented error. Playwright surface spec `developer-view-execute.spec.js` covers the Admin path. Product-docs 8.2 Developer Views + REST match the shipped contract.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No hard-gate bugs. Behavioral tests cover Admin success, non-Admin 403, missing 404 (null), external/traversal/blank 400, resource normalization for user apps, SPA execute success/400/403, and gap filtering. Custom-view URL parse uses `/` as the **application resource** separator (URL/app page), not `File.separator`.

## Change-class closure

| Companion | Status |
|-----------|--------|
| rest resource + OpenAPI | present (`ViewResource.executeView` docs + 403) |
| Adaptor interface | present (javadoc; no signature change) |
| sitemanage apibridge | present (`ViewAdaptor.resolveCustomViewResource` + Admin gate) |
| Mockito resource tests | present (`ViewResourceTest` 400/403) |
| Spring test stub | present (`TestViewAdaptor.executeView` unchanged) |
| sitemanage unit tests | present (`ViewAdaptorExecuteTest` 403/400/user app) |
| WebUI SPA + Vitest | present (`ViewDetailPanel` Execute + `viewsApi.executeView`) |
| Playwright | present (`developer-view-execute.spec.js`) |
| product-docs | present (`developer-views.md`, `rest.md`) |

## Cross-platform path checklist

- Custom-view URL parse uses `/` as the **application resource** separator (URL/app page), not `File.separator`.
- Rejects `\`, leftover `..` after a single `../` strip, NUL, schemes, and non `app/resource` shapes.
- Segment allow-list is `[A-Za-z0-9_.-]`; no OS filesystem joins.
- Tests do not assert Unix-only filesystem paths.

## Issues

None (hard-gate).

## Memory patterns hit

- Incomplete change-class closure (rest↔sitemanage adaptor surface + SPA + Playwright + product-docs) — closed
- Behavioral tests for validation/rejection (403/404/400) — present
- Non-portable paths — not applicable (resource URLs)

## Build evidence (pre-PR)

- `cd rest && ../mvnw clean install` — BUILD SUCCESS; Tests run: 1412, Failures: 0 (`ViewResourceTest` 34)
- `cd projects/sitemanage && ../../mvnw clean install` — BUILD SUCCESS; Tests run: 2736, Failures: 0, Skipped: 125 (`ViewAdaptorExecuteTest` 37)
- `cd WebUI && ../mvnw clean install` — BUILD SUCCESS; Vitest Test Files 443 passed; Surefire Tests run: 69, Failures: 0
- `cd modules/perc-qa-automation && ../../mvnw clean install` — BUILD SUCCESS (Playwright live is C5, not this module’s Surefire)
- C2: no public method/ctor signature change on `IViewAdaptor.executeView`. Downstream sitemanage install green.
