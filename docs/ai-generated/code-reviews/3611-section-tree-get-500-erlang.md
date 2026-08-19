# Erlang review — #3611 section/tree GET 500 FastForward NavTree

**Branch:** `fix/issue-3611-section-tree-get-500`  
**Base:** `origin/main`  
**Date:** 2026-08-19  
**Reviewer:** Erlang (independent pre-commit)  
**Memory patterns hit:** missing behavioral tests; change-class completeness (product-docs + unit tests); FastForward rffNav* vs percNav* dual ids

## Summary

H2 QA sample sites keep `rffNavTree` items at content type **315**. ItemDefManager catalogs those editors, but after `perc.nav` the JCR `ms_configuration` map often only has `percNavTree` **1017**. `findNodesByIds` then threw `No content type info found for content type id: 315`, and `GET /section/tree/Corporate_Investments` returned HTTP 500 so Architecture Create section never mounted treeitems.

Fix: perc/rff alias lookup in `PSContentRepository` (shared `RXS_CT_NAV*` tables). Missing-NavTree empty-200 is unchanged. Does not seed a second NavTree. Does not allowlist 500.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral tests cover alias selection, existing-rff load without seed, and REST not mapping type-315 failures to empty-200.

Cross-platform path checklist: **clean** — no filesystem path construction.

## Issues

None (blocking).

## Change-class closure

| Companion | Status |
|-----------|--------|
| JCR type lookup alias (313–315 ↔ 1015–1017) | Done |
| `PSNavNameAliases.findRegisteredNavAliasTypeId` + tests | Done |
| sitemanage loadTree existing-rff / REST no-empty-on-315 | Done |
| `product-docs/8.2/admin/architecture-navigation.md` | Done |
| Do not seed second NavTree / do not allowlist 500 | Honored |

## Tests / builds observed

- `cd system && ../mvnw.cmd clean install` BUILD SUCCESS — `PSNavNameAliasesTest` Tests run: 5; `PSServicesContentmgrTypedTest` Tests run: 6
- `cd rest && ../mvnw.cmd clean install` BUILD SUCCESS (stale SNAPSHOT so sitemanage compiled)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` BUILD SUCCESS — `PSSiteSectionServiceLoadTreeEmptyTest` Tests run: 8; `PSSiteSectionRestServiceLoadTreeEmptyTest` Tests run: 6
- Live H2: `GET …/section/tree/Corporate_Investments` HTTP 200 with SectionNode children after perc-system hot-deploy
- Playwright: `npm run test:surface -- --path tests/architecture-create-section-noskip.spec.js` 2 passed; console-clean=yes; no new type-315 ERROR after Jetty restart
