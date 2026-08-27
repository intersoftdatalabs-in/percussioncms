# Erlang review — #3869 Developer Sites object-storage Build chrome

**Branch:** `feat/issue-3869-object-storage-build-chrome`  
**Date:** 2026-08-26  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI chrome + Vitest + Playwright + product-docs); Playwright HARD GATE for screen work; i18n keys (no hard-coded English); consume REST/SPI siblings without re-implementing them; Preview/Publish remain later slices (#3870 / #3868).

## Summary

Parent #2678 slice 2 (object-storage Build chrome). Developer Sites **Build Virtual Site** is shown for `sourceKind=object-storage` after save, same panel as git/csv/sql/http-json. Preview and Publish chrome stay hidden (slice 3 / later). i18n hint now says save then Build; no cloud URLs or access keys on the envelope. Vitest covers `shouldShowVirtualBuildChrome` and the panel Build success path. Playwright extends `developer-site-virtual-source.spec.js` with a live H2 Build against a local object-key fixture. Product-docs admin Sites (and peer developer/reference pages) document operators can Build after save.

REST `POST …/virtual/build` already runs `PSVirtualSiteBuildService.forSourceType` for the helper allow-list (SPI #3838 / persist #3839 on `main`). This slice does not re-implement REST publish, Preview chrome, or live S3 credentials.

## Scope

- `WebUI/src/main/ts/developer/virtualSiteBuild.ts` — Build chrome includes `object-storage`; Preview/Publish remain git/csv/sql/http-json
- `WebUI/src/main/ts/developer/VirtualSiteSourcePanel.tsx` — comment only (uses the helper)
- `WebUI/src/main/ts/developer/messages.ts` — `SITE_VIRT_OBJECT_STORAGE_HINT` i18n
- Vitest: `virtualSiteBuild.test.ts`, `VirtualSiteSourcePanel.test.tsx`
- Playwright: `developer-site-virtual-source.spec.js` + `object-storage-virtual-qa-fixture.js` + local Markdown fixture
- Product-docs: `product-docs/8.2/admin/sites.md`, `admin/publishing.md`, `developer/virtual-sites.md`, `developer/rest.md`, `reference/site-config.md`

## Issues

None.

## Cross-platform path review

- [x] No new `".../" +` or `"...\\" +` filesystem joins in Java/TS
- [x] Fixture helper uses `path.join` for host files; in-container dest is POSIX URL path (`/` only — Docker Linux cell)
- [x] UI/tests use operator-style examples (`C:/object-docs`) as field values, not OS file joins
- [x] Playwright live Build fills the in-container POSIX root returned by the helper
- [x] Line-ending assertions not added
- [x] Client validation still rejects `..` in root path (server NIO remains source of truth)

## Tests

- `virtualSiteBuild` — object-storage shows **Build** chrome; Preview/Publish stay false; git/csv/sql/http-json unchanged; repository / unknown stay hidden
- `VirtualSiteSourcePanel` — load/save show Build, hide Preview/Publish; click Build reports pagesWritten; hint contains Build Virtual Site
- Playwright — option still present; object-storage vs Git field visibility; live save+reload shows Build; live H2 Build pagesWritten > 0 then restore repository
- REST/SPI internals — consume factory on `main` (not re-implemented)

## Change-class closure

| Companion | Status |
|-----------|--------|
| Build chrome helper + i18n | yes |
| Vitest panel/build | yes |
| Playwright `developer-site-virtual-source.spec.js` + QA fixture | yes |
| Product-docs 8.2 admin Sites | yes |
| REST/SPI internals | consume SPI #3838 + persist #3839 on main; REST Build already uses factory |
| Preview chrome | out of scope (#3870) |
| Publish chrome / REST publish | out of scope (#3868) |
| Human QA assignment | not created |

## Gate

- Blocking bugs: 0
- May commit/push: yes
