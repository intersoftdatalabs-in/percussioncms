# Erlang review — #4115 Developer Sites sitemap-xml source chrome

**Branch:** `feat/issue-4115-sitemap-xml-source-chrome`  
**Date:** 2026-09-01  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI panel + Vitest + Playwright + product-docs); Playwright HARD GATE for screen work; omit-vs-empty remoteUrl keep/clear (#3568); consume REST persist sibling without re-implementing SPI; no secrets on REST envelope; Build/Preview/Publish chrome not shown until REST Build exists.

## Summary

Parent #2678 slice: Developer Sites Virtual Site source panel offers **Sitemap XML** (`sitemap-xml`) as a source-kind option, peer of icalendar (#3983). Operators can select it, set the existing **Root path** field, save, GET-roundtrip, and switch back to **Repository (traditional)**. Git remotes / config / site key stay Git-only. PUT never sends crawl URLs or credentials (`remoteUrl: ""` / `branch: ""` so a prior Git remote is cleared). **Build / Preview / Publish chrome is intentionally hidden** for sitemap-xml (later slices #4124+). REST persist is sibling #4114 / PR #4121 (MERGED on main, includes SPI factory). This slice consumes the contract and does not re-implement REST/SPI.

## Scope

- `WebUI/src/main/ts/developer/virtualSiteForm.ts` — `SOURCE_KIND_SITEMAP_XML`, normalize, `formToVirtualProps`
- `WebUI/src/main/ts/developer/virtualSiteBuild.ts` — comments only; Build/Preview/Publish remain git/csv/sql/http-json/object-storage/rss-atom/icalendar
- `WebUI/src/main/ts/developer/VirtualSiteSourcePanel.tsx` — select option + sitemap-xml hint + reuse root path
- `WebUI/src/main/ts/developer/messages.ts` — i18n keys (`perc.ui.developer@Sitemap XML`)
- `WebUI/src/main/ts/api/developer/types.ts` — allow-list javadoc
- Vitest: `virtualSiteForm.test.ts`, `virtualSiteBuild.test.ts`, `VirtualSiteSourcePanel.test.tsx`, `sitesApi.virtual.test.ts`
- Playwright: `modules/perc-qa-automation/frontend/tests/developer-site-virtual-source.spec.js` plus kind-option helper
- Product-docs: `product-docs/8.2/admin/sites.md`, `developer/virtual-sites.md`, `reference/site-config.md`
- Consumed (not re-implemented): REST persist #4114 / PR #4121 (SPI absorbed on main)

## Issues

None.

## Cross-platform path review

- [x] No new `".../" +` or `"...\\" +` filesystem joins
- [x] UI/tests use operator-style examples (`C:/sitemap-xml-docs`) as field values, not OS file joins
- [x] Playwright fill uses the same portable example style as HTTP JSON/SQL/CSV/object-storage/rss-atom/icalendar peers
- [x] Line-ending assertions not added
- [x] Client validation still rejects `..` in root path (server NIO remains source of truth)

## Tests

- `virtualSiteForm` — normalize sitemap-xml; unknown (`sql-api`) still repository; PUT clears leftover Git remotes and crawl URL; no password/Authorization/token; root-required / root-unsafe for sitemap-xml
- `virtualSiteBuild` — sitemap-xml does **not** show Build/Preview/Publish chrome
- `VirtualSiteSourcePanel` — option present; save GET-roundtrip; hint; chrome hidden; switch back to repository
- Playwright intercept save + live save+reload; required kind list includes sitemap-xml
