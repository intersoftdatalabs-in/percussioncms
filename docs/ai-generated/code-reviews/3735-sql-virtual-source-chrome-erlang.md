# Erlang review — #3735 Developer Sites SQL virtual source chrome

**Branch:** `feat/issue-3735-sql-virtual-source-chrome`  
**Date:** 2026-08-23  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI panel + Vitest + Playwright + product-docs); Playwright HARD GATE for screen work; omit-vs-empty remoteUrl keep/clear (#3568); consume REST/SPI siblings without re-implementing them.

## Summary

Parent #2678 slice. Developer Sites Virtual Site source panel offers **SQL database** (`sql-database`) as a source-kind option, peer of csv-filesystem (#3687). Operators can select it, set the existing **Root path** field, save, reload, and switch back to **Repository (traditional)** which hides Virtual Preview/Build/Publish chrome. Git remotes / config / site key stay Git-only. JDBC URL/user/query stay in `_config.yaml`; PUT never sends a password. Build/Publish/Preview chrome shows for sql-database (REST #3734 / SPI #3733). PUT for SQL sends `remoteUrl: ""` / `branch: ""` so a prior Git remote is cleared (REST 400 if leftover remote). Stacked on REST #3746 so live save+reload can persist.

## Scope

- `WebUI/src/main/ts/developer/virtualSiteForm.ts` — `SOURCE_KIND_SQL_DATABASE`, normalize, `formToVirtualProps`
- `WebUI/src/main/ts/developer/virtualSiteBuild.ts` — Build/Publish chrome includes sql-database
- `WebUI/src/main/ts/developer/VirtualSiteSourcePanel.tsx` — select option + SQL hint + reuse root path
- `WebUI/src/main/ts/developer/messages.ts` — i18n keys (`perc.ui.developer@SQL database`)
- `WebUI/src/main/ts/api/developer/types.ts` — allow-list javadoc
- Vitest: `virtualSiteForm.test.ts`, `virtualSiteBuild.test.ts`, `VirtualSiteSourcePanel.test.tsx`, `sitesApi.virtual.test.ts`
- Playwright: `modules/perc-qa-automation/frontend/tests/developer-site-virtual-source.spec.js`
- Product-docs: `product-docs/8.2/admin/sites.md`, `developer/virtual-sites.md`, `reference/site-config.md`
- Stacked (not re-implemented): SPI #3733 / REST #3734
- No QA #2962 assignment

## Issues

None.

## Cross-platform path review

- [x] No new `".../" +` or `"...\\" +` filesystem joins
- [x] UI/tests use operator-style examples (`C:/sql-docs`) as field values, not OS file joins
- [x] Playwright fill uses the same portable example as REST #3734 tests
- [x] Line-ending assertions not added

## Tests

- `virtualSiteForm` — normalize sql-database; PUT clears leftover Git remotes; no password key; root-required / root-unsafe for SQL
- `virtualSiteBuild` — sql-database shows Build/Publish chrome; repository / unknown (`sql-api`) do not
- `VirtualSiteSourcePanel` — option list includes sql-database; load root-only; save envelope; switch back to repository hides Preview/Build
- Playwright — option present; SQL vs Git field visibility; mocked save envelope + GET round-trip; live save+reload then restore repository
- REST/SPI internals — N/A for this slice (consume #3734 / #3733)

## Change-class closure

| Companion | Status |
|-----------|--------|
| Source-kind select + form helpers | yes |
| Vitest panel/form/build | yes |
| Playwright `developer-site-virtual-source.spec.js` | yes |
| Product-docs 8.2 admin Sites | yes |
| REST/SPI internals | stacked consume #3734 / #3733 (not re-implemented) |
| Human QA #2962 | not stolen |
