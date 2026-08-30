# Erlang review — #3983 Developer Sites icalendar source chrome

**Branch:** `feat/issue-3983-icalendar-source-chrome`  
**Date:** 2026-08-28  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class companions (WebUI panel + Vitest + Playwright + product-docs); Playwright HARD GATE for screen work; omit-vs-empty remoteUrl keep/clear (#3568); consume REST/SPI siblings without re-implementing them; no secrets on REST envelope.

## Summary

Parent #2678 slice 3 (icalendar source chrome). Developer Sites Virtual Site source panel offers **iCalendar** (`icalendar`) as a source-kind option, peer of rss-atom (#3889). Operators can select it, set the existing **Root path** field, save, GET-roundtrip, and switch back to **Repository (traditional)**. Git remotes / config / site key stay Git-only. PUT never sends CalDAV URLs or credentials (`remoteUrl: ""` / `branch: ""` so a prior Git remote is cleared). **Build / Preview / Publish chrome is intentionally hidden** for icalendar (later phase). REST persist is sibling #3982 / PR #3990; SPI is sibling #3986 / PR #3987 — this slice consumes the contract and does not re-implement REST/SPI.

## Scope

- `WebUI/src/main/ts/developer/virtualSiteForm.ts` — `SOURCE_KIND_ICALENDAR`, normalize, `formToVirtualProps`
- `WebUI/src/main/ts/developer/virtualSiteBuild.ts` — comments only; Build/Preview/Publish remain git/csv/sql/http-json/object-storage/rss-atom
- `WebUI/src/main/ts/developer/VirtualSiteSourcePanel.tsx` — select option + icalendar hint + reuse root path
- `WebUI/src/main/ts/developer/messages.ts` — i18n keys (`perc.ui.developer@iCalendar`)
- `WebUI/src/main/ts/api/developer/types.ts` — allow-list javadoc
- Vitest: `virtualSiteForm.test.ts`, `virtualSiteBuild.test.ts`, `VirtualSiteSourcePanel.test.tsx`, `sitesApi.virtual.test.ts`
- Playwright: `modules/perc-qa-automation/frontend/tests/developer-site-virtual-source.spec.js`
- Product-docs: `product-docs/8.2/admin/sites.md`, `developer/virtual-sites.md`
- Consumed (not re-implemented): REST persist #3982 / PR #3990 and SPI #3986 / PR #3987
- No QA assignment in this slice (human QA candidacy only after C5)

## Issues

None.

## Cross-platform path review

- [x] No new `".../" +` or `"...\\" +` filesystem joins
- [x] UI/tests use operator-style examples (`C:/icalendar-docs`) as field values, not OS file joins
- [x] Playwright fill uses the same portable example style as HTTP JSON/SQL/CSV/object-storage/rss-atom peers
- [x] Line-ending assertions not added
- [x] Client validation still rejects `..` in root path (server NIO remains source of truth)

## Tests

- `virtualSiteForm` — normalize icalendar; unknown (`sql-api`) still repository; PUT clears leftover Git remotes and CalDAV URL; no password/Authorization/token; root-required / root-unsafe for icalendar
- `virtualSiteBuild` — icalendar does **not** show Build/Preview/Publish chrome; git/csv/sql/http-json/object-storage/rss-atom unchanged; repository / unknown stay hidden
- `VirtualSiteSourcePanel` — option list includes icalendar; load root-only; save envelope; switch back to repository hides fields; no Build chrome after save
- Playwright — option present; unknown `sql-api` absent; icalendar vs Git field visibility; intercepted PUT/GET envelope; live save+reload then restore repository
- REST/SPI internals — N/A for this slice (consume #3982 allow-list; Build chrome out of scope)

## Change-class closure

| Companion | Status |
|-----------|--------|
| Source-kind select + form helpers | yes |
| Vitest panel/form/build | yes |
| Playwright `developer-site-virtual-source.spec.js` | yes |
| Product-docs 8.2 admin Sites | yes |
| REST/SPI internals | consume REST persist #3982 / PR #3990 (not re-implemented); Build chrome out of scope |
| Human QA assignment | not created |

## Gate

- Blocking bugs: 0
- May commit/push: yes
