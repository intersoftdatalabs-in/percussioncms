# Erlang review — #3722 numeric checkIn GUID

**Branch:** `fix/issue-3722-numeric-checkin-no-500`  
**Scope:** uncommitted vs `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for remaining untyped GUID assemble; no public API signature change; REST checkIn no-op instead of 500; product-docs N/A (Preview already documented)

## Summary

Cycle Verify residual of Explorer Preview `GET …/itemmanagement/workflow/checkIn/594` HTTP 500 (`Type is undetermined`). Cluster #3695 already maps bare numeric ids in `PSIdMapper.getGuid`. Remaining untyped path is `PSGuid.assemble` for a single numeric token with type bits 0 (what stale `makeGuid(String)` still calls). Assemble now sets `LEGACY_CONTENT`. `PSItemSummaryService.find` retries `getGuidFromContentId` if getGuid still throws. REST `checkIn` returns 200 no-op instead of HTTP 500 when the cause chain contains undetermined type.

No operator-visible Preview contract change.

## Issues

None.

## Cross-platform path checklist

- [x] No new filesystem `".../" +` or `"...\\" +` joins (CMS URL helpers use `/` for HTTP)
- [x] Tests do not assert OS path strings
- [x] N/A temp files / line endings / scripts

## Tests

- `PSGuidTest` — `new PSGuid("594")` is LEGACY_CONTENT; two-component still requires type
- `PSItemSummaryServiceNumericIdTest` — find retries `getGuidFromContentId` on undetermined type
- `PSItemWorkflowServiceNumericCheckInTest` — REST checkIn no-op on find IAE / DataServiceLoadException
- Playwright `explorer-preview-view.spec.js` numeric checkIn case (#3688 / #3722)
- Live C5: `perc-devctl qa-up --skip-image-build` TEST_CMS_URL=http://127.0.0.1:9993; docker cp utils + sitemanage (+ perc-system); in-cell StopJetty/StartJetty; qa-health RESULT:OK HTTP:200 HEALTH:healthy; Playwright `explorer-preview-view.spec.js` **3 passed 0 skipped**; console-clean=yes (pageerror); server.log-clean=yes (0 ERROR/FATAL, 0 `Type is undetermined`)

## Change-class companions

Untyped GUID assemble in utils; sitemanage checkIn/find defense; Playwright surface spec title; product-docs N/A.
