# Erlang review — #3652 Explorer-tree refresh after product-route Rename

**Branch:** `fix/issue-3652-explorer-tree-refresh-after-rename`  
**Base:** `origin/main`  
**Date:** 2026-08-20  
**Reviewer:** Erlang (independent of implementer)

## Summary

After pathmanagement `renameFolder` HTTP 200, the detail list showed the new
name but `[data-testid=explorer-tree]` stayed on the old child. `childrenEpoch`
reloaded `selectedPath` as a tree node key, but the product shell stores the
**list** path (`/Folders/$System$/Assets`) while the tree node is the finder
id (`/Assets`). The epoch miss left the renamed (and deleted) child stuck
under the old key.

This change canonicalizes tree keys (`//` + trailing slash), matches epoch
reloads against stored `listPath` and parent children, prunes stale child
keys, and keeps the reloaded parent expanded. Playwright companions already
exist; headers now cite #3652 / #3653. Product docs document Rename tree
refresh. Same-file fix also covers Cycle Verify #3653 (delete).

Memory patterns hit: incomplete change-class would be Vitest-only without
Playwright (companion present); URL `/` paths not OS joins; no Java API
shape change.

## Recommendation

approve

## Gate

May commit/push: **yes**

## Issues

None (hard-gate).

Cross-platform path checklist: CMS explorer paths are URL-style `/` (not
filesystem). `normalizeExplorerTreePathKey` collapses repository `//` and
trailing slashes; `\` is normalized only as a CMS path character, not used
to join OS files. Tests assert CMS path strings, not `Path.toString()`.

## Tests / evidence

- Vitest `ExplorerTree.test.tsx`: list-path vs tree-key rename epoch; old
  child path reloads parent and renders new name; delete epoch drops child.
- Existing shell rename/delete tests still pass.
- `cd WebUI && ../mvnw.cmd clean install` — BUILD SUCCESS. Vitest Tests
  2941 passed (387 files).
- `cd modules/perc-qa-automation && ../../mvnw.cmd clean install` — BUILD
  SUCCESS. Tests run: 0 (Playwright is npm).
- C5: `qa-up --skip-image-build` TEST_CMS_URL=`http://127.0.0.1:9993`;
  hot-copy `WebUI/target/generated-webui/cm/modern` into
  `perc-matrix-cms-h2` WAR; Jetty Stop/Start inside cell; `qa-health`
  RESULT:OK HEALTH:healthy.
- `npm run test:surface -- --path tests/explorer-rename-folder.spec.js` —
  2 passed, 0 skipped. console-clean=yes.
- `npm run test:surface -- --path tests/explorer-delete-folder.spec.js` —
  2 passed, 0 skipped.
- `npm run test:golden` — 2 passed.
- server.log ERROR/FATAL during test window: none (server.log-clean=yes).

downstream_checked: none (no Java `final`/`sealed` or public API signature
change).

## Change-class closure

WebUI product Explorer tree + existing Playwright specs + product-docs
Reduced actions Rename sentence. No rest/sitemanage API change. No new
Playwright file (peer specs updated in-place; do not re-wrap
RenameFolderItem).
