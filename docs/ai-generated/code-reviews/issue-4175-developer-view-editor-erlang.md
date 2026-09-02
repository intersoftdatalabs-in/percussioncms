# Erlang review — issue #4175 Developer view editor persist / 409

**Branch:** `fix/issue-4175-developer-view-editor`  
**Scope:** uncommitted vs `HEAD` / `origin/main`  
**Date:** 2026-09-02  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** H2 UI-07 POST 200 then GET list 0 / no 409; `ensureSearchRowPersisted` skipped on SEARCHID collision; catalog reload must not inherit HTML SEARCHID; SPA must keep created catalog row; behavioral tests for persist + 409

## Summary

H2 QA `developer-view-editor.spec.js` failed because create POST could return 200 while `GET /services/views` omitted the new name (`[data-vw-name]` count 0) and a second POST never 409'd.

Root cause: `ensureSearchRowPersisted` treated `SEARCHID OR INTERNALNAME` as “already persisted”. H2 next-number colliding with a seed row (e.g. Inbox SEARCHID=3) skipped the INSERT for the new name. `saveSearches` also restored HTML `SEARCHID` before `findAllViews`, which can select the SEARCHID-IS-NOT-NULL getSearches dataset.

Fix:

1. JDBC ensure inserts when **name** is missing; colliding SEARCHID gets `MAX(SEARCHID)+1`.
2. Do not restore HTML SEARCHID after save.
3. Unique check also uses `findAllViews` / `findAllSearches`.
4. SPA `upsertViewRow` keeps the created row if GET list lags.

## Cross-platform path checklist

N/A — JDBC uses constant table/column names, not filesystem path joins. Playwright uses URL paths only.

## Issues

None (hard-gate). Behavioral tests:

- `PSUiDesignWsViewPersistTest` — TYPE_VIEW insert; name-exists vs SEARCHID collision insert (id 3 Inbox + QaNewView → 4)
- `ViewAdaptorWriteTest` — create persist; 409 from findViews and from findAllViews
- Vitest `ViewsPanel` — upsert by name; catalog still shows created row when GET list omits it

Product-docs: `product-docs/8.2/admin/developer-views.md` — catalog lists after create; duplicate 409 stays on the editor.
