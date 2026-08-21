# Erlang review — #3687 Developer Sites CSV filesystem source kind UI

**Branch:** `feat/issue-3687-csv-filesystem-source-kind-ui`  
**Date:** 2026-08-21  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI panel + Vitest + Playwright + product-docs); Playwright HARD GATE for screen work; omit-vs-empty remoteUrl keep/clear (#3568); in-product Build stays git-filesystem only.

## Summary

Parent #2678 slice 3 of 3. Developer Sites Virtual Site source panel offers **CSV filesystem** (`csv-filesystem`) as a source-kind option, peer of git-filesystem (#2956). Operators can select it, set the existing **Root path** field, save, reload, and switch back to **Repository (traditional)** which hides virtual fields (save still clears `virtual.*`). Git remotes / config / site key stay Git-only. In-product Build/Publish chrome remains git-filesystem only (CSV assemble is SPI/offline; REST build is 400 for CSV — #3686). PUT for CSV sends `remoteUrl: ""` / `branch: ""` so a prior Git remote is cleared (omit would keep it and REST would 400). Stacked on REST #3691 so live save+reload can persist.

## Scope

- `WebUI/src/main/ts/developer/virtualSiteForm.ts` — `SOURCE_KIND_CSV_FILESYSTEM`, normalize, `formToVirtualProps`
- `WebUI/src/main/ts/developer/virtualSiteBuild.ts` — Build/Publish chrome git-filesystem only
- `WebUI/src/main/ts/developer/VirtualSiteSourcePanel.tsx` — select option + CSV hint + reuse root path
- `WebUI/src/main/ts/developer/messages.ts` — i18n keys (`perc.ui.developer@CSV filesystem`)
- Vitest: `virtualSiteForm.test.ts`, `virtualSiteBuild.test.ts`, `VirtualSiteSourcePanel.test.tsx`, `sitesApi.virtual.test.ts`
- Playwright: `modules/perc-qa-automation/frontend/tests/developer-site-virtual-source.spec.js`
- Product-docs: `product-docs/8.2/admin/sites.md` (stable `id: admin-sites`)
- No SPI/REST internals (slices #3685/#3686). No QA #2962 assignment.

## Issues

None.

## Cross-platform path review

- [x] No new `".../" +` or `"...\\" +` filesystem joins
- [x] UI/tests use operator-style examples (`C:/csv-docs`) as field values, not OS file joins
- [x] Playwright fill uses the same portable example as REST #3686 tests
- [x] Line-ending assertions not added

## Tests

- `virtualSiteForm` — normalize csv-filesystem; PUT clears leftover Git remotes; root-required / root-unsafe for CSV
- `virtualSiteBuild` — csv-filesystem does **not** show Build/Publish chrome
- `VirtualSiteSourcePanel` — option list includes csv-filesystem; load root-only; save envelope; switch back to repository hides fields
- Playwright — option present; CSV vs Git field visibility; mocked save envelope + GET round-trip; live save+reload then restore repository
- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS, Tests 2988 passed (focused Vitest 30 passed after remote-clear fix)

## Change-class closure

| Companion | Status |
|-----------|--------|
| Source-kind select + form helpers | yes |
| Vitest panel/form/build | yes |
| Playwright `developer-site-virtual-source.spec.js` | yes |
| Product-docs 8.2 admin Sites | yes |
| REST/SPI internals | N/A (slices #3686 / #3685) |
| Human QA #2962 | not stolen |
