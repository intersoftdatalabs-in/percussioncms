# Erlang review — issue #4014 REST AS-07 item-filter write

**Branch:** `fix/issue-4014-itemfilter-write`  
**Scope:** uncommitted vs HEAD (rest + sitemanage + product-docs 8.2)  
**Date:** 2026-08-30  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** change-class completeness (rest resource + existing adaptor interface + Spring stub + sitemanage impl + tests); Admin `IPSUserService.isAdminUser` 403; design-WS create/save/release; in-use delete 409; no stolen locks

## Summary

Admin POST/PUT/DELETE on `/services/itemfilters` persist assembly item filters (name, description, rules, parent filter) through existing `IItemFilterAdaptor.updateOrCreateItemFilter` / `deleteItemFilter`. sitemanage `ItemFilterAdaptor` uses `IPSSystemDesignWs.createItemFilters` / `loadItemFilters` / `saveItemFilters` / `deleteItemFilters` (same system design WS SOAP uses). No new SOAP. Duplicate name is 409. Unknown id is 404. In-use delete (content-list dependents) is 409. Non-Admin and missing session/user are 403. GET list/detail is unchanged as a catalog read and round-trips created filters.

Change-class companions: OpenAPI resource, Mockito `ItemFilterResourceTest` (18), Spring `TestItemFilterAdaptor` stub (`@Component` `@Lazy`), adaptor write tests (18) plus existing safe-key tests, product-docs 8.2 developer REST AS-07 write note. Reverse-deps of `IItemFilterAdaptor` are only the production adaptor and the rest test stub; interface signatures unchanged.

## Issues

None that block.

## Cross-platform path checklist

N/A — no filesystem path construction. Filter keys reject `/`, `\`, `..`, and NUL (existing `isSafeFilterKey`).

## Tests / evidence

- `cd rest && ../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 877, Failures: 0 (`ItemFilterResourceTest` 18/0)
- `cd projects/sitemanage && ../../mvnw.cmd clean install` — BUILD SUCCESS; Tests run: 1892, Failures: 0, Skipped: 125 (`ItemFilterAdaptorWriteTest` 18/0; `ItemFilterAdaptorSafeKeyTest` 1/0)
