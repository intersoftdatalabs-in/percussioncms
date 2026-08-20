# Erlang review — #3655 Explorer move-folder product route

**Branch:** `fix/issue-3655-explorer-move-folder-product`  
**Date:** 2026-08-20  
**Scope:** uncommitted vs `HEAD` (issue #3655 / parent #3102)  
**Memory patterns hit:** change-class closure (WebUI + sitemanage path wrap + Playwright + product-docs); behavioral tests; no-skip H2 UI proof; distinct spec files vs sibling copy/rename/delete; cross-platform path checklist (CMS `/` paths, not OS file I/O)

## Summary

Default Explorer Move of a selected empty folder (`ReducedActions` `data-testid=action-move`, flag off) posted `MoveFolderItem` but finder dest paths without a leading slash (`Assets/qa…`) 400'd with "Path must start with '//'". `PSPathItemService.moveItem` now converts finder `/Assets` (and slash-less `Assets/…`) via `PSPathUtils.getFolderPath` instead of `getFullFolderPath` (which double-prefixed the Assets root). `wrapMoveFolderItem` prefixes a missing finder slash. The shell wraps `onMove` to open the destination and bump `listEpoch` + `folderTreeEpoch`. Playwright on `spa.jsp?entry=explorer` (no `rxFolderMutations=1`) must not skip when a Sites/Assets parent exists. Distinct spec/helper files from copy (#3647), rename (#3645), and delete (#3646). Out of scope: rename/delete tree-refresh residuals (#3652/#3653), `rxFolderMutations=1`, item copy.

## Recommendation

approve

## Gate

**May commit/push: yes**

No bugs, missing behavioral tests, or non-portable path I/O in this diff.

## Issues

None.

## Cross-platform path checklist

- No new filesystem path joins
- URLs and CMS finder/repository paths use `/` — allowed
- Playwright helpers use `URL` searchParams, not OS separators
- Java `toMoveRepositoryPath` normalizes `\` then `/` before `getFolderPath`
- No Unix-only roots or Windows-only drive letters

## Companions (change class: WebUI product screen + pathmanagement move)

| Companion | Status |
|-----------|--------|
| Shell wrap (`onMove` → dest-open + `handleRefreshListAndTree`) | done |
| `PSPathItemService.moveItem` `getFolderPath` + leading-slash | done |
| `wrapMoveFolderItem` leading slash | done |
| Vitest `ContentExplorerShell.move.test.tsx` + wrap tests | done |
| `PSPathItemServiceMoveTest` / `PSPathUtilsTest` | done |
| Playwright `explorer-move-folder.spec.js` no-skip | done |
| Helper unit tests | done |
| `product-docs/8.2/admin/content-explorer.md` Move cell | done |
| Did not re-implement rename/delete tree-refresh (#3652/#3653) | done |

## Test evidence

- `WebUI` `../mvnw.cmd clean install` — BUILD SUCCESS (Vitest 2977 passed)
- `projects/sitemanage` `../../mvnw.cmd clean install` — BUILD SUCCESS (Tests run: 1352, Failures: 0)
- `modules/perc-qa-automation` `../../mvnw.cmd clean install` — BUILD SUCCESS
- `npm run test:unit` (frontend) — 424 passed including explorer-move-folder helpers
- C5: `perc-devctl qa-up` TEST_CMS_URL=http://127.0.0.1:9993; hot-copy WebUI `cm/modern/assets` + `sitemanage-8.2.0-SNAPSHOT.jar`; in-cell StopJetty/StartJetty; `qa-health` RESULT:OK; Playwright `explorer-move-folder.spec.js` **2 passed 0 skipped**; golden **2 passed**; console-clean=yes; server.log-clean=yes
