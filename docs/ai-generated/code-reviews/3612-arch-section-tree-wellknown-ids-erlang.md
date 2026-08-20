# Erlang review — #3612 Architecture section/tree GET 200 (well-known perc/rff ids)

**Branch:** `fix/issue-3612-arch-section-tree-200`  
**Base:** `origin/main`  
**Date:** 2026-08-19  
**Reviewer:** Erlang (independent pre-commit)  
**Memory patterns hit:** missing behavioral tests; change-class completeness (product-docs + unit tests + Playwright); FastForward rffNav* vs percNav* dual ids

## Summary

H2 QA FastForward sample sites keep `rffNavTree` items at content type **315**. After `perc.nav`, ItemDef no longer catalogs 313–315 (`without a known content type` on lucene indexes 313–315; `Invalid content type id (313)` at startup). The prior perc/rff JCR alias required ItemDef names for both the missing and registered ids, so `GET /sitemanage/section/tree/Corporate_Investments` still returned HTTP 500 (`No content type info found for content type id: 315`) on the cluster tip / skip-image-build cell.

Fix: infer nav role from well-known FastForward ids 313–315 and perc.nav ids 1015–1017 when the catalog has no name. Name-based alias still wins when present. Playwright bookmark / `?view=arch` wait for section/tree GET 200 and do not allowlist 500. Retired `siteArchitecture.jsp` host is not restored.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral tests cover name-known alias, name-unknown 315→1017, both-unknown well-known ids, and non-nav id 42. Live H2 GET 200 with SectionNode children after perc-system hot-deploy. Playwright `architecture-legacy-redirect.spec.js` 2 passed.

Cross-platform path checklist: **clean** — no filesystem path construction.

## Issues

None (blocking).

## Change-class closure

| Companion | Status |
|-----------|--------|
| JCR type lookup alias (313–315 ↔ 1015–1017) + well-known id role | Done |
| `PSNavNameAliases.findRegisteredNavAliasTypeId` + tests | Done |
| sitemanage loadTree existing-rff / REST no-empty-on-315 | Done (cherry-pick #3611) |
| Playwright bookmark / `?view=arch` GET 200, no 500 allowlist | Done |
| `product-docs/8.2/admin/architecture-navigation.md` | Done |

## Tests / builds observed

- `cd system && ../mvnw.cmd clean install` BUILD SUCCESS — Tests run: 2220, Failures: 0, Skipped: 238. `PSNavNameAliasesTest` 6/6; `PSServicesContentmgrTypedTest` 7/7.
- `cd projects/sitemanage && ../../mvnw.cmd clean install` BUILD SUCCESS — Tests run: 1266, Failures: 0, Skipped: 125. LoadTree empty tests 8+6.
- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` BUILD SUCCESS. `npm run test:unit` 334 passed.
- Live H2: `GET …/section/tree/Corporate_Investments` HTTP 200 with child sections after perc-system + sitemanage hot-deploy.
- Playwright: `npm run test:surface -- --path tests/architecture-legacy-redirect.spec.js` 2 passed; console-clean=yes; no new type-315 ERROR after Jetty restart.
