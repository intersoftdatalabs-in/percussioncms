# Erlang review: issue #4235 REST UI-07 custom URL view write

**Branch:** `feat/issue-4235-custom-url-view-write`  
**Base:** `origin/main`  
**Date:** 2026-09-03  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class closure (rest resource + sitemanage adaptor + Spring stub already present + product-docs); behavioral tests (not string-only); URL paths correctly use `/` (classic app URLs, not filesystem).

## Summary

Admin POST/PUT of a *user* CustomView with required `url` via existing `IPSUiDesignWs` (held lock, `overrideLock=false`). GET round-trips `url` and `customView=true`. Inbox-family / packaged `sys_cxViews` catalog keys stay 409. Blank/invalid URL 400; non-Admin 403; duplicate name 409. No SPA/Playwright (siblings).

## Cross-platform path checklist

- No new filesystem path joins.
- Custom-view `url` is a classic application URL (`../app/page.xml`); `/` is correct for that domain.
- Rejection of `\\`, NUL, `://`, and `..` after leading `../` is validation, not OS path I/O.

## Issues

None (bugs / missing behavioral tests / non-portable I/O).

## Change-class companions

| Companion | Status |
|-----------|--------|
| rest resource + OpenAPI text | updated |
| IViewAdaptor javadoc | updated |
| sitemanage ViewAdaptor persist | updated (`setCustom` + `setUrl`) |
| Mockito resource tests | ViewResourceTest |
| Adaptor tests (exact types / real `PSSearch`) | ViewAdaptorWriteTest, ViewAdaptorFieldsTest |
| Spring test stub | TestViewAdaptor already implements create/save |
| product-docs/8.2/developer/rest.md | View write contract |
| Playwright / SPA | N/A (out of scope; siblings #4236/#4237) |

## Builds

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1036, Failures: 0
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 2182, Failures: 0
- C2: no `final`/`sealed` or signature change; reverse-dep `projects/sitemanage` installed
