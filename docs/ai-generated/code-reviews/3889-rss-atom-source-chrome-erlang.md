# Erlang review — #3889 Developer Sites rss-atom source chrome

**Branch:** `feat/issue-3889-rss-atom-source-chrome`  
**Date:** 2026-08-27  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI panel + Vitest + Playwright + product-docs); Playwright HARD GATE for screen work; omit-vs-empty remoteUrl keep/clear (#3568); consume REST/SPI siblings without re-implementing them; no secrets on REST envelope.

## Summary

Parent #2678 slice (rss-atom source chrome). Developer Sites Virtual Site source panel offers **RSS / Atom** (`rss-atom`) as a source-kind option, peer of object-storage (#3856) and http-json (#3796). Operators can select it, set the existing **Root path** field, save, reload, and switch back to **Repository (traditional)**. Git remotes / config / site key stay Git-only. PUT never sends live feed URLs or credentials (`remoteUrl: ""` / `branch: ""` so a prior Git remote is cleared). **Build / Preview / Publish chrome is intentionally hidden** for rss-atom (later phase unless REST Build is on `main`; it is not). REST persist is sibling #3888 / PR #3898 — this slice consumes the contract and does not re-implement REST/SPI.

## Scope

- `WebUI/src/main/ts/developer/virtualSiteForm.ts` — `SOURCE_KIND_RSS_ATOM`, normalize, `formToVirtualProps`
- `WebUI/src/main/ts/developer/virtualSiteBuild.ts` — comments only; Build/Preview/Publish remain git/csv/sql/http-json (+ object-storage Build/Preview)
- `WebUI/src/main/ts/developer/VirtualSiteSourcePanel.tsx` — select option + rss-atom hint + reuse root path
- `WebUI/src/main/ts/developer/messages.ts` — i18n keys (`perc.ui.developer@RSS / Atom`)
- `WebUI/src/main/ts/api/developer/types.ts` — allow-list javadoc
- Vitest: `virtualSiteForm.test.ts`, `virtualSiteBuild.test.ts`, `VirtualSiteSourcePanel.test.tsx`, `sitesApi.virtual.test.ts`
- Playwright: `modules/perc-qa-automation/frontend/tests/developer-site-virtual-source.spec.js`
- Product-docs: `product-docs/8.2/admin/sites.md`, `developer/virtual-sites.md`
- Consumed (not re-implemented): REST persist #3888 / PR #3898
- No QA assignment in this slice (human QA candidacy only after C5)

## Issues

None.

## Cross-platform path review

- [x] No new `".../" +` or `"...\\" +` filesystem joins
- [x] UI/tests use operator-style examples (`C:/rss-atom-docs`) as field values, not OS file joins
- [x] Playwright fill uses the same portable example style as HTTP JSON/SQL/CSV/object-storage peers
- [x] Line-ending assertions not added
- [x] Client validation still rejects `..` in root path (server NIO remains source of truth)

## Tests

- `virtualSiteForm` — normalize rss-atom; unknown (`sql-api`) still repository; PUT clears leftover Git remotes and live feed URL; no password/Authorization/token; root-required / root-unsafe for rss-atom
- `virtualSiteBuild` — rss-atom does **not** show Build/Preview/Publish chrome; git/csv/sql/http-json/object-storage unchanged; repository / unknown stay hidden
- `VirtualSiteSourcePanel` — option list includes rss-atom; load root-only; save envelope; switch back to repository hides fields; no Build chrome after save
- Playwright — option present; unknown `sql-api` absent; rss-atom vs Git field visibility; intercepted PUT/GET envelope; live save+reload then restore repository
- REST/SPI internals — N/A for this slice (consume #3888 allow-list; Build chrome out of scope)

## Change-class closure

| Companion | Status |
|-----------|--------|
| Source-kind select + form helpers | yes |
| Vitest panel/form/build | yes |
| Playwright `developer-site-virtual-source.spec.js` | yes |
| Product-docs 8.2 admin Sites | yes |
| REST/SPI internals | consume REST persist #3888 / PR #3898 (not re-implemented); Build chrome out of scope |
| Human QA assignment | not created |

## Gate

- Blocking bugs: 0
- May commit/push: yes
