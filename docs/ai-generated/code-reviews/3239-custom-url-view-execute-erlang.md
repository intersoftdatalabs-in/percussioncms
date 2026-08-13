# Erlang review: #3239 custom-URL view execute (Inbox C1)

**Branch:** `feat/issue-3239-custom-url-view-execute`  
**Base:** `origin/main` (includes V1 merge from `feat/issue-3115-views-rest-execute` / PR #3235)  
**Date:** 2026-08-12  
**Persona:** Erlang (independent of implementer)

## Summary

Extends `POST /rest/views/{idOrName}/execute` so Inbox-family custom-URL views (`sys_cxViews/inbox` and documented peers) run via the classic app resource and map `Item/@sys_contentid` rows to Explorer items. Unsupported custom URLs stay an explicit 400. Missing request context / missing `sys_cxViews` resource is 503, not 500.

Companion closure matches rest/sitemanage change class: resource + adaptor javadoc + `ViewAdaptor` runner + Mockito `ViewResourceTest` + existing Spring `TestViewAdaptor` stub + `ViewAdaptorExecuteTest` + product-docs `rest.md`.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No hard-gate bugs. Behavioral tests cover Inbox success, unsupported/blank URL 400, URL allow-list, document mapping, result cap, and resource 503 rethrow. Custom-URL resolution is application-resource path hygiene (`/`), not OS filesystem joins.

## Change-class closure

| Companion | Status |
|-----------|--------|
| rest resource + OpenAPI | present (`ViewResource.executeView` docs updated) |
| Adaptor interface | present (javadoc; no signature change) |
| sitemanage apibridge | present (`ViewAdaptor.runCustomUrlView`) |
| Mockito resource tests | present (`ViewResourceTest` 17 tests) |
| Spring test stub | present (`TestViewAdaptor.executeView` already on V1) |
| sitemanage unit tests | present (`ViewAdaptorExecuteTest` 27 tests) |
| product-docs | present (`product-docs/8.2/developer/rest.md`) |
| Playwright / WebUI | N/A (Explorer leaf is #3240; Playwright #3241) |

## Cross-platform path checklist

- Custom-view URL parse uses `/` as the **application resource** separator (URL/app page), not `File.separator`.
- Rejects `\`, `..` after strip, NUL, and non-`sys_cxViews` apps.
- No new filesystem path construction.

## Issues

None (hard-gate).

## Memory patterns hit

- Incomplete change-class closure (rest↔sitemanage adaptor surface) — closed
- Behavioral tests for validation/rejection (unsupported custom URL) — present
- Non-portable paths — not applicable (resource URLs)

## Build evidence (pre-PR)

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 335, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1132, Failures: 0, Errors: 0, Skipped: 125
- C2: no public method/ctor signature change; `IViewAdaptor.executeView` already existed on V1. Downstream sitemanage install green.
