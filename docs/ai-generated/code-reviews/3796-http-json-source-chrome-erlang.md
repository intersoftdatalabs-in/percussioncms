# Erlang review — #3796 Developer Sites http-json source chrome

**Branch:** `feat/issue-3796-http-json-source-chrome`  
**Date:** 2026-08-25  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI panel + Vitest + Playwright + product-docs); Playwright HARD GATE for screen work; omit-vs-empty remoteUrl keep/clear (#3568); consume REST/SPI siblings without re-implementing them.

## Summary

Parent #2678 slice 3 of 3 (HTTP JSON source chrome). Developer Sites Virtual Site source panel offers **HTTP JSON** (`http-json`) as a source-kind option, peer of csv-filesystem (#3687) and sql-database (#3735). Operators can select it, set the existing **Root path** field, save, reload, and switch back to **Repository (traditional)**. Git remotes / config / site key stay Git-only. Catalog URL (`http.url`) or local fixture (`http.file`) stay in `_config.yaml`; PUT never sends Authorization or API keys. **Build / Preview / Publish chrome is intentionally hidden** for http-json (later phase). PUT sends `remoteUrl: ""` / `branch: ""` so a prior Git remote is cleared (REST 400 if leftover remote). SPI #3794 / PR #3798 is on `main` (allow-list includes `http-json`); this slice consumes it. REST OpenAPI/tests remain sibling #3795.

## Scope

- `WebUI/src/main/ts/developer/virtualSiteForm.ts` — `SOURCE_KIND_HTTP_JSON`, normalize, `formToVirtualProps`
- `WebUI/src/main/ts/developer/virtualSiteBuild.ts` — Build/Preview/Publish remain git/csv/sql only
- `WebUI/src/main/ts/developer/VirtualSiteSourcePanel.tsx` — select option + HTTP JSON hint + reuse root path
- `WebUI/src/main/ts/developer/messages.ts` — i18n keys (`perc.ui.developer@HTTP JSON`)
- `WebUI/src/main/ts/api/developer/types.ts` — allow-list javadoc
- Vitest: `virtualSiteForm.test.ts`, `virtualSiteBuild.test.ts`, `VirtualSiteSourcePanel.test.tsx`, `sitesApi.virtual.test.ts`
- Playwright: `modules/perc-qa-automation/frontend/tests/developer-site-virtual-source.spec.js`
- Product-docs: `product-docs/8.2/admin/sites.md`, `developer/virtual-sites.md`, `developer/rest.md`, `reference/site-config.md`
- Consumed (not re-implemented): SPI #3794 / REST helper allow-list on `main`
- No QA assignment

## Issues

None.

## Cross-platform path review

- [x] No new `".../" +` or `"...\\" +` filesystem joins
- [x] UI/tests use operator-style examples (`C:/http-json-docs`) as field values, not OS file joins
- [x] Playwright fill uses the same portable example style as SQL/CSV peers
- [x] Line-ending assertions not added

## Tests

- `virtualSiteForm` — normalize http-json; unknown (`sql-api`) still repository; PUT clears leftover Git remotes; no password/Authorization keys; root-required / root-unsafe for HTTP JSON
- `virtualSiteBuild` — http-json does **not** show Build/Preview/Publish chrome; git/csv/sql unchanged; repository / unknown stay hidden
- `VirtualSiteSourcePanel` — option list includes http-json; load root-only; save envelope; switch back to repository hides fields; no Build chrome after save
- Playwright — option present; HTTP JSON vs Git field visibility; mocked save envelope + GET round-trip; live save+reload then restore repository (H2 QA **20 passed**, including #3796 live + intercept tests)
- REST/SPI internals — N/A for this slice (consume #3794 allow-list; #3795 REST tests remain sibling)

## Change-class closure

| Companion | Status |
|-----------|--------|
| Source-kind select + form helpers | yes |
| Vitest panel/form/build | yes |
| Playwright `developer-site-virtual-source.spec.js` | yes |
| Product-docs 8.2 admin Sites | yes |
| REST/SPI internals | consume SPI #3794 on main (not re-implemented); Build chrome out of scope |
| Human QA assignment | not created |
