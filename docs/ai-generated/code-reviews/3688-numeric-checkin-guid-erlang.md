# Erlang review — #3688 numeric checkIn GUID

**Branch:** `fix/issue-3688-numeric-checkin-guid`  
**Scope:** uncommitted vs `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for new GUID mapping; URL paths correctly use `/`; no public API signature change; product-docs N/A (Preview already documented)

## Summary

Explorer Preview `GET …/itemmanagement/workflow/checkIn/594` passed a bare FastForward content id into `PSIdMapper.getGuid(String)`, which called untyped `guidMgr.makeGuid(id)`. `PSGuid.assemble` throws `Type is undetermined` when type bits 32–39 are zero. The mapper now routes those tokens through existing `getGuidFromContentId` / `makeGuid(raw, PSTypeEnum.LEGACY_CONTENT)`. Hyphenated `host-type-uuid` strings and packed longs that already carry a type still use untyped `makeGuid(String)`.

No operator-visible Preview contract change — product-docs Preview row already describes listed-page view-mode preview.

## Issues

None.

## Cross-platform path checklist

- [x] No new filesystem `".../" +` or `"...\\" +` joins (CMS URL helpers use `/` for HTTP)
- [x] Tests do not assert OS path strings
- [x] N/A temp files / line endings / scripts

## Tests

- `PSIdMapperNumericContentIdTest` — bare `594`, hyphenated GUID, packed long with type bits, `getGuids` / `getItemGuid`
- `PSItemSummaryServiceNumericIdTest` — `find("594")` uses `makeGuid(594L, LEGACY_CONTENT)`
- `PSItemWorkflowServiceNumericCheckInTest` — REST/service checkIn with numeric id
- Playwright helper unit tests for checkIn path + prefer content id 594
- Live C5: `perc-devctl qa-up` TEST_CMS_URL=http://127.0.0.1:50758; docker cp sitemanage + perc-system; in-cell StopJetty/StartJetty; qa-health RESULT:OK HTTP:200 HEALTH:healthy; Playwright `explorer-preview-view.spec.js` **3 passed 0 skipped**; console-clean=yes (pageerror); server.log-clean=yes (no `Failed to load: 594` / `Type is undetermined`)

## Change-class companions

Numeric-id mapping in sitemanage share DAO; no REST URL shape change; Playwright surface spec extended; product-docs N/A.
