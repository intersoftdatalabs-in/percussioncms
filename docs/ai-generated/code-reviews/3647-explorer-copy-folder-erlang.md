# Erlang review — #3647 Explorer copy-folder product route

**Branch:** `fix/issue-3647-explorer-copy-folder`  
**Date:** 2026-08-20  
**Scope:** uncommitted vs `HEAD` (issue #3647 / parent #3102)  
**Memory patterns hit:** change-class closure (WebUI + Playwright + product-docs); behavioral tests; no-skip H2 UI proof; distinct spec files vs sibling #3645/#3646; cross-platform path checklist (N/A — no filesystem I/O)

## Summary

Default Explorer Copy of a selected folder (`ReducedActions` / public REST `POST /rest/folders/copy/folder`, flag off) posted `CopyFolderItemRequest` but HTTP 500'd with "Path must start with '//'" because `FolderAdaptor.copyFolder` ran `getFolderPath` on the source only. Target finder paths (`/Assets/…`) and single-slash `/Folders/…` then failed `folderHelper.findFolder`. The adaptor now normalizes both sides. The shell wraps `onCopy` to open the destination and bump `listEpoch`. Playwright on `spa.jsp?entry=explorer` (no `rxFolderMutations=1`) must not skip when a Sites/Assets parent exists. Distinct spec/helper files from rename (#3645) and delete (#3646). Gap-matrix Sites/folders tree stays **Partial**. Out of scope: Subfolder Copy wizard (#2792), clipboard paste (#2408).

## Recommendation

approve

## Gate

**May commit/push: yes**

No bugs, missing behavioral tests, or non-portable path I/O in this diff.

## Issues

None.

## Cross-platform path checklist

- No new filesystem path joins
- URLs use `/` (REST `/folders/copy/folder` / SPA query) — allowed
- Playwright helpers use `URL` searchParams, not OS separators
- No Unix-only roots or Windows-only drive letters

## Companions (change class: WebUI product screen)

| Companion | Status |
|-----------|--------|
| Shell wrap (`onCopy` → dest + listEpoch) | done |
| `FolderAdaptor.copyFolder` getFolderPath on target | done |
| Vitest `ContentExplorerShell.copy.test.tsx` | done |
| `PSPathUtilsTest.testGetFolderPathCopyFolderDest` | done |
| Playwright `explorer-copy-folder.spec.js` no-skip | done |
| Helper unit tests | done |
| `product-docs/8.2/admin/content-explorer.md` Copy note | done |
| gap-matrix stays Partial + #3647 citation | done |
| Did not edit `ReducedActions.tsx` / `ExplorerTree.tsx` (sibling thrash) | done |

## Test evidence

- `WebUI` `../mvnw.cmd clean install` — BUILD SUCCESS (Vitest 2931 passed; Java Tests run: 63, Failures: 0)
- `modules/perc-qa-automation` `../../mvnw.cmd clean install` — BUILD SUCCESS
- `projects/sitemanage` `../../mvnw.cmd clean install` — BUILD SUCCESS (Tests run: 1342, Failures: 0; PSPathUtilsTest 6 passed)
- `npm run test:unit` (frontend) — 389 passed including explorer-copy-folder helpers
- C5: `perc-devctl qa-up` TEST_CMS_URL=http://127.0.0.1:9993; hot-copy WebUI `cm/modern/assets` + `sitemanage-8.2.0-SNAPSHOT.jar`; in-cell StopJetty/StartJetty; `qa-health` RESULT:OK; Playwright `explorer-copy-folder.spec.js` **2 passed 0 skipped**; golden **2 passed**; console-clean=yes; server.log-clean=yes (passing run)
