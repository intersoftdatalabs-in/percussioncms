# Erlang review — #3611 section/tree GET 500 FastForward NavTree

**Branch:** `fix/issue-3611-rff-navtree-jcr-alias`  
**Base:** `origin/main`  
**Date:** 2026-08-19  
**Reviewer:** Erlang (independent pre-commit)  
**Memory patterns hit:** missing behavioral tests; change-class completeness (product-docs + unit tests); FastForward rffNav* vs percNav* dual ids; ItemDef catalog drop of 313–315 names

## Summary

H2 QA sample sites keep `rffNavTree` items at content type **315**. ItemDefManager may catalog those editors, but after `perc.nav` the JCR `ms_configuration` map often only has `percNavTree` **1017**. Live H2 also drops FastForward names from the running catalog (`Invalid content type id (313)`), so a name-only perc/rff alias still 500s. `findNodesByIds` then threw `No content type info found for content type id: 315`, and `GET /section/tree/Corporate_Investments` returned HTTP 500 so Architecture Create section never mounted treeitems.

Fix: perc/rff alias lookup in `PSContentRepository` (shared `RXS_CT_NAV*` tables) plus well-known id roles 313–315 ↔ 1015–1017 when the catalog has no name. Missing-NavTree empty-200 is unchanged. Does not seed a second NavTree. Does not allowlist 500.

## Recommendation

**approve**

## Gate

**May commit/push: yes**

No blocking bugs. Behavioral tests cover name-based alias selection, well-known ids when names are missing, existing-rff load without seed, and REST not mapping type-315 failures to empty-200.

Cross-platform path checklist: **clean** — no filesystem path construction.

## Issues

None (blocking).

## Change-class closure

| Companion | Status |
|-----------|--------|
| JCR type lookup alias (313–315 ↔ 1015–1017) | Done |
| Well-known id role when ItemDef has no name | Done |
| `PSNavNameAliases.findRegisteredNavAliasTypeId` + tests | Done |
| sitemanage loadTree existing-rff / REST no-empty-on-315 | Done |
| `product-docs/8.2/admin/architecture-navigation.md` (tree GET 200 + Create section enabled) | Done |
| Playwright `architecture-create-section-noskip.spec.js` (no skip) | Existing; C5 proof required |
| Do not seed second NavTree / do not allowlist 500 | Honored |

## Tests / builds observed

- `cd system && ../mvnw.cmd clean install` BUILD SUCCESS (01:51). `PSNavNameAliasesTest` Tests run: 6, Failures: 0. `PSServicesContentmgrTypedTest` Tests run: 7, Failures: 0.
- `cd projects/sitemanage && ../../mvnw.cmd clean install` BUILD SUCCESS (58.7s). `PSSiteSectionServiceLoadTreeEmptyTest` Tests run: 8, Failures: 0. `PSSiteSectionRestServiceLoadTreeEmptyTest` Tests run: 6, Failures: 0.
- C2: no `extends PSNavNameAliases` / `new PSNavNameAliases() {` / `extends PSContentRepository` / `new PSContentRepository() {`. Additive public helpers only.
