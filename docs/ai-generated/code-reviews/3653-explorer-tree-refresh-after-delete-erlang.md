# Erlang review — #3653 / #3652 Explorer tree refresh after Delete/Rename

**Branch:** `fix/issue-3653-explorer-tree-parent-epoch`  
**Base:** `origin/main` (`441025a626`, cluster #3651)  
**Date:** 2026-08-20  
**Reviewer:** Erlang (independent of implementer)

## Summary

After #3651, product-route Delete/Rename still left `[data-testid=explorer-tree]` stale. `folderTreeEpoch` bumped, but `ExplorerTree` reloaded only `selectedPath` + `initialPath`. Product Explorer seeds `initialPath="/"`. Selected list path is often a repository `folderPath` (`/Folders/$System$/Assets`) while the visible tree node is keyed by finder `path` (`/Assets`). Force-reload therefore created an unused node and left expanded parent children stale.

The epoch collector now matches loaded children by `path` **or** `folderPath`, always includes the tree parent of `selectedPath`, and force-reloads using the original `PSPathItem` so `findChildren` hits `folderPath`. Expanded siblings stay cached (#3645). `onDelete` walks `folderPath` to the parent when the deleted folder is the open folder.

Playwright H2 QA: delete 2 passed 0 skipped; rename 2 passed 0 skipped; golden 2 passed. Gap-matrix not flipped to Present. Product-docs already describe list+tree refresh without View → Refresh.

Memory patterns hit: missing behavioral unit tests (now present for parent reload + repository `folderPath`); Playwright no-skip when H2 demo parent exists; WebUI screen companion; incomplete change-class closure (Vitest + existing Playwright specs); no Unix-only filesystem joins (CMS `/` paths only).

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None (hard-gate).

Cross-platform path checklist: N/A for filesystem I/O. Tree/list keys are CMS URL-style paths (`/`). `normalizeExplorerTreePathKey` collapses repository `//` and trailing slashes; it does not join OS file paths.

## Tests / evidence

- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS. Vitest 2946 passed (387 files), including `ExplorerTree.test.tsx` (19) and `ContentExplorerShell.delete.test.tsx` (2).
- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` — BUILD SUCCESS. Tests run: 0 (Playwright is npm).
- C5: `python docker/scripts/perc-devctl.py qa-up --skip-image-build` TEST_CMS_URL=`http://127.0.0.1:9993` CONTAINER=`perc-matrix-cms-h2`; `qa-health` RESULT:OK HEALTH:healthy; hot-copy `WebUI/target/generated-webui/cm/modern/assets` → WAR `cm/modern/assets`; Jetty Stop/Start inside cell; `qa-health` again RESULT:OK HEALTH:healthy.
- `npm run test:surface -- --path tests/explorer-delete-folder.spec.js` — 2 passed, 0 skipped. console-clean=yes.
- `npm run test:surface -- --path tests/explorer-rename-folder.spec.js` — 2 passed, 0 skipped. console-clean=yes.
- `npm run test:golden` — 2 passed.
- server.log-clean=yes (no ERROR/FATAL after Jetty restart).

downstream_checked: none (TypeScript-only; no Java `final`/`sealed` or public API signature change).
