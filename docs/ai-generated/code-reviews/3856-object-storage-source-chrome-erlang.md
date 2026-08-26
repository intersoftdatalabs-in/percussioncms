# Erlang review — #3856 Developer Sites object-storage source chrome

**Branch:** `feat/issue-3856-object-storage-source-chrome`  
**Date:** 2026-08-26  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI panel + Vitest + Playwright + product-docs); Playwright HARD GATE for screen work; omit-vs-empty remoteUrl keep/clear (#3568); consume REST/SPI siblings without re-implementing them; no secrets on REST envelope.

## Summary

Parent #2678 slice (object-storage source chrome). Developer Sites Virtual Site source panel offers **Object storage** (`object-storage`) as a source-kind option, peer of http-json (#3796). Operators can select it, set the existing **Root path** field, save, reload, and switch back to **Repository (traditional)**. Git remotes / config / site key stay Git-only. PUT never sends cloud URLs, IAM, or access keys. **Build / Preview / Publish chrome is intentionally hidden** for object-storage (later phase; slices #3857/#3858). PUT sends `remoteUrl: ""` / `branch: ""` so a prior Git remote is cleared (REST 400 if leftover remote). SPI #3838 / PR #3844 and REST persist #3839 / PR #3849 are on `main`; this slice consumes them.

## Scope

- `WebUI/src/main/ts/developer/virtualSiteForm.ts` — `SOURCE_KIND_OBJECT_STORAGE`, normalize, `formToVirtualProps`
- `WebUI/src/main/ts/developer/virtualSiteBuild.ts` — Build/Preview/Publish remain git/csv/sql/http-json only
- `WebUI/src/main/ts/developer/VirtualSiteSourcePanel.tsx` — select option + object-storage hint + reuse root path
- `WebUI/src/main/ts/developer/messages.ts` — i18n keys (`perc.ui.developer@Object storage`)
- `WebUI/src/main/ts/api/developer/types.ts` — allow-list javadoc
- Vitest: `virtualSiteForm.test.ts`, `virtualSiteBuild.test.ts`, `VirtualSiteSourcePanel.test.tsx`, `sitesApi.virtual.test.ts`
- Playwright: `modules/perc-qa-automation/frontend/tests/developer-site-virtual-source.spec.js`
- Product-docs: `product-docs/8.2/admin/sites.md`, `developer/virtual-sites.md`, `developer/rest.md`, `reference/site-config.md`, `admin/publishing.md`
- Consumed (not re-implemented): SPI #3838 / REST persist #3839 on `main`
- No QA assignment in this slice (human QA candidacy only after C5)

## Issues

None.

## Cross-platform path review

- [x] No new `".../" +` or `"...\\" +` filesystem joins
- [x] UI/tests use operator-style examples (`C:/object-docs`) as field values, not OS file joins
- [x] Playwright fill uses the same portable example style as HTTP JSON/SQL/CSV peers
- [x] Line-ending assertions not added
- [x] Client validation still rejects `..` in root path (server NIO remains source of truth)

## Tests

- `virtualSiteForm` — normalize object-storage; unknown (`sql-api`) still repository; PUT clears leftover Git remotes; no password/Authorization/IAM/s3 keys; root-required / root-unsafe for object-storage
- `virtualSiteBuild` — object-storage does **not** show Build/Preview/Publish chrome; git/csv/sql/http-json unchanged; repository / unknown stay hidden
- `VirtualSiteSourcePanel` — option list includes object-storage; load root-only; save envelope; switch back to repository hides fields; no Build chrome after save
- Playwright — option present; unknown `sql-api` absent; object-storage vs Git field visibility; live save+reload then restore repository
- REST/SPI internals — N/A for this slice (consume #3838/#3839 allow-list; Build chrome out of scope)

## Change-class closure

| Companion | Status |
|-----------|--------|
| Source-kind select + form helpers | yes |
| Vitest panel/form/build | yes |
| Playwright `developer-site-virtual-source.spec.js` | yes |
| Product-docs 8.2 admin Sites | yes |
| REST/SPI internals | consume SPI #3838 + REST #3839 on main (not re-implemented); Build chrome out of scope |
| Human QA assignment | not created |

## Gate

- Blocking bugs: 0
- May commit/push: yes
