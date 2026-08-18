# Erlang review — #3558 path/item rollback-only 500

**Scope:** `fix/issue-3558-subfolder-copy-rollback-500` vs `cluster/night-issue-20260818-explorer-shell`.
**Memory patterns hit:** Spring TX rollback-only from swallowed/failed `siteDataService.find(id)`; missing behavioral tests; change-class closure (product-docs + Playwright companion).
**Cross-platform path checklist:** CMS finder `/` paths only (`requestedRelativeSitePath`, `folderPathLeaf`). No OS filesystem I/O.

## Summary

Cycle Verify residual of #3553: Cancel / item-click already unmounted the Subfolder Copy overlay on the cluster tip, but Playwright empty-console failed because Explorer `GET /path/item/{path}` returned HTTP 500 (`UnexpectedRollbackException`: transaction rollback-only).

Root cause: `PSSitePathItemService.findItem` called `siteDataService.find(siteId)` with the finder slug. Sample sites use SITENAME `Corporate_Investments` and FOLDER_ROOT `//Sites/CorporateInvestments`. `find("CorporateInvestments")` (and `find("Demo")` for a typed missing path) poisons the Spring TX; the REST mapper then reports 500 instead of the folder item or 404. Explorer `defaultResolveFolderId` hits that endpoint for the current folder after `tryEnterFolder`.

Fix: resolve the site via `findByPath` then `findAll` + `siteFolderNameMatches` / folder-root leaf. Never call `find(id)` on this path. Missing `/Sites/Demo/Home` is 404. Folder-root slug returns the site item with `PathItem.path` set to the requested relative path so `DispatchingPathService` validation passes.

## Recommendation

approve

## Gate

May commit/push: yes

## Issues

None (hard-gate).

Behavioral tests: `PSSitePathItemServiceFindItemSiteResolveTest` (folder-root without `find(id)`, sitename `findByPath`, missing Demo/Home 404, CMS path leaf). Product-docs browse + Subfolder Copy Next notes updated. C5: `explorer-subfolder-copy` 5 passed; Cycle Verify surface set 21 passed. No new `rollback-only` after hot-deploy.

> Co-Authored by Grok Build 1.0.5 using grok-4.6 with agent night-issue-prs.
