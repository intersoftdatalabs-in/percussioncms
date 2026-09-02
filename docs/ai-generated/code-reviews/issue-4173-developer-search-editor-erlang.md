# Erlang review — issue #4173 Developer search editor persist / 409

**Branch:** `fix/issue-4173-developer-search-editor`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Date:** 2026-09-02  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** H2 UI-06 POST 200 then GET list 0 / no 409; `ensureSearchRowPersisted` skipped on SEARCHID collision; catalog reload must not inherit HTML SEARCHID; SPA must keep created catalog row; behavioral tests for persist + 409; peer Views #4175 / PR #4178

## Summary

H2 QA `developer-search-editor.spec.js` failed because create POST could return 200 while `GET /services/searches` omitted the new name (`[data-sr-name]` count 0) and a second POST never 409'd.

Root cause is the same catalog family as views (shared `PSX_SEARCHES`): `ensureSearchRowPersisted` treated `SEARCHID OR INTERNALNAME` as “already persisted”. H2 next-number colliding with a seed row (e.g. Inbox SEARCHID=3) skipped the INSERT for the new **search** name. `saveSearches` also restored HTML `SEARCHID` before `findAllSearches`. Unique check used only name summaries, not `findAllSearches` / `findAllViews`.

Fix:

1. JDBC ensure inserts when **name** is missing; colliding SEARCHID gets `MAX(SEARCHID)+1`.
2. Do not restore HTML SEARCHID after save.
3. Unique check also uses `findAllSearches` / `findAllViews`.
4. SPA `upsertSearchRow` keeps the created row if GET list lags.

## Cross-platform path checklist

N/A — JDBC uses constant table/column names, not filesystem path joins. Playwright uses URL paths only.

## Issues

None (hard-gate). Behavioral tests:

- `PSUiDesignWsViewPersistTest` — TYPE_STANDARDSEARCH insert when SEARCHID collides (id 3 Inbox + QaNewSearch → 4)
- `SearchAdaptorWriteTest` — create persist; 409 from findSearches and from findAllSearches
- Vitest `SearchesPanel` — upsert by name; catalog still shows created row when GET list omits it

Product-docs: `product-docs/8.2/admin/developer-searches.md` — catalog lists after create; duplicate 409 stays on the editor.
