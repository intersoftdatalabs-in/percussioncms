# Erlang review — #3352 Seed NavTree for sample sites

**Scope:** `fix/issue-3352-seed-navtree-sample-sites` vs `origin/main`  
**Recommendation:** approve  
**Gate:** May commit/push: yes  
**Memory patterns hit:** empty list vs error envelope; sample-site seed is folders-only; create-on-first-open vs installer XML

## Summary

`installSampleSites` seeds `RXSITES` + site root folders (350/351) and Pages/Files children. It does **not** insert percNavTree / rffNavTree items. Slice 2 (#3218) mapped that to HTTP 200 empty; human QA #3155 Failed because Navigation rendered 0 treeitems.

Fix: `PSSiteSectionService.loadTree` / `loadRoot` create a percNavTree on first open when the site has a folder root and no nav child. Sample-site Default Workflow NPEs in `sys_wfPerformTransition` on check-in, so create **saves without check-in**, attaches to the folder, then best-effort checkin. Empty 200 remains when the folder is missing or create fails. SPA treats an id-bearing root (including Jackson array ids) as `role="tree"`.

## Issues

None (bugs / missing behavioral tests / non-portable I/O).

## Cross-platform path checklist

- CMS folder roots are repository paths (`//Sites/…`), not OS filesystem joins.
- No new `"/" +` filesystem construction.
- Playwright / Vitest helpers compare site names and JSON, not OS path strings.

## Tests

- sitemanage: create-on-first-open, concurrent re-find, workflow fallback, empty-when-create-fails, no-folder-root, REST pass-through
- WebUI Vitest: root-only `role=tree` + `loadSectionTree` keeps id
- perc-qa-automation unit: sample demo names; root-only payload is not empty
- Playwright live spec: demo sites must have a NavTree root and ≥1 treeitem

## C5

H2 `qa-up` + surface `architecture-nav-tree-live.spec.js` (and empty mocked spec) after hot-deploy of the new sitemanage SNAPSHOT.
