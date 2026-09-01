# Erlang review — issue #4095 view create persist

**Branch:** `fix/issue-4095-view-create-persist`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Date:** 2026-09-01  
**Recommendation:** approve (C5 Playwright 3 passed: create listed, delete omits, duplicate 409, Inbox not deleted)  
**Gate:** May commit/push: yes  
**Memory patterns hit:** behavioral tests for new persist logic; change-class companions (adaptor + design WS + product-docs + XML app cache); no path I/O in this diff (SQL identifiers are constants)

## Summary

H2 REST UI-07 create returned 200 then GET list/detail missed the new name (`[data-vw-name]` count 0; duplicate 409 never surfaced) because:

1. `ViewAdaptor.loadAllViews` used `findViews` + `loadViews`, and H2 `loadViews` remaps rows to `View_All` (same hole as UI-06). List now uses `findAllViews()`.
2. `saveViews` delegates to `saveSearches`. `updateSearches` Dataset431 (`SEARCHID` HTML IS NOT NULL) is DELETE-only (`allowInserts=no`). Injecting HTML `SEARCHID` skipped Dataset11143 INSERT.
3. `getSearches*` resource cache was keyed only by `sys_lang`, so create-then-list stayed stale.

Fix (peer #4084 / PR #4088): clear inherited `SEARCHID` on save; skip delete-then-insert for unpersisted locks; JDBC `PSX_SEARCHES` ensure-insert when the XML resource writes 0 rows; disable `getSearches*` resource cache; adaptor reload requires catalog visibility. Views share `PSX_SEARCHES`; `invalidateSearchCatalog` also evicts `ALL_VIEWS_CACHE_KEY`.

## Cross-platform path checklist

N/A — JDBC uses constant table/column names, not filesystem paths.

## Issues

None (hard-gate). Behavioral tests:

- `PSUiDesignWsViewPersistTest` — view TYPE INSERT state; `searchRowSpec` mapping; H2 mem INSERT visible to SELECT
- `PSUiDesignWsSearchPersistTest` — shared saveSearches persist helpers
- `ViewAdaptorWriteTest` — create then `findAllViews`; 500 if catalog miss; 409 duplicate
- `ViewAdaptorExecuteTest` — Inbox synthesis when catalog is View_All / empty

Product-docs (`product-docs/8.2/developer/rest.md`, `admin/developer-views.md`) state list+GET-by-name after create.

## Suggestion (non-blocking)

`reloadAfterWrite` is adaptor-level insurance; the durable fix is `PSUiDesignWs.ensureSearchRowPersisted` plus Dataset11143 selection. Keep both: POST must not return 200 for a row `findViews` cannot see. Overlaps PR #4088 on `saveSearches`; views need that shared INSERT path.
