# Erlang review — issue #4084 search create persist

**Branch:** `fix/issue-4084-search-create-persist`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Date:** 2026-08-31  
**Recommendation:** approve (C5 Playwright 2 passed after catalog-first loadSearches + JDBC ensure-delete)  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for new persist logic; change-class companions (adaptor + design WS + product-docs + XML app cache); no path I/O in this diff (SQL identifiers are constants)

## Summary

H2 REST UI-06 create returned 200 then GET list/detail 404 because:

1. `SearchAdaptor.loadCatalog` used `findSearches` + `loadSearches`, and H2 `loadSearches` remaps rows to `View_All` (same hole as UI-07). List now uses `findAllSearches()`.
2. `updateSearches` Dataset431 (`SEARCHID` HTML IS NOT NULL) is DELETE-only (`allowInserts=no`). Injecting HTML `SEARCHID` (or inheriting it) skipped the Action/@dbAction INSERT pipe (Dataset11143, `SEARCHID` IS NULL).
3. `getSearches*` resource cache was keyed only by `sys_lang`, so create-then-list stayed stale.

Fix: clear inherited `SEARCHID` on save; skip delete-then-insert for unpersisted locks; JDBC `PSX_SEARCHES` ensure-insert when the XML resource writes 0 rows; disable `getSearches*` resource cache; adaptor reload requires catalog visibility.

## Cross-platform path checklist

N/A — JDBC uses constant table/column names, not filesystem paths.

## Issues

None (hard-gate). Behavioral tests:

- `PSUiDesignWsSearchPersistTest` (6) — community/INSERT state; cache flush; `searchRowSpec` mapping; H2 mem INSERT visible to SELECT
- `SearchAdaptorWriteTest` — create then `findAllSearches`; 500 if catalog miss; 409 duplicate
- `PSSearchXmlRoundTripTest.standardSearchXmlIsNotAView`

Product-docs (`product-docs/8.2/developer/rest.md`, `admin/developer-searches.md`) state list+GET-by-name after create.

## Suggestion (non-blocking)

`reloadAfterWrite` is adaptor-level insurance; the durable fix is `PSUiDesignWs.ensureSearchRowPersisted` plus Dataset11143 selection. Keep both: POST must not return 200 for a row `findSearches` cannot see.
